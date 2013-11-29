package models.database

import play.api.db.slick.Config.driver.simple._
import models._

private[models] case class GameToUser() extends Table[(Long, Long)]("GAME_TO_USER") {
  def matchId = column[Long]("GAME_ID")

  def userId64 = column[Long]("USER_ID64")

  def gameFK = foreignKey("GAME_FK", matchId, Game.g)(_.matchId)

  def userFK = foreignKey("USER_FK", userId64, User.u)(_.id64)

  def * = matchId ~ userId64
}

private[models] object GameToUser {
  val gtu = new GameToUser()
}