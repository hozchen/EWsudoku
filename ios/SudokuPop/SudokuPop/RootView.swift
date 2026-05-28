import SwiftUI

struct RootView: View {
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("themeMode") private var themeMode = "auto"
    @AppStorage("language") private var languageRaw = AppLanguage.zhCN.rawValue
    @State private var game = SudokuGame()
    @State private var screen: Screen = .splash
    @State private var gameTime = 0
    @State private var scores = ScoreStore.load()

    private var language: AppLanguage {
        get { AppLanguage(rawValue: languageRaw) ?? .zhCN }
        nonmutating set { languageRaw = newValue.rawValue }
    }

    private var strings: GameStrings { translations[language] ?? translations[.zhCN]! }

    private var theme: SudokuTheme {
        if themeMode == SudokuTheme.modernPop.rawValue { return .modernPop }
        if themeMode == SudokuTheme.vibrantNight.rawValue { return .vibrantNight }
        return colorScheme == .dark ? .vibrantNight : .modernPop
    }

    var body: some View {
        ZStack {
            theme.bg.ignoresSafeArea()
            switch screen {
            case .splash:
                SplashScreen(theme: theme, strings: strings) { screen = .welcome }
            case .welcome:
                PlayfulBackground(theme: theme)
                WelcomeScreen(strings: strings, theme: theme, hasSavedGame: false) {
                    screen = .difficulty
                } onLeaderboard: {
                    scores = ScoreStore.load()
                    screen = .leaderboard
                } onSettings: {
                    screen = .settings
                }
            case .difficulty:
                PlayfulBackground(theme: theme)
                DifficultySelectScreen(strings: strings, theme: theme) { difficulty in
                    game.generateNewGame(difficulty)
                    gameTime = 0
                    screen = .game
                } onBack: {
                    screen = .welcome
                }
            case .game:
                SudokuGameScreen(strings: strings, game: game, theme: theme, time: $gameTime) {
                    if game.isSolved {
                        ScoreStore.saveWin(difficulty: game.currentDifficulty, seconds: gameTime)
                    }
                    scores = ScoreStore.load()
                    screen = .welcome
                }
            case .leaderboard:
                LeaderboardScreen(strings: strings, theme: theme, scores: scores) { screen = .welcome }
            case .settings:
                SettingsScreen(strings: strings, theme: theme, language: language, themeMode: themeMode) { newMode in
                    themeMode = newMode
                } onLanguageChange: { newLanguage in
                    language = newLanguage
                } onBack: {
                    screen = .welcome
                }
            }
        }
        .preferredColorScheme(theme.isDark ? .dark : .light)
    }
}

struct SplashScreen: View {
    let theme: SudokuTheme
    let strings: GameStrings
    let onFinish: () -> Void
    @State private var animate = false

    var body: some View {
        ZStack {
            Color(red: 0.067, green: 0.094, blue: 0.153).ignoresSafeArea()
            Canvas { context, size in
                context.fill(Path(ellipseIn: CGRect(x: -120, y: -80, width: size.width * 0.95, height: size.width * 0.95)), with: .color(theme.primary.opacity(0.92)))
                context.fill(Path(ellipseIn: CGRect(x: size.width * 0.48, y: size.height * 0.56, width: size.width * 0.95, height: size.width * 0.95)), with: .color(theme.secondary.opacity(0.88)))
                context.fill(Path(ellipseIn: CGRect(x: size.width * 0.78, y: size.height * 0.16, width: 68, height: 68)), with: .color(theme.accent.opacity(0.9)))
            }
            VStack(spacing: 24) {
                SudokuMark(theme: theme)
                Text(strings.appName)
                    .font(.system(size: 30, weight: .black))
                    .foregroundStyle(.white)
            }
            .scaleEffect(animate ? 1 : 0.72)
            .opacity(animate ? 1 : 0)
            .animation(.spring(response: 0.7, dampingFraction: 0.62), value: animate)
        }
        .task {
            animate = true
            try? await Task.sleep(for: .seconds(1.5))
            onFinish()
        }
    }
}

struct SudokuMark: View {
    let theme: SudokuTheme
    private let digits = ["1", "3", "5", "5", "7", "4", "2", "0", "9"]

