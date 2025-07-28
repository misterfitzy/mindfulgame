# High-Resolution Smoke Mixing Specification

## Overview
This specification outlines enhancements to transform the current fluid dynamics system into a photorealistic smoke mixing simulation with significantly improved visual resolution and detail.

## Current State Analysis

### Existing Strengths
- ✅ Excellent fluid physics foundation (Navier-Stokes equations)
- ✅ Real-time velocity fields and pressure solving
- ✅ Self-sustaining turbulence with vorticity confinement
- ✅ Progressive mixing intensity over time
- ✅ Touch interaction with persistent vortices

### Resolution Limitations
- **Grid Resolution**: 64×48 cells (3,072 total) - adequate but not smoke-like
- **Render Sampling**: 1-4 pixel steps based on quality - creates visible blockiness
- **Turbulence Detail**: 3-octave noise - insufficient for fine smoke tendrils
- **Color Gradients**: Linear interpolation - lacks realistic smoke opacity variations

## Target Visual Quality

### Realistic Smoke Characteristics
1. **Fine Tendrils**: Wispy, thread-like structures that branch and curl
2. **Smooth Gradients**: Seamless color transitions without pixelation
3. **Density Variations**: Areas of thick and thin smoke with natural opacity
4. **Fractal Detail**: Self-similar patterns at multiple scales
5. **Organic Motion**: Natural swirling and curling motions
6. **Edge Softness**: Feathered boundaries rather than hard edges

## Enhancement Specifications

### 1. Adaptive High-Resolution Grid System

#### Grid Resolution Tiers
```kotlin
enum class ResolutionTier {
    PERFORMANCE(64, 48),    // 3,072 cells - current
    STANDARD(96, 72),       // 6,912 cells - 2x detail
    HIGH(128, 96),          // 12,288 cells - 4x detail
    ULTRA(192, 144),        // 27,648 cells - 9x detail
    EXTREME(256, 192)       // 49,152 cells - 16x detail
}
```

#### Dynamic Resolution Selection
- **Device Performance Detection**: CPU cores, RAM, GPU capabilities
- **Real-time FPS Monitoring**: Automatically downgrade if FPS drops below 45
- **Battery Level Consideration**: Reduce resolution on low battery
- **User Override**: Manual quality selection

#### Implementation Strategy
```kotlin
class AdaptiveResolutionManager {
    private var currentTier = ResolutionTier.STANDARD
    private val fpsMonitor = FPSMonitor(targetFPS = 60, minFPS = 45)
    
    fun updateResolution(deviceMetrics: DeviceMetrics, batteryLevel: Float) {
        val recommendedTier = calculateOptimalTier(deviceMetrics, batteryLevel)
        val performanceTier = fpsMonitor.getPerformanceConstrainedTier()
        currentTier = minOf(recommendedTier, performanceTier)
    }
}
```

### 2. Sub-Pixel Rendering Enhancement

#### Bilinear Interpolation
- **Grid-to-Pixel Mapping**: Smooth interpolation between discrete grid cells
- **Sub-pixel Accuracy**: Calculate exact color values between grid points
- **Gradient Preservation**: Maintain smooth color transitions

#### Multi-Sampling Anti-Aliasing
```kotlin
class SubPixelRenderer {
    private val samplePattern = arrayOf(
        floatArrayOf(-0.25f, -0.25f),  // Top-left
        floatArrayOf(0.25f, -0.25f),   // Top-right
        floatArrayOf(-0.25f, 0.25f),   // Bottom-left
        floatArrayOf(0.25f, 0.25f)     // Bottom-right
    )
    
    fun renderPixel(x: Int, y: Int): Color {
        // Sample at 4 sub-pixel locations and average
        val samples = samplePattern.map { offset ->
            sampleColorField(x + offset[0], y + offset[1])
        }
        return averageColors(samples)
    }
}
```

#### Temporal Anti-Aliasing
- **Frame Blending**: Combine multiple frames for smoother motion
- **Motion Vectors**: Track fluid movement for accurate temporal sampling
- **Jitter Patterns**: Vary sampling positions across frames

### 3. Enhanced Turbulence Detail

#### Multi-Octave Noise Enhancement
```kotlin
class EnhancedTurbulenceGenerator {
    private val octaves = 6  // Increased from 3
    private val frequencies = floatArrayOf(1f, 2f, 4f, 8f, 16f, 32f)
    private val amplitudes = floatArrayOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f, 0.03125f)
    
    fun generateTurbulence(x: Float, y: Float, time: Float): Float {
        var noise = 0f
        for (i in 0 until octaves) {
            noise += amplitudes[i] * perlinNoise(
                x * frequencies[i], 
                y * frequencies[i], 
                time * frequencies[i] * 0.1f
            )
        }
        return noise
    }
}
```

#### Fractal Vortex System
- **Micro-Vortices**: Small-scale rotational structures (radius 1-2 cells)
- **Vortex Hierarchies**: Large vortices spawn smaller ones
- **Persistence**: Vortices maintain coherence over multiple frames
- **Natural Decay**: Gradual weakening following realistic physics

