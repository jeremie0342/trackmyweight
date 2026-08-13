package com.kps.trackmyweight.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kps.trackmyweight.data.seed.EquipmentMedia
import com.kps.trackmyweight.data.seed.ExerciseSeed

/** Préfixe Coil pour lire un fichier embarqué dans `assets/`. */
private const val ASSET_SCHEME = "file:///android_asset/"

/** Ratio des visuels sources (850 × 567). */
private const val MEDIA_ASPECT_RATIO = 1.5f

private const val CROSSFADE_MS = 900
private const val HOLD_MS = 450

/**
 * Démonstration animée d'un exercice.
 *
 * Chaque exercice embarque deux images — position de départ et position finale.
 * Les faire alterner en fondu suffit à montrer l'amplitude du mouvement, sans
 * décodeur GIF ni vidéo : deux fichiers statiques et une interpolation d'alpha.
 *
 * [mediaPath] est la racine sans suffixe, telle que stockée dans
 * `ExerciseEntity.mediaPath` (ex. `exercises/bench_press`). Null pour un
 * exercice créé par l'utilisateur : on n'affiche alors rien plutôt qu'un cadre
 * vide.
 */
@Composable
fun ExerciseDemo(
    mediaPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    if (mediaPath == null) return

    val start = "$ASSET_SCHEME$mediaPath${ExerciseSeed.MEDIA_START_SUFFIX}"
    val end = "$ASSET_SCHEME$mediaPath${ExerciseSeed.MEDIA_END_SUFFIX}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(MEDIA_ASPECT_RATIO)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = start,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        // La position finale est superposée et son opacité oscille : quand elle
        // est à 0 on voit le départ, à 1 la fin. Une seule image bouge.
        val endAlpha = if (animated) {
            val transition = rememberInfiniteTransition(label = "demo")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = CROSSFADE_MS + HOLD_MS),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "endAlpha",
            ).value
        } else {
            0f
        }

        AsyncImage(
            model = end,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().alpha(endAlpha),
        )
    }
}

/**
 * Vignette carrée d'un exercice, pour les listes et sélecteurs.
 * Toujours la position de départ : en petit, une animation serait du bruit.
 */
@Composable
fun ExerciseThumbnail(
    mediaPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (mediaPath == null) {
            // Exercice personnalisé : initiale plutôt qu'un carré vide.
            Text(
                contentDescription?.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = "$ASSET_SCHEME$mediaPath${ExerciseSeed.MEDIA_START_SUFFIX}",
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Visuel d'un équipement de salle, à partir de sa clé ([EquipmentMedia]).
 * N'affiche rien si l'équipement n'a pas de visuel — c'est le cas des
 * accessoires sans forme distinctive.
 */
@Composable
fun EquipmentThumbnail(
    equipmentKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
) {
    val path = EquipmentMedia.pathFor(equipmentKey) ?: return
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = "$ASSET_SCHEME$path",
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