    var body: some View {
        Grid(horizontalSpacing: 4, verticalSpacing: 4) {
            ForEach(0..<3, id: \.self) { row in
                GridRow {
                    ForEach(0..<3, id: \.self) { col in
                        let index = row * 3 + col
                        Text(digits[index])
                            .font(.system(size: 22, weight: .black))
                            .foregroundStyle(index.isMultiple(of: 2) ? theme.primary : theme.secondary)
                            .frame(width: 34, height: 34)
                            .background([Color(red: 0.91, green: 0.85, blue: 1), Color(red: 0.81, green: 0.97, blue: 0.91), Color(red: 1, green: 0.95, blue: 0.72)][index % 3])
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }
        }
        .padding(10)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color(red: 0.067, green: 0.094, blue: 0.153), lineWidth: 3))
    }
}

struct PlayfulBackground: View {
    let theme: SudokuTheme

    var body: some View {
        Canvas { context, size in
            context.fill(Path(ellipseIn: CGRect(x: -40, y: 80, width: 220, height: 220)), with: .color(theme.primary.opacity(0.12)))
            context.fill(Path(ellipseIn: CGRect(x: size.width - 210, y: size.height * 0.32, width: 300, height: 300)), with: .color(theme.secondary.opacity(0.15)))
            context.fill(Path(ellipseIn: CGRect(x: 40, y: size.height * 0.72, width: 180, height: 180)), with: .color(theme.accent.opacity(0.15)))
        }
        .ignoresSafeArea()
    }
}

struct WelcomeScreen: View {
    let strings: GameStrings
    let theme: SudokuTheme
    let hasSavedGame: Bool
    let onNewGame: () -> Void
    let onLeaderboard: () -> Void
    let onSettings: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Text(strings.appName)
                .font(.system(size: 60, weight: .black))
                .foregroundStyle(theme.textMain)
                .minimumScaleFactor(0.65)
            Spacer().frame(height: 56)
            PopButton(text: strings.newGame, color: theme.primary, textColor: .white, action: onNewGame)
            PopButton(text: strings.continueGame, color: theme.secondary, textColor: theme.textMain, enabled: hasSavedGame) {}
            PopButton(text: strings.leaderboard, color: theme.accent, textColor: theme.textMain, action: onLeaderboard)
            Button(action: onSettings) {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(theme.textMain.opacity(0.7))
                    .frame(width: 56, height: 56)
            }
            Spacer()
        }
        .padding(24)
    }
}

struct PopButton: View {
    let text: String
    let color: Color
    let textColor: Color
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(enabled ? textColor : textColor.opacity(0.3))
                .frame(maxWidth: 270, minHeight: 60)
                .background(enabled ? color : color.opacity(0.2))
                .clipShape(Capsule())
        }
        .disabled(!enabled)
    }
}

struct DifficultySelectScreen: View {
    let strings: GameStrings
    let theme: SudokuTheme
    let onDifficultySelected: (Difficulty) -> Void
    let onBack: () -> Void

    var body: some View {
        VStack {
            TopBackButton(strings: strings, theme: theme, onBack: onBack)
            Spacer()
            Text(strings.selectDifficulty)
                .font(.system(size: 38, weight: .black))
                .foregroundStyle(theme.textMain)
                .minimumScaleFactor(0.7)
            Spacer().frame(height: 42)
            ForEach(Difficulty.allCases) { difficulty in
                PopButton(text: label(for: difficulty), color: difficulty == .master ? theme.accent : theme.secondary, textColor: theme.textMain) {
                    onDifficultySelected(difficulty)
                }
            }
            Spacer()
        }
        .padding(16)
    }

    private func label(for difficulty: Difficulty) -> String {
        switch difficulty {
        case .beginner: strings.beginner
        case .normal: strings.normal
        case .hard: strings.hard
        case .pro: strings.pro
        case .master: strings.master
        }
    }
}

struct TopBackButton: View {
    let strings: GameStrings
    let theme: SudokuTheme
    let onBack: () -> Void

    var body: some View {
        HStack {
            Button(action: onBack) {
                Label(strings.back, systemImage: "chevron.left")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(theme.textMain)
            }
            Spacer()
        }
    }
}

func formatTime(_ seconds: Int) -> String {
    let safe = max(0, seconds)
    return String(format: "%02d:%02d", safe / 60, safe % 60)
}
