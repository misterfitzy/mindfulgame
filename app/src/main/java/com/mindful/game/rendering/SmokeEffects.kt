package com.mindful.game.rendering

import android.graphics.Color
import kotlin.math.*

/**
 * Advanced smoke effects utility for photorealistic smoke rendering
 * with density-based opacity, volumetric lighting, and turbulence calculation.
 */
class SmokeEffects {
    
    fun calculateSmokeColor(
        whiteIntensity: Float, 
        blackIntensity: Float, 
        turbulence: Float,
        position: Pair<Float, Float>
    ): Int {
        val (x, y) = position
        val totalIntensity = whiteIntensity + blackIntensity
        
        if (totalIntensity <= 0f) return Color.TRANSPARENT
        
        // Calculate base mixing
        val mixRatio = blackIntensity / totalIntensity
        val density = totalIntensity.coerceIn(0f, 1f)
        
        // Smoke density affects opacity and color
        val baseOpacity = (density * 255f).toInt().coerceIn(0, 255)
        
        // Color temperature based on density and turbulence
        val temperature = density * (1f + turbulence * 0.3f)
        
        // Warmer colors in dense, turbulent areas
        val red = (128 + temperature * 80f + sin(x * 10f) * 10f).toInt().coerceIn(0, 255)
        val green = (128 + temperature * 60f + cos(y * 12f) * 8f).toInt().coerceIn(0, 255)
        val blue = (128 + temperature * 40f + sin(x * 8f + y * 8f) * 6f).toInt().coerceIn(0, 255)
        
        // Apply mixing ratio
        val finalRed = (red * (1f - mixRatio) + 50 * mixRatio).toInt().coerceIn(0, 255)
        val finalGreen = (green * (1f - mixRatio) + 50 * mixRatio).toInt().coerceIn(0, 255)
        val finalBlue = (blue * (1f - mixRatio) + 50 * mixRatio).toInt().coerceIn(0, 255)
        
        return Color.argb(baseOpacity, finalRed, finalGreen, finalBlue)
    }
    
    fun applyVolumetricLighting(
        baseColor: Int,
        depth: Float,
        lightDirection: Pair<Float, Float> = 0.3f to -0.7f
    ): Int {
        val (lightX, lightY) = lightDirection
        
        // Simulate light scattering through smoke
        val scattering = (1f - depth * 0.3f).coerceIn(0.7f, 1f)
        
        val red = (Color.red(baseColor) * scattering).toInt().coerceIn(0, 255)
        val green = (Color.green(baseColor) * scattering).toInt().coerceIn(0, 255)
        val blue = (Color.blue(baseColor) * scattering).toInt().coerceIn(0, 255)
        
        return Color.argb(Color.alpha(baseColor), red, green, blue)
    }
    
    fun calculateTurbulenceIntensity(
        velocityX: Float,
        velocityY: Float,
        neighbors: FloatArray
    ): Float {
        // Calculate vorticity (curl) from velocity field
        val curl = abs(velocityX - velocityY) * 0.5f
        
        // Add spatial turbulence from color gradients
        val gradientVariance = if (neighbors.size >= 4) {
            val variance = neighbors.map { (it - neighbors.average()).pow(2) }.average()
            sqrt(variance).toFloat()
        } else 0f
        
        return (curl + gradientVariance * 2f).coerceIn(0f, 1f)
    }
    
    fun calculateDensity(whiteIntensity: Float, blackIntensity: Float): Float {
        val totalIntensity = whiteIntensity + blackIntensity
        return totalIntensity.coerceIn(0f, 1f)
    }
    
    fun calculateMixingActivity(whiteIntensity: Float, blackIntensity: Float): Float {
        return if (whiteIntensity > 0.05f && blackIntensity > 0.05f) {
            min(whiteIntensity, blackIntensity) * 2f
        } else 0f
    }
    
    fun generateSmokeNoise(x: Float, y: Float, time: Float, octaves: Int = 3): Float {
        var noise = 0f
        var amplitude = 1f
        var frequency = 1f
        var maxValue = 0f
        
        for (i in 0 until octaves) {
            noise += sin(x * frequency + time) * cos(y * frequency + time * 0.7f) * amplitude
            maxValue += amplitude
            amplitude *= 0.5f
            frequency *= 2f
        }
        
        return noise / maxValue
    }
    
    fun applyTemperatureGradient(baseColor: Int, temperature: Float): Int {
        val warmth = temperature.coerceIn(0f, 1f)
        
        val red = (Color.red(baseColor) * (1f + warmth * 0.2f)).toInt().coerceIn(0, 255)
        val green = (Color.green(baseColor) * (1f + warmth * 0.1f)).toInt().coerceIn(0, 255)
        val blue = (Color.blue(baseColor) * (1f - warmth * 0.1f)).toInt().coerceIn(0, 255)
        
        return Color.argb(Color.alpha(baseColor), red, green, blue)
    }
}
