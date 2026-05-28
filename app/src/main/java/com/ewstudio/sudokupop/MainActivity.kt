package com.ewstudio.sudokupop

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class Screen {
    SPLASH, WELCOME, DIFFICULTY_SELECT, GAME, LEADERBOARD, SETTINGS
}

enum class Language(val label: String) {
    ZH_CN("简体中文"), ZH_TW("繁體中文"), EN("English"), JA("日本語")
}

data class GameStrings(
    val appName: String, val subtitle: String, val newGame: String, val continueGame: String,
    val dailyChallenge: String, val leaderboard: String, val settings: String, val selectDifficulty: String,
    val cancel: String, val close: String, val errors: String, val undo: String,
    val hint: String, val noteOn: String, val noteOff: String, val erase: String,
    val quitTitle: String, val quitDesc: String, val quitConfirm: String,
    val victory: String, val gameOver: String, val timeLabel: String, val errorMax: String,
    val noRecords: String, val back: String, val done: String, val themeLabel: String,
    val langLabel: String, val autoTheme: String, val best: String, val master: String, val pro: String,
    val hard: String, val normal: String, val beginner: String, val autoNotes: String,
    val streakLabel: String, val dailyDone: String,
)

data class DailyChallengeProgress(
    val streak: Int,
    val completedToday: Boolean,
)

data class DifficultyStats(
    val attempts: Int,
    val wins: Int,
    val totalTime: Long,
    val totalErrors: Int,
    val totalHints: Int,
)

val Translations = mapOf(
    Language.ZH_CN to GameStrings("Sudoku Pop", "波普潮流版", "新游戏", "继续游戏", "每日挑战", "积分榜", "设置", "选择难度", "取消", "关闭", "错误", "撤销", "提示", "笔记:开", "笔记:关", "擦除", "退出游戏?", "确定要退出吗? 进度将自动保存。", "退出", "挑战成功!", "游戏结束", "用时", "已错3次", "暂无数据", "返回", "确定", "主题选择", "语言选择", "跟随系统", "最高纪录", "大师", "专业", "困难", "普通", "入门者", "全开", "连续", "今日已完成"),
    Language.ZH_TW to GameStrings("Sudoku Pop", "波普潮流版", "新遊戲", "繼續遊戲", "每日挑戰", "積分榜", "設置", "選擇難度", "取消", "關閉", "錯誤", "撤銷", "提示", "筆記:開", "筆記:關", "擦除", "退出遊戲?", "確定要退出嗎? 進度將自動保存。", "退出", "挑戰成功!", "遊戲結束", "用時", "已錯3次", "暫無數據", "返回", "確定", "主題選擇", "語言選擇", "跟隨系統", "最高紀錄", "大師", "專業", "困難", "普通", "入門者", "全開", "連續", "今日已完成"),
    Language.EN to GameStrings("Sudoku Pop", "Playful Edition", "New Game", "Continue", "Daily Challenge", "Leaderboard", "Settings", "Difficulty", "Cancel", "CLOSE", "ERRORS", "Undo", "Hint", "Note:ON", "Note:OFF", "Erase", "Quit Game?", "Are you sure? Progress will be saved.", "QUIT", "Victory!", "Game Over", "Time", "3 errors", "No Records", "Back", "DONE", "Themes", "Language", "Follow System", "BEST", "Master", "Pro", "Hard", "Normal", "Beginner", "All Notes", "Streak", "Done today"),
    Language.JA to GameStrings("Sudoku Pop", "ポップ版", "新規ゲーム", "再開する", "デイリーチャレンジ", "リーダーボード", "設定", "難易度を選択", "キャンセル", "閉じる", "ミス", "元に戻す", "ヒント", "メモ:オン", "メモ:オフ", "消しゴム", "終了しますか？", "ゲームを終了しますか？進捗は保存されます。", "終了", "完全勝利！", "ゲームオーバー", "時間", "3回ミス", "記録なし", "戻る", "決定", "テーマ", "言語", "システムに従う", "ベスト記録", "達人", "プロ", "難しい", "普通", "初級", "全メモ", "連続", "本日完了")
)

