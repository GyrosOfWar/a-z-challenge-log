package util

/*
 * User: Martin
 * Date: 26.11.13
 * Time: 11:58
 */
object Util {
  /* Zips three lists into a list of 3-tuples. */
  def zip3(l1: List[_], l2: List[_], l3: List[_]): List[(_, _, _)] = {
    def zip3$(l1$: List[_], l2$: List[_], l3$: List[_], acc: List[(_, _, _)]): List[(_, _, _)] = l1$ match {
      case Nil => acc.reverse
      case l1$head :: l1$tail => zip3$(l1$tail, l2$.tail, l3$.tail, Tuple3(l1$head, l2$.head, l3$.head) :: acc)
    }
    zip3$(l1, l2, l3, List[(_, _, _)]())
  }

  def convertToSteamId32(steamId64: Long): Int = (steamId64 - 76561197960265728L).toInt

  def convertToSteamId64(steamId32: Int): Long = steamId32.toLong + 76561197960265728L


}
