# Smoke Resolution Implementation Plan

## Executive Summary
This document provides a practical implementation roadmap for enhancing the fluid mixing system to achieve photorealistic smoke appearance with significantly improved visual resolution. The plan is designed for phased execution with immediate visual improvements.

## Quick Win: Immediate Resolution Boost (15 minutes)

### Step 1: Increase Grid Resolution
**File**: `MindfulGame/app/src/main/java/com/mindful/game/physics/FluidDynamicsEngine.kt`

**Current State**:
```kotlin
private var gridWidth = 64
private var gridHeight = 48
```

**Enhanced State**:
```kotlin
private var gridWidth = 128  // 2x increase
private var gridHeight = 96  // 2x increase
```

**Impact**: Immediately doubles visual detail with 4x more simulation cells

### Step 2: Improve Renderer Sampling
**File**: `MindfulGame/app/src/main/java/com/mindful/game/rendering/FluidRenderer.kt`

**Current Sampling**:
```kotlin
private val sampleRate = intArrayOf(4, 2, 1) // Sample every N pixels based on quality
```

**Enhanced Sampling**:
```kotlin
private val sampleRate = intArrayOf(2, 1, 1) // Always high quality, sub-pixel on ultra
```

**Impact**: Eliminates pixelation, provides smooth gradients

## Phase 1: Core Visual Enhancement (1-2 hours)

### 1.1 Enhanced FluidDynamicsEngine

#### Multi-Scale Turbulence Enhancement
Add to `FluidDynamicsEngine.kt`:

```kotlin
// Enhanced turbulence parameters
private val turbulenceOctaves = 5  // Increased from implicit 3
private val turbulenceFrequencies = floatArrayOf(1f, 2f, 4f, 8f, 16f)
private val turbulenceAmplitudes = floatArrayOf(1f, 0.6f, 0.36f, 0.216f, 0.1296f)

private fun injectEnhancedTurbulence(deltaTime: Float) {
    noiseOffset += deltaTime * 0.5f
    
    val centerX = gridWidth / 2
    val injectionRadius = (mixingIntensity * gridWidth * 0.3f).toInt()
    
    for (y in 0 until gridHeight) {
        for (x in (centerX - injectionRadius)..(centerX + injectionRadius)) {
            if (x < 0 || x >= gridWidth) continue
            
            val index = x + y * gridWidth
            val distanceFromCenter = abs(x - centerX).toFloat()
            val normalizedDistance = distanceFromCenter / injectionRadius.coerceAtLeast(1)
            
            if (normalizedDistance <= 1f) {
                val noiseX = x.toFloat() / gridWidth
                val noiseY = y.toFloat() / gridHeight
                
                // Multi-octave turbulence
                var combinedNoise = 0f
                for (octave in 0 until turbulenceOctaves) {
                    val freq = turbulenceFrequencies[octave]
                    val amp = turbulenceAmplitudes[octave]
                    
                    val noise = sin((noiseX * freq + noiseOffset) * PI * 2) * 
                               cos((noiseY * freq + noiseOffset * 0.7) * PI * 2)
                    combinedNoise += noise.toFloat() * amp
                }
                
                val falloff = (1f - normalizedDistance) * mixingIntensity
                val turbulentForce = combinedNoise * falloff * 25f
                
                // Add curl-like motion
                velocityX[index] += turbulentForce * cos(globalTime + noiseY * PI).toFloat()
                velocityY[index] += turbulentForce * sin(globalTime + noiseX * PI).toFloat()
                
                // Micro-vortices for fine detail
                if (random.nextFloat() < 0.015f * deltaTime * mixingIntensity) {
                    addMicroVortex(x, y, (random.nextFloat() - 0.5f) * 15f)
                }
            }
        }
    }
}

private fun addMicroVortex(centerX: Int, centerY: Int, strength: Float) {
    val radius = 3 // Smaller than current 5
    for (dy in -radius..radius) {
        for (dx in -radius..radius) {
            val x = centerX + dx
            val y = centerY + dy
            if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) continue
            
            val distance = sqrt((dx * dx + dy * dy).toFloat())
            if (distance < radius && distance > 0) {
                val index = x + y * gridWidth
                val falloff = (1f - distance / radius) * strength
                
                velocityX[index] += -dy.toFloat() / distance * falloff
                velocityY[index] += dx.toFloat() / distance * falloff
            }
        }
    }
}
```

#### Adaptive Resolution System
Add to `FluidDynamicsEngine.kt`:

