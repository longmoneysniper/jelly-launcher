package app.jellylauncher.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jellylauncher.HomeUiState
import app.jellylauncher.data.LaunchableApp
import app.jellylauncher.data.ResumeCard
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlay: (ResumeCard) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    // This is the home screen: Back should never leave it.
    BackHandler {}

    var focusedId by remember { mutableStateOf<String?>(null) }
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
        // Backdrop of the focused item, with gradients so text and rows stay readable.
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
                    0f to Bg,
                    0.45f to Bg.copy(alpha = 0.85f),
                    1f to Color.Transparent,
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    0.7f to Bg.copy(alpha = 0.9f),
                    1f to Bg,
                )
            )
        )

        Column(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 16.dp)) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.userName.isNotEmpty()) "Welcome back, ${state.userName}" else "Home",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                )
                Clock()
            }

            // Details of the focused item
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 48.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (hero != null) {
                    Column(Modifier.fillMaxWidth(0.5f)) {
                        Text(
                            hero.title,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (hero.subtitle.isNotEmpty()) {
                            Text(hero.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        if (hero.overview.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                hero.overview,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (hero.remaining.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(hero.remaining, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Continue Watching
            SectionTitle("Continue Watching")
            if (state.cards.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(state.cards, key = { _, card -> card.id }) { index, card ->
                        ResumeCardView(
                            card = card,
                            focusRequester = if (index == 0) firstCardFocus else null,
                            onFocused = { focusedId = card.id },
                            onClick = { onPlay(card) },
                        )
                    }
                }
            } else {
                RowMessage(state = state, onRetry = onRetry)
            }

            // Apps
            Spacer(Modifier.height(4.dp))
            SectionTitle("Apps")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.apps, key = { it.packageName }) { app ->
                    AppTile(app = app, onClick = { onLaunchApp(app) })
                }
                item(key = "jellyfin-settings") {
                    SettingsTile(onClick = onSettings)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 48.dp),
    )
}

@Composable
private fun RowMessage(state: HomeUiState, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(190.dp).padding(horizontal = 48.dp),
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
private fun ResumeCardView(
    card: ResumeCard,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    Column(Modifier.width(240.dp)) {
        FocusCard(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            focusRequester = focusRequester,
            onFocused = onFocused,
            onClick = onClick,
        ) {
            Box(Modifier.fillMaxSize().background(CardBg))
            if (card.cardImage != null) {
                AsyncImage(
                    model = card.cardImage,
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    card.title,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (card.progress > 0f) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(card.progress.coerceIn(0f, 1f)).background(Accent)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            card.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            card.subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppTile(app: LaunchableApp, onClick: () -> Unit) {
    Column(Modifier.width(160.dp)) {
        FocusCard(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            onClick = onClick,
        ) {
            Box(Modifier.fillMaxSize().background(CardBg), contentAlignment = Alignment.Center) {
                if (app.isBanner) {
                    Image(
                        bitmap = app.image,
                        contentDescription = app.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Image(
                        bitmap = app.image,
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsTile(onClick: () -> Unit) {
    Column(Modifier.width(160.dp)) {
        FocusCard(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            onClick = onClick,
        ) {
            Box(Modifier.fillMaxSize().background(CardBg), contentAlignment = Alignment.Center) {
                Text("⚙", color = Color.White, fontSize = 30.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Jellyfin settings", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1)
    }
}

/** A focusable tile that grows and gets a white outline when focused with the D-pad. */
@Composable
private fun FocusCard(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "scale")
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(if (focused) Modifier.border(3.dp, Color.White, shape) else Modifier)
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
    val date = remember(minute) { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(now)) }

    Column(horizontalAlignment = Alignment.End) {
        Text(time, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(date, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}
