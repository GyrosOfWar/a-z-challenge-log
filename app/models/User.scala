package models

import scala.concurrent.{Await, Future}
import play.api.db.slick.Config.driver.simple._
import controllers._
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import concurrent.duration._

/*
 * User: martin
 * Date: 10/8/13
 * Time: 2:43 PM
 */
//case class User(username: String, passwordHash: HashSaltPair, email: String, matches: List[Game], id: Option[Long] = None)
case class User(steamId64: Long, steamId32: Int, friendlyName: String) extends Table[User]("USERS") {
  private var _games = Vector.empty[Game]

  var loggedIn = false

  def id64 = column[Long]("U_ID64", O.PrimaryKey)

  def id32 = column[Int]("U_ID32")

  //def games = foreignKey("MATCH_ID", id, Game)(g: Game => g.)

  def games = _games

  def friendlyNameCol = column[String]("U_NAME")

  def * = id64 ~ id32 ~ friendlyNameCol <>(User.apply _, User.unapply _)

  def addGame(g: Game) {
    _games = _games :+ g
  }

  def addGames(gs: Seq[Game]) {
    _games = _games ++ gs
  }
}

// TODO foreign key in Games
object User {
  private var userList = List.empty[User]

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
    // I need to block here because otherwise findById won't succeed
    val result = Await.result(friendlyName, 15 seconds)
    val user = User(steamId64, steamId32, result)
    userList = user :: userList
    user.loggedIn = true
    user
  }

  def findById(steamId32: Long): Option[User] = {
    userList.find(_.steamId32 == steamId32)
  }

  def isLoggedIn(steamId32: Long): Boolean = {
    findById(steamId32) match {
      case Some(user) if user.loggedIn => true
      case _ => false
    }
  }
}