import org.specs2.mutable.Specification
import models.{MatchDetails, Hero, Game, User}
import play.api.test._
import play.api.test.Helpers._
import org.joda.time.DateTime

/*
 * User: Martin
 * Date: 30.11.13
 * Time: 20:07
 */
class UserSpec extends Specification {
  "A user" should {
    "be inserted into the database" in {
      running(FakeApplication()) {
        val user = User.create(76561197990455509L, 30189781)
        val fromDb = User.findById(30189781).get
        user shouldEqual fromDb
      }
    }

    "have games added to him" in {
      running(FakeApplication()) {
        val user = User.create(76561197990455509L, 30189781)
        val game = new Game(404166021, DateTime.now, Hero.findById(48).get, MatchDetails(win = true, 9, 2, 15, 784, 666))
        User.addGame(user, game)
        val games = User.findGames(user)
        val g = Game.findById(404166021)
        println("game g: " + g)
        games must contain(game)
      }
    }
  }

}
