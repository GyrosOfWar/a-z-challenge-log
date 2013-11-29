package models

import controllers.Application
import play.api.db.slick.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, Writes, JsValue}
import play.api.libs.ws._
import play.api.Logger.logger
import play.api.Play.current
import scala.util.{Success, Failure}
import slick.driver.H2Driver.simple._
import util.Util.zip3
import models.database.Heroes

case class Hero(id: Int, name: String, imageUrl: String) extends Ordered[Hero] {
  def compare(that: Hero): Int = this.name.compare(that.name)
}

object Hero {
  private var _inDb = false
  val h = new Heroes

  implicit val writesHero = new Writes[Hero] {
    override def writes(h: Hero): JsValue = {
      Json.obj(
        "id" -> h.id,
        "name" -> h.name,
        "imageUrl" -> h.imageUrl
      )
    }
  }

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

  def findById(id: Int): Option[Hero] = {
    DB.withSession {
      implicit session: Session =>
        h.byId(id).firstOption
    }
  }

}
