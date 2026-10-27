package com.mobiiot.writerom.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.ui.theme.WriteRomTheme

object UiBridge {

    interface UiCallbacks {
        fun onRefreshStorage()
        fun onModeChange(mode: FillMode)
        fun onRemainingBytesChange(bytes: Long)
        fun onPercentageChange(percentage: Int)
        fun onSpecificSizeChange(bytes: Long)
        fun onContentTypeChange(type: FillContentType)
        fun onRandomDataToggle(isRandom: Boolean)
        fun onChunkSizeChange(sizeMb: Int)
        fun onMediaGalleryToggle(enable: Boolean)
        fun onConcurrencyChange(threads: Int)
        fun onFragmentationToggle(enabled: Boolean)
        fun onFragmentSizeLevelChange(level: com.mobiiot.writerom.model.FragmentSizeLevel)
        fun onPunchHolesToggle(enabled: Boolean)
        fun onStartTask()
        fun onPauseTask()
        fun onResumeTask()
        fun onStopTask()
        fun onCleanAll()
        fun onStopClean()
        fun onRequestStoragePermission()
        fun onRequestContactsPermission()
        fun onFloatingHudToggle(enabled: Boolean)
        fun onRequestOverlayPermission()
    }

    class BridgeController(
        private val callbacks: UiCallbacks
    ) {
        var state by mutableStateOf(StorageUiState())

        fun updateState(newState: StorageUiState) {
            state = newState
        }

        fun getCallbacks(): UiCallbacks = callbacks
    }

    @JvmStatic
    fun bindComposeView(
        composeView: ComposeView,
        controller: BridgeController
    ) {
        composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                WriteRomTheme {
                    val currentState = controller.state
                    val cb = controller.getCallbacks()

                    StorageFillerScreen(
                        uiState = currentState,
                        onRefreshStorage = { cb.onRefreshStorage() },
                        onModeChange = { cb.onModeChange(it) },
                        onRemainingBytesChange = { cb.onRemainingBytesChange(it) },
                        onPercentageChange = { cb.onPercentageChange(it) },
                        onSpecificSizeChange = { cb.onSpecificSizeChange(it) },
                        onContentTypeChange = { cb.onContentTypeChange(it) },
                        onRandomDataToggle = { cb.onRandomDataToggle(it) },
                        onChunkSizeChange = { cb.onChunkSizeChange(it) },
                        onMediaGalleryToggle = { cb.onMediaGalleryToggle(it) },
                        onConcurrencyChange = { cb.onConcurrencyChange(it) },
                        onFragmentationToggle = { cb.onFragmentationToggle(it) },
                        onFragmentSizeLevelChange = { cb.onFragmentSizeLevelChange(it) },
                        onPunchHolesToggle = { cb.onPunchHolesToggle(it) },
                        onStart = { cb.onStartTask() },
                        onPause = { cb.onPauseTask() },
                        onResume = { cb.onResumeTask() },
                        onStop = { cb.onStopTask() },
                        onCleanAll = { cb.onCleanAll() },
                        onStopClean = { cb.onStopClean() },
                        onRequestStoragePermission = { cb.onRequestStoragePermission() },
                        onRequestContactsPermission = { cb.onRequestContactsPermission() },
                        onFloatingHudToggle = { cb.onFloatingHudToggle(it) },
                        onRequestOverlayPermission = { cb.onRequestOverlayPermission() }
                    )
                }
            }
        }
    }
}
