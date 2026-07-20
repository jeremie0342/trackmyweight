package com.kps.trackmyweight.ui.pulse

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.enums.HeartRateSource
import com.kps.trackmyweight.data.repository.HabitRepository
import com.kps.trackmyweight.domain.calc.PulsePpgDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MEASURE_DURATION_SEC = 20

data class PpgUiState(
    val isMeasuring: Boolean = false,
    val elapsedSec: Int = 0,
    val bpm: Int? = null,
    val quality: Float? = null,
    val errorMessage: String? = null,
    val fingerNotDetected: Boolean = false,
)

@HiltViewModel
class PulsePpgViewModel @Inject constructor(
    private val habitRepo: HabitRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PpgUiState())
    val state: StateFlow<PpgUiState> = _state.asStateFlow()

    // Samples et timestamps collectés depuis le thread d'analyse caméra
    private val samples = mutableListOf<Float>()
    private var startTimeNanos: Long = 0L

    fun start() {
        samples.clear()
        startTimeNanos = System.nanoTime()
        _state.update {
            PpgUiState(isMeasuring = true, elapsedSec = 0)
        }
        viewModelScope.launch {
            while (_state.value.isMeasuring) {
                delay(200)
                val elapsed = ((System.nanoTime() - startTimeNanos) / 1_000_000_000L).toInt()
                _state.update { it.copy(elapsedSec = elapsed) }
                if (elapsed >= MEASURE_DURATION_SEC) {
                    finish()
                    break
                }
            }
        }
    }

    /** Appelé depuis l'Analyzer caméra pour chaque frame. */
    fun onFrameLuminance(avgY: Float) {
        if (!_state.value.isMeasuring) return
        samples += avgY
        // Détecte "pas de doigt" : luminance trop élevée (flash direct sans obstruction)
        // ou trop basse pendant plusieurs secondes.
        if (samples.size == 30) {
            val avg = samples.average()
            _state.update { it.copy(fingerNotDetected = avg > 220.0 || avg < 20.0) }
        }
    }

    private fun finish() {
        val elapsedSec = ((System.nanoTime() - startTimeNanos) / 1_000_000_000L).toFloat()
        val fps = if (elapsedSec > 0f) samples.size / elapsedSec else 0f
        val verdict = PulsePpgDetector.estimate(samples.toList(), fps)
        _state.update {
            if (verdict == null) {
                it.copy(
                    isMeasuring = false,
                    errorMessage = "Impossible d'extraire un rythme fiable. Recommence en couvrant bien le flash + le capteur avec le bout du doigt.",
                )
            } else {
                it.copy(
                    isMeasuring = false,
                    bpm = verdict.bpm,
                    quality = verdict.quality,
                )
            }
        }
    }

    fun reset() {
        samples.clear()
        _state.value = PpgUiState()
    }

    fun saveMeasured(onDone: () -> Unit) {
        val bpm = _state.value.bpm ?: return
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            habitRepo.saveMorningPulse(today, bpm, HeartRateSource.CAMERA_PPG)
            reset()
            onDone()
        }
    }

    fun cancel() {
        samples.clear()
        _state.update { PpgUiState() }
    }
}

@Composable
fun PulsePpgScreen(
    onBack: () -> Unit,
    vm: PulsePpgViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // Executor pour l'analyzer (une seule instance, réutilisée)
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (hasPermission) {
            // PreviewView invisible-ish (petit, en haut) — l'important c'est l'ImageAnalysis
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    bindPulseCamera(
                        ctx = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        analyzerExecutor = analyzerExecutor,
                        onLuminance = vm::onFrameLuminance,
                    )
                    previewView
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp)
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        // Bouton fermer
        IconButton(
            onClick = { vm.cancel(); onBack() },
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Fermer")
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Pouls au repos — mesure caméra",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Pose ton doigt sur le flash + capteur photo (à l'arrière du téléphone). Ne bouge pas pendant 20 s.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                state.bpm != null -> {
                    Text("${state.bpm}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold)
                    Text("BPM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.quality?.let { q ->
                        val level = when {
                            q > 0.85f -> "Excellente qualité"
                            q > 0.65f -> "Bonne qualité"
                            else -> "Signal faible — reprends si possible"
                        }
                        Text(level, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { vm.reset(); vm.start() }) { Text("Recommencer") }
                        Button(onClick = { vm.saveMeasured(onBack) }) { Text("Enregistrer") }
                    }
                }
                state.isMeasuring -> {
                    Text(
                        "${state.elapsedSec} / $MEASURE_DURATION_SEC s",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    LinearProgressIndicator(
                        progress = { state.elapsedSec.toFloat() / MEASURE_DURATION_SEC },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.fingerNotDetected) {
                        Text(
                            "Couvre bien le flash + le capteur avec ton doigt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    CircularProgressIndicator()
                }
                else -> {
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(onClick = { vm.start() }, enabled = hasPermission) {
                        Text(if (state.errorMessage == null) "Démarrer la mesure" else "Réessayer")
                    }
                    if (!hasPermission) {
                        TextButton(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Autoriser la caméra")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (state.bpm == null && !state.isMeasuring) {
                TextButton(onClick = { vm.cancel(); onBack() }) { Text("Annuler") }
            }
        }
    }
}

private fun bindPulseCamera(
    ctx: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    analyzerExecutor: java.util.concurrent.Executor,
    onLuminance: (Float) -> Unit,
) {
    val providerFuture = ProcessCameraProvider.getInstance(ctx)
    providerFuture.addListener({
        val provider = providerFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(android.util.Size(320, 240))
            .build()
        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
            val avg = averageLuminanceCenter(imageProxy)
            imageProxy.close()
            if (avg != null) onLuminance(avg)
        }
        val selector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        runCatching {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            if (camera.cameraInfo.hasFlashUnit()) {
                camera.cameraControl.enableTorch(true)
            }
        }
    }, ContextCompat.getMainExecutor(ctx))
}

/** Moyenne du plan Y (luminance) sur une région centrale 60×60 de l'image. */
private fun averageLuminanceCenter(image: ImageProxy): Float? {
    val plane = image.planes.getOrNull(0) ?: return null
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val w = image.width
    val h = image.height
    val cx = w / 2
    val cy = h / 2
    val r = 30 // fenêtre 60x60
    var sum = 0L
    var count = 0
    for (y in (cy - r) until (cy + r)) {
        if (y < 0 || y >= h) continue
        for (x in (cx - r) until (cx + r)) {
            if (x < 0 || x >= w) continue
            val idx = y * rowStride + x * pixelStride
            if (idx < 0 || idx >= buffer.capacity()) continue
            sum += buffer.get(idx).toInt() and 0xFF
            count++
        }
    }
    return if (count == 0) null else sum.toFloat() / count
}
