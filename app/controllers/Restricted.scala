package controllers

import play.api.mvc._

import models._
import views._

/*
 * User: Martin
 * Date: 13.11.13
 * Time: 20:21
 */
object Restricted extends Controller with Secured {
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
  def gamesFor(heroId: Int) = IsAuthenticated {
    userId =>
      request =>
        Ok(s"test test test derp! $heroId")
  }

}

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user's email
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

  def withUser(f: User => Request[AnyContent] => Result) = IsAuthenticated { userId => implicit request =>
    User.findById(userId.toInt).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }
}