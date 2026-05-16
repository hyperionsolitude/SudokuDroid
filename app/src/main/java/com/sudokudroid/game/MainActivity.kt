package com.sudokudroid.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import java.util.Random

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

enum class AppLanguage { TR, EN }

data class GameStrings(
    val title: String,
    val difficultyLabel: String,
    val easy: String,
    val medium: String,
    val hard: String,
    val pause: String,
    val resume: String,
    val newGame: String,
    val winTitle: String,
    val winMessage: (String) -> String,
    val winButton: String
)

val TR_Strings = GameStrings(
    title = "Anne için Sudoku",
    difficultyLabel = "Zorluk",
    easy = "Kolay",
    medium = "Orta",
    hard = "Zor",
    pause = "Duraklat",
    resume = "Devam Et",
    newGame = "Yeni Oyun",
    winTitle = "Tebrikler!",
    winMessage = { time -> "Bulmacayı $time sürede çözdünüz! Harika bir iş çıkardınız!" },
    winButton = "Harika!"
)

val EN_Strings = GameStrings(
    title = "Sudoku for Mom",
    difficultyLabel = "Difficulty",
    easy = "Easy",
    medium = "Medium",
    hard = "Hard",
    pause = "Pause",
    resume = "Resume",
    newGame = "New Game",
    winTitle = "Congratulations!",
    winMessage = { time -> "You solved the puzzle in $time! Great job!" },
    winButton = "Awesome!"
)

// Theme Color Sets
data class SudokuThemeColors(
    val background: Color,
    val gridLine: Color,
    val highlight: Color,
    val selected: Color,
    val fixed: Color,
    val user: Color,
    val error: Color,
    val textPrimary: Color
)

val DarkThemeColors = SudokuThemeColors(
    background = Color.Black,
    gridLine = Color.White,
    highlight = Color(0xFF333333),
    selected = Color(0xFF4D4D4D),
    fixed = Color.White,
    user = Color.White,
    error = Color(0xFFEF5350),
    textPrimary = Color.White
)

