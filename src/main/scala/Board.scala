package org.saegesser.puzzle

import ArrowPuzzle._

/** Represents a tile.
  *
  * Tiles have a label and a set of 4 edges.
  */
sealed trait Tile {
  def label: String
  def edges: Edges

  /** Return all the rotations of this tile that match
    * the given constraints. The returned tiles are
    * Fixed tiles.
    */
  def withConstraint(constraint: Constraint): Vector[FixedTile]

  /** Return string representation of this tile suitable for program output.
    */
  def show: String = s"$label. $edges"

  /** A predicate useful for finding rotations satisfying a constraint.
    *
    * Note: Based on profiling data this is the workhorse of the
    * algorithm, accounting for about 3% of CPU time.  If anyone is
    * looking for optimzation this is the place to start.  MAS -
    * 3/5/2018.
    */
  private[puzzle] def matchesConstraint(es: Edges, c: Constraint): Boolean =
    es match {
      case Edges(t, r, b, l) =>
        c.t.map(_ == t).getOrElse(true) && // Either no  constraint or a matching constraint
        c.r.map(_ == r).getOrElse(true) &&
        c.b.map(_ == b).getOrElse(true) &&
        c.l.map(_ == l).getOrElse(true)
    }
}

object Tile {
  /** Create a new FreeTile with the given label and
    * edges specified as an array of Strings. These strings
    * must be of the form "CS" where C is a valid Color code
    * and S is a valid Shape code.
    */
  def apply(label: String, edges: Array[String]): Tile =
    FreeTile(label, Edges(edges))
}

/** A Free tile.
  */
case class FreeTile(label: String, edges: Edges) extends Tile {
  // Compute all _unique_ rotations of the tile's edges.
  val rotations = edges.rotations

  /** Return all the rotations of this tile that match
    * the given constraints. The returned tiles are
    * Fixed tiles.
    */
  def withConstraint(c: Constraint): Vector[FixedTile] = {
    rotations
      .filter(r => matchesConstraint(r, c))
      .map { es => FixedTile(label, es) }
  }
}

case class FixedTile(label: String, edges: Edges) extends Tile {
  /** Return all the rotations of this tile that match
    * the given constraints. A Fixed tile cannot be rotated
    * so either either the tiles current edge configuration matches
    * the constraint or not.
    */
  def withConstraint(c: Constraint): Vector[FixedTile] = {
    if(matchesConstraint(edges, c)) Vector(this)
    else Vector()
  }
}

/** Represents a game board.
  *
  * This is a sequence of tiles given the board configuration, starting
  * with the top left corner and going left to right and top to bottom.
  */
class Board(val tiles: Vector[Tile]) {
  import Board._

  // collect all the Free tiles witih their board positions.
  val freeTiles = tiles.zipWithIndex.collect { case (t: FreeTile, i: Int) => (t, i) }

  // A board is solved if all the tiles are fixed
  def isSolved: Boolean =
    freeTiles.isEmpty

  // A board signature is the list of labels left-to-right, top-to-bottom.
  def signature: String =
    tiles.map(_.label).mkString("")

  /** If the tile at idx is Fixed then return the shape required to match the given side.
    */
  @inline
  def requiredEdgeAt(idx: Int, side: EdgeSide): Option[EdgeValue] =
    tiles(idx) match {
      case FixedTile(_, edges) => Some(matchingEdgeValue(edges.side(side)))
      case FreeTile(_, _)      => None
    }

  /** Return the constraints for a given board position.
    *
    * Constraints are defined by the edges of adjacent Fixed tiles.
    */
  def constraintsFor(idx: Int): Constraint = {
    adjacencies(idx) match { case (t, r, b, l) =>
      Constraint(
        t.flatMap(e => requiredEdgeAt(e.idx, e.side)),
        r.flatMap(e => requiredEdgeAt(e.idx, e.side)),
        b.flatMap(e => requiredEdgeAt(e.idx, e.side)),
        l.flatMap(e => requiredEdgeAt(e.idx, e.side))
      )
    }
  }

  private val sep = "+--------+--------+--------+\n"
  /** Generate a string representation of this board suitable for
    * program output.
    */
  def show: String = {
    val sb = new scala.collection.mutable.StringBuilder(sep)

    def prRow(row: Vector[Tile]): String = {
      val l1 = s"|${row(0).label}  ${row(0).edges.t}   |${row(1).label}  ${row(1).edges.t}   |${row(2).label}  ${row(2).edges.t}   |\n"
      val l2 = s"|${row(0).edges.l}    ${row(0).edges.r}|${row(1).edges.l}    ${row(1).edges.r}|${row(2).edges.l}    ${row(2).edges.r}|\n"
      val l3 = s"|   ${row(0).edges.b}   |   ${row(1).edges.b}   |   ${row(2).edges.b}   |\n"

      l1 ++ l2 ++ l3 ++ sep
    }

    tiles.sliding(3, 3).foldLeft(sb) { case (a, r) => sb ++= prRow(r) }

    sb.toString
  }
}

object Board {
  // A convience class tracking a board location and an edge
  case class EdgeId(idx: Int, side: EdgeSide)

  /** Edge adjacencies for a 3x3 board.
    *
    * There is an entry for each position on the board starting with
    * the top left corner. Each entry contains a 4-tuple that
    * corresponds top, right, bottom and left edges of that
    * position. These values identify the edge that is adjacent to
    * that edge and that board position. A value of None indicates
    * that there is no adjacent edge.
    *
    * For example, the first entry in this table indicates that at
    * position 0 (the top left corner), there is no adjacent edge
    * for the top edge, the right edge is adjacent to the left edge
    * of position 1 (the top middle position) the bottom edge is
    * adjacent to the top edge of position 3 (middle left position)
    * and there is no edge adjacent to the left edge.
    */
  val adjacencies: Vector[(Option[EdgeId], Option[EdgeId], Option[EdgeId], Option[EdgeId])] =
    Vector(
      //Top                         Right                      Bottom                    Left                            Index
      (None,                        Some(EdgeId(1, EdgeLeft)), Some(EdgeId(3, EdgeTop)), None),                       // 0
      (None,                        Some(EdgeId(2, EdgeLeft)), Some(EdgeId(4, EdgeTop)), Some(EdgeId(0, EdgeRight))), // 1
      (None,                        None,                      Some(EdgeId(5, EdgeTop)), Some(EdgeId(1, EdgeRight))), // 2
      (Some(EdgeId(0, EdgeBottom)), Some(EdgeId(4, EdgeLeft)), Some(EdgeId(6, EdgeTop)), None),                       // 3
      (Some(EdgeId(1, EdgeBottom)), Some(EdgeId(5, EdgeLeft)), Some(EdgeId(7, EdgeTop)), Some(EdgeId(3, EdgeRight))), // 4
      (Some(EdgeId(2, EdgeBottom)), None,                      Some(EdgeId(8, EdgeTop)), Some(EdgeId(4, EdgeRight))), // 5
      (Some(EdgeId(3, EdgeBottom)), Some(EdgeId(7, EdgeLeft)), None,                     None),                       // 6
      (Some(EdgeId(4, EdgeBottom)), Some(EdgeId(8, EdgeLeft)), None,                     Some(EdgeId(6, EdgeRight))), // 7
      (Some(EdgeId(5, EdgeBottom)), None,                      None,                     Some(EdgeId(7, EdgeRight)))  // 8
    )
}
