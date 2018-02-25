package org.saegesser.puzzle

/** Defines useful types used by the puzzle solver.
  */
object ArrowPuzzle {
  // Enumerates the sides of a tile
  type EdgeSide = Int
  final val EdgeTop: EdgeSide    = 0
  final val EdgeRight: EdgeSide  = 1
  final val EdgeBottom: EdgeSide = 2
  final val EdgeLeft: EdgeSide   = 3

  // Enumerates all the colors
  type Color = Char
  final val Red: Color    = 'R'
  final val Green: Color  = 'G'
  final val Blue: Color   = 'B'
  final val Yellow: Color = 'Y'

  // Enumerates the shapes
  type Shape = Int
  final val Tail: Shape = 0
  final val Head: Shape = 1

  // The value of an edge is a color and a shape.
  case class EdgeValue(color: Color, shape: Shape) {
    override def toString: String = s"$color$shape"
  }

  object EdgeValue {
    /** Create an EdgeValue from a string.
      *
      * The string must have the form "CS" where C is a color value
      * ("R", "G", "B", "Y") and S is a shape value ("0", "1").  This
      * is the formated used in the input file.
      */
    def fromString(value: String): EdgeValue =
      EdgeValue(value(0), Integer.parseInt("" + value(1)))
  }

  /** A representation of a tile's edges.
    */
  case class Edges(t: EdgeValue, r: EdgeValue, b: EdgeValue, l: EdgeValue) {
    /** A string representation of the edges in the format required for the program's output.
      */
    override def toString: String = s"<$t, $r, $b, $l>"

    /** Return a specific EdgeValue given an EdgeSide.
      */
    def side(s: EdgeSide): EdgeValue =
      s match {
        case EdgeTop    => t
        case EdgeRight  => r
        case EdgeBottom => b
        case EdgeLeft   => l
      }

    /** Compute all unique roations of the Edges.
      */
    def rotations: Vector[Edges] =
      Vector(
        Edges(t, r, b, l),
        Edges(r, b, l, t),
        Edges(b, l, t, r),
        Edges(l, t, r, b)).distinct
  }

  object Edges {
    def apply(es: Array[String]): Edges =
      es.map(EdgeValue.fromString) match {
        case Array(t, r, b, l) => Edges(t, r, b, l)
        case _                 => throw new IllegalArgumentException(s"Invalid edge array $es")
      }
  }

  /** Compute the value of the edge that matches the given edge.
    *
    * This is an edge with the same color and the other side of the
    * given edges symbol.
    */
  def matchingEdgeValue(e: EdgeValue): EdgeValue =
    e.copy(shape=((e.shape+1) % 2))

  /** Represents a constraint on a tile.
    *
    * The four optional EdgeValues specify the constraints to be applied to a tile. For example,
    * a Constraint(None, Some(EdgeValue(Red, Tail)), Some(EdgeValue(Green, Head)), None) indicates
    * that a tile matching this constraint must have a right edge of Red/Head and a bottom edge of
    * Green/Tail. The top and left edges are unconstrained.
    */
  case class Constraint(t: Option[EdgeValue], r: Option[EdgeValue], b: Option[EdgeValue], l: Option[EdgeValue])
}
