import SwiftUI

struct SudokuGameScreen: View {
    let strings: GameStrings
    @Bindable var game: SudokuGame
    let theme: SudokuTheme
    @Binding var time: Int
    let onBack: () -> Void

    @State private var selectedCell: (Int, Int)?
    @State private var isNoteMode = false
    @State private var showExitConfirm = false
    @State private var hintMessage: String?

    var body: some View {
        GeometryReader { proxy in
            let boardSize = min(proxy.size.width - 32, proxy.size.height - 330)
            VStack(spacing: 14) {
                HStack {
                    Button {
                        showExitConfirm = true
                    } label: {
                        Label(strings.close, systemImage: "chevron.left")
                            .font(.system(size: 14, weight: .bold))
                    }
                    .foregroundStyle(theme.textMain.opacity(0.65))
                    Spacer()
                    Text(formatTime(time))
                        .font(.system(size: 22, weight: .black))
                        .foregroundStyle(theme.textMain)
                    Spacer()
                    Text("\(strings.errors): \(game.errorCount)/\(game.currentDifficulty.maxErrors)")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(game.errorCount > 0 ? theme.error : theme.textMain.opacity(0.42))
                }

                SudokuBoardView(game: game, theme: theme, selectedCell: selectedCell, boardSize: boardSize) { row, col in
                    selectedCell = (row, col)
                }
                .padding(8)
                .background(theme.cardBg)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .shadow(color: .black.opacity(theme.isDark ? 0 : 0.12), radius: 8, y: 4)

                Spacer(minLength: 4)

                HStack {
                    ToolButton(systemName: "arrow.uturn.backward", label: strings.undo, color: theme.cardBg, tint: theme.secondary, action: game.undo)
                    ToolButton(systemName: "square.grid.3x3", label: strings.autoNotes, color: theme.accent, tint: .black, action: game.fillAllNotes)
                    ToolButton(systemName: "lightbulb.fill", label: strings.hint, color: theme.accent, tint: .black) {
                        let hint = game.useHint(selectedCell: selectedCell)
                        hintMessage = hint.map { "\($0.technique): 第\($0.row + 1)行 第\($0.col + 1)列 = \($0.value)" }
                            ?? "提示次数已用完 (\(game.currentDifficulty.maxHints))"
                    }
                    ToolButton(systemName: "delete.left.fill", label: strings.erase, color: theme.cardBg, tint: theme.textMain) {
                        guard let selectedCell else { return }
                        game.onCellInput(row: selectedCell.0, col: selectedCell.1, value: 0, isNoteMode: false)
                    }
                    ToolButton(systemName: "paintbrush.fill", label: isNoteMode ? strings.noteOn : strings.noteOff, color: isNoteMode ? theme.primary : theme.cardBg, tint: isNoteMode ? .white : theme.textMain) {
                        isNoteMode.toggle()
                    }
                }
                .padding(.horizontal, 2)

                NumberPad(game: game, theme: theme, isNoteMode: isNoteMode, selectedCell: selectedCell)
            }
            .padding(16)
        }
        .onAppear {
            selectedCell = findFirstPlayableCell()
        }
        .task {
            while !game.isSolved && !game.isGameOver {
                try? await Task.sleep(for: .seconds(1))
                if !game.isSolved && !game.isGameOver { time += 1 }
            }
        }
        .alert(strings.quitTitle, isPresented: $showExitConfirm) {
            Button(strings.cancel, role: .cancel) {}
            Button(strings.quitConfirm, role: .destructive, action: onBack)
        } message: {
            Text(strings.quitDesc)
        }
        .alert(game.isSolved ? strings.victory : strings.gameOver, isPresented: .constant(game.isSolved || game.isGameOver)) {
            Button("OK", action: onBack)
        } message: {
            Text("\(strings.timeLabel): \(formatTime(time))")
        }
        .alert("Hint", isPresented: Binding(get: { hintMessage != nil }, set: { if !$0 { hintMessage = nil } })) {
            Button("OK") { hintMessage = nil }
        } message: {
            Text(hintMessage ?? "")
        }
    }

    private func findFirstPlayableCell() -> (Int, Int)? {
        for row in 0..<gridSize {
            for col in 0..<gridSize where !game.initialBoard[row][col] && game.board[row][col] == 0 {
                return (row, col)
            }
        }
        return nil
    }
}

struct SudokuBoardView: View {
    let game: SudokuGame
    let theme: SudokuTheme
    let selectedCell: (Int, Int)?
    let boardSize: CGFloat
    let onCellTap: (Int, Int) -> Void

