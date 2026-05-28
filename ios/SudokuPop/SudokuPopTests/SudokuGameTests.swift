import XCTest
@testable import SudokuPop

final class SudokuGameTests: XCTestCase {
    func testGeneratedGamesArePlayableForEveryDifficulty() {
        for difficulty in Difficulty.allCases {
            let game = SudokuGame()
            game.generateNewGame(difficulty)

            XCTAssertEqual(game.currentDifficulty, difficulty)
            XCTAssertFalse(game.isSolved)
            XCTAssertFalse(game.isGameOver)
            XCTAssertTrue(difficulty.givensRange.contains(countGivens(game.board)))
            XCTAssertTrue(hasEditableCell(game))
            assertValidSolvedBoard(game.solvedBoard)
        }
    }

    func testCorrectInputsCanFinishGame() {
        let game = SudokuGame()
        game.generateNewGame(.beginner)

        for row in 0..<gridSize {
            for col in 0..<gridSize where !game.initialBoard[row][col] {
                game.onCellInput(row: row, col: col, value: game.solvedBoard[row][col], isNoteMode: false)
            }
        }

        XCTAssertTrue(game.isSolved)
        XCTAssertFalse(game.isGameOver)
    }

    func testWrongInputsTriggerGameOverAtDifficultyLimit() {
        let game = SudokuGame()
        game.generateNewGame(.beginner)
        let cells = editableCells(game).prefix(game.currentDifficulty.maxErrors)

        for (row, col) in cells {
            let wrong = (1...gridSize).first { $0 != game.solvedBoard[row][col] }!
            game.onCellInput(row: row, col: col, value: wrong, isNoteMode: false)
        }

        XCTAssertEqual(game.errorCount, game.currentDifficulty.maxErrors)
        XCTAssertTrue(game.isGameOver)
    }

    private func assertValidSolvedBoard(_ board: [[Int]]) {
        let expected = Set(1...gridSize)
        for i in 0..<gridSize {
            XCTAssertEqual(Set(board[i]), expected)
            XCTAssertEqual(Set((0..<gridSize).map { board[$0][i] }), expected)
        }
    }

    private func countGivens(_ board: [[Int]]) -> Int {
        board.reduce(0) { $0 + $1.filter { $0 != 0 }.count }
    }

    private func hasEditableCell(_ game: SudokuGame) -> Bool {
        !editableCells(game).isEmpty
    }

    private func editableCells(_ game: SudokuGame) -> [(Int, Int)] {
        var cells: [(Int, Int)] = []
        for row in 0..<gridSize {
            for col in 0..<gridSize where !game.initialBoard[row][col] {
                cells.append((row, col))
            }
        }
        return cells
    }
}
