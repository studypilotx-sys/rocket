package com.studypilot.app.guardian

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.view.PreviewView
import com.studypilot.app.R
import com.studypilot.app.data.model.PresenceState
import java.util.Locale

/**
 * Native Android floating overlay window that displays "Guardian Active" above other apps
 * (e.g., YouTube, Chrome, PDF reader).
 * 
 * Features:
 * - Movable / draggable across the screen with touch tracking
 * - Displays active timer and presence status badge
 * - Contains PreviewView for real CameraX front-camera presence analysis
 * - Has "End Session" action to cleanly terminate the session
 * - Has "Open App" action to bring StudyPilot to foreground
 */
class GuardianOverlayManager(
    private val context: Context,
    private val onEndSessionClicked: () -> Unit,
    private val onOpenAppClicked: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isOverlayAttached = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var timerTextView: TextView? = null
    private var statusTextView: TextView? = null
    private var statusDotView: View? = null
    private var previewView: PreviewView? = null

    val isShowing: Boolean
        get() = isOverlayAttached

    fun getCameraPreviewView(): PreviewView? = previewView

    fun showOverlay() {
        if (isOverlayAttached) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 120
        }

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_guardian_overlay, null)
        overlayView = view

        timerTextView = view.findViewById(R.id.text_overlay_timer)
        statusTextView = view.findViewById(R.id.text_overlay_status)
        statusDotView = view.findViewById(R.id.view_overlay_status_dot)
        previewView = view.findViewById(R.id.preview_overlay_camera)

        val headerView = view.findViewById<View>(R.id.layout_overlay_header)
        val btnClose = view.findViewById<ImageView>(R.id.btn_overlay_end)

        btnClose?.setOnClickListener {
            onEndSessionClicked()
        }

        view.setOnClickListener {
            onOpenAppClicked()
        }

        // Draggable floating touch handler
        headerView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    try {
                        if (isOverlayAttached) {
                            windowManager.updateViewLayout(view, params)
                        }
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(view, params)
            isOverlayAttached = true
        } catch (e: Exception) {
            isOverlayAttached = false
        }
    }

    fun updateTimer(activeSeconds: Long) {
        val mins = activeSeconds / 60
        val secs = activeSeconds % 60
        val formatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        timerTextView?.text = formatted
    }

    fun updatePresenceState(state: PresenceState, countdownSec: Int = 0) {
        when (state) {
            PresenceState.PRESENT -> {
                statusTextView?.text = "PRESENT"
                statusTextView?.setTextColor(0xFF4CAF50.toInt())
                statusDotView?.setBackgroundColor(0xFF4CAF50.toInt())
            }
            PresenceState.ABSENT -> {
                if (countdownSec > 0) {
                    statusTextView?.text = "LEAVING (${countdownSec}s)"
                    statusTextView?.setTextColor(0xFFFF9800.toInt())
                    statusDotView?.setBackgroundColor(0xFFFF9800.toInt())
                } else {
                    statusTextView?.text = "ABSENT!"
                    statusTextView?.setTextColor(0xFFE53935.toInt())
                    statusDotView?.setBackgroundColor(0xFFE53935.toInt())
                }
            }
            PresenceState.UNKNOWN -> {
                statusTextView?.text = "SCANNING"
                statusTextView?.setTextColor(0xFFFFB300.toInt())
                statusDotView?.setBackgroundColor(0xFFFFB300.toInt())
            }
        }
    }

    fun hideOverlay() {
        if (!isOverlayAttached) return
        try {
            overlayView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        timerTextView = null
        statusTextView = null
        statusDotView = null
        previewView = null
        isOverlayAttached = false
    }
}