    var body: some View {
        let cellSize = boardSize / CGFloat(gridSize)
        ZStack {
            VStack(spacing: 0) {
                ForEach(0..<gridSize, id: \.self) { row in
                    HStack(spacing: 0) {
                        ForEach(0..<gridSize, id: \.self) { col in
                            SudokuCellView(
                                value: game.board[row][col],
                                notes: game.notesFor(row: row, col: col),
                                isInitial: game.initialBoard[row][col],
                                isSelected: selectedCell?.0 == row && selectedCell?.1 == col,
                                isSameGroup: sameGroup(row: row, col: col),
                                isSameValue: sameValue(row: row, col: col),
                                isError: game.hasError(row: row, col: col),
                                size: cellSize,
                                theme: theme
                            )
                            .onTapGesture { onCellTap(row, col) }
                        }
                    }
                }
            }
            GridLines(theme: theme)
        }
        .frame(width: boardSize, height: boardSize)
    }

    private func sameGroup(row: Int, col: Int) -> Bool {
        guard let selectedCell else { return false }
        return row == selectedCell.0 || col == selectedCell.1 || (row / 3 == selectedCell.0 / 3 && col / 3 == selectedCell.1 / 3)
    }

    private func sameValue(row: Int, col: Int) -> Bool {
        guard let selectedCell else { return false }
        let value = game.board[selectedCell.0][selectedCell.1]
        return value != 0 && game.board[row][col] == value
    }
}

struct SudokuCellView: View {
    let value: Int
    let notes: Set<Int>
    let isInitial: Bool
    let isSelected: Bool
    let isSameGroup: Bool
    let isSameValue: Bool
    let isError: Bool
    let size: CGFloat
    let theme: SudokuTheme

    var body: some View {
        ZStack {
            backgroundColor
            if value != 0 {
                Text("\(value)")
                    .font(.system(size: size * 0.58, weight: .heavy))
                    .foregroundStyle(numberColor)
            } else if !notes.isEmpty {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 3), spacing: 0) {
                    ForEach(1...9, id: \.self) { note in
                        Text(notes.contains(note) ? "\(note)" : "")
                            .font(.system(size: max(7, size * 0.17), weight: .bold))
                            .foregroundStyle(isSelected ? .white.opacity(0.72) : theme.textMain.opacity(0.46))
                            .frame(width: size / 3, height: size / 3)
                    }
                }
                .padding(3)
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .padding(0.5)
        .contentShape(Rectangle())
    }

    private var backgroundColor: Color {
        if isSelected { return theme.primary }
        if isSameValue { return theme.accent.opacity(0.6) }
        if isSameGroup { return theme.secondary.opacity(0.15) }
        return theme.bg.opacity(0.3)
    }

    private var numberColor: Color {
        if isError { return theme.error }
        if isSelected { return .white }
        if isInitial { return theme.textMain }
        return theme.primary
    }
}

struct GridLines: View {
    let theme: SudokuTheme

    var body: some View {
        GeometryReader { proxy in
            Path { path in
                let cell = proxy.size.width / 9
                for i in 1...2 {
                    let pos = cell * CGFloat(i * 3)
                    path.move(to: CGPoint(x: pos, y: 0))
                    path.addLine(to: CGPoint(x: pos, y: proxy.size.height))
                    path.move(to: CGPoint(x: 0, y: pos))
                    path.addLine(to: CGPoint(x: proxy.size.width, y: pos))
                }
            }
            .stroke(theme.textMain.opacity(theme.isDark ? 0.15 : 0.25), lineWidth: theme.isDark ? 1.5 : 2)
        }
        .allowsHitTesting(false)
    }
}

struct ToolButton: View {
    let systemName: String
    let label: String
    let color: Color
    let tint: Color
    let action: () -> Void

    var body: some View {
        VStack(spacing: 4) {
            Button(action: action) {
                Image(systemName: systemName)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(tint)
                    .frame(width: 54, height: 54)
                    .background(color)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(.white.opacity(0.1), lineWidth: 1))
            }
            Text(label)
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(tint.opacity(0.75))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity)
    }
}

struct NumberPad: View {
    let game: SudokuGame
    let theme: SudokuTheme
    let isNoteMode: Bool
    let selectedCell: (Int, Int)?

    var body: some View {
        let remaining = game.remainingCounts()
        HStack(spacing: 7) {
            ForEach(1...9, id: \.self) { value in
                let count = remaining[value, default: 0]
                let isFinished = count == 0 && !isNoteMode
                VStack(spacing: 3) {
                    Text(count > 0 ? "\(count)" : "•")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(theme.textMain.opacity(0.4))
                    Button {
                        guard let selectedCell else { return }
                        game.onCellInput(row: selectedCell.0, col: selectedCell.1, value: value, isNoteMode: isNoteMode)
                    } label: {
                        Text("\(value)")
                            .font(.system(size: 20, weight: .black))
                            .foregroundStyle(isFinished ? theme.textMain.opacity(0.15) : isNoteMode ? theme.textMain : .white)
                            .frame(maxWidth: .infinity, minHeight: 62)
                            .background(isFinished ? theme.bg.opacity(0.4) : isNoteMode ? theme.accent : theme.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .disabled(isFinished)
                }
            }
        }
        .padding(.bottom, 8)
    }
}
