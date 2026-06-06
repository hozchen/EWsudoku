package com.ewstudio.sudokupop

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

// 9. 硬编码 Magic Number 消除：抽离常量
object SudokuConstants {
    const val GRID_SIZE = 9
    const val SUBGRID_SIZE = 3
    const val MAX_ERRORS = 3
    const val MAX_HINTS = 3
    const val MAX_UNDO_SIZE = 50
}

enum class Difficulty(
    val label: String,
    val givensRange: IntRange,
    val targetRating: TechniqueRating,
    val maxErrors: Int,
    val maxHints: Int,
) {
    BEGINNER("入门者", 50..56, TechniqueRating.EASY, maxErrors = 6, maxHints = 5),
    NORMAL("普通", 43..49, TechniqueRating.MEDIUM, maxErrors = 5, maxHints = 4),
    HARD("困难", 35..41, TechniqueRating.HARD, maxErrors = 4, maxHints = 3),
    PRO("专业", 29..34, TechniqueRating.EXPERT, maxErrors = 3, maxHints = 3),
    MASTER("大师", 24..29, TechniqueRating.EVIL, maxErrors = 3, maxHints = 2)
}

enum class TechniqueRating {
    EASY,
    MEDIUM,
    HARD,
    EXPERT,
    EVIL
}

data class HintResult(
    val row: Int,
    val col: Int,
    val value: Int,
    val technique: String,
)

data class GameState(
    val board: List<IntArray>, // 使用 List 包装以方便快照
    val notes: Map<Pair<Int, Int>, Set<Int>>,
    val errorCount: Int,
    val errorCells: Map<Pair<Int, Int>, Boolean>,
    val isSolved: Boolean,
    val isGameOver: Boolean
)

object DailyChallengeTracker {
    private val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        isLenient = false
    }

    fun isCompletedToday(lastCompletedDateKey: String?, currentDateKey: String): Boolean {
        return lastCompletedDateKey == currentDateKey
    }

    fun nextStreak(lastCompletedDateKey: String?, currentDateKey: String, currentStreak: Int): Int {
        if (isCompletedToday(lastCompletedDateKey, currentDateKey)) return currentStreak
        return if (lastCompletedDateKey == previousDateKey(currentDateKey)) currentStreak + 1 else 1
    }

    private fun previousDateKey(dateKey: String): String {
        val date = keyFormat.parse(dateKey) ?: return ""
        val calendar = Calendar.getInstance(Locale.US)
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return keyFormat.format(calendar.time)
    }
}

// 2. 将 SudokuGame 升级为 ViewModel，确保 Compose 状态追踪正确
class SudokuGame : ViewModel() {
    var board by mutableStateOf(Array(SudokuConstants.GRID_SIZE) { IntArray(SudokuConstants.GRID_SIZE) { 0 } })
    var initialBoard by mutableStateOf(Array(SudokuConstants.GRID_SIZE) { BooleanArray(SudokuConstants.GRID_SIZE) { false } })
    var solvedBoard = Array(SudokuConstants.GRID_SIZE) { IntArray(SudokuConstants.GRID_SIZE) { 0 } }
    
