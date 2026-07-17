package com.kps.trackmyweight.ui.photos

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kps.trackmyweight.data.db.entity.ProgressPhotoEntity
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.ui.common.ChoiceTile
import java.io.File

@Composable
fun PhotosScreen(
    onOpenCamera: () -> Unit = {},
    vm: PhotosViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = pendingUri
            if (uri != null) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    vm.capture(bytes, widthPx = null, heightPx = null)
                }
            }
        }
        pendingUri = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onOpenCamera()
                },
                icon = { Icon(Icons.Outlined.CameraAlt, null) },
                text = { Text(if (state.isCapturing) "Sauvegarde..." else "Prendre (overlay)") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Photos",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )

            AngleSelector(state.selectedAngle, onSelect = vm::selectAngle)

            val forAngle = state.byAngle[state.selectedAngle].orEmpty()
            if (forAngle.size >= 2) {
                CompareSection(state.compareFrom, state.compareTo)
            }

            if (forAngle.isEmpty()) {
                Text(
                    "Aucune photo ${state.selectedAngle.label()} pour le moment. Prends-en une pour commencer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Galerie — ${state.selectedAngle.label()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    val ctxLocal = LocalContext.current
                    val scopeLocal = rememberCoroutineScope()
                    androidx.compose.material3.TextButton(onClick = {
                        scopeLocal.launch {
                            val file = vm.generateTimelapse()
                            if (file != null) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    ctxLocal, "${ctxLocal.packageName}.fileprovider", file
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctxLocal.startActivity(android.content.Intent.createChooser(intent, "Partager le timelapse"))
                            }
                        }
                    }) { Text("Timelapse") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(forAngle, key = { it.id }) { p ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = File(p.thumbnailPath),
                                contentDescription = "${p.angle} ${p.date}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 120.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { vm.setCompareTo(p.id) },
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(p.date.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AngleSelector(current: PhotoAngle, onSelect: (PhotoAngle) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoAngle.entries.forEach { a ->
            Box(modifier = Modifier.weight(1f)) {
                ChoiceTile(
                    title = a.short(),
                    selected = a == current,
                    onClick = { onSelect(a) },
                )
            }
        }
    }
}

@Composable
private fun CompareSection(from: ProgressPhotoEntity?, to: ProgressPhotoEntity?) {
    if (from == null || to == null || from.id == to.id) return
    var sliderPos by remember { mutableFloatStateOf(0.5f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Comparaison ${from.date} → ${to.date}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            AsyncImage(
                model = File(from.thumbnailPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(TopClip(sliderPos)),
            ) {
                AsyncImage(
                    model = File(to.thumbnailPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = (sliderPos * 400).dp) // approx
                    .height(2.dp)
                    .background(Color.White),
            )
        }
        Slider(
            value = sliderPos,
            onValueChange = { sliderPos = it },
            valueRange = 0f..1f,
        )
    }
}

/** Découpe qui garde la portion basse à partir de `pos * hauteur`. */
private class TopClip(private val pos: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val top = size.height * pos.coerceIn(0f, 1f)
        return androidx.compose.ui.graphics.Outline.Rectangle(
            androidx.compose.ui.geometry.Rect(0f, top, size.width, size.height),
        )
    }
}

private fun PhotoAngle.short() = when (this) {
    PhotoAngle.FRONT -> "Face"
    PhotoAngle.SIDE_LEFT -> "Profil G"
    PhotoAngle.SIDE_RIGHT -> "Profil D"
    PhotoAngle.BACK -> "Dos"
}
private fun PhotoAngle.label() = when (this) {
    PhotoAngle.FRONT -> "de face"
    PhotoAngle.SIDE_LEFT -> "profil gauche"
    PhotoAngle.SIDE_RIGHT -> "profil droit"
    PhotoAngle.BACK -> "de dos"
}
