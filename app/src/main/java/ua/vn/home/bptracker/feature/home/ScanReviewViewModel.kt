package ua.vn.home.bptracker.feature.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.feature.ocr.OcrOutcome

sealed interface ScanReviewState {
    data class Recognizing(val image: Bitmap) : ScanReviewState
    data class Ready(
        val image: Bitmap,
        val sys: String,
        val dia: String,
        val pulse: String,
        val recognized: Boolean,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null
    ) : ScanReviewState {
        val sysInt = sys.toIntOrNull()
        val diaInt = dia.toIntOrNull()
        val pulseInt = pulse.toIntOrNull()

        val sysValid = sysInt in 40..300
        val diaValid = diaInt in 20..200
        val pulseValid = pulseInt in 30..250

        val isValid = sysValid && diaValid && pulseValid
        
        val zone: BpZone? = if (sysInt != null && diaInt != null) {
            BpZone.classify(sysInt, diaInt)
        } else null
    }
}

class ScanReviewViewModel : ViewModel() {
    private val ocrEngine = ServiceLocator.ocrEngine
    private val repository = ServiceLocator.measurementRepository
    
    private val _state = MutableStateFlow<ScanReviewState?>(null)
    val state: StateFlow<ScanReviewState?> = _state.asStateFlow()

    fun initWithImage(image: Bitmap) {
        _state.value = ScanReviewState.Recognizing(image)
        
        viewModelScope.launch(Dispatchers.Default) {
            val outcome = ocrEngine.recognize(image)
            
            _state.value = when (outcome) {
                is OcrOutcome.Success -> ScanReviewState.Ready(
                    image = image,
                    sys = outcome.sys.toString(),
                    dia = outcome.dia.toString(),
                    pulse = outcome.pul.toString(),
                    recognized = true
                )
                is OcrOutcome.Failure -> ScanReviewState.Ready(
                    image = image,
                    sys = "",
                    dia = "",
                    pulse = "",
                    recognized = false,
                    error = "OCR failed: ${outcome.reason}"
                )
            }
        }
    }

    fun onSysChange(value: String) {
        val current = _state.value as? ScanReviewState.Ready ?: return
        _state.value = current.copy(sys = value.filter { it.isDigit() }, error = null)
    }

    fun onDiaChange(value: String) {
        val current = _state.value as? ScanReviewState.Ready ?: return
        _state.value = current.copy(dia = value.filter { it.isDigit() }, error = null)
    }

    fun onPulseChange(value: String) {
        val current = _state.value as? ScanReviewState.Ready ?: return
        _state.value = current.copy(pulse = value.filter { it.isDigit() }, error = null)
    }

    fun save() {
        val current = _state.value as? ScanReviewState.Ready ?: return
        if (!current.isValid) return

        viewModelScope.launch {
            _state.value = current.copy(saving = true, error = null)
            try {
                repository.createMeasurement(
                    sys = current.sysInt!!,
                    dia = current.diaInt!!,
                    pulse = current.pulseInt!!
                )
                _state.value = current.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _state.value = current.copy(saving = false, error = e.message ?: "Save failed")
            }
        }
    }

    fun recognizeRemote() {
        val current = _state.value as? ScanReviewState.Ready ?: return
        val image = current.image
        
        _state.value = ScanReviewState.Recognizing(image)
        
        viewModelScope.launch(Dispatchers.Default) {
            val outcome = ocrEngine.recognizeRemote(image)
            _state.value = when (outcome) {
                is OcrOutcome.Success -> ScanReviewState.Ready(
                    image = image,
                    sys = outcome.sys.toString(),
                    dia = outcome.dia.toString(),
                    pulse = outcome.pul.toString(),
                    recognized = true
                )
                is OcrOutcome.Failure -> ScanReviewState.Ready(
                    image = image,
                    sys = "", dia = "", pulse = "",
                    recognized = false,
                    error = "Remote OCR failed: ${outcome.reason}"
                )
            }
        }
    }

    fun reset() {
        _state.value = null
    }
}