```kotlin
enum class ResolutionMode {
    PERFORMANCE(64, 48),
    STANDARD(96, 72), 
    HIGH(128, 96),
    ULTRA(192, 144)
}

private var currentResolution = ResolutionMode.HIGH
private var targetFPS = 60f
private var currentFPS = 60f

fun setResolutionMode(mode: ResolutionMode) {
    if (mode != currentResolution) {
        currentResolution = mode
        gridWidth = mode.width
        gridHeight = mode.height
        initializeArrays()
        reset()
    }
}

fun updatePerformanceAdaptation(fps: Float) {
    currentFPS = currentFPS * 0.9f + fps * 0.1f // Smooth FPS
    
    // Auto-adjust resolution based on performance
    when {
        currentFPS < 45f && currentResolution != ResolutionMode.PERFORMANCE -> {
            val newMode = when (currentResolution) {
                ResolutionMode.ULTRA -> ResolutionMode.HIGH
                ResolutionMode.HIGH -> ResolutionMode.STANDARD
                ResolutionMode.STANDARD -> ResolutionMode.PERFORMANCE
                else -> currentResolution
            }
            setResolutionMode(newMode)
        }
        currentFPS > 55f && currentResolution != ResolutionMode.ULTRA -> {
            val newMode = when (currentResolution) {
                ResolutionMode.PERFORMANCE -> ResolutionMode.STANDARD
                ResolutionMode.STANDARD -> ResolutionMode.HIGH
                ResolutionMode.HIGH -> ResolutionMode.ULTRA
                else -> currentResolution
            }
            setResolutionMode(newMode)
        }
    }
}
```

### 1.2 Enhanced FluidRenderer

#### Sub-Pixel Interpolation
Replace the `renderFluidField` method in `FluidRenderer.kt`:

```kotlin
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
    
    bitmap.setPixels(pixelArray, 0, width, 0, 0, width, height)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
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
    
    val normalizedWhite = whiteIntensity / totalIntensity
    val normalizedBlack = blackIntensity / totalIntensity
    
    // Base gray value
    val grayValue = (normalizedWhite * 255).toInt().coerceIn(0, 255)
    
    // Calculate density for smoke-like effects
    val density = totalIntensity.coerceIn(0f, 1f)
    val mixingActivity = if (whiteIntensity > 0.05f && blackIntensity > 0.05f) {
        kotlin.math.min(whiteIntensity, blackIntensity) * 2f
    } else 0f
    
    // Enhanced color mixing with density effects
    if (mixingActivity > 0.1f) {
        // Active mixing zone - add subtle color variations
        val turbulence = (sin(x * 20f) * cos(y * 15f) * 0.1f + 1f) / 2f
        val warmth = density * turbulence
        
        val red = (grayValue + warmth * 15f).toInt().coerceIn(0, 255)
        val green = (grayValue + warmth * 10f).toInt().coerceIn(0, 255)
        val blue = (grayValue - warmth * 5f).toInt().coerceIn(0, 255)
        
        return Color.rgb(red, green, blue)
    } else {
        // Pure regions - smooth gradients
        return Color.rgb(grayValue, grayValue, grayValue)
    }
}
```

### 1.3 Performance Integration

#### Update GameView for Performance Monitoring
Add to `GameView.kt`:

```kotlin
private var frameCounter = 0
private var lastFPSUpdate = 0L
private var currentFPS = 60f

private fun updatePerformanceMetrics() {
    frameCounter++
    val currentTime = System.currentTimeMillis()
    
    if (currentTime - lastFPSUpdate >= 1000) {
        currentFPS = frameCounter * 1000f / (currentTime - lastFPSUpdate)
        frameCounter = 0
        lastFPSUpdate = currentTime
        
        // Update physics engine performance adaptation
        if (isGameInitialized) {
            val physicsEngine = gameEngine.getPhysicsEngine()
            if (physicsEngine is FluidDynamicsEngine) {
                physicsEngine.updatePerformanceAdaptation(currentFPS)
            }
        }
    }
}
```

And modify the game loop in `GameThread.run()`:

```kotlin
// Add this call after rendering
updatePerformanceMetrics()
```

## Phase 2: Advanced Visual Effects (2-3 hours)

### 2.1 Density-Based Smoke Rendering

Create new file: `MindfulGame/app/src/main/java/com/mindful/game/rendering/SmokeEffects.kt`

```kotlin
package com.mindful.game.rendering

import android.graphics.Color
import kotlin.math.*

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
}
```

### 2.2 Edge Feathering System

Add to `FluidRenderer.kt`:

