package org.saegesser.test

import scala.util.Random
import scala.concurrent._
import scala.concurrent.duration._
import org.scalacheck._
import com.typesafe.scalalogging.StrictLogging
import org.saegesser.puzzle._

/** Some simple property based tests for the puzzle solver.
  */
object SolverProperties extends Properties("ArrowPuzzleSolver") with StrictLogging {
  import ArrowPuzzle._

  import Gen._
  import Prop._

  def genColor: Gen[Color] = oneOf(Red, Green, Blue, Yellow)

  def genShape: Gen[Shape] = oneOf(0, 1)

  def genEdge: Gen[EdgeValue] =
    for {
      c <- genColor
      s <- genShape
    } yield EdgeValue(c, s)

  def matchingEdge(e: EdgeValue): EdgeValue =
    e.copy(shape = (e.shape+1) % 2)

  def edgesMatch(e1: EdgeValue, e2: EdgeValue): Boolean =
    e1.color == e2.color && e1.shape + e2.shape == 1

  def genEdgePair: Gen[(EdgeValue, EdgeValue)] =
    genEdge map { e => (e, matchingEdge(e)) }

  /** Create a solved board from the given edges and edge pairs.
    */
  def makeBoard(es: List[EdgeValue], eps: List[(EdgeValue, EdgeValue)]) = {
    val vecEdges = es.toVector
    val vecPairs = eps.toVector
    val t1 = FreeTile("1", Edges(vecEdges(0), vecPairs(0)._1, vecPairs(1)._1, vecEdges(1)))
    val t2 = FreeTile("2", Edges(vecEdges(2), vecPairs(1)._1, vecPairs(2)._1, vecPairs(0)._2))
    val t3 = FreeTile("3", Edges(vecEdges(3), vecEdges(4), vecPairs(4)._1, vecPairs(1)._2))

    val t4 = FreeTile("4", Edges(vecPairs(1)._2, vecPairs(5)._1, vecPairs(6)._1, vecEdges(5)))
    val t5 = FreeTile("5", Edges(vecPairs(2)._2, vecPairs(7)._1, vecPairs(8)._1, vecPairs(5)._2))
    val t6 = FreeTile("6", Edges(vecPairs(4)._2, vecEdges(6), vecPairs(9)._1, vecPairs(7)._2))

    val t7 = FreeTile("7", Edges(vecPairs(6)._2, vecPairs(10)._1, vecEdges(7), vecEdges(8)))
    val t8 = FreeTile("8", Edges(vecPairs(8)._2, vecPairs(11)._1, vecEdges(8), vecPairs(10)._2))
    val t9 = FreeTile("9", Edges(vecPairs(9)._2, vecEdges(10), vecEdges(11), vecPairs(11)._2))

    val tiles = Vector(t1, t2, t3, t4, t5, t6, t7, t8, t9)
    new Board(tiles)
  }

  /** Generator of solved boards.
    */
  def genSolvedBoard: Gen[Board] =
    for {
      es <- listOfN(12, genEdge)       // Edges that don't require a match
      eps <- listOfN(12, genEdgePair)  // Pairs of matched edges
    } yield makeBoard(es, eps)

  /** Generator of shuffled boards.
    *
    * First generates a solved board and then randomizes it.
    */
  val genShuffledBoard: Gen[Board] =
    genSolvedBoard map { shuffleBoard }

  /** Check if the given board is solved.
    */
  def isSolved(board: Board): Boolean = {
    val ts = board.tiles

    edgesMatch(ts(0).edges.r, ts(1).edges.l) &&
    edgesMatch(ts(1).edges.r, ts(2).edges.l) &&
    edgesMatch(ts(3).edges.t, ts(0).edges.b) &&
    edgesMatch(ts(4).edges.t, ts(1).edges.b) &&
    edgesMatch(ts(5).edges.t, ts(2).edges.b) &&
    edgesMatch(ts(3).edges.r, ts(4).edges.l) &&
    edgesMatch(ts(4).edges.r, ts(5).edges.l) &&
    edgesMatch(ts(6).edges.t, ts(3).edges.b) &&
    edgesMatch(ts(7).edges.t, ts(4).edges.b) &&
    edgesMatch(ts(8).edges.t, ts(5).edges.b) &&
    edgesMatch(ts(6).edges.r, ts(7).edges.l) &&
    edgesMatch(ts(7).edges.r, ts(8).edges.l)
  }

  /** Randomly select one of the four rotations of the given tile.
    */
  def randomRotateTile(t: Tile): Tile =
    Random.nextInt(4) match {
      case 0 => t
      case 1 => FreeTile(t.label, Edges(t.edges.r, t.edges.b, t.edges.l, t.edges.t))
      case 2 => FreeTile(t.label, Edges(t.edges.b, t.edges.l, t.edges.t, t.edges.r))
      case 3 => FreeTile(t.label, Edges(t.edges.l, t.edges.t, t.edges.r, t.edges.b))
    }

  /** Relabel a shuffled board so that the tiles are ordered 1 to 9
    * starting in the top left corner.
    */
  def reLabelTiles(tiles: Vector[Tile]): Vector[Tile] =
    Stream.from(1).zip(tiles).map { case (l, t) => FreeTile(l.toString, t.edges) }.toVector

  /** Shuffle the given board. This rearranges the tiles and randomly
    * rotates the tiles. The tiles are then relabeled starting from 1.
    *
    * This results are a board suitable for passing to the solver as
    * if it had just been read from an input file.
    */
  def shuffleBoard(board: Board): Board =
    new Board(reLabelTiles(Random.shuffle(board.tiles.map(randomRotateTile))))

  property("solves solveable board") = forAll(genSolvedBoard) { board =>
    val shuffled = shuffleBoard(board)
    val results = ArrowPuzzleSolver.simpleSolver(shuffled)(SymmetryBuilder)

    ("evidence" + shuffled.show + "->" + results.map(_.show)) |: all(
      "Size" |: results.size >= 1,
      "Solved" |: results.toList.map(isSolved).exists(_ == false) == false
    )
  }

  property("solves solveable board with SymmetryBuilder2") = forAll(genSolvedBoard) { board =>
    val shuffled = shuffleBoard(board)
    val results = ArrowPuzzleSolver.simpleSolver(shuffled)(SymmetryBuilder2)

    ("evidence" + shuffled.show + "->" + results.map(_.show)) |: all(
      "Size" |: results.size >= 1,
      "Solved" |: results.toList.map(isSolved).exists(_ == false) == false
    )
  }

  property("solves solveable board in parallel") = forAll(genSolvedBoard) { board =>
    val shuffled = shuffleBoard(board)
    val results = Await.result(ArrowPuzzleSolver.parSimpleSolver(shuffled)(SymmetryBuilder), 30.seconds)

    ("evidence" + shuffled.show + "->" + results.map(_.show)) |: all(
      "Size" |: results.size >= 1,
      "Solved" |: results.toList.map(isSolved).exists(_ == false) == false
    )
  }
}
