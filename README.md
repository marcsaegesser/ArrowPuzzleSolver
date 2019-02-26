# Scramble Squares Puzzle Solver

One of the benefits of having a son doing a CS major is that sometimes
he sends me his homework assignments just because he knows I'll get a
kick out of solving them.  He's usually right.

This assignment is from a class on Creative Problem Solving and Team
Programming. The allowed languages were C, C++, Java and Python. I
guess I'll fail since I worked on the problem only by myself and
implemented the solution in Scala.

Kudos to the class for being polyglot, but raspberries for not
allowing lots of interesting languages.

And, of course, this is being posted after the real assignment is due.
I did not provide any assistance to anyone taking the class, including
my son.

## The Problem
The problem consists of a 3x3 square of tiles. Each tile has four
sides, each with a symbol. The symbols are half of an arrow (one half is
the tail, other half is the head). Each symbol is also one of four
colors: red, green, blue and yellow. The goal is to rearrange
the tiles so that every pair of adjacent edges has the same color and
matching symbols (i.e. one head and one tail). The allowed operations
on the board are swapping two tiles and rotating a tile.

The input to the program is a set of nine tiles and the output should
show all unique solutions, up to rotation. There may be several unique
solutions. Each tile is assigned a label when it is read. Labels are
integer values from 1 to 9.

Any solution to the puzzle is really one of four solutions, which are
just the rotations of the whole board by 90, 180 and 270 degrees. Only
one of these rotations should be included in the results. A given
arrangement of tiles can be identified by its _signature_, the
sequence of tile labels from left to right, top to bottom
(e.g. 123456789). The output should only include the solution rotation
which has the lexicographically smallest signature. For example, if
123456789 is a solution then so is 741852063,
987654321, 369258147. Only solution 123456789 should be shown. Also,
the output should be sorted by board signature with smallest values
first.

The problem statement also includes details of the input file format
and the required output format for displaying the solved boards. I
won't reproduce all that here as it isn't very interesting and should
be clear from the code and sample data.

## The Solution
Tiles are marked as either _Fixed_ or _Free_. A fixed tile can no longer
be moved or rotated and becomes a _constraint_ on the solution. To place
a Fixed tile next to one or more other Fixed tiles requires that all
the adjacent edges of the Fixed tiles are matched (same color,
different ends of an arrow). A Free tile provides no constraints on
the board; any edge (either Fixed or Free) is allowed next to a
Free tile's edges. The board begins with all nine tiles Free.

For a given board the board builder generates all valid boards that
can be reached by placing one more Fixed tile subject to the
constraints of any existing Fixed tiles. We start by trying to fix
tiles in the top left corner. Since the board begins as all Free, the
first tile Fixed will have no constraints (note: see the discussion
below on symmetry constraints). If all tiles are unique this results
in 36 new boards (i.e. one board each with each of the four rotations
of the nine tiles in the top left corner). The resulting boards are
then each fed back into the board builder to generate all of the new
boards reachable from these initial boards. If there are no valid boards
reachable from a given tile configuration the board builder returns an
empty list of boards and this branch of the solution space is
abandoned. This algorithm is repeated until all solutions have been
found.

## The Use of Symmetry
The simple algorithm above will find all solutions to the puzzle,
including all four rotations of each unique solution. The solver would
then need to discard all the solutions that are rotations of the
desired solution. It is much more efficient to never generate these
duplicate rotations in the first place.

This can be achieved by a few additional constraints in the board builder
based on the symmetry properties of the puzzle.

Because we only want the rotation with the smallest signature we can
immediately see that the tiles labled 7, 8 and 9 should never be
placed in the top-left corner.  If they were then there must at least
one other corner with a tile labled with smaller number. If we were to
find a solution to the puzzle with a 7, 8 or 9 in the top-left corner
there there will always be another, rotated, solution that has the
smaller corner in the top-left position. Rather than find, and later
discard, these solutions we simply stop before we ever generate
them. More generally, we should never place a tile in a corner if that
tile's label is less than the label of the tile already placed in the
top-left corner. If such a tile was placed and we then found solutions
those solutions would be duplicates of the solutions found when the
smaller tile was placed in the top-left corner. By exluding this scenario
from the search space we avoid finding these duplicates.

## Optimal Tile Placement Order
There is one more optimaztion to be applied to the preceeding
algorithm.  When a new tile is to be placed it can have 0, 1, 2, 3 or
4 constraints depending one how many fixed tiles it is adjacent to. If
we place tiles starting at the top left and proceed left to right and
top to bottom, then the first tile will have 0 constraints, the next
1, etc. The resulting list of tile constraint numbers is 0, 1, 1, 1, 2,
2, 1, 2, 2. We don't encounter a two edge constraint until attempting
to place a tile in the fifth position on the board. However, if we
instead places into positions 1, 2, 5, 4, 3, 6, 7, 8, 9 (numbering
from the top, left corner) then the edge constraint counts are 0, 1,
1, 2, 1, 2, 1, 2, 2. Notice that the first two-edge constraint now
happens in the fourth position. A two-edge constraint is much less
likely to be matched to the remaining free tiles than a single-edge
constraint. So encountering this constraint sooner greatly reducdes
the solution space that needs to be searched.

Experimental results comparing this optimization to the original
algorithm show almost a 30% decrease in the time required to find all
valid solutions.

## Results
The initial board solver, without the optimal tile placement order,
solves 10,000 solvable puzzles in around 64,000 milliseconds, or about
6.4 milliseconds per puzzle. The solver with the optimal placement
pattern solves 10,000 puzzles in about 45,000 milliseconds or about
4.5 milliseconds per puzzle.

Here are some representative runs using the Scala REPL on my laptop, a
Lenovo P50 with an Intel Core i7 at 2.6GHz. This is obviously not a
definitive measure of performance but these results have been pretty
stable and are useful for relative comparisons of different algorithms.

```scala
scala> timeSolutions(boardStream.take(10000).toList)(ArrowPuzzleSolver.simpleSolver(_)(SymmetryBuilder))
res1: String = 10000 solved in 63874ms. Avg=6.3874ms/puzzle

scala> timeSolutions(boardStream.take(10000).toList)(ArrowPuzzleSolver.simpleSolver(_)(SymmetryBuilder2))
res4: String = 10000 solved in 45011ms. Avg=4.5011ms/puzzle
```

The boardStream value is defined as

```scala
val boardStream = Stream.continually(genShuffledBoard.sample).filter(_.isDefined).map(_.get)
```

Where the `genShuffledBoard` is a ScalaCheck generator that creates
random solvable boards. It works by creating a random solved board and
then shuffling and rotating the tiles. Solving solvable boards will in
general take longer than solving truly random board configurations
because there will always be at least one configuration that places
all 9 tiles, while boards with no solutions may be invalidated quite
quickly.

Switching to the `scala.parallel.collection` by adding `.par` to
the results of `builder.boardsFrom` reduced execution time to below 3ms
per board. CPU utilization was about 85% for all 8 cores.

```scala
scala> timeSolutions(boardStream.take(10000).toList)(ArrowPuzzleSolver.parSimpleSolver(_)(SymmetryBuilder2))
res1: String = 10000 solved in 26743ms. Avg=2.6743ms/puzzle
```




