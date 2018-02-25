package org.saegesser.test

import org.saegesser.puzzle._

object TestUtil {

  // This is the smaple board from the problem description.
  val testInput =
    """Y1,G0,R0,B1
      |Y0,B1,R1,B0
      |Y1,R1,G0,B0
      |Y1,G0,B0,G1
      |B0,G0,R1,Y1
      |Y0,G1,R1,B0
      |R0,B1,Y1,G0
      |Y0,R1,B1,G0
      |R0,G1,Y1,B0""".stripMargin

  // Tiles generated from the sample input. Useful for testing.
  val testTiles =
    Stream.from(1).zip(testInput.lines.toList)
      .map { case (i, l) =>
        Tile(i.toString, l.split(",").map(_.trim))
      }.toVector

  /** Given a sequence of boards and a solver time how long it takes
    * to solve all of the boards. Return a string with the results.
    */
  def timeSolutions(boards: List[Board])(solver: Board => List[Board]): String = {
    val start = System.currentTimeMillis
    boards foreach { solver }
    val end = System.currentTimeMillis
    val duration = end - start
    s"${boards.size} solved in ${duration}ms. Avg=${duration*1.0/boards.size}ms/puzzle"
  }

  import SolverProperties._
  /** An infinite stream of solvable boards using the shuffled board
    * generator from the property based tests.
    */
  val boardStream = Stream.continually(genShuffledBoard.sample).filter(_.isDefined).map(_.get)
}
