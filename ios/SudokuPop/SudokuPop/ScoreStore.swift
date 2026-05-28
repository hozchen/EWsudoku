import Foundation

struct ScoreEntry: Codable, Identifiable {
    let id: UUID
    let seconds: Int
    let date: String

    init(seconds: Int, date: String) {
        self.id = UUID()
        self.seconds = seconds
        self.date = date
    }
}

struct ScoreStore {
    private static let key = "scores"

    static func load() -> [Difficulty: [ScoreEntry]] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let raw = try? JSONDecoder().decode([String: [ScoreEntry]].self, from: data) else {
            return [:]
        }
        return Dictionary(uniqueKeysWithValues: raw.compactMap { key, value in
            guard let difficulty = Difficulty(rawValue: key) else { return nil }
            return (difficulty, value)
        })
    }

    static func saveWin(difficulty: Difficulty, seconds: Int) {
        var scores = load()
        let date = DateFormatter.scoreDate.string(from: Date())
        var entries = scores[difficulty, default: []]
        entries.append(ScoreEntry(seconds: seconds, date: date))
        scores[difficulty] = Array(entries.sorted { $0.seconds < $1.seconds }.prefix(10))
        save(scores)
    }

    private static func save(_ scores: [Difficulty: [ScoreEntry]]) {
        let raw = Dictionary(uniqueKeysWithValues: scores.map { ($0.key.rawValue, $0.value) })
        if let data = try? JSONEncoder().encode(raw) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}

private extension DateFormatter {
    static let scoreDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}