#### Curl Noise Implementation
```kotlin
class CurlNoiseField {
    fun calculateCurl(x: Float, y: Float, time: Float): Vector2 {
        val epsilon = 0.01f
        
        // Calculate partial derivatives of potential field
        val dPdy = (potential(x, y + epsilon, time) - potential(x, y - epsilon, time)) / (2 * epsilon)
        val dPdx = (potential(x + epsilon, y, time) - potential(x - epsilon, y, time)) / (2 * epsilon)
        
        // Curl = (∂P/∂y, -∂P/∂x)
        return Vector2(dPdy, -dPdx)
    }
    
    private fun potential(x: Float, y: Float, time: Float): Float {
        // Multi-octave Perlin noise for potential field
        return generateTurbulence(x, y, time)
    }
}
```

### 4. Smoke-Specific Visual Effects

#### Density-Based Rendering
```kotlin
class SmokeRenderer {
    fun calculateSmokeColor(whiteIntensity: Float, blackIntensity: Float, density: Float): Color {
        val baseColor = lerp(Color.WHITE, Color.BLACK, blackIntensity / (whiteIntensity + blackIntensity))
        
        // Apply density for realistic opacity
        val alpha = (density * 255).toInt().coerceIn(0, 255)
        
        // Add subtle color variations
        val tint = calculateSmokeTint(density)
        
        return blendColors(baseColor, tint, alpha)
    }
    
    private fun calculateSmokeTint(density: Float): Color {
        // Warmer colors in dense areas, cooler in thin areas
        val warmth = density.coerceIn(0f, 1f)
        val red = (200 + warmth * 55).toInt()
        val green = (200 + warmth * 45).toInt()
        val blue = (200 + warmth * 35).toInt()
        return Color.rgb(red, green, blue)
    }
}
```

#### Volumetric Lighting Effects
- **Light Scattering**: Simulate light interaction with smoke particles
- **Depth Gradients**: Darker areas appear "deeper" in the smoke volume
- **Rim Lighting**: Subtle highlighting at smoke edges

#### Edge Feathering
```kotlin
class EdgeFeathering {
    fun applyFeathering(colorField: FloatArray, width: Int, height: Int) {
        val featherRadius = 2
        val temp = colorField.copyOf()
        
        for (y in featherRadius until height - featherRadius) {
            for (x in featherRadius until width - featherRadius) {
                val index = x + y * width
                val gradient = calculateGradientMagnitude(temp, x, y, width)
                
                if (gradient > 0.1f) {
                    // Apply Gaussian blur to high-gradient areas
                    colorField[index] = applyGaussianBlur(temp, x, y, width, featherRadius)
                }
            }
        }
    }
}
```

### 5. Performance Optimization Strategy

#### Level-of-Detail (LOD) System
```kotlin
class LODManager {
    fun calculateLOD(centerX: Float, centerY: Float, viewportWidth: Int, viewportHeight: Int): Array<Array<Int>> {
        val lodGrid = Array(viewportHeight) { Array(viewportWidth) { 0 } }
        
        for (y in 0 until viewportHeight) {
            for (x in 0 until viewportWidth) {
                val distanceFromCenter = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                val normalizedDistance = distanceFromCenter / (min(viewportWidth, viewportHeight) * 0.5f)
                
                lodGrid[y][x] = when {
                    normalizedDistance < 0.3f -> 3  // Ultra detail
                    normalizedDistance < 0.6f -> 2  // High detail
                    normalizedDistance < 0.9f -> 1  // Medium detail
                    else -> 0                       // Low detail
                }
            }
        }
        
        return lodGrid
    }
}
```

#### Temporal Upsampling
- **Physics at 30fps**: Fluid simulation runs at half frame rate
- **Rendering at 60fps**: Interpolate between physics frames for smooth visuals
- **Motion Prediction**: Estimate intermediate states using velocity fields

#### Memory Management
```kotlin
class MemoryManager {
    private val bitmapPool = mutableListOf<Bitmap>()
    private val arrayPool = mutableListOf<FloatArray>()
    
    fun recycleBitmap(bitmap: Bitmap) {
        if (bitmapPool.size < MAX_BITMAP_POOL_SIZE) {
            bitmapPool.add(bitmap)
        } else {
            bitmap.recycle()
        }
    }
    
    fun getBitmap(width: Int, height: Int): Bitmap {
        return bitmapPool.removeFirstOrNull() 
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}
```

### 6. Advanced Color Mixing

#### Physically-Based Color Blending
```kotlin
class PhysicalColorMixer {
    fun mixColors(color1: Color, color2: Color, mixingRatio: Float, turbulence: Float): Color {
        // Convert to linear color space for accurate mixing
        val linear1 = sRGBToLinear(color1)
        val linear2 = sRGBToLinear(color2)
        
        // Apply turbulence-based mixing
        val effectiveMixing = mixingRatio * (1f + turbulence * 0.5f)
        val mixed = lerp(linear1, linear2, effectiveMixing)
        
        // Convert back to sRGB
        return linearToSRGB(mixed)
    }
    
    private fun sRGBToLinear(color: Color): LinearColor {
        // Apply gamma correction for physically accurate mixing
        val r = pow(color.red / 255f, 2.2f)
        val g = pow(color.green / 255f, 2.2f)
        val b = pow(color.blue / 255f, 2.2f)
        return LinearColor(r, g, b)
    }
}
```

