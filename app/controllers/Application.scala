package controllers

import play.api.mvc._

import views._

import play.api.Play
import play.api.libs.Files
import play.api.Logger.logger

object Application extends Controller {
  val SteamApiKey = loadApiKey()

  def index = Action {
    implicit request =>
      val isLoggedIn = session.get(Security.username).isDefined
      logger.info(session.get(Security.username).toString)
      val warning = flash.get("warning")
      val error = flash.get("error")
      val success = flash.get("success")
      Ok(html.index(isLoggedIn, warningMessage = warning, successMessage = success, errorMessage = error))
  }

  private def loadApiKey(): String = {
    import play.api.Play.current
    val fp = Play.getFile("conf/apiKey.txt")
    Files.readFile(fp)
  }
}