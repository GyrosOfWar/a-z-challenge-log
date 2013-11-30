package models

import scala.concurrent.{Await, Future}
import play.api.db.slick.Config.driver.simple._
import controllers._
import play.api.libs.ws.WS
import concurrent.duration._
import play.api.db.slick.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import scala.slick.session.Session
import util.Util
import models.database._
import play.api.Logger.logger

/*
 * User: martin
 * Date: 10/8/13
 * Time: 2:43 PM
 */

case class User(steamId64: Long, steamId32: Int, friendlyName: String) {
  var loggedIn = false
}


// TODO foreign key in Games
object User {
  val u = new Users

  private def querySteamApi(steamId: String): Future[String] = {
    val url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + Application.SteamApiKey + "&steamids=" + steamId
    val holder = WS.url(url)
    val requestFuture = holder.get()
    val personaName = requestFuture.map {
      f => (f.json \\ "personaname").map(_.as[String]).head
    }

    personaName
  }

  def create(steamId64: Long, steamId32: Int): User = {
    val friendlyName = querySteamApi(steamId64.toString)
    // I need to block for the result of the future
    // here because otherwise findById won't succeed
    // There's probably a better solution for this though
    val result = Await.result(friendlyName, 15 seconds)
    val user = User(steamId64, steamId32, result)

    DB.withSession {
      implicit session: Session =>
        u.insert(user)
    }
    user.loggedIn = true
    user
  }

  def hasGames(user: User): Boolean = {
    DB.withSession {
      implicit session: Session =>
        val query = Query(User.u)
        query.flatMap(_.games).list.length > 0
    }

  }

  def findById(steamId32: Int): Option[User] = {
    DB.withSession {
      implicit session: Session =>
        val user = u.byId(Util.convertToSteamId64(steamId32)).firstOption
        logger.info(s"User.findById: User = ${user.toString}")
        logger.info(s"User.findById: user.loggedIn = ${user.getOrElse("Not a valid user!")}")
        user
    }
  }

  /* This works, but I don't know why. */
  def isLoggedIn(steamId32: Int): Boolean = {
    findById(steamId32) match {
      case Some(user) if user.loggedIn => true
      case _ => false
    }
  }

  def addGame(user: User, g: Game) {
    DB.withSession {
      implicit session: Session =>
        Game.g.insert(g)
        GameToUser.gtu.insert(g.matchId, user.steamId64)
    }
  }

  def addGames(user: User, gs: Seq[Game]) {
    DB.withSession {
      implicit session: Session =>
        for (g <- gs) {
          Game.g.insert(g)
          GameToUser.gtu.insert(g.matchId, user.steamId64)
        }
    }
  }

  def findGames(user: User): Seq[Game] = {
    DB.withSession {
      implicit session: Session =>
        val query = Query(User.u)
        query.flatMap(_.games).list
    }
  }
}