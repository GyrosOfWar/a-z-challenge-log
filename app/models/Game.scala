package models

import controllers._
import org.joda.time.DateTime
import play.api.db.slick.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, Writes, JsObject}
import play.api.libs.ws.{Response, WS}
import play.api.Play.current
import scala.concurrent.Future
import scala.slick.session.Session
import models.database.Games
import util.Util.zip3
import play.api.Logger.logger

/**
 * User: Martin Tomasi
 * Date: 21.11.13
 * Time: 15:11
 */

class Game(val matchId: Long, val date: DateTime, val hero: Hero, val details: MatchDetails) {
  override def toString = s"matchId: $matchId $date $hero"
}

case class MatchDetails(win: Boolean, kills: Int, deaths: Int, assists: Int, gpm: Int, xpm: Int)

object MatchDetails {
  implicit val writesMatchDetails = new Writes[MatchDetails] {
    override def writes(m: MatchDetails) = {
      Json.obj(
        "win" -> m.win,
        "kills" -> m.kills,
        "deaths" -> m.deaths,
        "assists" -> m.assists,
        "gpm" -> m.gpm,
        "xpm" -> m.xpm
      )
    }
  }
}

object Game {
  val g = new Games
  private var _cache = Map.empty[Int, Vector[Game]]

  implicit val writesGame = new Writes[Game] {
    override def writes(g: Game) = {
      Json.obj(
        "match_id" -> g.matchId,
        "date" -> g.date.toDate.getTime / 1000L,
        "hero" -> g.hero,
        "details" -> g.details
      )
    }
  }

  def cache = _cache

  /* Used to create a Game object from what's saved in the database
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

  /* Used to make a Games object to save in the database from a Game object that I get from
     parsing JSON from the Steam API
   */

  def unapply(g: Game): Option[(Long, Long, Int, Int, Int, Int, Int, Int, Boolean)] = {
    // TODO test time conversion
    Some(g.matchId, g.date.toDate.getTime / 1000L, g.hero.id, g.details.kills,
      g.details.deaths, g.details.assists, g.details.gpm, g.details.xpm, g.details.win)
  }

  private def getMatchDetails(matchId: Long, steamId32: Int, playerSlot: Int): Future[MatchDetails] = {
    val url = s"https://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/V001/?key=${Application.SteamApiKey}&match_id=$matchId"
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
        val playedRadiant = playerSlot < 100
        val didIWin = {
          if (radiantWon && playedRadiant) true
          else if (!radiantWon && !playedRadiant) true
          else false
        }

        MatchDetails(didIWin, kills, deaths, assists, gpm, xpm)
    }
  }

  private def checkCache(steamId32: Int, heroId: Option[Int] = None): Option[Seq[Game]] = {
    _cache.get(steamId32) flatMap {
      games => heroId.map {
        id => games.filter(_.hero.id == id)
      }
    }
  }

  // TODO add parameters for start/end date or start/end match id and number of games to fetch
  // TODO cache results
  def getGamesFor(steamId32: Int, heroId: Option[Int] = None): Future[Seq[Game]] = {
    val cached = checkCache(steamId32, heroId)
    cached match {
      case Some(games) => Future(games)
      case None =>
    }

    val hero = heroId.map(id => "&hero_id=" + id).getOrElse("")
    logger.info(s"Getting matches for hero $heroId")
    val url = s"https://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/V001/?key=${Application.SteamApiKey}&account_id=$steamId32&game_mode=1" + hero
    val request = WS.url(url)
    val future: Future[Response] = request.get()
    val myId = steamId32

    future.onFailure {
      case e: Exception => logger.error("Error reaching the Steam API.", e)
    }

    future flatMap {
      v =>
        if (v.status == 500) {
          logger.error("Internal server error in the Steam API response.")
        }

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
            d =>
              val game = new Game(matchId, new DateTime(startTime * 1000L), hero, d)
              val buf = _cache.get(myId).getOrElse(Vector.empty[Game]) :+ game
              _cache += steamId32 -> buf
              game
          }
        }
        // Turns a Seq[Future[Game]] into a Future[Seq[Game]]
        Future.sequence(games)
    }
  }

  def findById(matchId: Long) = {
    DB.withSession {
      implicit session: Session =>
        g.byId(matchId).firstOption
    }
  }

}