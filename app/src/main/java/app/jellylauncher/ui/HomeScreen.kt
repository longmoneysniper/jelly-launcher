package app.jellylauncher.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jellylauncher.HomeUiState
import app.jellylauncher.data.LaunchableApp
import app.jellylauncher.data.ResumeCard
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Date

private val Gold = Color(0xFFFFC145)
private const val DOCK_APP_COUNT = 6

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlay: (ResumeCard) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onAllApps: () -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit,
    onRetry: () -> Unit,
) {
    // This is the home screen: Back should never leave it.
    BackHandler {}

    // Size things from the screen height so the layout fits any TV.
    val u = (LocalConfiguration.current.screenHeightDp / 540f).coerceIn(0.6f, 1.8f)
    val posterHeight = (190 * u).dp
    val posterWidth = (127 * u).dp
    val dockTile = (46 * u).dp

    var focusedId by remember { mutableStateOf<String?>(null) }
    var dockLabel by remember { mutableStateOf("") }
    val hero = state.cards.firstOrNull { it.id == focusedId } ?: state.cards.firstOrNull()

    // Put focus on the newest Continue Watching item whenever the list's first entry changes.
    val firstCardFocus = remember { FocusRequester() }
    val firstId = state.cards.firstOrNull()?.id
    LaunchedEffect(firstId) {
        if (firstId != null) {
            delay(150)
            runCatching { firstCardFocus.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        // Backdrop of the focused item, dimmed so text and icons stay readable.
        Crossfade(targetState = hero?.heroImage, animationSpec = tween(500), label = "hero") { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Bg.copy(alpha = 0.93f),
                    0.5f to Bg.copy(alpha = 0.75f),
                    1f to Bg.copy(alpha = 0.5f),
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to Bg,
                )
            )
        )

        Column(Modifier.fillMaxSize().padding(top = 20.dp, bottom = 16.dp)) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logo()
                Clock()
            }

            // Main area: details on the left, posters on the right
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(0.4f).padding(end = 24.dp)) {
                    if (hero != null) HeroDetails(hero)
                }
                Column(Modifier.weight(0.6f)) {
                    Text(
                        "Continue watching",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    if (state.cards.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(start = 8.dp, end = 48.dp, top = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(state.cards, key = { _, card -> card.id }) { index, card ->
                                PosterCard(
                                    card = card,
                                    width = posterWidth,
                                    height = posterHeight,
                                    focusRequester = if (index == 0) firstCardFocus else null,
                                    onFocused = {
                                        focusedId = card.id
                                        dockLabel = ""
                                    },
                                    onClick = { onPlay(card) },
                                )
                            }
                        }
                    } else {
                        RowMessage(state = state, height = posterHeight, onRetry = onRetry)
                    }
                }
            }

            // Name of the focused dock app
            Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
                Text(dockLabel, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }

            // Dock
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.08f))
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.08f))
                            ),
                            RoundedCornerShape(26.dp),
                        )
                        .padding(9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (app in state.apps.take(DOCK_APP_COUNT)) {
                        FocusCard(
                            modifier = Modifier.size(dockTile),
                            shape = RoundedCornerShape(12.dp),
                            onFocused = { dockLabel = app.label },
                            onClick = { onLaunchApp(app) },
                        ) {
                            Box(Modifier.fillMaxSize().background(CardBg))
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                            )
                        }
                    }
                    FocusCard(
                        modifier = Modifier.size(dockTile),
                        shape = RoundedCornerShape(12.dp),
                        onFocused = { dockLabel = "All apps" },
                        onClick = onAllApps,
                    ) {
                        Box(Modifier.fillMaxSize().background(CardBg), contentAlignment = Alignment.Center) {
                            AllAppsGlyph()
                        }
                    }
                    FocusCard(
                        modifier = Modifier.size(dockTile),
                        shape = RoundedCornerShape(12.dp),
                        onFocused = { dockLabel = "Server sign-in" },
                        onClick = onSettings,
                    ) {
                        Box(Modifier.fillMaxSize().background(CardBg), contentAlignment = Alignment.Center) {
                            SettingsGlyph()
                        }
                    }
                    FocusCard(
                        modifier = Modifier.size(dockTile),
                        shape = RoundedCornerShape(12.dp),
                        onFocused = { dockLabel = "Launcher setup" },
                        onClick = onSetup,
                    ) {
                        Box(Modifier.fillMaxSize().background(CardBg), contentAlignment = Alignment.Center) {
                            JellyMark(Modifier.size(26.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroDetails(hero: ResumeCard) {
    Column {
        if (hero.kicker.isNotEmpty()) {
            Text(
                hero.kicker,
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            hero.title,
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (hero.subtitle.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                hero.subtitle,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hero.remaining.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(hero.remaining, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }
        if (hero.progress > 0f) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.width(170.dp).height(3.dp).background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(hero.progress.coerceIn(0f, 1f)).background(Gold))
            }
        }
    }
}

@Composable
private fun RowMessage(state: HomeUiState, height: Dp, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(height).padding(start = 8.dp, end = 48.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            state.error != null -> Column {
                Text(state.error, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
            state.loading -> Text("Loading…", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            else -> Text(
                "Nothing in progress. Start something in Jellyfin and it will show up here.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun PosterCard(
    card: ResumeCard,
    width: Dp,
    height: Dp,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(width).height(height),
        focusRequester = focusRequester,
        onFocused = onFocused,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize().background(CardBg))
        val image = card.poster ?: card.cardImage
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                card.title,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(10.dp),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (card.progress > 0f) {
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(card.progress.coerceIn(0f, 1f)).background(Gold))
            }
        }
    }
}

/** A focusable tile that grows and gets a gold outline when focused with the D-pad. */
@Composable
internal fun FocusCard(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    shape: Shape = RoundedCornerShape(10.dp),
    onFocused: () -> Unit = {},
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "scale")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(if (focused) Modifier.border(3.dp, Gold, shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(onClick = onClick),
        content = content,
    )
}

@Composable
private fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        JellyMark(Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Text("Encore", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

/** The logo: a "resume" ring (a progress arc with a gold dot where you left off) around a play button. */
@Composable
fun JellyMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width / 96f
        val stroke = 7f * s
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF16233A), Color(0xFF0A0F1A))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f * s, 22f * s),
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(Color(0xFF7B5CFF), Color(0xFF00A4DC), Color(0xFF4DE3C4), Color(0xFF7B5CFF))),
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(20f * s, 20f * s),
            size = androidx.compose.ui.geometry.Size(56f * s, 56f * s),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        drawCircle(Gold, radius = 5.5f * s, center = Offset(20f * s, 48f * s))
        val tri = Path().apply {
            moveTo(42f * s, 36f * s); lineTo(64f * s, 48f * s); lineTo(42f * s, 60f * s); close()
        }
        drawPath(tri, Color.White)
        drawPath(tri, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
private fun AllAppsGlyph() {
    Canvas(Modifier.size(24.dp)) {
        val cell = size.width / 3f
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = cell * 0.18f,
                    center = Offset(cell * (col + 0.5f), cell * (row + 0.5f)),
                )
            }
        }
    }
}

@Composable
private fun SettingsGlyph() {
    Canvas(Modifier.size(24.dp)) {
        val ys = listOf(0.2f, 0.5f, 0.8f)
        val knobs = listOf(0.3f, 0.7f, 0.45f)
        ys.forEachIndexed { i, y ->
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(0f, size.height * y),
                end = Offset(size.width, size.height * y),
                strokeWidth = 3f,
            )
            drawCircle(
                color = Color.White,
                radius = size.width * 0.11f,
                center = Offset(size.width * knobs[i], size.height * y),
            )
        }
    }
}

@Composable
private fun Clock() {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val minute = now / 60_000
    val time = remember(minute) { DateFormat.getTimeFormat(context).format(Date(now)) }

    Text(time, color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
}
