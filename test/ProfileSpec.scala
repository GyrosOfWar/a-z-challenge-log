import org.specs2.mutable.Specification
import play.api.test._
import play.api.test.Helpers._
import controllers._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/*
 * User: Martin
 * Date: 01.12.13
 * Time: 02:05
 */
class ProfileSpec extends Specification {
  "A user profile" should {
    "not be accessible from a not logged in user" in {
      running(FakeApplication()) {
        val future = route(FakeRequest(GET, "/profile")).get
        val location = Await.result(future, Duration.Inf).header.headers("Location")
        val flash = Await.result(future, Duration.Inf).header.headers("Set-Cookie")
        println(location)
        println(flash)

        "test" mustEqual "test "
      }
    }
  }
}

