import Foundation
import Observation

let gridSize = 9
let subgridSize = 3

enum Difficulty: String, CaseIterable, Identifiable, Codable {
    case beginner
    case normal
    case hard
    case pro
    case master

    var id: String { rawValue }
    var givensRange: ClosedRange<Int> {
        switch self {
        case .beginner: 50...56
        case .normal: 43...49
        case .hard: 35...41
        case .pro: 29...34
        case .master: 24...29
        }
    }
    var maxErrors: Int {
        switch self {
        case .beginner: 6
        case .normal: 5
        case .hard: 4
        case .pro, .master: 3
        }
    }
    var maxHints: Int {
        switch self {
        case .beginner: 5
        case .normal: 4
        case .hard, .pro: 3
        case .master: 2
        }
    }
}

struct HintResult {
    let row: Int
    let col: Int
    let value: Int
    let technique: String
}

private struct GameSnapshot {
    let board: [[Int]]
    let notes: [String: Set<Int>]
    let errorCount: Int
    let errorCells: Set<String>
    let isSolved: Bool
    let isGameOver: Bool
}

@Observable
final class SudokuGame {
    var board = Array(repeating: Array(repeating: 0, count: gridSize), count: gridSize)
    var initialBoard = Array(repeating: Array(repeating: false, count: gridSize), count: gridSize)
    var solvedBoard = Array(repeating: Array(repeating: 0, count: gridSize), count: gridSize)
    var notes: [String: Set<Int>] = [:]
    var errorCells: Set<String> = []
    var errorCount = 0
    var hintsUsed = 0
    var currentDifficulty: Difficulty = .normal
    var isSolved = false
    var isGameOver = false

    private var undoStack: [GameSnapshot] = []

    func generateNewGame(_ difficulty: Difficulty = .normal) {
        currentDifficulty = difficulty
        isSolved = false
        isGameOver = false
        errorCount = 0
        hintsUsed = 0
        notes.removeAll()
        errorCells.removeAll()
        undoStack.removeAll()

        var full = Array(repeating: Array(repeating: 0, count: gridSize), count: gridSize)
        _ = solve(&full)
        solvedBoard = full
        board = carvePuzzle(from: full, targetGivens: Int.random(in: difficulty.givensRange))
        initialBoard = board.map { row in row.map { $0 != 0 } }
    }

    func onCellInput(row: Int, col: Int, value: Int, isNoteMode: Bool) {
        guard !initialBoard[row][col], !isSolved, !isGameOver else { return }
        saveState()
        let key = cellKey(row, col)
        if isNoteMode {
            if value == 0 {
                notes.removeValue(forKey: key)
            } else {
                var values = notes[key, default: []]
                if values.contains(value) { values.remove(value) } else { values.insert(value) }
                notes[key] = values
            }
            return
        }

        board[row][col] = value
        if value == 0 {
            errorCells.remove(key)
        } else if value == solvedBoard[row][col] {
            errorCells.remove(key)
            notes.removeValue(forKey: key)
            clearNotes(row: row, col: col, value: value)
            checkWin()
        } else {
            errorCells.insert(key)
            errorCount += 1
            if errorCount >= currentDifficulty.maxErrors {
                isGameOver = true
            }
        }
    }

    func undo() {
        guard let last = undoStack.popLast() else { return }
        board = last.board
        notes = last.notes
        errorCount = last.errorCount
        errorCells = last.errorCells
        isSolved = last.isSolved
        isGameOver = last.isGameOver
    }

    func canUndo() -> Bool { !undoStack.isEmpty }

    func fillAllNotes() {
        guard !isSolved, !isGameOver else { return }
        saveState()
        for row in 0..<gridSize {
            for col in 0..<gridSize where board[row][col] == 0 {
                notes[cellKey(row, col)] = Set((1...gridSize).filter { isValid(board, row: row, col: col, value: $0) })
            }
        }
    }

    func useHint(selectedCell: (Int, Int)?) -> HintResult? {
        guard hintsUsed < currentDifficulty.maxHints else { return nil }
        let hint = logicalHint()
            ?? selectedCell.flatMap { row, col in
                !initialBoard[row][col] && board[row][col] != solvedBoard[row][col]
                    ? HintResult(row: row, col: col, value: solvedBoard[row][col], technique: "答案提示")
                    : nil
            }
            ?? firstOpenHint()
        guard let hint else { return nil }

        saveState()
        board[hint.row][hint.col] = hint.value
        hintsUsed += 1
        errorCells.remove(cellKey(hint.row, hint.col))
        clearNotes(row: hint.row, col: hint.col, value: hint.value)
        checkWin()
        return hint
    }

    func remainingCounts() -> [Int: Int] {
        var counts = Dictionary(uniqueKeysWithValues: (1...gridSize).map { ($0, gridSize) })
        for row in 0..<gridSize {
            for col in 0..<gridSize {
                let value = board[row][col]
                if value != 0, value == solvedBoard[row][col] {
                    counts[value, default: gridSize] -= 1
                }
            }
        }
        return counts
    }

    func notesFor(row: Int, col: Int) -> Set<Int> {
        notes[cellKey(row, col), default: []]
    }

    func hasError(row: Int, col: Int) -> Bool {
        errorCells.contains(cellKey(row, col))
    }

