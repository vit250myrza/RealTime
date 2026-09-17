package com.opensource.gpstime

import android.graphics.Color
import androidx.core.graphics.ColorUtils

object UiColorUtils {

    fun getContrastingTextColor(color: Int): Int {
        val luminance = ColorUtils.calculateLuminance(color)
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    fun adjustAlpha(color: Int, alpha: Float): Int {
        val alphaInt = Math.round(Color.alpha(color) * alpha).coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alphaInt shl 24)
    }

    /**
     * Ensures that [color] has at least [minContrast]:1 contrast ratio against [backgroundColor].
     * If it doesn't, adjusts lightness in HSL space until required contrast is achieved.
     */
    fun ensureContrast(color: Int, backgroundColor: Int, minContrast: Double = 4.5): Int {
        if (ColorUtils.calculateContrast(color, backgroundColor) >= minContrast) {
            return color
        }
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        val bgLuminance = ColorUtils.calculateLuminance(backgroundColor)

        if (bgLuminance < 0.5) {
            // Dark background: increase lightness
            while (hsl[2] < 1.0f && ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), backgroundColor) < minContrast) {
                hsl[2] = (hsl[2] + 0.04f).coerceAtMost(1.0f)
            }
        } else {
            // Light background: decrease lightness
            while (hsl[2] > 0.0f && ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), backgroundColor) < minContrast) {
                hsl[2] = (hsl[2] - 0.04f).coerceAtLeast(0.0f)
            }
        }
        return ColorUtils.HSLToColor(hsl)
    }
}
