package controllers

import play.api.mvc._

import views._

import play.api.Play
import play.api.libs.Files


object Application extends Controller {
  val SteamApiKey = loadApiKey()

  def index = Action {
    implicit request =>
      val isLoggedIn = session.get(Security.username).isDefined
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