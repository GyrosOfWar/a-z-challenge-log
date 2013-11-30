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

  def gamesFor(heroId: Int) = IsAuthenticatedAsync {
    userId =>
      request =>
        val games = Game.getGamesFor(userId.toInt, Some(heroId))
        games map (g => Ok(Json.toJson(g)))
  }

  def allGames = IsAuthenticated {
    userId =>
      request =>
        val user = User.findById(userId.toInt).getOrElse(throw new IllegalArgumentException("Bad user!"))
        val games = User.findGames(user)
        Ok(Json.toJson(games))
  }

  def hasGames = IsAuthenticated {
    userId =>
      request =>
      // TODO make this a separate function
        val user = User.findById(userId.toInt).getOrElse(throw new IllegalArgumentException("bad hero!"))
        if (User.hasGames(user)) {
          Ok("true")
        }
        else {
          Ok("false")
        }
  }
}

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user's user ID.
   */
  private def userId(request: RequestHeader) = request.session.get(Security.username)

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
    Security.Authenticated(userId, onUnauthorized) {
      user => Action(request => f(user)(request))
    }
  }

  /**
   * Asynchronous action for authenticated users.
   * @param f Action
   * @return Action to perform
   */
  def IsAuthenticatedAsync(f: => String => Request[AnyContent] => Future[SimpleResult]) = {
    Security.Authenticated(userId, onUnauthorized) {
      user => Action.async(request => f(user)(request))
    }
  }
}