enum class SudokuTheme(val label: String, val bg: Color, val primary: Color, val secondary: Color, val accent: Color, val textMain: Color, val cardBg: Color, val error: Color, val isDark: Boolean) {
    MODERN_POP("潮流波普", Color(0xFFF3F8F2), Color(0xFF7E3AF2), Color(0xFF31C48D), Color(0xFFFACA15), Color(0xFF111928), Color(0xFFFFFFFF), Color(0xFFE02424), false),
    VIBRANT_NIGHT("赛博霓虹", Color(0xFF0B0E14), Color(0xFFFF2E63), Color(0xFF08D9D6), Color(0xFFFFD369), Color(0xFFF9FAFB), Color(0xFF1F2937), Color(0xFFF87171), true)
}

class MainActivity : ComponentActivity() {
    private lateinit var soundPool: SoundPool
    private var correctSoundId: Int = 0
    private var errorSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("sudoku_prefs", MODE_PRIVATE)
        
        // 1. 初始化 SoundPool 时增加资源检查
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        soundPool = SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build()
        correctSoundId = loadSound("correct"); errorSoundId = loadSound("error")

        setContent {
            // 使用 ViewModel 替代直接持有 Class 实例
            val game: SudokuGame = viewModel()
            val savedMode = prefs.getString("theme_mode", "auto") ?: "auto"
            var themeMode by remember { mutableStateOf(savedMode) }
            val isDark = isSystemInDarkTheme()
            val currentTheme by remember(themeMode, isDark) {
                derivedStateOf {
                    when (themeMode) {
                        "MODERN_POP" -> SudokuTheme.MODERN_POP
                        "VIBRANT_NIGHT" -> SudokuTheme.VIBRANT_NIGHT
                        else -> if (isDark) SudokuTheme.VIBRANT_NIGHT else SudokuTheme.MODERN_POP
                    }
                }
            }
            var currentLang by remember { mutableStateOf(Language.valueOf(prefs.getString("lang", "ZH_CN") ?: "ZH_CN")) }
            var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
            var gameTime by remember { mutableLongStateOf(0L) }
            var isDailyChallenge by remember { mutableStateOf(false) }
            var dailyProgress by remember { mutableStateOf(loadDailyChallengeProgress(prefs)) }
            val s = Translations[currentLang]!!

            LaunchedEffect(currentLang) { prefs.edit().putString("lang", currentLang.name).apply() }
            LaunchedEffect(themeMode) { prefs.edit().putString("theme_mode", themeMode).apply() }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = currentTheme.bg) {
                    when (currentScreen) {
                        Screen.SPLASH -> SplashScreen(currentTheme, s) { currentScreen = Screen.WELCOME }
                        Screen.WELCOME -> {
                            PlayfulBackground(currentTheme)
                            WelcomeScreen(
                                s,
                                currentTheme,
                                prefs.getBoolean("has_saved_game", false),
                                dailyProgress,
                                { currentScreen = Screen.DIFFICULTY_SELECT },
                                {
                                    game.generateDailyChallenge(todayKey())
                                    gameTime = 0L
                                    isDailyChallenge = true
                                    currentScreen = Screen.GAME
                                },
                                {
                                    gameTime = game.loadProgress(prefs)
                                    isDailyChallenge = false
                                    currentScreen = Screen.GAME
                                },
                                { currentScreen = Screen.LEADERBOARD },
                                { currentScreen = Screen.SETTINGS },
                            )
                        }
                        Screen.DIFFICULTY_SELECT -> {
                            PlayfulBackground(currentTheme)
                            DifficultySelectScreen(s, currentTheme, { diff -> game.generateNewGame(diff); gameTime = 0L; isDailyChallenge = false; currentScreen = Screen.GAME }, { currentScreen = Screen.WELCOME })
                        }
                        Screen.GAME -> SudokuScreen(s, game, currentTheme, gameTime, { gameTime++; if (gameTime % 5 == 0L) game.saveProgress(prefs, gameTime) }, {
                            if (game.isSolved) {
                                saveScore(prefs, game.currentDifficulty, gameTime)
                                saveGameStats(prefs, game.currentDifficulty, true, gameTime, game.errorCount, game.hintsUsed)
                                if (isDailyChallenge) {
                                    saveDailyChallengeWin(prefs)
                                    dailyProgress = loadDailyChallengeProgress(prefs)
                                }
                                prefs.edit().putBoolean("has_saved_game", false).apply()
                            } else if (game.isGameOver) {
                                saveGameStats(prefs, game.currentDifficulty, false, gameTime, game.errorCount, game.hintsUsed)
                                prefs.edit().putBoolean("has_saved_game", false).apply()
                            }
                            isDailyChallenge = false
                            currentScreen = Screen.WELCOME
                        }, { soundPool.play(correctSoundId, 1f, 1f, 1, 0, 1f) }, { soundPool.play(errorSoundId, 1f, 1f, 1, 0, 1f) })
                        Screen.LEADERBOARD -> LeaderboardScreen(s, currentTheme, loadAllScores(prefs), loadAllStats(prefs), { currentScreen = Screen.WELCOME })
                        Screen.SETTINGS -> SettingsScreen(s, currentTheme, currentLang, themeMode, { themeMode = it }, { currentLang = it }, { currentScreen = Screen.WELCOME })
                    }
                }
            }
        }
    }

    private fun loadSound(resName: String): Int {
        val resId = resources.getIdentifier(resName, "raw", packageName)
        // 增加容错：如果 resId 为 0（即 raw 文件夹或文件不存在），直接返回 0，不再尝试 load
        return if (resId != 0) soundPool.load(this, resId, 1) else 0
    }
    
    private fun saveScore(prefs: android.content.SharedPreferences, difficulty: Difficulty, time: Long) {
        val key = "scores_${difficulty.name}"
        val existing = prefs.getString(key, "") ?: ""
        val scores = if (existing.isEmpty()) mutableListOf<Pair<Long, String>>() else existing.split("|").mapNotNull { 
            val parts = it.split(",")
            if (parts.size >= 2) parts[0].toLong() to parts[1] else null
        }.toMutableList()
        scores.add(time to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        val saveStr = scores.sortedBy { it.first }.take(10).joinToString("|") { "${it.first},${it.second}" }
        prefs.edit().putString(key, saveStr).apply()
    }
    
    private fun loadAllScores(prefs: android.content.SharedPreferences): Map<Difficulty, List<Pair<Long, String>>> {
        return Difficulty.entries.associateWith { diff ->
            val data = prefs.getString("scores_${diff.name}", "") ?: ""
            if (data.isEmpty()) emptyList() else data.split("|").mapNotNull { 
                val parts = it.split(",")
                if (parts.size >= 2) parts[0].toLong() to parts[1] else null
            }
        }
    }

    private fun saveGameStats(prefs: android.content.SharedPreferences, difficulty: Difficulty, won: Boolean, time: Long, errors: Int, hints: Int) {
        val prefix = "stats_${difficulty.name}"
        prefs.edit().apply {
            putInt("${prefix}_attempts", prefs.getInt("${prefix}_attempts", 0) + 1)
            putInt("${prefix}_wins", prefs.getInt("${prefix}_wins", 0) + if (won) 1 else 0)
            putLong("${prefix}_time", prefs.getLong("${prefix}_time", 0L) + time)
            putInt("${prefix}_errors", prefs.getInt("${prefix}_errors", 0) + errors)
            putInt("${prefix}_hints", prefs.getInt("${prefix}_hints", 0) + hints)
            apply()
        }
    }

    private fun loadAllStats(prefs: android.content.SharedPreferences): Map<Difficulty, DifficultyStats> {
        return Difficulty.entries.associateWith { diff ->
            val prefix = "stats_${diff.name}"
            DifficultyStats(
                attempts = prefs.getInt("${prefix}_attempts", 0),
                wins = prefs.getInt("${prefix}_wins", 0),
                totalTime = prefs.getLong("${prefix}_time", 0L),
                totalErrors = prefs.getInt("${prefix}_errors", 0),
                totalHints = prefs.getInt("${prefix}_hints", 0),
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }
}

private fun todayKey(): String {
    return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
}

private fun loadDailyChallengeProgress(prefs: android.content.SharedPreferences): DailyChallengeProgress {
    val today = todayKey()
    val lastCompleted = prefs.getString("daily_last_completed", null)
    return DailyChallengeProgress(
        streak = prefs.getInt("daily_streak", 0),
        completedToday = DailyChallengeTracker.isCompletedToday(lastCompleted, today),
    )
}

private fun saveDailyChallengeWin(prefs: android.content.SharedPreferences) {
    val today = todayKey()
    val lastCompleted = prefs.getString("daily_last_completed", null)
    val currentStreak = prefs.getInt("daily_streak", 0)
    val nextStreak = DailyChallengeTracker.nextStreak(lastCompleted, today, currentStreak)
    prefs.edit()
        .putString("daily_last_completed", today)
        .putInt("daily_streak", nextStreak)
        .apply()
}

@Composable
fun SplashScreen(theme: SudokuTheme, s: GameStrings, onFinish: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    // 10. 处理 Compose 动画警告，添加 label
    val scale by animateFloatAsState(targetValue = if (startAnimation) 1f else 0.72f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "splash_scale")
    val alpha by animateFloatAsState(targetValue = if (startAnimation) 1f else 0f, animationSpec = tween(650), label = "splash_alpha")
    LaunchedEffect(Unit) { startAnimation = true; delay(2000L); onFinish() }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111827)), contentAlignment = Alignment.Center) {
        SplashPopBackground(theme)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
            SplashSudokuMark(theme)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = s.appName, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = alpha))
        }
    }
}

