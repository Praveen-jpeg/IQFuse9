package com.example.iqfuse8

/**
 * Represents a complete Tango puzzle with its grid and constraints.
 */
data class TangoPuzzleModel(
    val id: String = "",
    val day: Int = 0,
    val grid: List<String> = emptyList(),      // Complete solution grid
    val visibleCells: List<Pair<Int, Int>> = emptyList(), // Coordinates of initially visible cells (i,j)
    val horizontalConstraints: List<String> = emptyList(),
    val verticalConstraints: List<String> = emptyList()
) {
    companion object {
        /**
         * Converts a List<String> grid representation to Array<CharArray>
         */
        fun gridToCharArray(grid: List<String>): Array<CharArray> {
            return grid.map { it.toCharArray() }.toTypedArray()
        }

        /**
         * Converts an Array<CharArray> grid to List<String> for Firestore storage
         */
        fun charArrayToGrid(grid: Array<CharArray>): List<String> {
            return grid.map { String(it) }
        }

        /**
         * Converts a List<String> constraints representation to Array<Array<Char>>
         */
        fun constraintsToCharArray(constraints: List<String>): Array<Array<Char>> {
            return constraints.map { row ->
                row.toCharArray().map { it }.toTypedArray()
            }.toTypedArray()
        }

        /**
         * Converts an Array<Array<Char>> constraints to List<String> for Firestore storage
         */
        fun charArrayToConstraints(constraints: Array<Array<Char>>): List<String> {
            return constraints.map { String(it.toCharArray()) }
        }

        /**
         * Create a partial display grid showing only visible cells
         */
        fun createPartialGrid(completeGrid: Array<CharArray>, visibleCells: List<Pair<Int, Int>>): Array<CharArray> {
            val size = completeGrid.size
            val partialGrid = Array(size) { CharArray(size) { ' ' } }
            
            // Fill in the visible cells from the complete grid
            for ((row, col) in visibleCells) {
                if (row in 0 until size && col in 0 until size) {
                    partialGrid[row][col] = completeGrid[row][col]
                }
            }
            
            return partialGrid
        }

        /**
         * Create a sample puzzle for initialization purposes
         */
        fun createSamplePuzzle(day: Int): TangoPuzzleModel {
            // Create a default puzzle layout - ensure 6 characters per line for 6x6 grid
            val grid = when (day % 10) {
                0 -> listOf(
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀"
                )
                1 -> listOf(
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾"
                )
                2 -> listOf(
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☀☾☾☀☀",
                    "☀☀☾☾☀☀",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾"
                )
                3 -> listOf(
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀"
                )
                4 -> listOf(
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☀☀☾☾",
                    "☾☀☀☀☾☾",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀"
                )
                5 -> listOf(
                    "☀☀☾☾☀☀",
                    "☀☀☾☾☀☀",
                    "☾☾☀☀☾☾",
                    "☾☾☀☀☾☾",
                    "☀☀☾☾☀☀",
                    "☀☀☾☾☀☀"
                )
                6 -> listOf(
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☾☀☀☾",
                    "☾☀☀☾☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀"
                )
                7 -> listOf(
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☀☾☾☀",
                    "☀☾☾☀☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾"
                )
                8 -> listOf(
                    "☀☀☾☾☀☀",
                    "☀☀☾☾☀☀",
                    "☾☾☀☀☾☾",
                    "☾☾☀☀☾☾",
                    "☀☀☾☾☀☀",
                    "☀☀☾☾☀☀"
                )
                9 -> listOf(
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀"
                )
                else -> listOf(
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀",
                    "☀☾☀☾☀☾",
                    "☾☀☾☀☾☀"
                )
            }

            // Generate 16 random visible cells based on day seed
            val random = java.util.Random(day.toLong())
            val visibleCells = mutableListOf<Pair<Int, Int>>()

            // Add 16 unique random cells
            while (visibleCells.size < 16) {
                val row = random.nextInt(6)
                val col = random.nextInt(6)
                val pair = Pair(row, col)
                if (!visibleCells.contains(pair)) {
                    visibleCells.add(pair)
                }
            }

            // Define horizontal constraints (between rows)
            val horizontalConstraints = listOf(
                "      ", // No constraints between rows 0 and 1
                "      ", // No constraints between rows 1 and 2
                "      ", // No constraints between rows 2 and 3
                "= =    ", // Constraints between rows 3 and 4
                "x   x  ", // Constraints between rows 4 and 5
                "      "  // No more rows after 5
            )

            // Define vertical constraints (between columns)
            val verticalConstraints = listOf(
                "     ", // No constraints between columns 0 and 1
                "  x  ", // Constraints between columns 1 and 2
                "     ", // No constraints between columns 2 and 3
                "    x", // Constraints between columns 3 and 4
                "     ", // No constraints between columns 4 and 5
                "     "  // No more columns after 5
            )

            return TangoPuzzleModel(
                id = "puzzle_$day",
                day = day,
                grid = grid,
                visibleCells = visibleCells,
                horizontalConstraints = horizontalConstraints,
                verticalConstraints = verticalConstraints
            )
        }
    }
} 