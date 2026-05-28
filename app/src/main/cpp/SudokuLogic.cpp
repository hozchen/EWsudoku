#include "SudokuLogic.h"
#include <cstring>
#include <algorithm>
#include <random>

SudokuLogic::SudokuLogic() {
    reset();
}

void SudokuLogic::reset() {
    std::memset(board, 0, sizeof(board));
    std::memset(fixed, 0, sizeof(fixed));
}

bool SudokuLogic::setValue(int row, int col, int value) {
    if (row < 0 || row >= 9 || col < 0 || col >= 9) return false;
    if (fixed[row][col]) return false;
    if (value != 0 && !isValid(row, col, value)) return false;
    board[row][col] = value;
    return true;
}

int SudokuLogic::getValue(int row, int col) const {
    if (row < 0 || row >= 9 || col < 0 || col >= 9) return 0;
    return board[row][col];
}

bool SudokuLogic::isFixed(int row, int col) const {
    if (row < 0 || row >= 9 || col < 0 || col >= 9) return false;
    return fixed[row][col];
}

bool SudokuLogic::isValid(int row, int col, int value) const {
    for (int i = 0; i < 9; ++i) {
        if (board[row][i] == value) return false;
        if (board[i][col] == value) return false;
    }
    int startRow = (row / 3) * 3;
    int startCol = (col / 3) * 3;
    for (int i = 0; i < 3; ++i) {
        for (int j = 0; j < 3; ++j) {
            if (board[startRow + i][startCol + j] == value) return false;
        }
    }
    return true;
}

void SudokuLogic::generateNewGame(int difficulty) {
    reset();
    solve(0, 0); // Generate a full board

    // Randomly remove numbers based on difficulty
    std::random_device rd;
    std::mt19937 g(rd());
    int cellsToRemove = 40 + difficulty * 10;

    std::vector<int> positions(81);
    for (int i = 0; i < 81; ++i) positions[i] = i;
    std::shuffle(positions.begin(), positions.end(), g);

    for (int i = 0; i < cellsToRemove; ++i) {
        int r = positions[i] / 9;
        int c = positions[i] % 9;
        board[r][c] = 0;
    }

    for (int r = 0; r < 9; ++r) {
        for (int c = 0; c < 9; ++c) {
            if (board[r][c] != 0) fixed[r][c] = true;
        }
    }
}

bool SudokuLogic::solve(int row, int col) {
    if (row == 9) return true;
    int nextRow = (col == 8) ? row + 1 : row;
    int nextCol = (col == 8) ? 0 : col + 1;

    if (board[row][col] != 0) return solve(nextRow, nextCol);

    std::vector<int> nums = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    std::random_device rd;
    std::mt19937 g(rd());
    std::shuffle(nums.begin(), nums.end(), g);

    for (int num : nums) {
        if (isValid(row, col, num)) {
            board[row][col] = num;
            if (solve(nextRow, nextCol)) return true;
            board[row][col] = 0;
        }
    }
    return false;
}