val LightThemeColors = SudokuThemeColors(
    background = Color(0xFFF5F5F5),
    gridLine = Color.Black,
    highlight = Color(0xFFE0E0E0),
    selected = Color(0xFFB3E5FC),
    fixed = Color(0xFF212121),
    user = Color.Black,
    error = Color(0xFFD32F2F),
    textPrimary = Color(0xFF212121)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var language by remember { mutableStateOf(AppLanguage.TR) }
            val colors = if (isDarkTheme) DarkThemeColors else LightThemeColors

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme(background = colors.background)
                              else lightColorScheme(background = colors.background)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    SudokuScreen(
                        isDarkTheme = isDarkTheme,
                        language = language,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onLanguageToggle = {
                            language = if (language == AppLanguage.TR) AppLanguage.EN else AppLanguage.TR
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SudokuScreen(isDarkTheme: Boolean, language: AppLanguage, onThemeToggle: () -> Unit, onLanguageToggle: () -> Unit) {
    val colors = if (isDarkTheme) DarkThemeColors else LightThemeColors
    val strings = if (language == AppLanguage.TR) TR_Strings else EN_Strings
    var gameLogic by remember { mutableStateOf(SudokuGameLogic()) }
    var currentDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    var board by remember { mutableStateOf(gameLogic.generateGame(currentDifficulty)) }
    var initialBoard by remember { mutableStateOf(board.map { it.copyOf() }.toTypedArray()) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var errorCells by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var showWinDialog by remember { mutableStateOf(false) }

    var secondsElapsed by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(true) } // Starts paused
    var currentJoke by remember { mutableStateOf<String?>(null) }
    var lastJokeResumeTime by remember { mutableLongStateOf(0L) }
    val jokeMessages = remember { listOf("YETER ARTIK", "OGLUNLA ILGILEN", "BIR ORDAYIM BIR BURDAYIM", "KAYBOL") }

    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000)
            secondsElapsed++

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastJokeResumeTime > 3 * 60 * 1000L) {
                if (java.util.Random().nextInt(100) < 3) { // 3% chance every second
                    currentJoke = jokeMessages.random()
                    isPaused = true
                }
            }
        }
    }

    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun checkWin() {
        var won = true
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board[r][c] == 0 || !gameLogic.checkCell(r, c, board[r][c])) {
                    won = false
                    break
                }
            }
            if (!won) break
        }
        if (won) {
            showWinDialog = true
            isPaused = true
        } else {
            showWinDialog = won
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = colors.textPrimary
                    )
                }
                IconButton(onClick = onLanguageToggle) {
                    Icon(
                        painter = painterResource(if (language == AppLanguage.TR) R.drawable.flag_tr else R.drawable.flag_us),
                        contentDescription = "Toggle Language",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = strings.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.difficultyLabel}: ${if (currentDifficulty == Difficulty.EASY) strings.easy else if (currentDifficulty == Difficulty.MEDIUM) strings.medium else strings.hard}",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                IconButton(onClick = {
                    if (currentJoke != null) {
                        lastJokeResumeTime = System.currentTimeMillis()
                        currentJoke = null
                    }
                    isPaused = !isPaused
                }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause/Resume",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = formatTime(secondsElapsed),
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.background)
                .border(3.dp, colors.textPrimary, RoundedCornerShape(4.dp))
                .drawWithContent {
                    drawContent()
                    val w = size.width
                    val h = size.height

                    for (i in 1..8) {
                        val thickness = if (i % 3 == 0) 4.dp.toPx() else 2.dp.toPx()

                        // Vertical lines
                        drawLine(
                            color = colors.gridLine,
                            start = Offset((i * w / 9f) - thickness / 2, 0f),
                            end = Offset((i * w / 9f) - thickness / 2, h),
                            strokeWidth = thickness
                        )

                        // Horizontal lines
                        drawLine(
                            color = colors.gridLine,
                            start = Offset(0f, (i * h / 9f) - thickness / 2),
                            end = Offset(w, (i * h / 9f) - thickness / 2),
                            strokeWidth = thickness
                        )
                    }
                }
        ) {
            Column {
                for (row in 0 until 9) {
                    Row {
                        for (col in 0 until 9) {
                            SudokuCell(
                                value = board[row][col],
                                isFixed = initialBoard[row][col] != 0,
                                isSelected = selectedCell == Pair(row, col),
                                isRowHighlighted = selectedCell?.first == row,
                                isColHighlighted = selectedCell?.second == col,
                                isError = errorCells.contains(Pair(row, col)),
                                onClick = {
                                    if (!isPaused) {
                                        selectedCell = Pair(row, col)
                                    }
                                },
                                colors = colors,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }

            if (isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .clickable {
                            if (currentJoke != null) {
                                lastJokeResumeTime = System.currentTimeMillis()
                                currentJoke = null
                            }
                            isPaused = !isPaused
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.resume,
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Button(onClick = {
                    if (currentJoke != null) {
                        lastJokeResumeTime = System.currentTimeMillis()
                        currentJoke = null
                    }
                    isPaused = !isPaused
                }, modifier = Modifier.weight(1f)) {
                    Text(if (isPaused) strings.resume else strings.pause)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        gameLogic = SudokuGameLogic()
                        currentDifficulty = Difficulty.EASY
                        board = gameLogic.generateGame(currentDifficulty)
                        initialBoard = board.map { it.copyOf() }.toTypedArray()
                        errorCells = emptySet()
                        selectedCell = null
                        showWinDialog = false
                        secondsElapsed = 0
                        isPaused = true
                        currentJoke = null
                        lastJokeResumeTime = 0L
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.newGame)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (num in 1..9) {
                    NumberButton(num.toString(), modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        selectedCell?.let { (row, col) ->
                            if (!isPaused && initialBoard[row][col] == 0) {
                                board[row][col] = num
                                if (!gameLogic.checkCell(row, col, num)) {
                                    errorCells = errorCells + Pair(row, col)
                                } else {
                                    errorCells = errorCells - Pair(row, col)
                                }
                                board = board.map { it.copyOf() }.toTypedArray()
                                checkWin()
                            }
                        }
                    }
                }
                NumberButton("X", isErase = true, modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    selectedCell?.let { (row, col) ->
                        if (!isPaused && initialBoard[row][col] == 0) {
                            board[row][col] = 0
                            errorCells = errorCells - Pair(row, col)
                            board = board.map { it.copyOf() }.toTypedArray()
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DifficultyButton(strings.easy) {
                currentDifficulty = Difficulty.EASY
                gameLogic = SudokuGameLogic()
                board = gameLogic.generateGame(currentDifficulty)
                initialBoard = board.map { it.copyOf() }.toTypedArray()
                errorCells = emptySet()
                selectedCell = null
                showWinDialog = false
                secondsElapsed = 0
                isPaused = true
                currentJoke = null
                lastJokeResumeTime = 0L
            }
            DifficultyButton(strings.medium) {
                currentDifficulty = Difficulty.MEDIUM
                gameLogic = SudokuGameLogic()
                board = gameLogic.generateGame(currentDifficulty)
                initialBoard = board.map { it.copyOf() }.toTypedArray()
                errorCells = emptySet()
                selectedCell = null
                showWinDialog = false
                secondsElapsed = 0
                isPaused = true
                currentJoke = null
                lastJokeResumeTime = 0L
            }
            DifficultyButton(strings.hard) {
                currentDifficulty = Difficulty.HARD
                gameLogic = SudokuGameLogic()
                board = gameLogic.generateGame(currentDifficulty)
                initialBoard = board.map { it.copyOf() }.toTypedArray()
                errorCells = emptySet()
                selectedCell = null
                showWinDialog = false
                secondsElapsed = 0
                isPaused = true
                currentJoke = null
                lastJokeResumeTime = 0L
            }
        }
    }

    if (showWinDialog) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VictoryEffect()

            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .wrapContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF333333) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.winTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.winMessage(formatTime(secondsElapsed)),
                        fontSize = 18.sp,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showWinDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.winButton)
                    }
                }
            }
        }
    }

    if (currentJoke != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.7f))
                .clickable {
                    if (currentJoke != null) {
                        lastJokeResumeTime = System.currentTimeMillis()
                        currentJoke = null
                    }
                    isPaused = !isPaused
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(colors.background, RoundedCornerShape(16.dp))
                    .border(2.dp, colors.error, RoundedCornerShape(16.dp))
                    .padding(32.dp)
            ) {
                AnimatedJokeText(
                    text = currentJoke!!,
                    color = colors.error
                )
            }
        }
    }
}

