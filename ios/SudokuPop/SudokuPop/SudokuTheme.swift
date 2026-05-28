import SwiftUI

enum Screen {
    case splash
    case welcome
    case difficulty
    case game
    case leaderboard
    case settings
}

enum AppLanguage: String, CaseIterable, Identifiable, Codable {
    case zhCN
    case zhTW
    case en
    case ja

    var id: String { rawValue }

    var label: String {
        switch self {
        case .zhCN: "简体中文"
        case .zhTW: "繁體中文"
        case .en: "English"
        case .ja: "日本語"
        }
    }
}

struct GameStrings {
    let appName: String
    let subtitle: String
    let newGame: String
    let continueGame: String
    let leaderboard: String
    let settings: String
    let selectDifficulty: String
    let cancel: String
    let close: String
    let errors: String
    let undo: String
    let hint: String
    let noteOn: String
    let noteOff: String
    let erase: String
    let quitTitle: String
    let quitDesc: String
    let quitConfirm: String
    let victory: String
    let gameOver: String
    let timeLabel: String
    let noRecords: String
    let back: String
    let done: String
    let themeLabel: String
    let langLabel: String
    let autoTheme: String
    let best: String
    let master: String
    let pro: String
    let hard: String
    let normal: String
    let beginner: String
    let autoNotes: String
}

let translations: [AppLanguage: GameStrings] = [
    .zhCN: GameStrings(appName: "Sudoku Pop", subtitle: "波普潮流版", newGame: "新游戏", continueGame: "继续游戏", leaderboard: "积分榜", settings: "设置", selectDifficulty: "选择难度", cancel: "取消", close: "关闭", errors: "错误", undo: "撤销", hint: "提示", noteOn: "笔记:开", noteOff: "笔记:关", erase: "擦除", quitTitle: "退出游戏?", quitDesc: "确定要退出吗? 进度将自动保存。", quitConfirm: "退出", victory: "挑战成功!", gameOver: "游戏结束", timeLabel: "用时", noRecords: "暂无数据", back: "返回", done: "确定", themeLabel: "主题选择", langLabel: "语言选择", autoTheme: "跟随系统", best: "最高纪录", master: "大师", pro: "专业", hard: "困难", normal: "普通", beginner: "入门者", autoNotes: "全开"),
    .zhTW: GameStrings(appName: "Sudoku Pop", subtitle: "波普潮流版", newGame: "新遊戲", continueGame: "再開遊戲", leaderboard: "積分榜", settings: "設置", selectDifficulty: "選擇難度", cancel: "取消", close: "關閉", errors: "錯誤", undo: "撤銷", hint: "提示", noteOn: "筆記:開", noteOff: "筆記:關", erase: "擦除", quitTitle: "退出遊戲?", quitDesc: "確定要退出嗎? 進度將自動保存。", quitConfirm: "退出", victory: "挑戰成功!", gameOver: "遊戲結束", timeLabel: "用時", noRecords: "暫無數據", back: "返回", done: "確定", themeLabel: "主題選擇", langLabel: "語言選擇", autoTheme: "跟隨系統", best: "最高紀錄", master: "大師", pro: "專業", hard: "困難", normal: "普通", beginner: "入門者", autoNotes: "全開"),
    .en: GameStrings(appName: "Sudoku Pop", subtitle: "Playful Edition", newGame: "New Game", continueGame: "Continue", leaderboard: "Leaderboard", settings: "Settings", selectDifficulty: "Difficulty", cancel: "Cancel", close: "Close", errors: "Errors", undo: "Undo", hint: "Hint", noteOn: "Note:ON", noteOff: "Note:OFF", erase: "Erase", quitTitle: "Quit Game?", quitDesc: "Are you sure? Progress will be saved.", quitConfirm: "Quit", victory: "Victory!", gameOver: "Game Over", timeLabel: "Time", noRecords: "No Records", back: "Back", done: "Done", themeLabel: "Themes", langLabel: "Language", autoTheme: "Follow System", best: "Best", master: "Master", pro: "Pro", hard: "Hard", normal: "Normal", beginner: "Beginner", autoNotes: "All Notes"),
    .ja: GameStrings(appName: "Sudoku Pop", subtitle: "ポップ版", newGame: "新規ゲーム", continueGame: "再開する", leaderboard: "リーダーボード", settings: "設定", selectDifficulty: "難易度を選択", cancel: "キャンセル", close: "閉じる", errors: "ミス", undo: "元に戻す", hint: "ヒント", noteOn: "メモ:オン", noteOff: "メモ:オフ", erase: "消しゴム", quitTitle: "終了しますか？", quitDesc: "ゲームを終了しますか？進捗は保存されます。", quitConfirm: "終了", victory: "完全勝利！", gameOver: "ゲームオーバー", timeLabel: "時間", noRecords: "記録なし", back: "戻る", done: "決定", themeLabel: "テーマ", langLabel: "言語", autoTheme: "システムに従う", best: "ベスト記録", master: "達人", pro: "プロ", hard: "難しい", normal: "普通", beginner: "初級", autoNotes: "全メモ")
]

enum SudokuTheme: String, CaseIterable, Identifiable {
    case modernPop
    case vibrantNight

    var id: String { rawValue }
    var label: String { self == .modernPop ? "潮流波普" : "赛博霓虹" }
    var isDark: Bool { self == .vibrantNight }
    var bg: Color { self == .modernPop ? Color(red: 0.953, green: 0.973, blue: 0.949) : Color(red: 0.043, green: 0.055, blue: 0.078) }
    var primary: Color { self == .modernPop ? Color(red: 0.494, green: 0.227, blue: 0.949) : Color(red: 1.0, green: 0.18, blue: 0.388) }
    var secondary: Color { self == .modernPop ? Color(red: 0.192, green: 0.769, blue: 0.553) : Color(red: 0.031, green: 0.851, blue: 0.839) }
    var accent: Color { self == .modernPop ? Color(red: 0.98, green: 0.792, blue: 0.082) : Color(red: 1.0, green: 0.827, blue: 0.412) }
    var textMain: Color { self == .modernPop ? Color(red: 0.067, green: 0.098, blue: 0.157) : Color(red: 0.976, green: 0.98, blue: 0.984) }
    var cardBg: Color { self == .modernPop ? .white : Color(red: 0.122, green: 0.161, blue: 0.216) }
    var error: Color { self == .modernPop ? Color(red: 0.878, green: 0.141, blue: 0.141) : Color(red: 0.973, green: 0.443, blue: 0.443) }
}
