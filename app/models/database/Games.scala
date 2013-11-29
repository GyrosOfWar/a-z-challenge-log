package models.database

import models._
import play.api.db.slick.Config.driver.simple._

private[models] case class Games() extends Table[Game]("GAMES") {
  def matchId = column[Long]("MATCH_ID", O.PrimaryKey)

  def date = column[Long]("DATE")

  def heroId = column[Int]("HERO_ID")

  def kills = column[Int]("KILLS")

  def deaths = column[Int]("DEATHS")

  def assists = column[Int]("ASSISTS")

  def gpm = column[Int]("GPM")

  def xpm = column[Int]("XPM")

  def win = column[Boolean]("WIN")

  def * = matchId ~ date ~ heroId ~ kills ~ deaths ~ assists ~ gpm ~ xpm ~ win <>(Game.apply _, Game.unapply _)

  def users = GameToUser.gtu.filter(_.matchId == matchId).flatMap(_.userFK)

  def byId = createFinderBy(_.matchId)
}