#### Spectral Color Mixing
- **Wavelength-Based**: More accurate color mixing using light wavelengths
- **Metameric Effects**: Handle colors that appear same but mix differently
- **Conservation Laws**: Maintain color mass during mixing

### 7. Real-Time Quality Adaptation

#### Performance Monitoring
```kotlin
class PerformanceMonitor {
    private val frameTimeHistory = CircularBuffer<Long>(60)
    private var currentQuality = Quality.HIGH
    
    fun onFrameComplete(frameTime: Long) {
        frameTimeHistory.add(frameTime)
        
        val averageFrameTime = frameTimeHistory.average()
        val currentFPS = 1000f / averageFrameTime
        
        when {
            currentFPS < 45f -> downgradeQuality()
            currentFPS > 55f && currentQuality < Quality.ULTRA -> upgradeQuality()
        }
    }
    
    private fun downgradeQuality() {
        currentQuality = when (currentQuality) {
            Quality.ULTRA -> Quality.HIGH
            Quality.HIGH -> Quality.MEDIUM
            Quality.MEDIUM -> Quality.LOW
            Quality.LOW -> Quality.MINIMUM
            else -> currentQuality
        }
    }
}
```

#### Battery-Aware Optimization
- **Power Level Detection**: Reduce quality when battery is low
- **Thermal Throttling**: Monitor device temperature
- **Background Mode**: Minimal quality when app is not in focus

## Implementation Priority

### Phase 1: Core Resolution Enhancement (High Priority)
1. **Grid Resolution Scaling**: Implement adaptive resolution tiers
2. **Sub-Pixel Interpolation**: Add bilinear filtering to renderer
3. **Basic Multi-Sampling**: 2x2 sampling for smoother edges

### Phase 2: Advanced Turbulence (Medium Priority)
1. **Enhanced Noise**: Implement 5-6 octave noise generation
2. **Curl Noise**: Add curl-based velocity field generation
3. **Micro-Vortices**: Implement small-scale vortex injection

### Phase 3: Visual Polish (Medium Priority)
1. **Density-Based Rendering**: Implement smoke-like opacity effects
2. **Edge Feathering**: Add Gaussian blur to high-gradient areas
3. **Color Enhancements**: Implement physically-based color mixing

### Phase 4: Performance Optimization (Low Priority)
1. **LOD System**: Implement center-focused detail levels
2. **Temporal Upsampling**: Decouple physics and rendering rates
3. **Memory Management**: Add object pooling and recycling

## Expected Visual Improvements

### Before Enhancement
- ❌ Visible grid cells in mixing areas
- ❌ Pixelated color transitions
- ❌ Limited turbulence detail
- ❌ Hard edges between color regions
- ❌ Uniform density appearance

### After Enhancement
- ✅ Smooth, continuous smoke-like gradients
- ✅ Fine tendrils and wispy structures
- ✅ Rich turbulence detail at multiple scales
- ✅ Soft, feathered edges
- ✅ Natural density variations
- ✅ Photorealistic smoke mixing behavior

## Performance Targets

### Minimum Requirements
- **60 FPS** on mid-range devices (3GB RAM, Snapdragon 660)
- **45 FPS minimum** during intensive mixing
- **< 100MB** peak memory usage
- **< 5% CPU** when in background

### Optimal Performance
- **60 FPS steady** on high-end devices
- **120 FPS** on flagship devices with high refresh displays
- **Adaptive quality** maintains performance across device range
- **Battery efficient** operation for extended sessions

## Quality Validation Metrics

### Visual Quality
1. **Gradient Smoothness**: No visible pixelation in color transitions
2. **Detail Richness**: At least 5 levels of visible turbulence detail
3. **Edge Softness**: Feathered boundaries with sub-pixel accuracy
4. **Motion Fluidity**: Smooth temporal progression without artifacts

### Performance Quality
1. **Frame Rate Stability**: < 5% variance in frame time
2. **Memory Efficiency**: No memory leaks during extended sessions
3. **Battery Impact**: < 10% additional battery drain vs. current
4. **Thermal Management**: No thermal throttling under normal use

## Success Criteria

### User Experience
- **"Looks like real smoke"** - Natural, organic appearance
- **"Incredibly smooth"** - No visible pixelation or artifacts
- **"Responsive and fluid"** - Maintains performance across devices
- **"Mesmerizing detail"** - Rich, evolving visual complexity

### Technical Benchmarks
- **4x visual resolution** improvement over current implementation
- **Maintained 60 FPS** on target devices
- **Sub-pixel accuracy** in color gradients
- **Multi-scale turbulence** with 5+ detail levels

This specification provides a comprehensive roadmap for transforming the current excellent fluid dynamics foundation into a photorealistic smoke mixing simulation that meets the highest visual quality standards while maintaining optimal performance across a wide range of Android devices.
