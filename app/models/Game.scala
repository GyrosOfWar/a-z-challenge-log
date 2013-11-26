package models

import scala.concurrent.Future
import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import controllers._
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import util.Profiling.timedCall

import util.Util.zip3

/**
 * User: Martin Tomasi
 * Date: 21.11.13
 * Time: 15:11
 */

case class Game(matchId: Long, date: DateTime, hero: Hero, details: MatchDetails, win: Boolean)

case class Games() extends Table[(Long, Int, Long)]("GAMES") {
  def matchIdCol = column[Long]("MATCH_ID")

  def dateCol = column[Long]("DATE")

  def heroIdCol = column[Int]("HERO_ID")

  def * = matchIdCol ~ heroIdCol ~ dateCol
}

case class MatchDetails(radiantWon: Boolean, kills: Int, deaths: Int, assists: Int, gpm: Int, xpm: Int)

object Game {

  def getMatchDetails(matchId: Long, steamId32: Int): Future[MatchDetails] = {
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
            val hero = Hero.getForId(heroId).getOrElse(throw new IllegalArgumentException("Bad hero!"))
            // Get the match details from the API
            val details = getMatchDetails(matchId, myId)

            details map {
              d =>
                val playedRadiant = if (slot < 100) true else false
                val didIWin = {
                  if (playedRadiant && d.radiantWon) true
                  else if (!playedRadiant && !d.radiantWon) true
                  else false
                }
                Game(matchId, new DateTime(startTime * 1000l), hero, d, didIWin)
            }
          }
          // Turns a Seq[Future[Game]] into a Future[Seq[Game]]
          Future.sequence(games)
        }
    }
  }

}