@Composable
private fun SplashPopBackground(theme: SudokuTheme) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = theme.primary.copy(alpha = 0.92f), radius = size.width * 0.48f, center = Offset(size.width * 0.15f, size.height * 0.12f))
        drawCircle(color = theme.secondary.copy(alpha = 0.88f), radius = size.width * 0.56f, center = Offset(size.width * 1.02f, size.height * 0.86f))
        drawCircle(color = theme.accent.copy(alpha = 0.9f), radius = 34f, center = Offset(size.width * 0.82f, size.height * 0.18f))
        drawCircle(color = Color(0xFFF43F5E).copy(alpha = 0.85f), radius = 26f, center = Offset(size.width * 0.18f, size.height * 0.82f))
    }
}

@Composable
private fun SplashSudokuMark(theme: SudokuTheme) {
    val tileColors = listOf(
        Color(0xFFE8D8FF), Color(0xFFF3F8F2), Color(0xFFCFF7E7),
        Color(0xFFFFF3B8), Color(0xFFE8D8FF), Color(0xFFFFF3B8),
        Color(0xFFCFF7E7), Color(0xFFF3F8F2), Color(0xFFE8D8FF)
    )
    val digitColors = listOf(theme.primary, Color(0xFF111827), theme.secondary, Color(0xFFF43F5E), theme.primary, Color(0xFF111827), theme.accent, theme.secondary, theme.primary)
    val digits = listOf("1", "3", "5", "5", "7", "4", "2", "0", "9")

    Column(
        modifier = Modifier
            .size(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(3.dp, Color(0xFF111827), RoundedCornerShape(24.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in 0 until 3) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(tileColors[index]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(digits[index], color = digitColors[index], fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable fun PlayfulBackground(theme: SudokuTheme) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = theme.primary.copy(alpha = 0.12f), radius = 120f, center = center.copy(x = 80f, y = 150f))
        drawCircle(color = theme.secondary.copy(alpha = 0.15f), radius = 180f, center = center.copy(x = size.width - 50f, y = size.height * 0.4f))
        drawCircle(color = theme.accent.copy(alpha = 0.15f), radius = 100f, center = center.copy(x = size.width * 0.2f, y = size.height * 0.8f))
    }
}

@Composable fun WelcomeScreen(s: GameStrings, theme: SudokuTheme, hasSavedGame: Boolean, dailyProgress: DailyChallengeProgress, onNewGame: () -> Unit, onDailyChallenge: () -> Unit, onContinue: () -> Unit, onLeaderboard: () -> Unit, onSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.appName, fontSize = 60.sp, fontWeight = FontWeight.Black, color = theme.textMain)
        Spacer(modifier = Modifier.height(80.dp))
        PopButton(s.newGame, theme.primary, Color.White, onClick = onNewGame)
        Spacer(modifier = Modifier.height(20.dp))
        PopButton(s.dailyChallenge, theme.accent, theme.textMain, onClick = onDailyChallenge)
        Spacer(modifier = Modifier.height(8.dp))
        DailyChallengeBadge(s, theme, dailyProgress)
        Spacer(modifier = Modifier.height(20.dp))
        PopButton(s.continueGame, theme.secondary, theme.textMain, enabled = hasSavedGame, onClick = onContinue)
        Spacer(modifier = Modifier.height(20.dp))
        PopButton(s.leaderboard, theme.cardBg, theme.textMain, onClick = onLeaderboard)
        Spacer(modifier = Modifier.height(40.dp))
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, s.settings, modifier = Modifier.size(32.dp), tint = theme.textMain.copy(alpha = 0.7f)) }
    }
}

