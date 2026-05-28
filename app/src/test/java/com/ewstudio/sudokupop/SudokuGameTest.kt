package com.ewstudio.sudokupop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameTest {
    @Test
    fun generatedGamesArePlayableForEveryDifficulty() {
        Difficulty.entries.forEach { difficulty ->
            val game = SudokuGame()

            game.generateNewGame(difficulty)

            assertEquals(difficulty, game.currentDifficulty)
            assertFalse(game.isSolved)
            assertFalse(game.isGameOver)
            assertValidSolvedBoard(game.solvedBoard)
            assertTrue(countGivens(game.board) in difficulty.givensRange)
            assertTrue(hasAtLeastOneEditableCell(game))
        }
    }

    @Test
    fun hintPlacesCorrectValueAndCountsUsage() {
        val game = SudokuGame()
        game.generateNewGame(Difficulty.BEGINNER)
        val editable = firstEditableCell(game)

        val hint = game.useHint(editable)

        requireNotNull(hint)
        assertEquals(game.solvedBoard[hint.row][hint.col], hint.value)
        assertEquals(hint.value, game.board[hint.row][hint.col])
        assertEquals(1, game.hintsUsed)
        assertFalse(game.errorCells[hint.row to hint.col] ?: false)
    }

    @Test
    fun correctInputsCanFinishGame() {
        val game = SudokuGame()
        game.generateNewGame(Difficulty.BEGINNER)

        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (!game.initialBoard[r][c]) {
                    game.onCellInput(r, c, game.solvedBoard[r][c], isNoteMode = false)
                }
            }
        }

        assertTrue(game.isSolved)
        assertFalse(game.isGameOver)
    }

    @Test
    fun dailyChallengeIsStableForSameDate() {
        val first = SudokuGame()
        val second = SudokuGame()

        first.generateDailyChallenge("20260528")
        second.generateDailyChallenge("20260528")

        assertEquals(Difficulty.NORMAL, first.currentDifficulty)
        assertBoardsEqual(first.board, second.board)
        assertBoardsEqual(first.solvedBoard, second.solvedBoard)
    }

    @Test
    fun dailyChallengeStreakContinuesOnlyFromPreviousDay() {
        assertEquals(4, DailyChallengeTracker.nextStreak("20260527", "20260528", 3))
        assertEquals(1, DailyChallengeTracker.nextStreak("20260526", "20260528", 8))
        assertEquals(5, DailyChallengeTracker.nextStreak("20260528", "20260528", 5))
        assertTrue(DailyChallengeTracker.isCompletedToday("20260528", "20260528"))
        assertFalse(DailyChallengeTracker.isCompletedToday("20260527", "20260528"))
    }

    @Test
    fun wrongInputsTriggerGameOverAtDifficultyLimit() {
        val game = SudokuGame()
        game.generateNewGame(Difficulty.BEGINNER)
        val editableCells = allEditableCells(game).take(game.currentDifficulty.maxErrors)

        editableCells.forEach { (r, c) ->
            val wrong = (1..SudokuConstants.GRID_SIZE).first { it != game.solvedBoard[r][c] }
            game.onCellInput(r, c, wrong, isNoteMode = false)
        }

        assertEquals(game.currentDifficulty.maxErrors, game.errorCount)
        assertTrue(game.isGameOver)
    }

    private fun assertValidSolvedBoard(board: Array<IntArray>) {
        val expected = (1..SudokuConstants.GRID_SIZE).toSet()
        for (i in 0 until SudokuConstants.GRID_SIZE) {
            assertEquals(expected, board[i].toSet())
            assertEquals(expected, (0 until SudokuConstants.GRID_SIZE).map { r -> board[r][i] }.toSet())
        }
        for (br in 0 until SudokuConstants.SUBGRID_SIZE) {
            for (bc in 0 until SudokuConstants.SUBGRID_SIZE) {
                val values = mutableSetOf<Int>()
                for (r in br * 3 until br * 3 + 3) {
                    for (c in bc * 3 until bc * 3 + 3) {
                        values.add(board[r][c])
                    }
                }
                assertEquals(expected, values)
            }
        }
    }

    private fun assertBoardsEqual(expected: Array<IntArray>, actual: Array<IntArray>) {
        assertEquals(expected.size, actual.size)
        expected.indices.forEach { row ->
            assertTrue(expected[row].contentEquals(actual[row]))
        }
    }

    private fun countGivens(board: Array<IntArray>): Int {
        return board.sumOf { row -> row.count { it != 0 } }
    }

    private fun hasAtLeastOneEditableCell(game: SudokuGame): Boolean {
        return allEditableCells(game).isNotEmpty()
    }

    private fun firstEditableCell(game: SudokuGame): Pair<Int, Int> {
        return allEditableCells(game).first()
    }

    private fun allEditableCells(game: SudokuGame): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (!game.initialBoard[r][c]) cells.add(r to c)
            }
        }
        return cells
    }
}
