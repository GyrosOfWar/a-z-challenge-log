package models.database

import models._
import play.api.db.slick.Config.driver.simple._

/*
 * User: Martin
 * Date: 29.11.13
 * Time: 18:41
 */
private[models] case class Users() extends Table[User]("USERS") {
  def id64 = column[Long]("U_ID64", O.PrimaryKey)

  def id32 = column[Int]("U_ID32")

  def games = GameToUser.gtu.filter(_.userId64 == id64).flatMap(_.gameFK)

  def friendlyNameCol = column[String]("U_NAME")

  def * = id64 ~ id32 ~ friendlyNameCol <>(User.apply _, User.unapply _)

  val byId = createFinderBy(_.id64)

}