@Composable fun DailyChallengeBadge(s: GameStrings, theme: SudokuTheme, dailyProgress: DailyChallengeProgress) {
    val label = if (dailyProgress.completedToday) {
        "${s.dailyDone} · ${s.streakLabel} ${dailyProgress.streak}"
    } else {
        "${s.streakLabel} ${dailyProgress.streak}"
    }
    Text(
        text = label,
        color = theme.textMain.copy(alpha = 0.56f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable fun PopButton(text: String, color: Color, textColor: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(0.65f).height(60.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(alpha = 0.2f))) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if(enabled) textColor else textColor.copy(alpha = 0.3f))
    }
}

@Composable fun SudokuScreen(s: GameStrings, game: SudokuGame, theme: SudokuTheme, time: Long, onTimeTick: () -> Unit, onBack: () -> Unit, onPlayCorrect: () -> Unit, onPlayError: () -> Unit) {
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isNoteMode by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val cellSize = min( (config.screenWidthDp - 40).toFloat() / 9, (config.screenHeightDp - 420).toFloat() / 9 ).dp

    BackHandler { showExitConfirm = true }
    LaunchedEffect(game.initialBoard) {
        selectedCell = findFirstPlayableCell(game)
    }
    LaunchedEffect(game.isSolved, game.isGameOver) { while (!game.isSolved && !game.isGameOver) { delay(1000L); onTimeTick() } }

    if (showExitConfirm) {
        AlertDialog(onDismissRequest = { showExitConfirm = false }, title = { Text(s.quitTitle) }, text = { Text(s.quitDesc) }, confirmButton = { Button(onClick = onBack) { Text(s.quitConfirm) } }, dismissButton = { TextButton(onClick = { showExitConfirm = false }) { Text(s.cancel) } })
    }
    if (game.isSolved || game.isGameOver) {
        val success = game.isSolved
        AlertDialog(
            onDismissRequest = onBack,
            title = {
                Text(
                    if (success) s.victory else s.gameOver,
                    color = if (success) theme.primary else theme.error,
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ResultMoodAnimation(success = success, theme = theme)
                    Spacer(Modifier.height(12.dp))
                    Text("${s.timeLabel}: ${formatTime(time)}")
                }
            },
            confirmButton = { Button(onClick = onBack) { Text("OK") } },
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showExitConfirm = true }) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.textMain.copy(alpha = 0.6f))
                Spacer(Modifier.width(4.dp))
                Text(s.close, color = theme.textMain.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) 
            }
            Spacer(modifier = Modifier.weight(1f))
            val timeText = remember(time) { formatTime(time) }
            Text(timeText, fontSize = 22.sp, fontWeight = FontWeight.Black, color = theme.textMain)
            Spacer(modifier = Modifier.weight(1f))
            Text("${s.errors}: ${game.errorCount}/${game.currentDifficulty.maxErrors}", color = if(game.errorCount>0) theme.error else theme.textMain.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = theme.cardBg), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            SudokuBoard(game, theme, selectedCell, cellSize) { r, c -> selectedCell = r to c }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceAround) {
            GameToolButton(Icons.Default.History, s.undo, theme.cardBg, theme.secondary) { game.undo() }
            GameToolButton(Icons.Default.AutoFixHigh, s.autoNotes, theme.accent.copy(alpha = 0.9f), Color.Black) { game.fillAllNotes() }
            GameToolButton(Icons.Default.Lightbulb, s.hint, theme.accent.copy(alpha = 0.9f), Color.Black) {
                val hint = game.useHint(selectedCell)
                val message = hint?.let { "${it.technique}: 第${it.row + 1}行 第${it.col + 1}列 = ${it.value}" } ?: "提示次数已用完 (${game.currentDifficulty.maxHints})"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            GameToolButton(Icons.AutoMirrored.Filled.Backspace, s.erase, theme.cardBg, theme.textMain) {
                val r = selectedCell?.first ?: -1
                val c = selectedCell?.second ?: -1
                if (r != -1 && c != -1) game.onCellInput(r, c, 0, false)
            }
            GameToolButton(Icons.Default.Brush, if (isNoteMode) s.noteOn else s.noteOff, if(isNoteMode) theme.primary else theme.cardBg, if(isNoteMode) Color.White else theme.textMain) { isNoteMode = !isNoteMode }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val remaining by remember { derivedStateOf { game.getRemainingCounts() } }
        Row(modifier = Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..SudokuConstants.GRID_SIZE) {
                val count = remaining[i] ?: 0; val isFin = count == 0 && !isNoteMode
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if(count>0) "$count" else "•", fontSize = 10.sp, color = theme.textMain.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(width = (config.screenWidthDp / 11).dp, height = 62.dp).clip(RoundedCornerShape(14.dp)).background(if(isFin) theme.bg.copy(alpha = 0.4f) else if(isNoteMode) theme.accent else theme.primary).clickable(enabled = !isFin) {
                        val r = selectedCell?.first ?: -1; val c = selectedCell?.second ?: -1
                        if (r != -1 && c != -1) {
                            val oldErr = game.errorCount; game.onCellInput(r, c, i, isNoteMode)
                            if(!isNoteMode) {
                                if(game.errorCount > oldErr) { triggerVibration(context, 250); onPlayError() }
                                else { triggerVibration(context, 40); onPlayCorrect() }
                            }
                        }
                    }, contentAlignment = Alignment.Center) { Text("$i", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if(isFin) theme.textMain.copy(alpha = 0.15f) else if(isNoteMode) theme.textMain else Color.White) }
                }
            }
        }
    }
}

