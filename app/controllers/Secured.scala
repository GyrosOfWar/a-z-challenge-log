package controllers

import play.api.mvc._
import scala.concurrent.Future

/*
 * User: Martin
 * Date: 03.12.13
 * Time: 13:28
 */
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