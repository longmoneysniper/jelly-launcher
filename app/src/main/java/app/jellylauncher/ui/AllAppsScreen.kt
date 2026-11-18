package app.jellylauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jellylauncher.data.LaunchableApp
import kotlinx.coroutines.delay

@Composable
fun AllAppsScreen(
    apps: List<LaunchableApp>,
    onLaunch: (LaunchableApp) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) {
            delay(150)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
            Text(
                "All apps",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    Column {
                        FocusCard(
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                            focusRequester = if (index == 0) firstFocus else null,
                            onClick = { onLaunch(app) },
                        ) {
                            Box(Modifier.fillMaxSize().background(CardBg), contentAlignment = Alignment.Center) {
                                if (app.banner != null) {
                                    Image(
                                        bitmap = app.banner,
                                        contentDescription = app.label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Image(
                                        bitmap = app.icon,
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
            }
        }
    }
}
