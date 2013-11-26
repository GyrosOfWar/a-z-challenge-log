package models

import collection.JavaConverters._
import concurrent.Await
import concurrent.duration._
import controllers.Application
import java.nio.charset.Charset
import java.nio.file.{Paths, Files}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws._
import slick.driver.H2Driver.simple._

import util.Util.zip3

case class Hero(id: Int, name: String, imageUrl: String) extends Table[Hero]("HEROES") with Ordered[Hero] {
  def idCol = column[Int]("HERO_ID")

  def nameCol = column[String]("HERO_NAME")

  def imageUrlCol = column[String]("IMAGE_URL")

  def * = idCol ~ nameCol ~ imageUrlCol <>(Hero.apply _, Hero.unapply _)

  def compare(that: Hero): Int = this.name.compare(that.name)
}

object Hero {
  private var allHeroes: Seq[Hero] = Nil
  private var fetchedFromApi = false
  private var fetchedFromFile = false
  // If true, reads the heroes.json file from classpath
  private val EnableApiHeroes = true
  private val ClasspathFileName = "heroes.json"

  private def parseJson(json: JsValue): Seq[Hero] = {
    val names = (json \\ "localized_name").map(_.as[String])
    val ids = (json \\ "id").map(_.as[Int])
    val imageUrls = (json \\ "name") map {
      jsVal =>
        val str = jsVal.as[String]
        "http://media.steampowered.com/apps/dota2/images/heroes/" + str.substring(14, str.length) + "_lg.png"
    }

    // Drop the last hero (LC) because she's not released yet.
    //val fetchedHeroes = (for (i <- 0 until names.size) yield Hero(ids(i), names(i), imageUrls(i))).take(names.length - 1)
    val heroes = zip3(ids toList, names toList, imageUrls toList).map {
      case (id: Int, name: String, url: String) => Hero(id, name, url)
    }.take(names.length - 1)
    heroes.sorted
  }

  // TODO make asynchronous and not ugly
  // TODO make it save stuff to database
  def getAll: Seq[Hero] = {
    def fetchFromUrl(): JsValue = {
      val url = "https://api.steampowered.com/IEconDOTA2_570/GetHeroes/v0001/?key=" + Application.SteamApiKey + "&language=en_us"
      val future = WS.url(url).get()
      Await.result(future, 15 seconds).json
    }

    def fetchFromClasspath(): JsValue = {
      val path = getClass.getClassLoader.getResource(ClasspathFileName).toURI
      val lines = Files.readAllLines(Paths.get(path), Charset.defaultCharset()).asScala mkString "\n"
      Json.parse(lines)
    }

    if (EnableApiHeroes) {
      if (fetchedFromApi) {
        this.allHeroes
      }
      else {
        val heroes = parseJson(fetchFromUrl())
        this.allHeroes = heroes
        fetchedFromApi = true
        heroes
      }
    }
    else {
      if (fetchedFromFile) {
        this.allHeroes
      }
      else {
        val heroes = parseJson(fetchFromClasspath())
        this.allHeroes = heroes
        fetchedFromFile = true
        heroes
      }
    }

  }

  def getForId(id: Int): Option[Hero] = {
    if(allHeroes == Nil) getAll
    allHeroes.find(_.id == id)
  }

}
