package models

import scala.concurrent.Future
import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import controllers._
import play.api.libs.ws.WS
import play.api.libs.json.JsObject
import util.Profiling.timedCall
import util.Util.zip3
import play.api.db.slick.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import scala.slick.session.Session

/**
 * User: Martin Tomasi
 * Date: 21.11.13
 * Time: 15:11
 */

class Game(val matchId: Long, val date: DateTime, val hero: Hero, val details: MatchDetails)

case class Games() extends Table[Game]("GAMES") {
  def matchId = column[Long]("MATCH_ID")

  def date = column[Long]("DATE")

  def heroId = column[Int]("HERO_ID")

  def kills = column[Int]("KILLS")

  def deaths = column[Int]("DEATHS")

  def assists = column[Int]("ASSISTS")

  def gpm = column[Int]("GPM")

  def xpm = column[Int]("XPM")

  def win = column[Boolean]("WIN")

  def * = matchId ~ date ~ heroId ~ kills ~ deaths ~ assists ~ gpm ~ xpm ~ win <>(Game.apply _, Game.unapply _)

  def byId = createFinderBy(_.matchId)
}

case class MatchDetails(win: Boolean, kills: Int, deaths: Int, assists: Int, gpm: Int, xpm: Int)

object Game {
  val g = new Games
  /*
    Used to create a Game object from what's saved in the database
   */
  def apply(matchId: Long, date: Long, heroId: Int, kills: Int,
            deaths: Int, assists: Int, gpm: Int, xpm: Int, win: Boolean): Game = {
    new Game(
      matchId,
      new DateTime(date * 1000L),
      Hero.findById(heroId).getOrElse(throw new IllegalArgumentException("Bad hero!")),
      MatchDetails(win, kills, deaths, assists, gpm, xpm)
    )
  }

  /*
    Used to make a Games object to save in the database from a Game object that I get from
    parsing JSON from the Steam API
   */

  def unapply(g: Game): Option[(Long, Long, Int, Int, Int, Int, Int, Int, Boolean)] = {
    // TODO test time conversion
    Some(g.matchId, g.date.toDate.getTime / 1000L, g.hero.id, g.details.kills,
      g.details.deaths, g.details.assists, g.details.gpm, g.details.xpm, g.details.win)
  }

  def getMatchDetails(matchId: Long, steamId32: Int, playerSlot: Int): Future[MatchDetails] = {
    val url = s"https://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/V001/?key=${Application.SteamApiKey}&match_id=$matchId"
    // TODO Fetch match details to find out the winner of the game
    val request = WS.url(url)
    val future = request.get()

    future map {
      r =>
        val json = r.json \ "result"
        val radiantWon = (json \ "radiant_win").as[Boolean]
        val players = (json \ "players").as[List[JsObject]]
        val player =
          (for {p <- players if (p \ "account_id").as[Int] == steamId32}
          yield p).head
        val kills = (player \ "kills").as[Int]
        val deaths = (player \ "deaths").as[Int]
        val assists = (player \ "assists").as[Int]
        val gpm = (player \ "gold_per_min").as[Int]
        val xpm = (player \ "xp_per_min").as[Int]
        MatchDetails(radiantWon, kills, deaths, assists, gpm, xpm)
    }
  }

  // TODO add parameters for start/end date or start/end match id and number of games to fetch
  def getGamesFor(steamId32: Int): Future[Seq[Game]] = {
    val url = s"https://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/V001/?key=${Application.SteamApiKey}&account_id=$steamId32"
    val request = WS.url(url)
    val future = request.get()
    val myId = steamId32

    future flatMap {
      v =>
        timedCall("getGamesFor") {
          val json = v.json \ "result"
          val matches = (json \ "matches").as[List[JsObject]]
          val matchIds = matches.map {
            obj => (obj \ "match_id").as[Int]
          }
          val startTimes = matches.map {
            obj => (obj \ "start_time").as[Long]
          }
          val players = matches.flatMap {
            obj => (obj \ "players").as[List[JsObject]]
          }
          val accountIds = players.map {
            p => (p \ "account_id").as[Long]
          }
          val heroIds = players.map {
            p => (p \ "hero_id").as[Short]
          }
          val playerSlots = players.map {
            p => (p \ "player_slot").as[Short]
          }

          val matchTuples = matchIds.zip(startTimes)
          val playerTuples = zip3(accountIds.toList, heroIds.toList, playerSlots.toList)
          // build Game objects from the JSON data
          val games = for (i <- 0 until matchTuples.length) yield {
            val (matchId, startTime) = matchTuples(i)
            val start = i * 10
            val end = start + 10
            val ps = playerTuples.slice(start, end)
            // find the hero ID and player slot for the given player
            val (heroId, slot) = ps.collect {
              case (accId: Long, hId: Short, playerSlot: Short) if accId == myId => (hId, playerSlot)
            }.head
            val hero = Hero.findById(heroId).getOrElse(throw new IllegalArgumentException("Bad hero!"))
            // Get the match details from the API
            val details = getMatchDetails(matchId, myId, slot)

            details map {
              d => new Game(matchId, new DateTime(startTime * 1000L), hero, d)
            }
          }
          // Turns a Seq[Future[Game]] into a Future[Seq[Game]]
          Future.sequence(games)
        }
    }
  }

  def findById(matchId: Long) = {
    DB.withSession {
      implicit session: Session =>
        g.byId(matchId).firstOption
    }
  }

}