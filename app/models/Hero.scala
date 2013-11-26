package models

import controllers.Application
import play.api.libs.json.JsValue
import play.api.libs.ws._
import slick.driver.H2Driver.simple._
import util.Util.zip3
import scala.util.{Success, Failure}
import play.api.Logger.logger
import play.api.db.slick.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import scala.slick.session.Session

case class Hero(id: Int, name: String, imageUrl: String) extends Ordered[Hero] {
  def compare(that: Hero): Int = this.name.compare(that.name)
}

case class Heroes() extends Table[Hero]("HEROES") {
  def idCol = column[Int]("HERO_ID")

  def nameCol = column[String]("HERO_NAME")

  def imageUrlCol = column[String]("IMAGE_URL")

  def * = idCol ~ nameCol ~ imageUrlCol <>(Hero.apply _, Hero.unapply _)

}

object Hero {
  private var _inDb = false
  val h = new Heroes

  private def parseJson(json: JsValue): Seq[Hero] = {
    val names = (json \\ "localized_name").map(_.as[String])
    val ids = (json \\ "id").map(_.as[Int])
    val imageUrls = (json \\ "name") map {
      jsVal =>
        val str = jsVal.as[String]
        "http://media.steampowered.com/apps/dota2/images/heroes/" + str.substring(14, str.length) + "_lg.png"
    }

    val heroes = zip3(ids toList, names toList, imageUrls toList).map {
      case (id: Int, name: String, url: String) => Hero(id, name, url)
    }.take(names.length - 1)
    heroes.sorted
  }

  def persistToDb() {
    if (!_inDb) {
      logger.info("Persisting heroes to database.")
      val url = "https://api.steampowered.com/IEconDOTA2_570/GetHeroes/v0001/?key=" + Application.SteamApiKey + "&language=en_us"
      val future = WS.url(url).get()

      future onComplete {
        case Success(result) =>
          logger.info(s"Received a result")
          val json = result.json
          val heroes = parseJson(json)
          DB.withSession {
            implicit session: Session =>
              for (hero <- heroes) {
                //logger.info(s"Adding ${hero.name} to database!")
                h.insert(hero)
              }
          }

        case Failure(t) =>
          logger.error("Error when fetching heroes: ", t)
      }
      _inDb = true

    }
    else {
      logger.info("Heroes already in database, doing nothing.")
    }
  }

  def getForId(id: Int): Option[Hero] = {
    val q = Query(h)
    DB.withSession {
      implicit session: Session =>
        q.filter(_.idCol == id).list().headOption
    }
  }

}
