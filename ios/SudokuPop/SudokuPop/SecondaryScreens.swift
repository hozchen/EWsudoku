import SwiftUI

struct SettingsScreen: View {
    let strings: GameStrings
    let theme: SudokuTheme
    let language: AppLanguage
    let themeMode: String
    let onThemeModeChange: (String) -> Void
    let onLanguageChange: (AppLanguage) -> Void
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            TopBackButton(strings: strings, theme: theme, onBack: onBack)
            Text(strings.settings)
                .font(.system(size: 42, weight: .black))
                .foregroundStyle(theme.textMain)
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text(strings.themeLabel)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(theme.textMain)
                        .padding(.top, 8)
                    SettingRow(title: strings.autoTheme, selected: themeMode == "auto", theme: theme) { onThemeModeChange("auto") }
                    ForEach(SudokuTheme.allCases) { item in
                        SettingRow(title: item.label, selected: themeMode == item.rawValue, theme: theme) { onThemeModeChange(item.rawValue) }
                    }

                    Text(strings.langLabel)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(theme.textMain)
                        .padding(.top, 20)
                    ForEach(AppLanguage.allCases) { item in
                        SettingRow(title: item.label, selected: language == item, theme: theme) { onLanguageChange(item) }
                    }

                    Text("Version 1.0")
                        .font(.system(size: 12))
                        .foregroundStyle(theme.textMain.opacity(0.22))
                        .padding(.top, 18)
                }
            }
        }
        .padding(16)
    }
}

struct SettingRow: View {
    let title: String
    let selected: Bool
    let theme: SudokuTheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(selected ? theme.primary : theme.textMain.opacity(0.45))
                Text(title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(theme.textMain)
                Spacer()
            }
            .padding(.vertical, 12)
        }
    }
}

struct LeaderboardScreen: View {
    let strings: GameStrings
    let theme: SudokuTheme
    let scores: [Difficulty: [ScoreEntry]]
    let onBack: () -> Void
    @State private var selectedDifficulty: Difficulty = .normal

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            TopBackButton(strings: strings, theme: theme, onBack: onBack)
            Text(strings.best)
                .font(.system(size: 42, weight: .black))
                .foregroundStyle(theme.textMain)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Difficulty.allCases) { difficulty in
                        Button(label(for: difficulty)) {
                            selectedDifficulty = difficulty
                        }
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(selectedDifficulty == difficulty ? .white : theme.primary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(selectedDifficulty == difficulty ? theme.primary : theme.cardBg)
                        .clipShape(Capsule())
                    }
                }
                .padding(.vertical, 4)
            }

            VStack(spacing: 0) {
                let entries = scores[selectedDifficulty, default: []]
                if entries.isEmpty {
                    Spacer()
                    Text(strings.noRecords)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(theme.textMain.opacity(0.25))
                    Spacer()
                } else {
                    ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                        HStack {
                            Text("\(index + 1). \(formatTime(entry.seconds))")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(theme.textMain)
                            Spacer()
                            Text(entry.date)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(theme.textMain.opacity(0.45))
                        }
                        .padding(.vertical, 14)
                        if index != entries.count - 1 {
                            Divider().opacity(0.25)
                        }
                    }
                    Spacer()
                }
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(theme.cardBg)
            .clipShape(RoundedRectangle(cornerRadius: 28))
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
