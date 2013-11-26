import models.Hero
import play.api.GlobalSettings
import play.api.Application

/*
 * User: Martin
 * Date: 13.11.13
 * Time: 12:09
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
    //lazy val database = Database.forDataSource(DB.getDataSource())
    Hero.persistToDb()

  }

}
