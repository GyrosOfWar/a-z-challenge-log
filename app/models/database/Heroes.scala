package models.database

import play.api.db.slick.Config.driver.simple._
import models._

/*
 * User: Martin
 * Date: 29.11.13
 * Time: 18:42
 */

private[models] case class Heroes() extends Table[Hero]("HEROES") {
  def id = column[Int]("HERO_ID")

  def name = column[String]("HERO_NAME")

  def imageUrl = column[String]("IMAGE_URL")

  def * = id ~ name ~ imageUrl <>(Hero.apply _, Hero.unapply _)

  val byId = createFinderBy(_.id)

}