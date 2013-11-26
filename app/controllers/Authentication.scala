package controllers

import play.api.mvc._
import models._

import org.openid4java._
import consumer.ConsumerManager
import message.ParameterList
import message.ax._
import discovery.Identifier

import play.api.Logger.logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.{Failure, Success}

/*
 * User: Martin
 * Date: 13.11.13
 * Time: 21:08
 */
object Authentication extends Controller {
  val SteamOpenID = "https://steamcommunity.com/openid"
  val manager = new ConsumerManager()
  manager.setMaxAssocAttempts(0)
  val discovered = manager.associate(manager.discover(SteamOpenID))

  def login = Action {
    implicit request =>
      val authReq = manager.authenticate(discovered, routes.Authentication.openIDCallback().absoluteURL(false))
      val params = FetchRequest.createFetchRequest()
      params.addAttribute("email", "http://axschema.org/contact/email", true)
      params.addAttribute("namePerson", "http://axschema.org/namePerson", true)
      authReq.addExtension(params)
      Redirect(authReq.getDestinationUrl(true))
  }

  def openIDCallback() = Action {
    request =>
      logger.info("authenticated")
      try {
        // extract the parameters from the authentication response
        // (which comes in as a HTTP request from the OpenID provider)
        val p = request.queryString
        //logger.info("parameters:" + p)

        def convert(src: Map[String, Seq[String]]): java.util.Map[String, Object] = {
          val result = new java.util.HashMap[String, Object]()
          for (a <- src.keys) {
            val extractedLocalValue = src(a).toArray
            val value = if (extractedLocalValue.length > 1) {
              extractedLocalValue
            } else {
              extractedLocalValue(0)
            }
            result.put(a, value)
            //logger.info(a + "=" + value)
          }
          result
        }
        val response = new ParameterList(convert(p))
        val receivingURL = "http://" + request.host + request.uri
        val verification = manager.verify(receivingURL.toString, response, discovered)
        // examine the verification result and extract the verified identifier
        val verified: Identifier = verification.getVerifiedId
        if (verified != null) {
          //val authSuccess: AuthSuccess = verification.getAuthResponse.asInstanceOf[AuthSuccess]
          val userUrl = verified.getIdentifier
          val steamId64 = userUrl.substring(36, userUrl.length).toLong
          val steamId32 = (steamId64 - 76561197960265728L).toInt
          logger.debug(s"id url: $userUrl, steamID64: $steamId64, steamId32: $steamId32")
          val user = User.create(steamId64, steamId32)
          val games = Game.getGamesFor(steamId32)
          games onComplete {
            case Success(result) =>
              user.addGames(result)
            case Failure(t) =>
              logger.error("Error: ", t)
          }

          Redirect(routes.Restricted.profile()).withSession(Security.username -> steamId32.toString)

        } else {
          Unauthorized("not valid: [" + verified + "]")
        }
      } catch {
        case e: OpenIDException =>
          e.printStackTrace()
          Unauthorized("error to the user: " + e)
      }
  }

  def logout = Action {
    implicit request =>
      session.get(Security.username).foreach {
        id =>
          User.findById(id.toLong).foreach {
            user =>
              logger.info("Logged out user!")
              user.loggedIn = false
          }
      }
      Redirect(routes.Application.index).withNewSession
  }
}
