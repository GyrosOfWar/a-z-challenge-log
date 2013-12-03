package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import concurrent.Future

import models._
import views._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import play.api.Logger.logger

/*
 * User: Martin
 * Date: 13.11.13
 * Time: 20:21
 */
object Profile extends Controller with Secured {
  val form = Form(
    single("selected" -> number)
  )

  /**
   * Display restricted area only if user is logged in.
   */
  def profile = IsAuthenticated {
    userId =>
      request =>
        User.findById(userId.toInt).map {
          user =>
            Ok(html.profile(user, form))
        }.getOrElse(Redirect(routes.Application.index()).flashing("error" -> "You need to login first."))
  }

  def addGame = IsAuthenticated {
    userId =>
      request =>
        val user = User.findById(userId.toInt).getOrElse(throw new IllegalArgumentException("Bad user!"))
        form.fold(
          badForm => BadRequest(html.profile(user, badForm)),
          matchId => {
            val game = Game.findById(matchId).getOrElse(throw new IllegalArgumentException("Bad game!"))
            logger.info(game.toString)
            User.addGame(user, game)
            Ok(html.profile(user, form))
          }
        )
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

