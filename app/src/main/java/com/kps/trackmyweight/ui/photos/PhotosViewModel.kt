package com.kps.trackmyweight.ui.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.ProgressPhotoEntity
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class PhotosUiState(
    val photos: List<ProgressPhotoEntity> = emptyList(),
    val byAngle: Map<PhotoAngle, List<ProgressPhotoEntity>> = emptyMap(),
    val selectedAngle: PhotoAngle = PhotoAngle.FRONT,
    val compareFrom: ProgressPhotoEntity? = null,
    val compareTo: ProgressPhotoEntity? = null,
    val isCapturing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val photoRepo: PhotoRepository,
) : ViewModel() {

    private val _selectedAngle = MutableStateFlow(PhotoAngle.FRONT)
    private val _compareIds = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))
    private val _error = MutableStateFlow<String?>(null)
    private val _capturing = MutableStateFlow(false)

    val state: StateFlow<PhotosUiState> = combine(
        photoRepo.observeAll(),
        _selectedAngle,
        _compareIds,
        combine(_error, _capturing) { e, c -> e to c },
    ) { photos, angle, cmp, ec ->
        val byAngle = photos.groupBy { it.angle }
        val forAngle = byAngle[angle].orEmpty().sortedByDescending { it.date }
        val fromP = cmp.first?.let { id -> photos.firstOrNull { it.id == id } } ?: forAngle.lastOrNull()
        val toP = cmp.second?.let { id -> photos.firstOrNull { it.id == id } } ?: forAngle.firstOrNull()
        PhotosUiState(
            photos = photos,
            byAngle = byAngle,
            selectedAngle = angle,
            compareFrom = fromP,
            compareTo = toP,
            isCapturing = ec.second,
            errorMessage = ec.first,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PhotosUiState())

    fun selectAngle(angle: PhotoAngle) {
        _selectedAngle.value = angle
        _compareIds.value = Pair(null, null)
    }

    fun setCompareFrom(id: Long) { _compareIds.value = _compareIds.value.copy(first = id) }
    fun setCompareTo(id: Long) { _compareIds.value = _compareIds.value.copy(second = id) }

    fun capture(bytes: ByteArray, widthPx: Int?, heightPx: Int?) {
        _capturing.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching {
                photoRepo.capture(todayLocal(), _selectedAngle.value, bytes, widthPx, heightPx)
            }.onSuccess { _capturing.value = false }
                .onFailure {
                    _capturing.value = false
                    _error.value = it.message ?: "Erreur pendant la capture"
                }
        }
    }

    fun delete(photo: ProgressPhotoEntity) {
        viewModelScope.launch {
            runCatching { photoRepo.delete(photo) }
                .onFailure { _error.value = it.message ?: "Erreur pendant la suppression" }
        }
    }

    fun clearError() { _error.value = null }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun Pair<Long?, Long?>.copy(first: Long? = this.first, second: Long? = this.second) =
        Pair(first, second)
}