@Composable
fun SudokuCell(
    value: Int,
    isFixed: Boolean,
    isSelected: Boolean,
    isRowHighlighted: Boolean,
    isColHighlighted: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    colors: SudokuThemeColors,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .background(
                when {
                    isSelected -> colors.selected
                    isRowHighlighted || isColHighlighted -> colors.highlight
                    isError -> colors.error
                    else -> colors.background
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (value == 0) "" else value.toString(),
            fontSize = 22.sp,
            fontWeight = if (isFixed) FontWeight.Bold else FontWeight.Normal,
            color = if (isError) Color.White else if (isFixed) colors.fixed else colors.user,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NumberButton(text: String, isErase: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = if (isErase) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors()
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DifficultyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 14.sp)
    }
}

@Composable
fun AnimatedJokeText(text: String, color: Color) {
    val randomEffect = remember { (0..2).random() }
    val infiniteTransition = rememberInfiniteTransition(label = "joke")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val offset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Text(
        text = text,
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .graphicsLayer {
                if (randomEffect == 1) translationX = offset
            }
            .alpha(if (randomEffect == 0) alpha else 1f)
    )
}

enum class ParticleType { CONFETTI, BALLOON }
data class Particle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val vx: Float,
    val type: ParticleType
)

@Composable
fun VictoryEffect() {
    val random = remember { Random() }
    val particles = remember {
        List(100) {
            val type = if (random.nextBoolean()) ParticleType.CONFETTI else ParticleType.BALLOON
            Particle(
                x = random.nextFloat(),
                y = if (type == ParticleType.CONFETTI) -0.1f else 1.1f,
                color = Color(random.nextInt()),
                size = if (type == ParticleType.CONFETTI) random.nextFloat() * 8f + 4f else random.nextFloat() * 20f + 15f,
                speed = if (type == ParticleType.CONFETTI) random.nextFloat() * 0.5f + 0.2f else -(random.nextFloat() * 0.3f + 0.1f),
                vx = random.nextFloat() * 0.01f - 0.005f,
                type = type
            )
        }
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while(true) {
            time += 0.01f
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.9f)) {
        particles.forEach { p ->
            var curY = (p.y + time * p.speed)
            if (p.type == ParticleType.CONFETTI) {
                while (curY > 1.1f) curY -= 1.2f
                if (curY < -0.1f) curY = -0.1f
            } else {
                while (curY < -0.2f) curY += 1.2f
                if (curY > 1.2f) curY = 1.2f
            }

            var curX = (p.x + time * p.vx)
            while (curX < 0) curX += 1f
            while (curX > 1) curX -= 1f

            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(curX * size.width, curY * size.height)
            )
        }
    }
}
