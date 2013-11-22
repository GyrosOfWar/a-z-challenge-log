import models.Hero
import org.specs2.mutable._

/**
 * User: Martin
 * Date: 09.10.13
 * Time: 01:52
 */
class HeroSpec extends Specification {
  "The Hero object" should {
    "return all the heroes" in {
      val result = Hero.getAll
      result.size must be > 100
      //result.head must have name "Abaddon"
    }
  }
}
