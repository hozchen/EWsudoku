#ifndef SUDOKU_LOGIC_H
#define SUDOKU_LOGIC_H

#include <vector>

class SudokuLogic {
public:
    SudokuLogic();
    void reset();
    bool setValue(int row, int col, int value);
    int getValue(int row, int col) const;
    bool isFixed(int row, int col) const;
    void generateNewGame(int difficulty); // 0: Easy, 1: Medium, 2: Hard

private:
    int board[9][9];
    bool fixed[9][9];

    bool isValid(int row, int col, int value) const;
    bool solve(int row, int col);
};

#endif // SUDOKU_LOGIC_H
