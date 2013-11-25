package models

import scala.concurrent.Future
import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import controllers._
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import util.Profiling.timedCall

/**
 * User: werner
 * Date: 21.11.13
 * Time: 15:11
 */
case class Game(matchId: Long, date: DateTime, hero: Hero, details: MatchDetails) extends Table[(Long, Int, Long)]("GAMES") {
  def matchIdCol = column[Long]("MATCH_ID")

  def dateCol = column[Long]("DATE")

  def heroIdCol = column[Int]("HERO_ID")

  // FIXME
  //def heroFK = foreignKey("HERO_FK", heroIdCol, Hero)(_.)

  def * = matchIdCol ~ heroIdCol ~ dateCol
}

case class MatchDetails(radiantWon: Boolean, kills: Int, deaths: Int, assists: Int, gpm: Int, xpm: Int)

object Game {

  def matchDetails(matchId: Long, steamId32: Int): Future[MatchDetails] = {
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

          val matchTuples = matchIds.zip(startTimes)
          val playerTuples = accountIds.zip(heroIds)

          val ms = for (i <- 0 until matchTuples.length) yield {
            val (matchId, startTime) = matchTuples(i)
            val start = i * 10
            val end = start + 10
            val ps = playerTuples.slice(start, end)
            val id = ps.collect {
              case (accId, heroId) if accId == myId => heroId
            }.head
            val hero = Hero.getForId(id).getOrElse(throw new IllegalArgumentException("Bad hero!"))
            val details = matchDetails(matchId, myId)

            details map { d => Game(matchId, new DateTime(startTime * 1000l), hero, d) }
          }

          Future.sequence(ms) map { _.toSeq }
        }
    }
  }

}