    var notes = mutableStateMapOf<Pair<Int, Int>, Set<Int>>()
    var errorCount by mutableIntStateOf(0)
    var hintsUsed by mutableIntStateOf(0)
    var currentDifficulty by mutableStateOf(Difficulty.NORMAL)
    var isSolved by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)
    var errorCells = mutableStateMapOf<Pair<Int, Int>, Boolean>()

    // 4. undoStack 增加上限限制
    private val undoStack = mutableListOf<GameState>()
    private data class RatedPuzzle(
        val puzzle: Array<IntArray>,
        val solution: Array<IntArray>,
        val rating: TechniqueRating,
        val givens: Int,
    )

    fun generateNewGame(difficulty: Difficulty = currentDifficulty) {
        startGame(difficulty, Random.Default)
    }

    fun generateDailyChallenge(dateKey: String) {
        val seed = "SudokuPop:$dateKey".hashCode()
        startGame(Difficulty.NORMAL, Random(seed))
    }

    private fun startGame(difficulty: Difficulty, random: Random) {
        currentDifficulty = difficulty
        isSolved = false
        isGameOver = false
        errorCount = 0
        hintsUsed = 0
        notes.clear()
        errorCells.clear()
        undoStack.clear()

        val ratedPuzzle = generateRatedPuzzle(difficulty, random)
        for (r in 0 until SudokuConstants.GRID_SIZE) solvedBoard[r] = ratedPuzzle.solution[r].copyOf()
        board = ratedPuzzle.puzzle
        initialBoard = Array(SudokuConstants.GRID_SIZE) { r ->
            BooleanArray(SudokuConstants.GRID_SIZE) { c -> ratedPuzzle.puzzle[r][c] != 0 }
        }
    }

    private fun generateRatedPuzzle(difficulty: Difficulty, random: Random): RatedPuzzle {
        var bestPuzzle: RatedPuzzle? = null
        val attempts = when (difficulty) {
            Difficulty.BEGINNER -> 6
            Difficulty.NORMAL -> 8
            Difficulty.HARD -> 10
            Difficulty.PRO, Difficulty.MASTER -> 12
        }

        repeat(attempts) {
            val fullBoard = Array(SudokuConstants.GRID_SIZE) { IntArray(SudokuConstants.GRID_SIZE) { 0 } }
            solve(fullBoard, random)
            val targetGivens = difficulty.givensRange.random(random)
            val puzzleBoard = carvePuzzle(fullBoard, targetGivens, random)
            val rating = ratePuzzleByTechniques(puzzleBoard)
            val givens = countGivens(puzzleBoard)
            val candidate = RatedPuzzle(
                puzzle = puzzleBoard,
                solution = Array(SudokuConstants.GRID_SIZE) { fullBoard[it].copyOf() },
                rating = rating,
                givens = givens,
            )

            if (candidate.matches(difficulty)) return candidate
            if (bestPuzzle == null || candidate.distanceTo(difficulty) < bestPuzzle!!.distanceTo(difficulty)) {
                bestPuzzle = candidate
            }
        }

        return bestPuzzle ?: run {
            val fullBoard = Array(SudokuConstants.GRID_SIZE) { IntArray(SudokuConstants.GRID_SIZE) { 0 } }
            solve(fullBoard, random)
            val puzzleBoard = carvePuzzle(fullBoard, difficulty.givensRange.last, random)
            RatedPuzzle(
                puzzle = puzzleBoard,
                solution = Array(SudokuConstants.GRID_SIZE) { fullBoard[it].copyOf() },
                rating = ratePuzzleByTechniques(puzzleBoard),
                givens = countGivens(puzzleBoard),
            )
        }
    }

    private fun carvePuzzle(fullBoard: Array<IntArray>, targetGivens: Int, random: Random): Array<IntArray> {
        val puzzleBoard = fullBoard.map { it.copyOf() }.toTypedArray()
        val targetEmpty = SudokuConstants.GRID_SIZE * SudokuConstants.GRID_SIZE - targetGivens
        var removed = 0

        for (pos in (0 until 81).shuffled(random)) {
            if (removed >= targetEmpty) break
            val r = pos / SudokuConstants.GRID_SIZE
            val c = pos % SudokuConstants.GRID_SIZE
            val oldValue = puzzleBoard[r][c]
            puzzleBoard[r][c] = 0

            if (!hasUniqueSolution(puzzleBoard)) {
                puzzleBoard[r][c] = oldValue
            } else {
                removed++
            }
        }

        return puzzleBoard
    }

    private fun RatedPuzzle.matches(difficulty: Difficulty): Boolean {
        if (givens !in difficulty.givensRange) return false
        return when (difficulty.targetRating) {
            TechniqueRating.EASY -> rating == TechniqueRating.EASY
            TechniqueRating.MEDIUM -> rating <= TechniqueRating.MEDIUM
            TechniqueRating.HARD -> rating in TechniqueRating.MEDIUM..TechniqueRating.HARD
            TechniqueRating.EXPERT -> rating in TechniqueRating.HARD..TechniqueRating.EXPERT
            TechniqueRating.EVIL -> rating >= TechniqueRating.EXPERT
        }
    }

    private fun RatedPuzzle.distanceTo(difficulty: Difficulty): Int {
        val ratingDistance = kotlin.math.abs(rating.ordinal - difficulty.targetRating.ordinal) * 10
        val givensDistance = when {
            givens < difficulty.givensRange.first -> difficulty.givensRange.first - givens
            givens > difficulty.givensRange.last -> givens - difficulty.givensRange.last
            else -> 0
        }
        return ratingDistance + givensDistance
    }

    private fun countGivens(puzzle: Array<IntArray>): Int {
        var count = 0
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (puzzle[r][c] != 0) count++
            }
        }
        return count
    }

    // 5. 增强 SharedPreferences 序列化
    fun saveProgress(prefs: SharedPreferences, time: Long) {
        prefs.edit().apply {
            putString("saved_board", board.joinToString(",") { it.joinToString("-") })
            putString("saved_initial", initialBoard.joinToString(",") { it.joinToString("-") })
            putString("saved_solved", solvedBoard.joinToString(",") { it.joinToString("-") })
            putInt("saved_errors", errorCount)
            putInt("saved_hints", hintsUsed)
            putString("saved_diff", currentDifficulty.name)
            putLong("saved_time", time)
            val notesStr = notes.map { "${it.key.first},${it.key.second}:${it.value.joinToString("-")}" }.joinToString("|")
            putString("saved_notes", notesStr)
            putBoolean("has_saved_game", !isSolved && !isGameOver)
            apply()
        }
    }

    fun loadProgress(prefs: SharedPreferences): Long {
        if (!prefs.getBoolean("has_saved_game", false)) return 0L
        return try {
            val bStr = prefs.getString("saved_board", "") ?: ""
            board = bStr.split(",").map { row -> row.split("-").map { it.toInt() }.toIntArray() }.toTypedArray()
            val iStr = prefs.getString("saved_initial", "") ?: ""
            initialBoard = iStr.split(",").map { row -> row.split("-").map { it.toBoolean() }.toBooleanArray() }.toTypedArray()
            val sStr = prefs.getString("saved_solved", "") ?: ""
            solvedBoard = sStr.split(",").map { row -> row.split("-").map { it.toInt() }.toIntArray() }.toTypedArray()
            errorCount = prefs.getInt("saved_errors", 0)
            hintsUsed = prefs.getInt("saved_hints", 0)
            currentDifficulty = Difficulty.valueOf(prefs.getString("saved_diff", "NORMAL") ?: "NORMAL")
            notes.clear()
            prefs.getString("saved_notes", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { entry ->
                val parts = entry.split(":")
                val coords = parts[0].split(",")
                val vals = parts[1].split("-").filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
                notes[coords[0].toInt() to coords[1].toInt()] = vals
            }
            prefs.getLong("saved_time", 0L)
        } catch (e: Exception) {
            e.printStackTrace() // 保留日志输出，不再静默失败
            0L
        }
    }

    private fun saveState() {
        if (undoStack.size >= SudokuConstants.MAX_UNDO_SIZE) undoStack.removeAt(0)
        undoStack.add(GameState(
            board = board.map { it.copyOf() },
            notes = notes.toMap(),
            errorCount = errorCount,
            errorCells = errorCells.toMap(),
            isSolved = isSolved,
            isGameOver = isGameOver
        ))
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val lastState = undoStack.removeAt(undoStack.size - 1)
            board = lastState.board.toTypedArray()
            notes.clear(); notes.putAll(lastState.notes)
            errorCount = lastState.errorCount
            errorCells.clear(); errorCells.putAll(lastState.errorCells)
            isSolved = lastState.isSolved
            isGameOver = lastState.isGameOver
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun fillAllNotes() {
        if (isSolved || isGameOver) return
        saveState()
        for (row in 0 until SudokuConstants.GRID_SIZE) {
            for (col in 0 until SudokuConstants.GRID_SIZE) {
                if (board[row][col] == 0) {
                    val possible = (1..SudokuConstants.GRID_SIZE).filter { isValid(board, row, col, it) }.toSet()
                    notes[row to col] = possible
                }
            }
        }
    }

    fun onCellInput(row: Int, col: Int, value: Int, isNoteMode: Boolean): Boolean {
        if (initialBoard[row][col] || isSolved || isGameOver) return false
        if (isNoteMode) {
            val currentSet = notes[row to col] ?: emptySet()
            if (value == 0) {
                if (currentSet.isEmpty()) return false
                saveState()
                notes.remove(row to col)
                return true
            }
            if (currentSet.contains(value)) {
                saveState()
                notes[row to col] = currentSet - value
                return true
            }
            if (!isValid(board, row, col, value)) return false
            saveState()
            notes[row to col] = currentSet + value
            return true
        }

        saveState()
        val newBoard = board.map { it.copyOf() }.toTypedArray()
        newBoard[row][col] = value
        board = newBoard

        if (value == 0) {
            errorCells.remove(row to col)
        } else if (value == solvedBoard[row][col]) {
            errorCells.remove(row to col)
            notes.remove(row to col)
            autoClearNotes(row, col, value)
            checkWin()
        } else {
            errorCells[row to col] = true
            errorCount++
            if (errorCount >= currentDifficulty.maxErrors) isGameOver = true
        }
        return true
    }

    private fun autoClearNotes(row: Int, col: Int, value: Int) {
        notes.remove(row to col)
        for (i in 0 until SudokuConstants.GRID_SIZE) {
            removeNoteFromCell(row, i, value)
            removeNoteFromCell(i, col, value)
        }
        val startRow = (row / SudokuConstants.SUBGRID_SIZE) * SudokuConstants.SUBGRID_SIZE
        val startCol = (col / SudokuConstants.SUBGRID_SIZE) * SudokuConstants.SUBGRID_SIZE
        for (r in startRow until startRow + SudokuConstants.SUBGRID_SIZE) {
            for (c in startCol until startCol + SudokuConstants.SUBGRID_SIZE) {
                removeNoteFromCell(r, c, value)
            }
        }
    }

    private fun removeNoteFromCell(r: Int, c: Int, value: Int) {
        val currentSet = notes[r to c]
        if (currentSet != null && currentSet.contains(value)) {
            notes[r to c] = currentSet - value
        }
    }

    fun useHint(selectedCell: Pair<Int, Int>?): HintResult? {
        if (hintsUsed >= currentDifficulty.maxHints) return null
        val hint = findNextLogicalHint()
            ?: selectedCell
                ?.takeIf { (r, c) -> !initialBoard[r][c] && board[r][c] != solvedBoard[r][c] }
                ?.let { (r, c) -> HintResult(r, c, solvedBoard[r][c], "答案提示") }
            ?: findFirstOpenHint()
            ?: return null

        val (r, c, value) = hint
        saveState()
        val newBoard = board.map { it.copyOf() }.toTypedArray()
        newBoard[r][c] = value
        board = newBoard
        hintsUsed++
        errorCells.remove(r to c)
        autoClearNotes(r, c, value)
        checkWin()
        return hint
    }

    fun getRemainingCounts(): Map<Int, Int> {
        val counts = (1..SudokuConstants.GRID_SIZE).associateWith { SudokuConstants.GRID_SIZE }.toMutableMap()
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                val v = board[r][c]
                if (v != 0 && v == solvedBoard[r][c]) {
                    counts[v] = (counts[v] ?: SudokuConstants.GRID_SIZE) - 1
                }
            }
        }
        return counts
    }

    private fun checkWin() {
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (board[r][c] != solvedBoard[r][c]) return
            }
        }
        isSolved = true
    }

    private fun findNextLogicalHint(): HintResult? {
        val candidates = buildCandidates(board) ?: return null

        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (board[r][c] == 0 && candidates[r][c].size == 1) {
                    return HintResult(r, c, candidates[r][c].first(), "裸单")
                }
            }
        }

        findHiddenSingle(candidates, boxesOnly = true)?.let { return it.copy(technique = "宫内扫描") }
        findHiddenSingle(candidates, boxesOnly = false)?.let { return it.copy(technique = "隐藏单") }
        return null
    }

    private fun findHiddenSingle(candidates: Array<Array<MutableSet<Int>>>, boxesOnly: Boolean): HintResult? {
        val units = if (boxesOnly) {
            (0 until SudokuConstants.SUBGRID_SIZE).flatMap { br ->
                (0 until SudokuConstants.SUBGRID_SIZE).map { bc -> boxCells(br, bc) }
            }
        } else {
            allUnits()
        }

        for (unit in units) {
            for (value in 1..SudokuConstants.GRID_SIZE) {
                val cells = unit.filter { (r, c) -> board[r][c] == 0 && value in candidates[r][c] }
                if (cells.size == 1) {
                    val (r, c) = cells.first()
                    return HintResult(r, c, value, "隐藏单")
                }
            }
        }
        return null
    }

    private fun findFirstOpenHint(): HintResult? {
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (!initialBoard[r][c] && board[r][c] != solvedBoard[r][c]) {
                    return HintResult(r, c, solvedBoard[r][c], "答案提示")
                }
            }
        }
        return null
    }

    private fun ratePuzzleByTechniques(puzzle: Array<IntArray>): TechniqueRating {
        val work = Array(SudokuConstants.GRID_SIZE) { puzzle[it].copyOf() }
        var maxRating = TechniqueRating.EASY
        var candidates = buildCandidates(work) ?: return TechniqueRating.EVIL

        while (!isComplete(work)) {
            if (hasEmptyCandidate(work, candidates)) return TechniqueRating.EVIL
            if (applyNakedSingles(work, candidates)) {
                candidates = buildCandidates(work) ?: return TechniqueRating.EVIL
                continue
            }
            if (applyBoxHiddenSingles(work, candidates)) {
                candidates = buildCandidates(work) ?: return TechniqueRating.EVIL
                continue
            }
            if (applyHiddenSingles(work, candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.MEDIUM)
                candidates = buildCandidates(work) ?: return TechniqueRating.EVIL
                continue
            }
            if (applyNakedPairs(candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.MEDIUM)
                continue
            }
            if (applyHiddenPairs(candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.MEDIUM)
                continue
            }
            if (applyPointingAndBoxLine(candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.HARD)
                continue
            }
            if (applyNakedTriples(candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.HARD)
                continue
            }
            if (applyXWing(candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.HARD)
                continue
            }
            if (applySwordfish(candidates)) {
                maxRating = maxOf(maxRating, TechniqueRating.EXPERT)
                continue
            }

            return if (countGivens(puzzle) <= 24) TechniqueRating.EVIL else TechniqueRating.EXPERT
        }

        return maxRating
    }

    private fun hasEmptyCandidate(board: Array<IntArray>, candidates: Array<Array<MutableSet<Int>>>): Boolean {
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (board[r][c] == 0 && candidates[r][c].isEmpty()) return true
            }
        }
        return false
    }

    private fun isComplete(board: Array<IntArray>): Boolean {
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (board[r][c] == 0) return false
            }
        }
        return true
    }

    private fun buildCandidates(board: Array<IntArray>): Array<Array<MutableSet<Int>>>? {
        return Array(SudokuConstants.GRID_SIZE) { r ->
            Array(SudokuConstants.GRID_SIZE) { c ->
                if (board[r][c] != 0) mutableSetOf(board[r][c])
                else {
                    val values = (1..SudokuConstants.GRID_SIZE).filter { isValid(board, r, c, it) }.toMutableSet()
                    if (values.isEmpty()) return null
                    values
                }
            }
        }
    }

    private fun applyNakedSingles(board: Array<IntArray>, candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (board[r][c] == 0 && candidates[r][c].size == 1) {
                    board[r][c] = candidates[r][c].first()
                    changed = true
                }
            }
        }
        return changed
    }

    private fun applyBoxHiddenSingles(board: Array<IntArray>, candidates: Array<Array<MutableSet<Int>>>): Boolean {
        for (boxRow in 0 until SudokuConstants.SUBGRID_SIZE) {
            for (boxCol in 0 until SudokuConstants.SUBGRID_SIZE) {
                val unit = boxCells(boxRow, boxCol)
                for (value in 1..SudokuConstants.GRID_SIZE) {
                    val cells = unit.filter { (r, c) -> board[r][c] == 0 && value in candidates[r][c] }
                    if (cells.size == 1) {
                        val (r, c) = cells.first()
                        board[r][c] = value
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun applyHiddenSingles(board: Array<IntArray>, candidates: Array<Array<MutableSet<Int>>>): Boolean {
        for (unit in allUnits()) {
            for (value in 1..SudokuConstants.GRID_SIZE) {
                val cells = unit.filter { (r, c) -> board[r][c] == 0 && value in candidates[r][c] }
                if (cells.size == 1) {
                    val (r, c) = cells.first()
                    board[r][c] = value
                    return true
                }
            }
        }
        return false
    }

    private fun applyNakedPairs(candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false
        for (unit in allUnits()) {
            val pairs = unit
                .filter { (r, c) -> candidates[r][c].size == 2 }
                .groupBy { (r, c) -> candidates[r][c].toSet() }
                .filterValues { it.size == 2 }

            for ((pair, pairCells) in pairs) {
                for ((r, c) in unit) {
                    if ((r to c) !in pairCells && candidates[r][c].size > 1 && candidates[r][c].removeAll(pair)) {
                        changed = true
                    }
                }
            }
        }
        return changed
    }

    private fun applyHiddenPairs(candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false
        for (unit in allUnits()) {
            val positionsByValue = (1..SudokuConstants.GRID_SIZE).associateWith { value ->
                unit.filter { (r, c) -> candidates[r][c].size > 1 && value in candidates[r][c] }.toSet()
            }
            val values = positionsByValue.keys.toList()
            for (i in values.indices) {
                for (j in i + 1 until values.size) {
                    val first = values[i]
                    val second = values[j]
                    val firstCells = positionsByValue[first].orEmpty()
                    if (firstCells.size == 2 && firstCells == positionsByValue[second].orEmpty()) {
                        val pair = setOf(first, second)
                        for ((r, c) in firstCells) {
                            val newSet = candidates[r][c].intersect(pair).toMutableSet()
                            if (newSet.size == 2 && newSet != candidates[r][c]) {
                                candidates[r][c].clear()
                                candidates[r][c].addAll(newSet)
                                changed = true
                            }
                        }
                    }
                }
            }
        }
        return changed
    }

    private fun applyNakedTriples(candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false
        for (unit in allUnits()) {
            val cells = unit.filter { (r, c) -> candidates[r][c].size in 2..3 }
            for (a in cells.indices) {
                for (b in a + 1 until cells.size) {
                    for (cIndex in b + 1 until cells.size) {
                        val tripleCells = listOf(cells[a], cells[b], cells[cIndex])
                        val triple = tripleCells.flatMap { (r, c) -> candidates[r][c] }.toSet()
                        if (triple.size == 3) {
                            for ((r, c) in unit) {
                                if ((r to c) !in tripleCells && candidates[r][c].size > 1 && candidates[r][c].removeAll(triple)) {
                                    changed = true
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed
    }

    private fun applyPointingAndBoxLine(candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false

        for (boxRow in 0 until SudokuConstants.SUBGRID_SIZE) {
            for (boxCol in 0 until SudokuConstants.SUBGRID_SIZE) {
                val boxCells = boxCells(boxRow, boxCol)
                for (value in 1..SudokuConstants.GRID_SIZE) {
                    val cells = boxCells.filter { (r, c) -> candidates[r][c].size > 1 && value in candidates[r][c] }
                    val rows = cells.map { it.first }.toSet()
                    val cols = cells.map { it.second }.toSet()
                    if (cells.size >= 2 && rows.size == 1) {
                        val row = rows.first()
                        for (c in 0 until SudokuConstants.GRID_SIZE) {
                            if (c / SudokuConstants.SUBGRID_SIZE != boxCol && candidates[row][c].remove(value)) changed = true
                        }
                    }
                    if (cells.size >= 2 && cols.size == 1) {
                        val col = cols.first()
                        for (r in 0 until SudokuConstants.GRID_SIZE) {
                            if (r / SudokuConstants.SUBGRID_SIZE != boxRow && candidates[r][col].remove(value)) changed = true
                        }
                    }
                }
            }
        }

        for (value in 1..SudokuConstants.GRID_SIZE) {
            for (row in 0 until SudokuConstants.GRID_SIZE) {
                val cols = (0 until SudokuConstants.GRID_SIZE).filter { c -> candidates[row][c].size > 1 && value in candidates[row][c] }
                if (cols.size >= 2 && cols.map { it / SudokuConstants.SUBGRID_SIZE }.toSet().size == 1) {
                    val boxRow = row / SudokuConstants.SUBGRID_SIZE
                    val boxCol = cols.first() / SudokuConstants.SUBGRID_SIZE
                    for ((r, c) in boxCells(boxRow, boxCol)) {
                        if (r != row && candidates[r][c].remove(value)) changed = true
                    }
                }
            }
            for (col in 0 until SudokuConstants.GRID_SIZE) {
                val rows = (0 until SudokuConstants.GRID_SIZE).filter { r -> candidates[r][col].size > 1 && value in candidates[r][col] }
                if (rows.size >= 2 && rows.map { it / SudokuConstants.SUBGRID_SIZE }.toSet().size == 1) {
                    val boxRow = rows.first() / SudokuConstants.SUBGRID_SIZE
                    val boxCol = col / SudokuConstants.SUBGRID_SIZE
                    for ((r, c) in boxCells(boxRow, boxCol)) {
                        if (c != col && candidates[r][c].remove(value)) changed = true
                    }
                }
            }
        }

        return changed
    }

    private fun applyXWing(candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false
        for (value in 1..SudokuConstants.GRID_SIZE) {
            val rowPairs = (0 until SudokuConstants.GRID_SIZE).mapNotNull { row ->
                val cols = (0 until SudokuConstants.GRID_SIZE).filter { col -> candidates[row][col].size > 1 && value in candidates[row][col] }
                if (cols.size == 2) row to cols else null
            }
            for (i in rowPairs.indices) {
                for (j in i + 1 until rowPairs.size) {
                    if (rowPairs[i].second == rowPairs[j].second) {
                        val rows = setOf(rowPairs[i].first, rowPairs[j].first)
                        for (row in 0 until SudokuConstants.GRID_SIZE) {
                            if (row !in rows) {
                                for (col in rowPairs[i].second) {
                                    if (candidates[row][col].remove(value)) changed = true
                                }
                            }
                        }
                    }
                }
            }

            val colPairs = (0 until SudokuConstants.GRID_SIZE).mapNotNull { col ->
                val rows = (0 until SudokuConstants.GRID_SIZE).filter { row -> candidates[row][col].size > 1 && value in candidates[row][col] }
                if (rows.size == 2) col to rows else null
            }
            for (i in colPairs.indices) {
                for (j in i + 1 until colPairs.size) {
                    if (colPairs[i].second == colPairs[j].second) {
                        val cols = setOf(colPairs[i].first, colPairs[j].first)
                        for (col in 0 until SudokuConstants.GRID_SIZE) {
                            if (col !in cols) {
                                for (row in colPairs[i].second) {
                                    if (candidates[row][col].remove(value)) changed = true
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed
    }

    private fun applySwordfish(candidates: Array<Array<MutableSet<Int>>>): Boolean {
        var changed = false
        for (value in 1..SudokuConstants.GRID_SIZE) {
            val rowLines = (0 until SudokuConstants.GRID_SIZE).mapNotNull { row ->
                val cols = (0 until SudokuConstants.GRID_SIZE).filter { col -> candidates[row][col].size > 1 && value in candidates[row][col] }
                if (cols.size in 2..3) row to cols.toSet() else null
            }
            for (a in rowLines.indices) {
                for (b in a + 1 until rowLines.size) {
                    for (cIndex in b + 1 until rowLines.size) {
                        val lines = listOf(rowLines[a], rowLines[b], rowLines[cIndex])
                        val cols = lines.flatMap { it.second }.toSet()
                        if (cols.size == 3) {
                            val rows = lines.map { it.first }.toSet()
                            for (row in 0 until SudokuConstants.GRID_SIZE) {
                                if (row !in rows) {
                                    for (col in cols) {
                                        if (candidates[row][col].size > 1 && candidates[row][col].remove(value)) changed = true
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val colLines = (0 until SudokuConstants.GRID_SIZE).mapNotNull { col ->
                val rows = (0 until SudokuConstants.GRID_SIZE).filter { row -> candidates[row][col].size > 1 && value in candidates[row][col] }
                if (rows.size in 2..3) col to rows.toSet() else null
            }
            for (a in colLines.indices) {
                for (b in a + 1 until colLines.size) {
                    for (cIndex in b + 1 until colLines.size) {
                        val lines = listOf(colLines[a], colLines[b], colLines[cIndex])
                        val rows = lines.flatMap { it.second }.toSet()
                        if (rows.size == 3) {
                            val cols = lines.map { it.first }.toSet()
                            for (col in 0 until SudokuConstants.GRID_SIZE) {
                                if (col !in cols) {
                                    for (row in rows) {
                                        if (candidates[row][col].size > 1 && candidates[row][col].remove(value)) changed = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed
    }

    private fun allUnits(): List<List<Pair<Int, Int>>> {
        val units = mutableListOf<List<Pair<Int, Int>>>()
        for (r in 0 until SudokuConstants.GRID_SIZE) {
            units.add((0 until SudokuConstants.GRID_SIZE).map { c -> r to c })
        }
        for (c in 0 until SudokuConstants.GRID_SIZE) {
            units.add((0 until SudokuConstants.GRID_SIZE).map { r -> r to c })
        }
        for (br in 0 until SudokuConstants.SUBGRID_SIZE) {
            for (bc in 0 until SudokuConstants.SUBGRID_SIZE) {
                units.add(boxCells(br, bc))
            }
        }
        return units
    }

    private fun boxCells(boxRow: Int, boxCol: Int): List<Pair<Int, Int>> {
        val startRow = boxRow * SudokuConstants.SUBGRID_SIZE
        val startCol = boxCol * SudokuConstants.SUBGRID_SIZE
        return (startRow until startRow + SudokuConstants.SUBGRID_SIZE).flatMap { r ->
            (startCol until startCol + SudokuConstants.SUBGRID_SIZE).map { c -> r to c }
        }
    }

    private fun solve(board: Array<IntArray>, random: Random = Random.Default): Boolean {
        for (row in 0 until SudokuConstants.GRID_SIZE) {
            for (col in 0 until SudokuConstants.GRID_SIZE) {
                if (board[row][col] == 0) {
                    val nums = (1..SudokuConstants.GRID_SIZE).shuffled(random)
                    for (num in nums) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num
                            if (solve(board, random)) return true
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Returns true if [board] has exactly one solution.
     * Uses MRV heuristic (minimum-remaining-values) to minimize branching.
     */
    private fun hasUniqueSolution(board: Array<IntArray>): Boolean {
        val copy = Array(SudokuConstants.GRID_SIZE) { board[it].copyOf() }
        return countSolutions(copy, limit = 2) == 1
    }

    /**
     * Counts solutions up to [limit], then stops.
     * Uses MRV heuristic: picks the empty cell with the fewest candidates first,
     * which dramatically reduces the search tree for sparse boards.
     */
    private fun countSolutions(board: Array<IntArray>, limit: Int): Int {
        var bestR = -1; var bestC = -1
        var bestCount = 10
        val candidates = mutableListOf<Int>()

        for (r in 0 until SudokuConstants.GRID_SIZE) {
            for (c in 0 until SudokuConstants.GRID_SIZE) {
                if (board[r][c] == 0) {
                    val cellCandidates = mutableListOf<Int>()
                    for (n in 1..SudokuConstants.GRID_SIZE) {
                        if (isValid(board, r, c, n)) cellCandidates.add(n)
                    }
                    if (cellCandidates.isEmpty()) return 0
                    if (cellCandidates.size < bestCount) {
                        bestCount = cellCandidates.size
                        bestR = r; bestC = c
                        candidates.clear(); candidates.addAll(cellCandidates)
                        if (bestCount == 1) break
                    }
                }
            }
        }

        if (bestR == -1) return 1 // board is full

        var count = 0
        // Shuffle to find alternative solutions faster
        candidates.shuffle()
        for (num in candidates) {
            board[bestR][bestC] = num
            count += countSolutions(board, limit - count)
            board[bestR][bestC] = 0
            if (count >= limit) return count
        }
        return count
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until SudokuConstants.GRID_SIZE) {
            if (board[row][i] == num) return false
            if (board[i][col] == num) return false
        }
        val startRow = (row / SudokuConstants.SUBGRID_SIZE) * SudokuConstants.SUBGRID_SIZE
        val startCol = (col / SudokuConstants.SUBGRID_SIZE) * SudokuConstants.SUBGRID_SIZE
        for (i in 0 until SudokuConstants.SUBGRID_SIZE) {
            for (j in 0 until SudokuConstants.SUBGRID_SIZE) {
                if (board[startRow + i][startCol + j] == num) return false
            }
        }
        return true
    }
}
