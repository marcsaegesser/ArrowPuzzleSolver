package org.saegesser.test

import org.saegesser.puzzle._

/** A simple main method useful for gathering profiling data.
  */
object PuzzleSolver {
  def main(args: Array[String]): Unit = {
    import TestUtil._
    val msg = timeSolutions(boardStream.take(100).toList)(ArrowPuzzleSolver.simpleSolver(_)(SymmetryBuilder))
    println(msg)
  }
}
