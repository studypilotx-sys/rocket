package com.studypilot.app.guardian

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Privacy-first on-device presence analyzer.
 * Processes camera frames entirely in memory to detect human presence.
 * Never saves, records, encodes, or uploads frames or biometric identifiers.
 */
class PresenceAnalyzer(
    private val onPresenceSampled: (isPersonDetected: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCount = 0

    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        // Throttle analysis to ~4-5 times per second for battery and CPU efficiency
        if (frameCount % 4 != 0) {
            imageProxy.close()
            return
        }

        try {
            val yPlane = imageProxy.planes.getOrNull(0)
            if (yPlane == null) {
                imageProxy.close()
                return
            }

            val buffer: ByteBuffer = yPlane.buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride

            // Analyze central upper ROI (30% to 70% width, 15% to 75% height)
            val startX = (width * 0.25).toInt()
            val endX = (width * 0.75).toInt()
            val startY = (height * 0.15).toInt()
            val endY = (height * 0.75).toInt()

            var totalLuminance = 0L
            var sampleCount = 0
            val step = 16 // Step sampling for fast lightweight computation

            for (y in startY until endY step step) {
                for (x in startX until endX step step) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.limit()) {
                        val lum = buffer.get(index).toInt() and 0xFF
                        totalLuminance += lum
                        sampleCount++
                    }
                }
            }

            if (sampleCount == 0) {
                imageProxy.close()
                return
            }

            val meanLuminance = totalLuminance / sampleCount.toDouble()

            // Calculate standard variance and edge gradients across the sampled grid
            var varianceSum = 0.0
            var edgeCount = 0

            for (y in startY until endY step step) {
                for (x in startX until endX step step) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.limit()) {
                        val lum = buffer.get(index).toInt() and 0xFF
                        val diff = lum - meanLuminance
                        varianceSum += diff * diff

                        // Edge contrast with horizontal neighbor
                        val nextXIndex = y * rowStride + (x + step) * pixelStride
                        if (nextXIndex < buffer.limit() && x + step < endX) {
                            val nextLum = buffer.get(nextXIndex).toInt() and 0xFF
                            if (kotlin.math.abs(lum - nextLum) > 22) {
                                edgeCount++
                            }
                        }
                    }
                }
            }

            val variance = varianceSum / sampleCount
            val stdDev = kotlin.math.sqrt(variance)
            val edgeRatio = edgeCount.toDouble() / sampleCount.coerceAtLeast(1)

            // A present human subject in front of camera creates natural luminance spread
            // (hair, skin, clothing, background separation) and edges (shoulders, head contour).
            // A blank ceiling, floor, empty room with flat illumination, or completely covered lens has
            // either stdDev < 12, edgeRatio < 0.04, or extreme pitch black / blown-out white.
            val isPresent = stdDev >= 13.0 &&
                    edgeRatio >= 0.05 &&
                    meanLuminance in 18.0..245.0

            onPresenceSampled(isPresent)
        } catch (_: Exception) {
            // Defensive catch to prevent crashes during camera lifecycle transitions
        } finally {
            imageProxy.close()
        }
    }
}