@Composable fun GameIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(54.dp).background(color, CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(icon, null, tint = tint) }
}

@Composable fun GameToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GameIconButton(icon, color, tint, onClick)
        Text(label, color = tint.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun findFirstPlayableCell(game: SudokuGame): Pair<Int, Int>? {
    for (row in 0 until SudokuConstants.GRID_SIZE) {
        for (col in 0 until SudokuConstants.GRID_SIZE) {
            if (!game.initialBoard[row][col] && game.board[row][col] == 0) return row to col
        }
    }
    return null
}

@Composable
private fun ResultMoodAnimation(success: Boolean, theme: SudokuTheme) {
    val transition = rememberInfiniteTransition(label = "result_mood")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "result_particles",
    )
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "result_bounce",
    )
    val pulse = sin(bounce * PI).toFloat()
    val moodOffset = if (success) (-10f * pulse).dp else (8f * pulse).dp
    val moodScale = if (success) 1f + 0.08f * pulse else 1f - 0.04f * pulse

    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (success) {
                val colors = listOf(theme.primary, theme.secondary, theme.accent, Color(0xFFF43F5E))
                for (i in 0 until 28) {
                    val lane = i / 28f
                    val y = ((progress + lane) % 1f) * size.height
                    val x = (size.width * ((i * 37) % 100) / 100f) + sin((progress * 6f + i) * PI).toFloat() * 12f
                    drawCircle(color = colors[i % colors.size], radius = 4f + (i % 3) * 1.5f, center = Offset(x, y))
                }
                for (i in 0 until 10) {
                    val angle = (i / 10f) * (PI * 2f) + progress * PI
                    drawCircle(
                        color = theme.accent.copy(alpha = 0.45f),
                        radius = 3.5f,
                        center = Offset(
                            x = center.x + cos(angle).toFloat() * (48f + 8f * sin(progress * PI).toFloat()),
                            y = center.y + sin(angle).toFloat() * 42f,
                        ),
                    )
                }
            } else {
                for (i in 0 until 18) {
                    val lane = i / 18f
                    val y = ((progress + lane) % 1f) * size.height
                    val x = size.width * ((i * 29) % 100) / 100f
                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = 0.55f),
                        start = Offset(x, y),
                        end = Offset(x - 5f, y + 18f),
                        strokeWidth = 3f,
                    )
                }
                drawCircle(color = theme.error.copy(alpha = 0.1f), radius = 58f + 8f * sin(progress * PI).toFloat(), center = center)
            }
        }
        Box(
            modifier = Modifier
                .offset(y = moodOffset)
                .scale(moodScale)
                .size(88.dp)
                .clip(CircleShape)
                .background(if (success) theme.accent else Color(0xFFE5E7EB))
                .border(4.dp, if (success) theme.primary else Color(0xFF6B7280), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (success) ":)" else ":(",
                color = if (success) Color(0xFF111827) else Color(0xFF374151),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable fun SudokuBoard(game: SudokuGame, theme: SudokuTheme, selectedCell: Pair<Int, Int>?, cellSize: Dp, onCellClick: (Int, Int) -> Unit) {
    val selR = selectedCell?.first ?: -1; val selC = selectedCell?.second ?: -1
    val selVal = if (selR != -1) game.board[selR][selC] else 0
    val subgridLineColor = if (theme.isDark) theme.textMain.copy(alpha = 0.15f) else theme.textMain.copy(alpha = 0.25f)
    val subgridLineWidth = if (theme.isDark) 1.5.dp else 2.dp
    val cellGap = 0.5.dp
    val lineWidthPx = with(LocalDensity.current) { subgridLineWidth.toPx() }

    Box(Modifier.padding(8.dp)) {
        Column {
            for (row in 0 until SudokuConstants.GRID_SIZE) { Row { for (col in 0 until SudokuConstants.GRID_SIZE) {
                key(row to col) {
                    SudokuCell(
                        value = game.board[row][col],
                        isInitial = game.initialBoard[row][col],
                        isSelected = row == selR && col == selC,
                        isSameGroup = row == selR || col == selC || (row / 3 == selR / 3 && col / 3 == selC / 3),
                        isSameValue = selVal != 0 && game.board[row][col] == selVal,
                        isError = game.errorCells[row to col] ?: false,
                        cellNotes = game.notes[row to col] ?: emptySet(),
                        cellSize = cellSize,
                        cellGap = cellGap,
                        theme = theme,
                        onClick = { onCellClick(row, col) }
                    )
                }
            } } }
        }

        Canvas(Modifier.matchParentSize()) {
            val cellW = size.width / SudokuConstants.GRID_SIZE
            val cellH = size.height / SudokuConstants.GRID_SIZE
            for (i in 1..2) {
                val x = cellW * 3 * i
                drawLine(subgridLineColor, Offset(x, 0f), Offset(x, size.height), lineWidthPx)
                val y = cellH * 3 * i
                drawLine(subgridLineColor, Offset(0f, y), Offset(size.width, y), lineWidthPx)
            }
        }
    }
}

private fun Modifier.cellBackground(
    isSelected: Boolean,
    isSameValue: Boolean,
    isSameGroup: Boolean,
    theme: SudokuTheme,
) = this.then(
    Modifier.background(
        when {
            isSelected -> theme.primary
            isSameValue -> theme.accent.copy(alpha = 0.6f)
            isSameGroup -> theme.secondary.copy(alpha = 0.15f)
            else -> theme.bg.copy(alpha = 0.3f)
        }
    )
)

@Composable
private fun SudokuCell(
    value: Int,
    isInitial: Boolean,
    isSelected: Boolean,
    isSameGroup: Boolean,
    isSameValue: Boolean,
    isError: Boolean,
    cellNotes: Set<Int>,
    cellSize: Dp,
    cellGap: Dp,
    theme: SudokuTheme,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(cellGap)
            .clip(RoundedCornerShape(6.dp))
            .cellBackground(isSelected, isSameValue, isSameGroup, theme)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                "$value",
                fontSize = (cellSize.value * 0.6).sp,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    isError -> theme.error
                    isSelected -> Color.White
                    isInitial -> theme.textMain
                    else -> theme.primary
                },
            )
        } else if (cellNotes.isNotEmpty()) {
            val noteFontSize = max(7f, cellSize.value * 0.17f).sp
            Column(Modifier.fillMaxSize().padding(3.dp)) {
                for (noteRow in 0 until SudokuConstants.SUBGRID_SIZE) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (noteCol in 0 until SudokuConstants.SUBGRID_SIZE) {
                            val noteValue = noteRow * SudokuConstants.SUBGRID_SIZE + noteCol + 1
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (noteValue in cellNotes) {
                                    Text(
                                        "$noteValue",
                                        fontSize = noteFontSize,
                                        lineHeight = noteFontSize,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        color = if (isSelected) Color.White.copy(alpha = 0.72f)
                                        else theme.textMain.copy(alpha = 0.46f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun SettingsScreen(s: GameStrings, theme: SudokuTheme, currentLang: Language, currentThemeMode: String, onThemeModeChange: (String) -> Unit, onLangChange: (Language) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.textMain)
                Spacer(Modifier.width(4.dp))
                Text(s.back, color = theme.textMain, fontWeight = FontWeight.Bold)
            }
        }
        Text(s.settings, fontSize = 42.sp, fontWeight = FontWeight.Black, color = theme.textMain, modifier = Modifier.padding(horizontal = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            item { Text(s.themeLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp)) }
            // "跟随系统" option
            item {
                Row(modifier = Modifier.fillMaxWidth().clickable { onThemeModeChange("auto") }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(currentThemeMode == "auto", { onThemeModeChange("auto") }, colors = RadioButtonDefaults.colors(selectedColor = theme.primary))
                    Text(s.autoTheme, color = theme.textMain, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }
            }
            SudokuTheme.entries.forEach { t -> item {
                Row(modifier = Modifier.fillMaxWidth().clickable { onThemeModeChange(t.name) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(currentThemeMode == t.name, { onThemeModeChange(t.name) }, colors = RadioButtonDefaults.colors(selectedColor = theme.primary))
                    Text(t.label, color = theme.textMain, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }
            } }
            item { Text(s.langLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp)) }
            Language.entries.forEach { l -> item {
                Row(modifier = Modifier.fillMaxWidth().clickable { onLangChange(l) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(currentLang == l, { onLangChange(l) }, colors = RadioButtonDefaults.colors(selectedColor = theme.primary))
                    Text(l.label, color = theme.textMain, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }
            } }
            item { Spacer(modifier = Modifier.height(32.dp)); Text("Version 1.0", fontSize = 12.sp, color = theme.textMain.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp)) }
        }
    }
}

@Composable fun LeaderboardScreen(s: GameStrings, theme: SudokuTheme, scoresMap: Map<Difficulty, List<Pair<Long, String>>>, statsMap: Map<Difficulty, DifficultyStats>, onBack: () -> Unit) {
    var selectedDifficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    val diffLabels = mapOf(Difficulty.BEGINNER to s.beginner, Difficulty.NORMAL to s.normal, Difficulty.HARD to s.hard, Difficulty.PRO to s.pro, Difficulty.MASTER to s.master)
    val stats = statsMap[selectedDifficulty] ?: DifficultyStats(0, 0, 0L, 0, 0)
    val winRate = if (stats.attempts == 0) 0 else stats.wins * 100 / stats.attempts
    val avgTime = if (stats.attempts == 0) "--:--" else formatTime(stats.totalTime / stats.attempts)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.textMain)
                Spacer(Modifier.width(4.dp))
                Text(s.back, color = theme.textMain, fontWeight = FontWeight.Bold) 
            }
        }
        Text(s.best, fontSize = 42.sp, fontWeight = FontWeight.Black, color = theme.textMain, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        ScrollableTabRow(selectedTabIndex = selectedDifficulty.ordinal, containerColor = Color.Transparent, contentColor = theme.primary, edgePadding = 8.dp, divider = {}) {
            Difficulty.entries.forEach { diff -> Tab(selected = selectedDifficulty == diff, onClick = { selectedDifficulty = diff }, text = { Text(diffLabels[diff]!!, fontSize = 12.sp, fontWeight = FontWeight.Bold) }) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("局数", "${stats.attempts}", theme)
            StatChip("胜率", "$winRate%", theme)
            StatChip("均时", avgTime, theme)
        }
        Card(modifier = Modifier.weight(1f).padding(top = 16.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = theme.cardBg)) {
            val scores = scoresMap[selectedDifficulty] ?: emptyList()
            if (scores.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.noRecords, color = theme.textMain.copy(alpha = 0.2f)) }
            else LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                itemsIndexed(scores) { index, item -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${index + 1}. ${formatTime(item.first)}", fontWeight = FontWeight.Bold, color = theme.textMain); Text(item.second, fontSize = 12.sp, color = theme.textMain.copy(alpha = 0.4f)) } }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable fun StatChip(label: String, value: String, theme: SudokuTheme) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.cardBg)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = theme.primary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = theme.textMain.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun DifficultySelectScreen(s: GameStrings, theme: SudokuTheme, onDifficultySelected: (Difficulty) -> Unit, onBack: () -> Unit) {
    val diffs = listOf(Difficulty.BEGINNER to s.beginner, Difficulty.NORMAL to s.normal, Difficulty.HARD to s.hard, Difficulty.PRO to s.pro, Difficulty.MASTER to s.master)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.textMain)
                Spacer(Modifier.width(4.dp))
                Text(s.back, color = theme.textMain, fontWeight = FontWeight.Bold) 
            }
        }
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(s.selectDifficulty, fontSize = 38.sp, fontWeight = FontWeight.Black, color = theme.textMain)
            Spacer(modifier = Modifier.height(48.dp))
            diffs.forEach { (d, label) -> PopButton(label, if(d == Difficulty.MASTER) theme.accent else theme.secondary, theme.textMain) { onDifficultySelected(d) }; Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// 6. 修复 formatTime 边缘情况
fun formatTime(seconds: Long): String {
    val totalSecs = max(0, seconds)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%02d:%02d".format(mins, secs)
}

fun triggerVibration(context: Context, duration: Long) {
    val vibrator = context.getSystemService(Vibrator::class.java)
    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
}
