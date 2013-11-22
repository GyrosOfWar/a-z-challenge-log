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
case class Game(matchId: Long, date: DateTime, hero: Hero) extends Table[(Long, Int, Long)]("GAMES") {
  def matchIdCol = column[Long]("MATCH_ID")

  def dateCol = column[Long]("DATE")

  def heroIdCol = column[Int]("HERO_ID")

  // FIXME
  //def heroFK = foreignKey("HERO_FK", heroIdCol, Hero)(_.)

  def * = matchIdCol ~ heroIdCol ~ dateCol
}

object Game {
  def getGamesFor(steamId32: String): Future[Seq[Game]] = {
    val url = s"https://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/V001/?key=${Application.SteamApiKey}&account_id=$steamId32"
    val request = WS.url(url)
    val future = request.get()
    val myId = steamId32.toInt

    future map {
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

          for (i <- 0 until matchTuples.length) yield {
            val (matchId, startTime) = matchTuples(i)
            val start = i * 10
            val end = start + 10
            val ps = playerTuples.slice(start, end)
            val id = ps.collect {
              case (accId, heroId) if accId == myId => heroId
            }.head
            val hero = Hero.getForId(id).getOrElse(throw new IllegalArgumentException("Bad hero!"))
            Game(matchId, new DateTime(startTime), hero)
          }
        }
    }
  }

}