    private func saveState() {
        if undoStack.count >= 50 { undoStack.removeFirst() }
        undoStack.append(GameSnapshot(board: board, notes: notes, errorCount: errorCount, errorCells: errorCells, isSolved: isSolved, isGameOver: isGameOver))
    }

    private func carvePuzzle(from full: [[Int]], targetGivens: Int) -> [[Int]] {
        var puzzle = full
        var removed = 0
        let targetEmpty = gridSize * gridSize - targetGivens
        for pos in Array(0..<81).shuffled() where removed < targetEmpty {
            let row = pos / gridSize
            let col = pos % gridSize
            let old = puzzle[row][col]
            puzzle[row][col] = 0
            var copy = puzzle
            if countSolutions(&copy, limit: 2) != 1 {
                puzzle[row][col] = old
            } else {
                removed += 1
            }
        }
        return puzzle
    }

    private func clearNotes(row: Int, col: Int, value: Int) {
        notes.removeValue(forKey: cellKey(row, col))
        for i in 0..<gridSize {
            removeNote(row: row, col: i, value: value)
            removeNote(row: i, col: col, value: value)
        }
        let startRow = row / subgridSize * subgridSize
        let startCol = col / subgridSize * subgridSize
        for r in startRow..<(startRow + subgridSize) {
            for c in startCol..<(startCol + subgridSize) {
                removeNote(row: r, col: c, value: value)
            }
        }
    }

    private func removeNote(row: Int, col: Int, value: Int) {
        let key = cellKey(row, col)
        guard var values = notes[key] else { return }
        values.remove(value)
        notes[key] = values
    }

    private func checkWin() {
        for row in 0..<gridSize {
            for col in 0..<gridSize where board[row][col] != solvedBoard[row][col] {
                return
            }
        }
        isSolved = true
    }

    private func logicalHint() -> HintResult? {
        let candidates = buildCandidates(board)
        for row in 0..<gridSize {
            for col in 0..<gridSize where board[row][col] == 0 && candidates[row][col].count == 1 {
                return HintResult(row: row, col: col, value: candidates[row][col].first!, technique: "裸单")
            }
        }
        for unit in boxUnits() + allUnits() {
            for value in 1...gridSize {
                let cells = unit.filter { row, col in board[row][col] == 0 && candidates[row][col].contains(value) }
                if cells.count == 1 {
                    return HintResult(row: cells[0].0, col: cells[0].1, value: value, technique: "隐藏单")
                }
            }
        }
        return nil
    }

    private func firstOpenHint() -> HintResult? {
        for row in 0..<gridSize {
            for col in 0..<gridSize where !initialBoard[row][col] && board[row][col] != solvedBoard[row][col] {
                return HintResult(row: row, col: col, value: solvedBoard[row][col], technique: "答案提示")
            }
        }
        return nil
    }
}

func cellKey(_ row: Int, _ col: Int) -> String { "\(row),\(col)" }

private func solve(_ board: inout [[Int]]) -> Bool {
    guard let (row, col) = emptyCell(in: board) else { return true }
    for value in Array(1...gridSize).shuffled() where isValid(board, row: row, col: col, value: value) {
        board[row][col] = value
        if solve(&board) { return true }
        board[row][col] = 0
    }
    return false
}

private func countSolutions(_ board: inout [[Int]], limit: Int) -> Int {
    guard let (row, col) = emptyCell(in: board) else { return 1 }
    var count = 0
    for value in 1...gridSize where isValid(board, row: row, col: col, value: value) {
        board[row][col] = value
        count += countSolutions(&board, limit: limit)
        board[row][col] = 0
        if count >= limit { return count }
    }
    return count
}

private func emptyCell(in board: [[Int]]) -> (Int, Int)? {
    for row in 0..<gridSize {
        for col in 0..<gridSize where board[row][col] == 0 {
            return (row, col)
        }
    }
    return nil
}

func isValid(_ board: [[Int]], row: Int, col: Int, value: Int) -> Bool {
    for i in 0..<gridSize {
        if board[row][i] == value || board[i][col] == value { return false }
    }
    let startRow = row / subgridSize * subgridSize
    let startCol = col / subgridSize * subgridSize
    for r in startRow..<(startRow + subgridSize) {
        for c in startCol..<(startCol + subgridSize) where board[r][c] == value {
            return false
        }
    }
    return true
}

private func buildCandidates(_ board: [[Int]]) -> [[Set<Int>]] {
    (0..<gridSize).map { row in
        (0..<gridSize).map { col in
            board[row][col] == 0 ? Set((1...gridSize).filter { isValid(board, row: row, col: col, value: $0) }) : [board[row][col]]
        }
    }
}

private func allUnits() -> [[(Int, Int)]] {
    var units: [[(Int, Int)]] = []
    for i in 0..<gridSize {
        units.append((0..<gridSize).map { (i, $0) })
        units.append((0..<gridSize).map { ($0, i) })
    }
    units.append(contentsOf: boxUnits())
    return units
}

private func boxUnits() -> [[(Int, Int)]] {
    var units: [[(Int, Int)]] = []
    for br in 0..<subgridSize {
        for bc in 0..<subgridSize {
            var cells: [(Int, Int)] = []
            for r in (br * subgridSize)..<(br * subgridSize + subgridSize) {
                for c in (bc * subgridSize)..<(bc * subgridSize + subgridSize) {
                    cells.append((r, c))
                }
            }
            units.append(cells)
        }
    }
    return units
}
