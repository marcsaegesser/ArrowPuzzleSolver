package org.saegesser.puzzle

// NOTE: The BoardBuilders are separated from the rest of the solver
// simply to make it easier to compare the performance of different
// solutions.

/** A BoardBuilder is able to generate all valid board configurations
  * reachable from a given board.
  */
trait BoardBuilder {
  /** Generate all board reachable for the given board.
    */
  def boardsFrom(board: Board): Vector[Board]
}

/** An implementation of a BoardBuilder that only returns boards that
  * match the symmetry rules. This prevents generating boards that
  * will cause duplicate solutions to be found. See README.md for the
  * details.
  */
object SymmetryBuilder extends BoardBuilder {
  /** Generate all the boards reachable from the given by fixing one more tile.
    * An empty Vector is returned if no new boards are reachable.
    *
    * Algorithm:
    *   - Locate the first free tile (left to right, top to bottom)
    *   - Determine the constraints for this location
    *   - Search all free tiles for constraint matches
    *   - Exclude matches that violate symmetry rules
    *   - For each match create a new board by swapping the match with the current and fixing the new tile
    */
  def boardsFrom(board: Board): Vector[Board] = {
    @inline
    def updateTiles(ts: Vector[Tile], free: Tile, fixed: Tile, currIdx: Int, destIdx: Int): Vector[Tile] = {
      if(currIdx != destIdx) ts.updated(destIdx, free).updated(currIdx, fixed)
      else                  ts.updated(currIdx, fixed)
    }

    board.freeTiles.headOption.map { case (free, point) =>  // The first free tile and its index
      val c = board.constraintsFor(point)                   // Constraints for point
      board.freeTiles
        .map { case (t, i) => (t.withConstraint(c), i) }    // Constraint search for point
        .filterNot { case (t, _) =>
          t.isEmpty ||                                      // Don't consider tiles that can't match
          (point == 0 && t.head.label >= "7") ||            // Symmetry constraints
          ((point == 2 || point == 6 || point == 8) && (t.head.label < board.tiles(0).label))
        }.flatMap { case (ts, i) =>
            ts.map(t => new Board(updateTiles(board.tiles, free, t, point, i)))
        }
    }.getOrElse(Vector())
  }
}

object SymmetryBuilder2 extends BoardBuilder {
  /** Generate all the boards reachable from the given by fixing one more tile.
    * An empty Vector is returned if no new boards are reachable.
    *
    * Algorithm:
    *   - Locate the next free tile in order (1, 2, 5, 4, 3, 6, 7, 8, 9)
    *   - Determine the constraints for this location
    *   - Search all free tiles for constraint matches
    *   - Exclude matches that violate symmetry rules
    *   - For each match create a new board by swapping the match with the current and fixing the new tile
    */
  def boardsFrom(board: Board): Vector[Board] = {
    @inline
    def updateTiles(ts: Vector[Tile], free: Tile, fixed: Tile, currIdx: Int, destIdx: Int): Vector[Tile] = {
      if(currIdx != destIdx) ts.updated(destIdx, free).updated(currIdx, fixed)
      else                  ts.updated(currIdx, fixed)
    }

    /* This implements a tile selection order of 1 2 5 4 3 6 7 8 9.
     *
     * This makes the placement of the 4th tile require 2 edge
     * constraints. In simple order the first 2-edge constraint
     * happens with the 5th placement.  Moving this one earlier
     * invalidates many boards sooner and significantly improves
     * performance.
     */
    val f =
      if(board.freeTiles.size == 7) Option(board.freeTiles(2))
      else if(board.freeTiles.size == 6) Option(board.freeTiles(1))
      else board.freeTiles.headOption

    f.map { case (free, point) =>                         // The free tile and its index
      val c = board.constraintsFor(point)                 // Constraints for point
      board.freeTiles
        .map { case (t, i) => (t.withConstraint(c), i) }  // Constraint search for point
        .filterNot { case (t, _) =>
          t.isEmpty ||                                     // Don't consider tiles that can't match
          (point == 0 && t.head.label >= "7") ||              // Symmetry constraints
          ((point == 2 || point == 6 || point == 8) && (t.head.label < board.tiles(0).label))
        }.flatMap { case (ts, i) =>
            ts.map(t => new Board(updateTiles(board.tiles, free, t, point, i)))
        }
    }.getOrElse(Vector())
  }
}

object ArrowPuzzleSolver {

  /** A simple recursive puzzle solver.
    */
  def simpleSolver(board: Board)(builder: BoardBuilder): List[Board] = {
    def helper(accum: List[Board], board: Board): List[Board] = {
      val newBoards = builder.boardsFrom(board)
      if(newBoards.isEmpty) {
        if(board.isSolved) board +: accum
        else               accum
      } else {
        newBoards.toList.flatMap(b => helper(accum, b))
      }
    }

    helper(List(), board).sortBy(_.signature)
  }

  /** A simple recursive puzzle solver.
    */
  def parSimpleSolver(board: Board)(builder: BoardBuilder): List[Board] = {
    def helper(accum: List[Board], board: Board): List[Board] = {
      val newBoards = builder.boardsFrom(board)
      if(newBoards.isEmpty) {
        if(board.isSolved) board +: accum
        else               accum
      } else {
        newBoards.par.flatMap(b => helper(accum, b)).toList
      }
    }

    helper(List(), board).sortBy(_.signature)
  }
}

