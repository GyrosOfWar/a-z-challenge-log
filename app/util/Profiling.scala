package util
import play.api.Logger.logger

/*
 * User: Martin
 * Date: 22.11.13
 * Time: 13:57
 */
object Profiling {
  private var times = Map.empty[String, Seq[Long]].withDefaultValue(List.empty[Long])

  def timedCall[A](functionName: String, printTime: Boolean = true)(block: => A): A = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()

    if (printTime)
      logger.info(s"$functionName: Time: ${BigDecimal(t1 - t0) / BigDecimal(1000000)} ms")
    times = times + (functionName -> (times(functionName) :+ (t1 - t0)))
    result
  }

  def printStats(functionName: String) {
    require(times(functionName).nonEmpty)

    val fnTimes = (for {
      (name, ts) <- times
      time <- ts
      if name == functionName
    } yield BigDecimal(time) / BigDecimal(1000000)).toSeq

    val median = fnTimes.sorted.apply(fnTimes.length / 2)
    logger.info(s"Showing profiling information for: $functionName")
    logger.info(s"Median time: $median ms")
    logger.info(s"Max/Min/Range: ${fnTimes.max} ms / ${fnTimes.min} ms / ${fnTimes.max - fnTimes.min} ms")
    logger.info(s"Number of samples: ${fnTimes.length}")
  }

}
