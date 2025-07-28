package com.mindful.game.physics

import com.mindful.game.core.interfaces.IPhysicsEngine
import kotlin.math.*
import kotlin.random.Random

/**
 * MVP implementation of physics engine using procedural noise for boundary simulation.
 * This implementation can be easily replaced with more sophisticated physics engines.
 */
class NoiseBasedEngine : IPhysicsEngine {
    
    private var screenWidth = 800
    private var screenHeight = 600
    
    // Boundary configuration
    private val boundaryResolution = 50 // Number of points along the boundary
    private val baseX = 0.5f // Center line of the screen
    
    // Noise parameters
    private var noiseIntensity = 0.0f
    private val maxNoiseIntensity = 0.15f
    private val noiseGrowthRate = 0.2f
    private var timeAccumulator = 0.0f
    
    // Touch restoration
    private val touchInfluenceRadius = 0.1f
    private val restorationStrength = 2.0f
    private var activeTouches = mutableListOf<TouchPoint>()
    
    // Boundary points
    private val boundaryPoints = FloatArray(boundaryResolution * 2)
    private val noiseOffsets = FloatArray(boundaryResolution)
    private val restorationFactors = FloatArray(boundaryResolution)
    
    // Random seed for consistent noise patterns
    private val random = Random(42)
    
    data class TouchPoint(val x: Float, val y: Float, val pressure: Float, var age: Float = 0f)
    
    init {
        reset()
    }
    
    override fun updateBoundary(deltaTime: Float) {
        timeAccumulator += deltaTime
        
        // Gradually increase noise intensity over time
        if (noiseIntensity < maxNoiseIntensity) {
            noiseIntensity = minOf(maxNoiseIntensity, noiseIntensity + noiseGrowthRate * deltaTime)
        }
        
        // Age and remove old touches
        activeTouches = activeTouches.filter { touch ->
            touch.age += deltaTime
            touch.age < 2.0f // Touch effects last 2 seconds
        }.toMutableList()
        
        // Update boundary points
        for (i in 0 until boundaryResolution) {
            val y = i.toFloat() / (boundaryResolution - 1)
            
            // Generate base noise
            val noise1 = sin(y * PI * 4 + timeAccumulator * 0.5) * 0.3
            val noise2 = sin(y * PI * 8 + timeAccumulator * 0.8) * 0.2
            val noise3 = sin(y * PI * 16 + timeAccumulator * 1.2) * 0.1
            val combinedNoise = (noise1 + noise2 + noise3).toFloat()
            
            // Apply noise intensity
            val noiseOffset = combinedNoise * noiseIntensity
            
            // Calculate restoration effect from touches
            var restorationEffect = 0f
            for (touch in activeTouches) {
                val distance = sqrt((touch.x - baseX).pow(2) + (touch.y - y).pow(2))
                if (distance < touchInfluenceRadius) {
                    val influence = (1 - distance / touchInfluenceRadius) * touch.pressure
                    val ageMultiplier = max(0f, 1f - touch.age / 2f)
                    restorationEffect += influence * ageMultiplier * restorationStrength
                }
            }
            
            // Apply restoration (reduces noise)
            restorationFactors[i] = max(0f, restorationFactors[i] - restorationEffect * deltaTime)
            val effectiveNoise = noiseOffset * (1f - restorationFactors[i])
            
            // Set boundary point
            val x = baseX + effectiveNoise
            boundaryPoints[i * 2] = x
            boundaryPoints[i * 2 + 1] = y
            
            noiseOffsets[i] = abs(effectiveNoise)
        }
    }
    
    override fun applyTouchInput(x: Float, y: Float, pressure: Float) {
        // Add new touch point
        activeTouches.add(TouchPoint(x, y, pressure))
        
        // Limit the number of active touches for performance
        if (activeTouches.size > 10) {
            activeTouches.removeAt(0)
        }
    }
    
    override fun getBoundaryPoints(): FloatArray {
        return boundaryPoints
    }
    
    override fun getRestorationProgress(): Float {
        // Calculate progress based on how close boundary is to center line
        val totalDeviation = noiseOffsets.sum()
        val maxPossibleDeviation = boundaryResolution * maxNoiseIntensity
        return if (maxPossibleDeviation > 0) {
            1f - (totalDeviation / maxPossibleDeviation)
        } else {
            1f
        }
    }
    
    override fun reset() {
        noiseIntensity = 0f
        timeAccumulator = 0f
        activeTouches.clear()
        
        // Initialize boundary points along center line
        for (i in 0 until boundaryResolution) {
            val y = i.toFloat() / (boundaryResolution - 1)
            boundaryPoints[i * 2] = baseX
            boundaryPoints[i * 2 + 1] = y
            noiseOffsets[i] = 0f
            restorationFactors[i] = 0f
        }
    }
    
    override fun setScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }
    
    override fun getLeftRegionIntensity(x: Float, y: Float): Float {
        // Find nearest boundary point
        val nearestBoundaryX = getBoundaryXAtY(y)
        
        return when {
            x < nearestBoundaryX -> 1f // Pure white region
            x < nearestBoundaryX + 0.02f -> {
                // Smooth transition zone
                val distance = (x - nearestBoundaryX) / 0.02f
                1f - distance
            }
            else -> 0f // Black region
        }
    }
    
    override fun getRightRegionIntensity(x: Float, y: Float): Float {
        // Find nearest boundary point
        val nearestBoundaryX = getBoundaryXAtY(y)
        
        return when {
            x > nearestBoundaryX -> 1f // Pure black region (inverted for right side)
            x > nearestBoundaryX - 0.02f -> {
                // Smooth transition zone
                val distance = (nearestBoundaryX - x) / 0.02f
                1f - distance
            }
            else -> 0f // White region
        }
    }
    
    private fun getBoundaryXAtY(y: Float): Float {
        // Find the boundary X coordinate at the given Y
        val index = (y * (boundaryResolution - 1)).toInt()
        val clampedIndex = index.coerceIn(0, boundaryResolution - 1)
        return boundaryPoints[clampedIndex * 2]
    }
}
