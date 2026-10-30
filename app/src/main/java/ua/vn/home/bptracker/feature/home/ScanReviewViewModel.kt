package ua.vn.home.bptracker.feature.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.data.repository.MeasurementRepository
import ua.vn.home.bptracker.feature.ocr.OcrEngine
import ua.vn.home.bptracker.feature.ocr.OcrOutcome
import java.io.File

sealed interface ScanReviewState {
    data class Recognizing(val image: Bitmap) : ScanReviewState
    data class Ready(
        val image: Bitmap,
        val sys: String,
        val dia: String,
        val pulse: String,
        val recognized: Boolean,
        val saveOperation: OperationUiState = OperationUiState.Idle,
        val ocrError: Int? = null,
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

class ScanReviewViewModel @JvmOverloads constructor(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val ocrEngine: OcrEngine = ServiceLocator.ocrEngine,
    private val repository: MeasurementRepository = ServiceLocator.measurementRepository,
) : ViewModel() {
    
    private val _state = MutableStateFlow<ScanReviewState?>(null)
    val state: StateFlow<ScanReviewState?> = _state.asStateFlow()

    init {
        val rawPath = savedStateHandle.get<String>("imagePath")
        val imagePath = if (rawPath != null && rawPath.contains("%")) Uri.decode(rawPath) else rawPath
        if (imagePath != null && _state.value == null) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val sys = savedStateHandle.get<String>("sys")?.toIntOrNull()
                    val dia = savedStateHandle.get<String>("dia")?.toIntOrNull()
                    val pul = savedStateHandle.get<String>("pul")?.toIntOrNull()
                    if (sys != null && dia != null && pul != null) {
                        initWithResult(bitmap, sys, dia, pul)
                    } else {
                        initWithImage(bitmap)
                    }
                }
            }
        }
    }

    fun initWithResult(image: Bitmap, sys: Int, dia: Int, pul: Int) {
        savedStateHandle["sys"] = sys.toString()
        savedStateHandle["dia"] = dia.toString()
        savedStateHandle["pul"] = pul.toString()
        _state.update {
            ScanReviewState.Ready(
                image = image,
                sys = sys.toString(),
                dia = dia.toString(),
                pulse = pul.toString(),
                recognized = true
            )
        }
    }

    fun initWithImage(image: Bitmap) {
        _state.update { ScanReviewState.Recognizing(image) }
        
        viewModelScope.launch(Dispatchers.Default) {
            val outcome = ocrEngine.recognize(image)
            
            _state.update {
                when (outcome) {
                    is OcrOutcome.Success -> {
                        savedStateHandle["sys"] = outcome.sys.toString()
                        savedStateHandle["dia"] = outcome.dia.toString()
                        savedStateHandle["pul"] = outcome.pul.toString()
                        ScanReviewState.Ready(
                            image = image,
                            sys = outcome.sys.toString(),
                            dia = outcome.dia.toString(),
                            pulse = outcome.pul.toString(),
                            recognized = true
                        )
                    }
                    is OcrOutcome.Failure -> {
                        Log.w("Ocr", "recognize failed: ${outcome.reason}")
                        ScanReviewState.Ready(
                            image = image,
                            sys = "",
                            dia = "",
                            pulse = "",
                            recognized = false,
                            ocrError = R.string.scan_review_ocr_failed
                        )
                    }
                }
            }
        }
    }

    fun onSysChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        savedStateHandle["sys"] = filtered
        _state.update { current ->
            if (current is ScanReviewState.Ready) {
                current.copy(sys = filtered, saveOperation = OperationUiState.Idle, ocrError = null)
            } else current
        }
    }

    fun onDiaChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        savedStateHandle["dia"] = filtered
        _state.update { current ->
            if (current is ScanReviewState.Ready) {
                current.copy(dia = filtered, saveOperation = OperationUiState.Idle, ocrError = null)
            } else current
        }
    }

    fun onPulseChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        savedStateHandle["pul"] = filtered
        _state.update { current ->
            if (current is ScanReviewState.Ready) {
                current.copy(pulse = filtered, saveOperation = OperationUiState.Idle, ocrError = null)
            } else current
        }
    }

    fun save() {
        val current = _state.value as? ScanReviewState.Ready ?: return
        if (!current.isValid) return

        viewModelScope.launch {
            _state.update { s ->
                (s as? ScanReviewState.Ready)?.copy(saveOperation = OperationUiState.InProgress, ocrError = null) ?: s
            }
            try {
                repository.createMeasurement(
                    sys = current.sysInt!!,
                    dia = current.diaInt!!,
                    pulse = current.pulseInt!!
                )
                _state.update { s ->
                    (s as? ScanReviewState.Ready)?.copy(saveOperation = OperationUiState.Success) ?: s
                }
            } catch (_: Exception) {
                _state.update { s ->
                    (s as? ScanReviewState.Ready)?.copy(saveOperation = OperationUiState.Error("Save failed")) ?: s
                }
            }
        }
    }

    fun recognizeRemote() {
        val current = _state.value as? ScanReviewState.Ready ?: return
        val image = current.image
        
        _state.update { ScanReviewState.Recognizing(image) }
        
        viewModelScope.launch(Dispatchers.Default) {
            val outcome = ocrEngine.recognizeRemote(image)
            _state.update {
                when (outcome) {
                    is OcrOutcome.Success -> {
                        savedStateHandle["sys"] = outcome.sys.toString()
                        savedStateHandle["dia"] = outcome.dia.toString()
                        savedStateHandle["pul"] = outcome.pul.toString()
                        ScanReviewState.Ready(
                            image = image,
                            sys = outcome.sys.toString(),
                            dia = outcome.dia.toString(),
                            pulse = outcome.pul.toString(),
                            recognized = true
                        )
                    }
                    is OcrOutcome.Failure -> ScanReviewState.Ready(
                        image = image,
                        sys = current.sys, 
                        dia = current.dia, 
                        pulse = current.pulse,
                        recognized = false,
                        ocrError = R.string.scan_review_ocr_failed
                    )
                }
            }
        }
    }

    fun reset() {
        _state.update { null }
    }
}
