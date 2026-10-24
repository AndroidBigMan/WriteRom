package com.mobiiot.writerom.ui.hud

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mobiiot.writerom.MainActivity
import com.mobiiot.writerom.model.FillProgress
import com.mobiiot.writerom.model.StorageInfo
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.WriteRomTheme
import com.mobiiot.writerom.utils.StorageHelper

/**
 * 悬浮窗微型仪表盘单例管理器：
 * 负责 WindowManager 挂载、磁吸吸附动画、手势坐标运算、生命周期绑定及状态分发。
 */
class FloatingHudManager private constructor(private val context: Context) {

    interface HudActionListener {
        fun onTogglePause()
        fun onStopTask()
        fun onHudVisibilityChanged(isVisible: Boolean)
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("write_rom_prefs", Context.MODE_PRIVATE)

    private var composeView: ComposeView? = null
    private var lifecycleOwner: FloatingHudLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    var isHudShown by mutableStateOf(false)
        private set

    private var actionListener: HudActionListener? = null

    // 响应式 Compose 状态
    private var formattedFreeState by mutableStateOf("0 B")
    private var freeBytesState by mutableLongStateOf(0L)
    private var percentUsedState by mutableDoubleStateOf(0.0)
    private var formattedSpeedState by mutableStateOf("0 KB/s")
    private var taskStatusState by mutableStateOf(TaskStatus.IDLE)
    private var activeThreadsState by mutableIntStateOf(1)
    private var isSlcExhaustedState by mutableStateOf(false)
    private var deviceTemperatureState by mutableFloatStateOf(0f)
    private var thermalStatusLabelState by mutableStateOf("正常")

    companion object {
        private const val PREF_KEY_HUD_ENABLED = "floating_hud_enabled"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: FloatingHudManager? = null

        @JvmStatic
        fun getInstance(context: Context): FloatingHudManager {
            return instance ?: synchronized(this) {
                instance ?: FloatingHudManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setActionListener(listener: HudActionListener?) {
        this.actionListener = listener
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isHudEnabledPref(): Boolean {
        return prefs.getBoolean(PREF_KEY_HUD_ENABLED, false)
    }

    fun setHudEnabledPref(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_HUD_ENABLED, enabled).apply()
    }

    /**
     * 显示悬浮窗
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showHud(): Boolean {
        if (!canDrawOverlays()) {
            return false
        }
        if (isHudShown && composeView != null) {
            return true
        }

        mainHandler.post {
            try {
                initAndAttachView()
                isHudShown = true
                setHudEnabledPref(true)
                actionListener?.onHudVisibilityChanged(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    /**
     * 隐藏/移除悬浮窗
     */
    fun hideHud() {
        if (!isHudShown && composeView == null) return

        mainHandler.post {
            try {
                composeView?.let { view ->
                    windowManager.removeViewImmediate(view)
                }
                lifecycleOwner?.onDestroy()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                composeView = null
                lifecycleOwner = null
                layoutParams = null
                isHudShown = false
                setHudEnabledPref(false)
                actionListener?.onHudVisibilityChanged(false)
            }
        }
    }

    /**
     * 切换显隐
     */
    fun toggleHud(): Boolean {
        return if (isHudShown) {
            hideHud()
            false
        } else {
            showHud()
        }
    }

    /**
     * 状态更新（来自 MainActivity.updateUiState）
     */
    fun updateState(state: StorageUiState) {
        mainHandler.post {
            formattedFreeState = state.formattedFree
            freeBytesState = state.freeBytes
            percentUsedState = state.percentUsed
            formattedSpeedState = state.formattedSpeed
            taskStatusState = state.taskStatus
            activeThreadsState = state.activeThreads
            isSlcExhaustedState = state.isSlcExhausted
            deviceTemperatureState = state.deviceTemperature
            thermalStatusLabelState = state.thermalStatusLabel
        }
    }

    /**
     * 后台服务进度实时更新（确保 MainActivity 切到后台时依然实时刷新）
     */
    fun updateProgress(progress: FillProgress, storageInfo: StorageInfo) {
        mainHandler.post {
            formattedFreeState = storageInfo.formattedFree
            freeBytesState = storageInfo.freeBytes
            percentUsedState = storageInfo.percentUsed
            formattedSpeedState = progress.formattedSpeed
            taskStatusState = progress.status
            activeThreadsState = progress.activeThreads
            isSlcExhaustedState = progress.isSlcExhausted
        }
    }

    private fun initAndAttachView() {
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = (screenHeight * 0.28f).toInt()
        }
        layoutParams = params

        // 宿主生命周期持有器绑定
        val owner = FloatingHudLifecycleOwner()
        lifecycleOwner = owner
        owner.onCreate()
        owner.onStart()

        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)

            setContent {
                WriteRomTheme {
                    FloatingHudComponent(
                        formattedFree = formattedFreeState,
                        freeBytes = freeBytesState,
                        percentUsed = percentUsedState,
                        formattedSpeed = formattedSpeedState,
                        taskStatus = taskStatusState,
                        activeThreads = activeThreadsState,
                        isSlcExhausted = isSlcExhaustedState,
                        deviceTemperature = deviceTemperatureState,
                        thermalStatusLabel = thermalStatusLabelState,
                        onDrag = { dx, dy -> handleDrag(dx, dy) },
                        onDragEnd = { snapToEdge() },
                        onTogglePause = { actionListener?.onTogglePause() },
                        onStopTask = { actionListener?.onStopTask() },
                        onOpenApp = { openMainActivity() },
                        onCloseHud = { hideHud() }
                    )
                }
            }
        }

        composeView = view
        windowManager.addView(view, params)
    }

    private fun handleDrag(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        val view = composeView ?: return

        params.x += dx.toInt()
        params.y += dy.toInt()

        // 垂直方向防移出屏幕
        val displayMetrics = context.resources.displayMetrics
        params.y = params.y.coerceIn(40, displayMetrics.heightPixels - 120)

        windowManager.updateViewLayout(view, params)
    }

    /**
     * 松手后磁吸贴边动画（自动靠左或靠右）
     */
    private fun snapToEdge() {
        val params = layoutParams ?: return
        val view = composeView ?: return
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val viewWidth = view.width.coerceAtLeast(100)

        val targetX = if (params.x + viewWidth / 2 < screenWidth / 2) {
            16
        } else {
            screenWidth - viewWidth - 16
        }

        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                if (composeView != null && layoutParams != null) {
                    params.x = animation.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
        animator.start()
    }

    private fun openMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }
}
