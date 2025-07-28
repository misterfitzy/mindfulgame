package com.mindful.game.rendering

import android.graphics.*
import com.mindful.game.core.interfaces.IBoundaryRenderer
import com.mindful.game.core.interfaces.IPhysicsEngine

/**
 * Advanced renderer that visualizes fluid dynamics with smooth color transitions
 * and gradient mixing effects.
 */
class FluidRenderer : IBoundaryRenderer {
    
    private val whitePaint = Paint().apply {
        isAntiAlias = true
    }
    
    private val blackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val boundaryPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
        alpha = 100
    }
    
    private val progressBarPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val progressBarBackgroundPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private var quality = 1 // 0=low, 1=medium, 2=high
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var pixelArray: IntArray? = null
    private var lastWidth = 0
    private var lastHeight = 0
    
    // Performance optimization - Enhanced sampling for smoke-like smoothness
    private val sampleRate = intArrayOf(2, 1, 1) // Always high quality, sub-pixel on ultra
    
    // Advanced smoke effects
    private val smokeEffects = SmokeEffects()
    
    override fun render(canvas: Canvas, physicsEngine: IPhysicsEngine, width: Int, height: Int) {
        // Initialize or resize bitmap if needed
        if (bitmap == null || width != lastWidth || height != lastHeight) {
            initializeBitmap(width, height)
        }
        
        // Clear canvas
        canvas.drawColor(Color.WHITE)
        
        // Render fluid field
        renderFluidField(canvas, physicsEngine, width, height)
        
        // Draw boundary line for reference (optional based on quality)
        if (quality > 0) {
            renderBoundaryLine(canvas, width, height, physicsEngine.getBoundaryPoints())
        }
        
        // Draw progress bar
        renderProgressBar(canvas, width, height, physicsEngine.getRestorationProgress())
        
        // Draw mixing indicator
        renderMixingIndicator(canvas, width, height, physicsEngine)
    }
    
    private fun initializeBitmap(width: Int, height: Int) {
        bitmap?.recycle()
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        this.canvas = Canvas(bitmap!!)
        pixelArray = IntArray(width * height)
        lastWidth = width
        lastHeight = height
    }
    
    private fun renderFluidField(canvas: Canvas, physicsEngine: IPhysicsEngine, width: Int, height: Int) {
        val bitmap = this.bitmap ?: return
        val pixelArray = this.pixelArray ?: return
        
        // Use sub-pixel sampling for ultra-smooth gradients
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = if (quality >= 2) {
                    // Multi-sampling for highest quality
                    sampleColorWithMultiSampling(x, y, width, height, physicsEngine)
                } else {
                    // Bilinear interpolation for medium quality
                    sampleColorWithInterpolation(x, y, width, height, physicsEngine)
                }
                
                pixelArray[y * width + x] = color
            }
        }
        
        // Apply edge feathering for highest quality
        if (quality >= 2) {
            applyEdgeFeathering(pixelArray, width, height)
        }
        
        bitmap.setPixels(pixelArray, 0, width, 0, 0, width, height)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }
    
    private fun calculateMixedColor(whiteIntensity: Float, blackIntensity: Float): Int {
        // Normalize intensities to ensure they sum to 1
        val totalIntensity = whiteIntensity + blackIntensity
        val normalizedWhite = if (totalIntensity > 0) whiteIntensity / totalIntensity else 0.5f
        val normalizedBlack = if (totalIntensity > 0) blackIntensity / totalIntensity else 0.5f
        
        // Create smooth gradient between white and black
        val grayValue = (normalizedWhite * 255).toInt().coerceIn(0, 255)
        
        // Add subtle color variations in mixing zones
        if (whiteIntensity > 0.1f && blackIntensity > 0.1f) {
            // In mixing zone - add subtle blue tint to show active mixing
            val mixingIntensity = kotlin.math.min(whiteIntensity, blackIntensity) * 2f
            val blueTint = (mixingIntensity * 30).toInt().coerceIn(0, 30)
            
            val red = (grayValue - blueTint / 2).coerceIn(0, 255)
            val green = (grayValue - blueTint / 2).coerceIn(0, 255)
            val blue = (grayValue + blueTint).coerceIn(0, 255)
            
            return Color.rgb(red, green, blue)
        }
        
        return Color.rgb(grayValue, grayValue, grayValue)
    }
    
    private fun sampleColorWithInterpolation(
        pixelX: Int, pixelY: Int, width: Int, height: Int, physicsEngine: IPhysicsEngine
    ): Int {
        val normalizedX = pixelX.toFloat() / width
        val normalizedY = pixelY.toFloat() / height
        
        // Sample at exact pixel center
        val whiteIntensity = physicsEngine.getLeftRegionIntensity(normalizedX, normalizedY)
        val blackIntensity = physicsEngine.getRightRegionIntensity(normalizedX, normalizedY)
        
        return calculateEnhancedMixedColor(whiteIntensity, blackIntensity, normalizedX, normalizedY)
    }
    
    private fun sampleColorWithMultiSampling(
        pixelX: Int, pixelY: Int, width: Int, height: Int, physicsEngine: IPhysicsEngine
    ): Int {
        val sampleOffsets = arrayOf(
            -0.25f to -0.25f,
            0.25f to -0.25f,
            -0.25f to 0.25f,
            0.25f to 0.25f
        )
        
        var totalRed = 0f
        var totalGreen = 0f
        var totalBlue = 0f
        
        for ((offsetX, offsetY) in sampleOffsets) {
            val sampleX = (pixelX + offsetX) / width
            val sampleY = (pixelY + offsetY) / height
            
            val whiteIntensity = physicsEngine.getLeftRegionIntensity(sampleX, sampleY)
            val blackIntensity = physicsEngine.getRightRegionIntensity(sampleX, sampleY)
            
            val color = calculateEnhancedMixedColor(whiteIntensity, blackIntensity, sampleX, sampleY)
            
            totalRed += Color.red(color)
            totalGreen += Color.green(color)
            totalBlue += Color.blue(color)
        }
        
        val avgRed = (totalRed / 4f).toInt().coerceIn(0, 255)
        val avgGreen = (totalGreen / 4f).toInt().coerceIn(0, 255)
        val avgBlue = (totalBlue / 4f).toInt().coerceIn(0, 255)
        
        return Color.rgb(avgRed, avgGreen, avgBlue)
    }
    
    private fun calculateEnhancedMixedColor(
        whiteIntensity: Float, blackIntensity: Float, x: Float, y: Float
    ): Int {
        val totalIntensity = whiteIntensity + blackIntensity
        if (totalIntensity <= 0f) return Color.GRAY
        
        // Calculate turbulence using noise generation
        val turbulence = smokeEffects.generateSmokeNoise(x, y, System.currentTimeMillis() / 1000f)
        
        // Use SmokeEffects for advanced color calculation
        val smokeColor = smokeEffects.calculateSmokeColor(
            whiteIntensity, blackIntensity, turbulence, x to y
        )
        
        // Apply volumetric lighting for depth
        val depth = smokeEffects.calculateDensity(whiteIntensity, blackIntensity)
        val litColor = smokeEffects.applyVolumetricLighting(smokeColor, depth)
        
        // Apply temperature gradient for realism
        val temperature = smokeEffects.calculateMixingActivity(whiteIntensity, blackIntensity)
        return smokeEffects.applyTemperatureGradient(litColor, temperature)
    }
    
    private fun renderBoundaryLine(
        canvas: Canvas,
        width: Int,
        height: Int,
        boundaryPoints: FloatArray
    ) {
        if (boundaryPoints.size < 4) return
        
        val numPoints = boundaryPoints.size / 2
        
        for (i in 0 until numPoints - 1) {
            val x1 = boundaryPoints[i * 2] * width
            val y1 = boundaryPoints[i * 2 + 1] * height
            val x2 = boundaryPoints[(i + 1) * 2] * width
            val y2 = boundaryPoints[(i + 1) * 2 + 1] * height
            
            canvas.drawLine(x1, y1, x2, y2, boundaryPaint)
        }
    }
    
    private fun renderProgressBar(canvas: Canvas, width: Int, height: Int, progress: Float) {
        val barWidth = width * 0.3f
        val barHeight = 20f
        val barX = width - barWidth - 30f
        val barY = 30f
        
        // Draw background
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, progressBarBackgroundPaint)
        
        // Draw progress
        canvas.drawRect(
            barX, 
            barY, 
            barX + barWidth * progress, 
            barY + barHeight, 
            progressBarPaint
        )
        
        // Draw border
        boundaryPaint.style = Paint.Style.STROKE
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, boundaryPaint)
        boundaryPaint.style = Paint.Style.FILL
        
        // Draw progress text
        val progressText = "Mixing: ${(progress * 100).toInt()}%"
        canvas.drawText(progressText, barX, barY - 10f, textPaint)
    }
    
    private fun renderMixingIndicator(canvas: Canvas, width: Int, height: Int, physicsEngine: IPhysicsEngine) {
        // Calculate mixing activity across the screen
        var mixingActivity = 0f
        val samplePoints = 20
        
        for (y in 0 until samplePoints) {
            for (x in 0 until samplePoints) {
                val normalizedX = x.toFloat() / samplePoints
                val normalizedY = y.toFloat() / samplePoints
                
                val whiteIntensity = physicsEngine.getLeftRegionIntensity(normalizedX, normalizedY)
                val blackIntensity = physicsEngine.getRightRegionIntensity(normalizedX, normalizedY)
                
                // Areas where both colors are present indicate active mixing
                if (whiteIntensity > 0.1f && blackIntensity > 0.1f) {
                    mixingActivity += kotlin.math.min(whiteIntensity, blackIntensity)
                }
            }
        }
        
        mixingActivity = (mixingActivity / (samplePoints * samplePoints)).coerceIn(0f, 1f)
        
        // Draw mixing activity indicator
        if (mixingActivity > 0.1f) {
            val indicatorSize = 15f + mixingActivity * 10f
            val indicatorX = 30f
            val indicatorY = 30f
            
            // Pulsing circle to show mixing activity
            val alpha = (128 + mixingActivity * 127).toInt()
            val mixingPaint = Paint().apply {
                color = Color.CYAN
                style = Paint.Style.FILL
                this.alpha = alpha
                isAntiAlias = true
            }
            
            canvas.drawCircle(indicatorX, indicatorY, indicatorSize, mixingPaint)
            
            // Text label
            textPaint.textSize = 18f
            canvas.drawText("MIXING", indicatorX + 25f, indicatorY + 5f, textPaint)
        }
    }
    
    override fun setQuality(quality: Int) {
        this.quality = quality.coerceIn(0, 2)
        
        // Adjust anti-aliasing based on quality
        val antiAlias = quality > 0
        whitePaint.isAntiAlias = antiAlias
        blackPaint.isAntiAlias = antiAlias
        boundaryPaint.isAntiAlias = antiAlias
        progressBarPaint.isAntiAlias = antiAlias
        textPaint.isAntiAlias = antiAlias
        
        // Adjust boundary line visibility
        boundaryPaint.alpha = when (quality) {
            0 -> 0      // Hidden in low quality
            1 -> 50     // Semi-transparent in medium
            2 -> 100    // More visible in high quality
            else -> 50
        }
    }
    
    override fun update(deltaTime: Float) {
        // Could add particle effects or other animations here
        // For now, the fluid simulation provides all the animation
    }
    
    private fun applyEdgeFeathering(pixelArray: IntArray, width: Int, height: Int) {
        val temp = pixelArray.copyOf()
        val featherRadius = 2
        
        for (y in featherRadius until height - featherRadius) {
            for (x in featherRadius until width - featherRadius) {
                val index = x + y * width
                
                // Calculate local gradient
                val gradient = calculateGradientMagnitude(temp, x, y, width)
                
                if (gradient > 50f) { // Threshold for edge detection
                    // Apply Gaussian blur to soften edges
                    var weightedSum = 0f
                    var totalWeight = 0f
                    
                    for (dy in -featherRadius..featherRadius) {
                        for (dx in -featherRadius..featherRadius) {
                            val distance = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                            if (distance <= featherRadius) {
                                val weight = kotlin.math.exp(-distance * distance / (2f * featherRadius * featherRadius / 3f))
                                val sampleIndex = (x + dx) + (y + dy) * width
                                
                                if (sampleIndex in temp.indices) {
                                    val grayValue = (Color.red(temp[sampleIndex]) + 
                                                   Color.green(temp[sampleIndex]) + 
                                                   Color.blue(temp[sampleIndex])) / 3f
                                    weightedSum += grayValue * weight
                                    totalWeight += weight
                                }
                            }
                        }
                    }
                    
                    if (totalWeight > 0f) {
                        val smoothedGray = (weightedSum / totalWeight).toInt().coerceIn(0, 255)
                        pixelArray[index] = Color.rgb(smoothedGray, smoothedGray, smoothedGray)
                    }
                }
            }
        }
    }
    
    private fun calculateGradientMagnitude(pixels: IntArray, x: Int, y: Int, width: Int): Float {
        val left = if (x > 0) Color.red(pixels[(x-1) + y * width]) else Color.red(pixels[x + y * width])
        val right = if (x < width-1) Color.red(pixels[(x+1) + y * width]) else Color.red(pixels[x + y * width])
        val top = if (y > 0) Color.red(pixels[x + (y-1) * width]) else Color.red(pixels[x + y * width])
        val bottom = if (y < pixels.size/width-1) Color.red(pixels[x + (y+1) * width]) else Color.red(pixels[x + y * width])
        
        val gradX = (right - left) / 2f
        val gradY = (bottom - top) / 2f
        
        return kotlin.math.sqrt(gradX * gradX + gradY * gradY)
    }
    
    fun cleanup() {
        bitmap?.recycle()
        bitmap = null
    }
}