```kotlin
private val smokeEffects = SmokeEffects()

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
                        val distance = sqrt((dx * dx + dy * dy).toFloat())
                        if (distance <= featherRadius) {
                            val weight = exp(-distance * distance / (2f * featherRadius * featherRadius / 3f))
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
    val bottom = if (y < height-1) Color.red(pixels[x + (y+1) * width]) else Color.red(pixels[x + y * width])
    
    val gradX = (right - left) / 2f
    val gradY = (bottom - top) / 2f
    
    return sqrt(gradX * gradX + gradY * gradY)
}
```

## Phase 3: Performance Optimization (1 hour)

### 3.1 Object Pooling
Create: `MindfulGame/app/src/main/java/com/mindful/game/utils/ObjectPool.kt`

```kotlin
package com.mindful.game.utils

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentLinkedQueue

class ObjectPool<T> private constructor(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val maxSize: Int
) {
    private val pool = ConcurrentLinkedQueue<T>()
    
    fun acquire(): T {
        return pool.poll() ?: factory()
    }
    
    fun release(obj: T) {
        if (pool.size < maxSize) {
            reset(obj)
            pool.offer(obj)
        }
    }
    
    companion object {
        fun <T> create(factory: () -> T, reset: (T) -> Unit, maxSize: Int = 10): ObjectPool<T> {
            return ObjectPool(factory, reset, maxSize)
        }
    }
}

// Bitmap pool for FluidRenderer
class BitmapPool(private val maxSize: Int = 5) {
    private val pool = mutableMapOf<String, ConcurrentLinkedQueue<Bitmap>>()
    
    fun acquire(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val key = "${width}x${height}_${config}"
        val queue = pool.getOrPut(key) { ConcurrentLinkedQueue() }
        
        return queue.poll()?.takeIf { !it.isRecycled } 
            ?: Bitmap.createBitmap(width, height, config)
    }
    
    fun release(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            val key = "${bitmap.width}x${bitmap.height}_${bitmap.config}"
            val queue = pool.getOrPut(key) { ConcurrentLinkedQueue() }
            
            if (queue.size < maxSize) {
                queue.offer(bitmap)
            } else {
                bitmap.recycle()
            }
        }
    }
    
    fun cleanup() {
        pool.values.forEach { queue ->
            queue.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            queue.clear()
        }
        pool.clear()
    }
}
```

### 3.2 Memory-Efficient FluidRenderer
Update `FluidRenderer.kt` to use object pooling:

```kotlin
private val bitmapPool = BitmapPool()
private val arrayPool = ObjectPool.create(
    factory = { IntArray(0) },
    reset = { /* Arrays are replaced, no reset needed */ },
    maxSize = 3
)

private fun initializeBitmap(width: Int, height: Int) {
    bitmap?.let { bitmapPool.release(it) }
    bitmap = bitmapPool.acquire(width, height)
    this.canvas = Canvas(bitmap!!)
    
    val newArraySize = width * height
    if (pixelArray?.size != newArraySize) {
        pixelArray?.let { arrayPool.release(it) }
        pixelArray = IntArray(newArraySize)
    }
    
    lastWidth = width
    lastHeight = height
}

fun cleanup() {
    bitmap?.let { bitmapPool.release(it) }
    bitmap = null
    pixelArray?.let { arrayPool.release(it) }
    pixelArray = null
    bitmapPool.cleanup()
}
```

## Implementation Timeline

### Week 1: Core Enhancement
- **Day 1**: Quick win implementation (Steps 1.1-1.2)
- **Day 2-3**: Phase 1 turbulence enhancements
- **Day 4-5**: Phase 1 rendering improvements
- **Weekend**: Testing and optimization

### Week 2: Advanced Features
- **Day 1-2**: Phase 2 smoke effects
- **Day 3-4**: Phase 2 edge feathering
- **Day 5**: Phase 3 performance optimization
- **Weekend**: Integration testing

## Success Metrics

### Visual Quality Targets
1. **Resolution**: 4x improvement in visual detail
2. **Smoothness**: No visible pixelation at any quality level
3. **Realism**: Recognizable smoke-like tendrils and swirls
4. **Performance**: Maintain 60fps on mid-range devices

### User Experience Goals
- **"Wow Factor"**: Immediate visual improvement recognition
- **Smooth Interaction**: No lag during touch manipulation
- **Device Compatibility**: Adaptive quality across device range
- **Battery Efficiency**: <10% additional power consumption

This implementation plan transforms your already excellent fluid dynamics foundation into a visually stunning smoke simulation while maintaining the robust performance and architecture you've established.
