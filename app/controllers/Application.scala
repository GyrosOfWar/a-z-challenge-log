package controllers

import play.api.mvc._

import views._

import models._
import play.api.Play
import play.api.libs.Files

object Application extends Controller {
  val SteamApiKey = loadApiKey()

  def index = Action {
    implicit request =>
      // Make sure there is no junk stored in the username variable
      val userId = session.get(Security.username) match {
        case Some(id) if id(0).isDigit => id.toInt
        case _ => 0
      }
      val isLoggedIn = User.isLoggedIn(userId)
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