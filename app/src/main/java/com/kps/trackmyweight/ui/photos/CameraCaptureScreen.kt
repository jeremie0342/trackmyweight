package com.kps.trackmyweight.ui.photos

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class CameraCaptureViewModel @Inject constructor(
    private val photoRepo: PhotoRepository,
) : ViewModel() {

    private val _angle = MutableStateFlow(PhotoAngle.FRONT)
    val angle: StateFlow<PhotoAngle> = _angle.asStateFlow()

    val overlayThumbnailPath: StateFlow<String?> =
        _angle
            .map { photoRepo.getLastForAngle(it)?.thumbnailPath }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAngle(a: PhotoAngle) { _angle.value = a }

    fun save(bytes: ByteArray, widthPx: Int?, heightPx: Int?, onDone: () -> Unit) {
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            photoRepo.capture(today, _angle.value, bytes, widthPx, heightPx)
            onDone()
        }
    }
}

@Composable
fun CameraCaptureScreen(
    onDone: () -> Unit,
    vm: CameraCaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val angle by vm.angle.collectAsState()
    val overlayPath by vm.overlayThumbnailPath.collectAsState()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                bindCamera(ctx, lifecycleOwner, previewView, lensFacing) { ic -> imageCapture = ic }
                previewView
            },
            update = { previewView ->
                bindCamera(context, lifecycleOwner, previewView, lensFacing) { ic -> imageCapture = ic }
            },
            modifier = Modifier.fillMaxSize(),
        )

        overlayPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().alpha(0.30f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhotoAngle.entries.forEach { a ->
                val selected = angle == a
                Text(
                    text = when (a) {
                        PhotoAngle.FRONT -> "Face"
                        PhotoAngle.SIDE_LEFT -> "Profil G"
                        PhotoAngle.SIDE_RIGHT -> "Profil D"
                        PhotoAngle.BACK -> "Dos"
                    },
                    color = if (selected) Color.Black else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color.White else Color.Black.copy(alpha = 0.5f))
                        .clickable { vm.setAngle(a) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp).align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            }) {
                Icon(Icons.Outlined.FlipCameraAndroid, contentDescription = "Bascule caméra", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        val cap = imageCapture ?: return@clickable
                        cap.takePicture(
                            ContextCompat.getMainExecutor(context) as Executor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bytes = image.toJpegBytes()
                                    val w = image.width
                                    val h = image.height
                                    image.close()
                                    vm.save(bytes, w, h, onDone)
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    exc.printStackTrace()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "Capture", tint = Color.Black)
            }
            Spacer(Modifier.size(48.dp))
        }
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int,
    onImageCapture: (ImageCapture) -> Unit,
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
        val provider = providerFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder().build()
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            onImageCapture(imageCapture)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun ImageProxy.toJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}
