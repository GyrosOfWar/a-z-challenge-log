import play.api.GlobalSettings
// Use H2Driver to connect to an H2 database

// Use the implicit threadLocalSession

import play.api.Application


/*
 * User: Martin
 * Date: 13.11.13
 * Time: 12:09
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
//    lazy val database = Database.forDataSource(DB.getDataSource())
//    database withSession {
//      User.ddl.create
//      val (hash, salt) = HashCreator.createHash("asdf", HashCreator.createSalt)
//      User.insert((Some(0l), "admin", hash ++ salt, "martin.tomasi@gmail.com"))
//    }
  }

}
