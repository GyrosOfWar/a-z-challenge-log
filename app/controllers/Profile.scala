package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import concurrent.Future

import models._
import views._
import play.api.libs.json._

/*
 * User: Martin
 * Date: 13.11.13
 * Time: 20:21
 */
object Profile extends Controller with Secured {
  /**
   * Display restricted area only if user is logged in.
   */
  def profile = IsAuthenticated {
    userId =>
      request =>
        User.findById(userId.toInt).map {
          user =>
            Ok(html.profile(user))
        }.getOrElse(Redirect(routes.Application.index()).flashing("error" -> "You need to login first."))
  }

  // TODO generate JSON for heroes with given heroId and send it back
  def gamesFor(heroId: Int) = IsAuthenticatedAsync {
    userId =>
      request =>
        val games = Game.getGamesFor(userId.toInt, Some(heroId))
        games map (g => Ok(Json.toJson(g)))
  }
}

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user's user ID.
   */
  private def username(request: RequestHeader) = request.session.get(Security.username)

  /**
   * Not authorized, forward to login
   */
  private def onUnauthorized(request: RequestHeader) = {
    Results.Redirect(routes.Application.index).flashing("error" -> "You need to login first.")
  }

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) {
      user => Action(request => f(user)(request))
    }
  }

  def IsAuthenticatedAsync(f: => String => Request[AnyContent] => Future[SimpleResult]) = {
    Security.Authenticated(username, onUnauthorized) {
      user => Action.async(request => f(user)(request))
    }
  }
}