package org.saegesser.puzzle

import scala.io.Source

/** The main program for the puzzle solver.
  *
  * This reads the input file specified on the command line, invokes
  * the solver using the most efficient algorithm and then outputs the
  * results as specified in the problem statement.
  *
  * See the README.me file for more details.
  */
object PuzzleSolver {
  def main(args: Array[String]): Unit = {
    if(args.size == 1) {
      val input = Source.fromFile(args(0))
      val tiles =
        Stream.from(1).zip(input.getLines.toVector)
          .map { case (i, l) =>
            Tile(i.toString, l.split(",").map(_.trim))
          }.toVector
      val board = new Board(tiles)
      val solns = ArrowPuzzleSolver.simpleSolver(board)(SymmetryBuilder2)
      val numSolns = solns.size

      val message =
        numSolns match {
          case 0 => "No solution found."
          case 1 => "1 unique solution found."
          case n => s"$n unique solutions found."
        }
      println("Input tiles:")
      board.tiles.foreach(t => println(t.show))
      println(s"\n$message")
      solns.foreach(b => println(b.show))
    } else {
      println("ERROR:  No input file provided.")
    }
  }
}
