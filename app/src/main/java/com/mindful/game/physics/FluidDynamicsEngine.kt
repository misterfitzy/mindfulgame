package com.mindful.game.physics

import com.mindful.game.core.interfaces.IPhysicsEngine
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced fluid dynamics engine that simulates realistic color mixing
 * using velocity fields, pressure solving, and vorticity confinement.
 */
class FluidDynamicsEngine : IPhysicsEngine {
    
    // Grid configuration - Enhanced resolution for smoke-like detail
    private var gridWidth = 128  // Doubled from 64 for 4x detail
    private var gridHeight = 96   // Doubled from 48 for 4x detail
    private var cellSize = 1f
    
    // Fluid simulation arrays
    private lateinit var velocityX: FloatArray
    private lateinit var velocityY: FloatArray
    private lateinit var prevVelocityX: FloatArray
    private lateinit var prevVelocityY: FloatArray
    private lateinit var pressure: FloatArray
    private lateinit var divergence: FloatArray
    
    // Color field arrays
    private lateinit var colorField: FloatArray  // 0.0 = black, 1.0 = white
    private lateinit var prevColorField: FloatArray
    private lateinit var turbulenceField: FloatArray
    
    // Boundary representation
    private val boundaryResolution = 100
    private val boundaryPoints = FloatArray(boundaryResolution * 2)
    
    // Simulation parameters - Enhanced for smoke-like behavior
    private val viscosity = 0.001f
    private val diffusion = 0.0001f
    private val vorticity = 0.5f
    private val timeStep = 0.016f
    private val densityDecay = 0.999f
    
    // Enhanced turbulence parameters
    private val turbulenceOctaves = 5  // Increased from implicit 3
    private val turbulenceFrequencies = floatArrayOf(1f, 2f, 4f, 8f, 16f)
    private val turbulenceAmplitudes = floatArrayOf(1f, 0.6f, 0.36f, 0.216f, 0.1296f)
    
    // Mixing parameters
    private var mixingIntensity = 0.0f
    private val maxMixingIntensity = 1.0f
    private val mixingGrowthRate = 0.1f
    private var globalTime = 0f
    
    // Adaptive resolution system
    enum class ResolutionMode(val width: Int, val height: Int) {
        PERFORMANCE(64, 48),
        STANDARD(96, 72), 
        HIGH(128, 96),
        ULTRA(192, 144)
    }
    
    private var currentResolution = ResolutionMode.HIGH
    private var targetFPS = 60f
    private var currentFPS = 60f
    
    // Touch interaction
    private val activeTouches = mutableListOf<TouchForce>()
    private val touchStrength = 50f
    private val touchRadius = 8f
    
    // Turbulence generation
    private val random = Random(42)
    private var noiseOffset = 0f
    
    data class TouchForce(
        val gridX: Int,
        val gridY: Int,
        val forceX: Float,
        val forceY: Float,
        val strength: Float,
        var age: Float = 0f
    )
    
    init {
        initializeArrays()
        reset()
    }
    
    private fun initializeArrays() {
        val size = gridWidth * gridHeight
        velocityX = FloatArray(size)
        velocityY = FloatArray(size)
        prevVelocityX = FloatArray(size)
        prevVelocityY = FloatArray(size)
        pressure = FloatArray(size)
        divergence = FloatArray(size)
        colorField = FloatArray(size)
        prevColorField = FloatArray(size)
        turbulenceField = FloatArray(size)
    }
    
    override fun updateBoundary(deltaTime: Float) {
        globalTime += deltaTime
        
        // Gradually increase mixing intensity
        if (mixingIntensity < maxMixingIntensity) {
            mixingIntensity = minOf(maxMixingIntensity, mixingIntensity + mixingGrowthRate * deltaTime)
        }
        
        // Age and remove old touches
        activeTouches.removeAll { touch ->
            touch.age += deltaTime
            touch.age > 3f
        }
        
        // Add continuous turbulence injection
        injectTurbulence(deltaTime)
        
        // Apply touch forces
        applyTouchForces()
        
        // Run fluid simulation steps
        velocityStep(deltaTime)
        colorStep(deltaTime)
        
        // Update boundary representation
        updateBoundaryRepresentation()
    }
    
    private fun injectTurbulence(deltaTime: Float) {
        noiseOffset += deltaTime * 0.5f
        
        // Inject turbulence along the center boundary and spreading outward
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
                    
                    // Multi-octave turbulence for smoke-like detail
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
                    
                    // Add curl-like motion for realistic fluid flow
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
    
    private fun addVortex(centerX: Int, centerY: Int, strength: Float) {
        val radius = 5
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = centerX + dx
                val y = centerY + dy
                if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) continue
                
                val distance = sqrt((dx * dx + dy * dy).toFloat())
                if (distance < radius && distance > 0) {
                    val index = x + y * gridWidth
                    val falloff = (1f - distance / radius) * strength
                    
                    // Create rotational velocity field
                    velocityX[index] += -dy.toFloat() / distance * falloff
                    velocityY[index] += dx.toFloat() / distance * falloff
                }
            }
        }
    }
    
    private fun addMicroVortex(centerX: Int, centerY: Int, strength: Float) {
        val radius = 3 // Smaller than regular vortices for fine detail
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = centerX + dx
                val y = centerY + dy
                if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) continue
                
                val distance = sqrt((dx * dx + dy * dy).toFloat())
                if (distance < radius && distance > 0) {
                    val index = x + y * gridWidth
                    val falloff = (1f - distance / radius) * strength
                    
                    // Create rotational velocity field for micro-scale detail
                    velocityX[index] += -dy.toFloat() / distance * falloff
                    velocityY[index] += dx.toFloat() / distance * falloff
                }
            }
        }
    }
    
    private fun applyTouchForces() {
        for (touch in activeTouches) {
            val radius = touchRadius.toInt()
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val x = touch.gridX + dx
                    val y = touch.gridY + dy
                    if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) continue
                    
                    val distance = sqrt((dx * dx + dy * dy).toFloat())
                    if (distance < touchRadius) {
                        val index = x + y * gridWidth
                        val falloff = (1f - distance / touchRadius) * touch.strength * (1f - touch.age / 3f)
                        
                        velocityX[index] += touch.forceX * falloff
                        velocityY[index] += touch.forceY * falloff
                        
                        // Create swirling motion around touch
                        val swirl = falloff * 10f
                        velocityX[index] += -dy.toFloat() * swirl / touchRadius
                        velocityY[index] += dx.toFloat() * swirl / touchRadius
                    }
                }
            }
        }
    }
    
    private fun velocityStep(deltaTime: Float) {
        // Store previous velocity
        System.arraycopy(velocityX, 0, prevVelocityX, 0, velocityX.size)
        System.arraycopy(velocityY, 0, prevVelocityY, 0, velocityY.size)
        
        // Diffusion
        diffuse(velocityX, prevVelocityX, viscosity, deltaTime)
        diffuse(velocityY, prevVelocityY, viscosity, deltaTime)
        
        // Projection (make velocity field divergence-free)
        project()
        
        // Store velocity for advection
        System.arraycopy(velocityX, 0, prevVelocityX, 0, velocityX.size)
        System.arraycopy(velocityY, 0, prevVelocityY, 0, velocityY.size)
        
        // Advection
        advect(velocityX, prevVelocityX, prevVelocityX, prevVelocityY, deltaTime)
        advect(velocityY, prevVelocityY, prevVelocityX, prevVelocityY, deltaTime)
        
        // Final projection
        project()
        
        // Vorticity confinement
        vorticityConfinement(deltaTime)
        
        // Apply velocity decay
        for (i in velocityX.indices) {
            velocityX[i] *= 0.99f
            velocityY[i] *= 0.99f
        }
    }
    
    private fun colorStep(deltaTime: Float) {
        // Store previous color field
        System.arraycopy(colorField, 0, prevColorField, 0, colorField.size)
        
        // Diffusion
        diffuse(colorField, prevColorField, diffusion, deltaTime)
        
        // Store for advection
        System.arraycopy(colorField, 0, prevColorField, 0, colorField.size)
        
        // Advection
        advect(colorField, prevColorField, velocityX, velocityY, deltaTime)
        
        // Apply mixing at boundaries between different colors
        applyMixing(deltaTime)
    }
    
    private fun applyMixing(deltaTime: Float) {
        val mixingRate = mixingIntensity * 0.5f * deltaTime
        
        for (y in 1 until gridHeight - 1) {
            for (x in 1 until gridWidth - 1) {
                val index = x + y * gridWidth
                val current = colorField[index]
                
                // Calculate gradient (measure of color difference)
                val gradX = (colorField[index + 1] - colorField[index - 1]) * 0.5f
                val gradY = (colorField[index + gridWidth] - colorField[index - gridWidth]) * 0.5f
                val gradientMagnitude = sqrt(gradX * gradX + gradY * gradY)
                
                // Apply mixing where gradients are high
                if (gradientMagnitude > 0.1f) {
                    val neighbors = arrayOf(
                        colorField[index - 1],
                        colorField[index + 1],
                        colorField[index - gridWidth],
                        colorField[index + gridWidth]
                    )
                    
                    val averageNeighbor = neighbors.average().toFloat()
                    colorField[index] = current + (averageNeighbor - current) * mixingRate * gradientMagnitude
                }
            }
        }
    }
    
    private fun diffuse(field: FloatArray, prev: FloatArray, diffusionRate: Float, deltaTime: Float) {
        val a = deltaTime * diffusionRate * gridWidth * gridHeight
        
        // Gauss-Seidel relaxation
        repeat(4) {
            for (y in 1 until gridHeight - 1) {
                for (x in 1 until gridWidth - 1) {
                    val index = x + y * gridWidth
                    field[index] = (prev[index] + a * (
                        field[index - 1] + field[index + 1] +
                        field[index - gridWidth] + field[index + gridWidth]
                    )) / (1 + 4 * a)
                }
            }
            setBoundary(field)
        }
    }
    
    private fun advect(field: FloatArray, prev: FloatArray, velX: FloatArray, velY: FloatArray, deltaTime: Float) {
        val dt0 = deltaTime * gridWidth
        
        for (y in 1 until gridHeight - 1) {
            for (x in 1 until gridWidth - 1) {
                val index = x + y * gridWidth
                
                // Trace particle backwards
                var backX = x - dt0 * velX[index]
                var backY = y - dt0 * velY[index]
                
                // Clamp to grid bounds
                backX = backX.coerceIn(0.5f, gridWidth - 1.5f)
                backY = backY.coerceIn(0.5f, gridHeight - 1.5f)
                
                // Bilinear interpolation
                val i0 = backX.toInt()
                val i1 = i0 + 1
                val j0 = backY.toInt()
                val j1 = j0 + 1
                
                val s1 = backX - i0
                val s0 = 1 - s1
                val t1 = backY - j0
                val t0 = 1 - t1
                
                field[index] = s0 * (t0 * prev[i0 + j0 * gridWidth] + t1 * prev[i0 + j1 * gridWidth]) +
                              s1 * (t0 * prev[i1 + j0 * gridWidth] + t1 * prev[i1 + j1 * gridWidth])
            }
        }
        setBoundary(field)
    }
    
    private fun project() {
        // Calculate divergence
        for (y in 1 until gridHeight - 1) {
            for (x in 1 until gridWidth - 1) {
                val index = x + y * gridWidth
                divergence[index] = -0.5f * (
                    velocityX[index + 1] - velocityX[index - 1] +
                    velocityY[index + gridWidth] - velocityY[index - gridWidth]
                ) / gridWidth
                pressure[index] = 0f
            }
        }
        
        setBoundary(divergence)
        setBoundary(pressure)
        
        // Solve for pressure
        repeat(10) {
            for (y in 1 until gridHeight - 1) {
                for (x in 1 until gridWidth - 1) {
                    val index = x + y * gridWidth
                    pressure[index] = (divergence[index] + 
                        pressure[index - 1] + pressure[index + 1] +
                        pressure[index - gridWidth] + pressure[index + gridWidth]) / 4f
                }
            }
            setBoundary(pressure)
        }
        
        // Subtract pressure gradient
        for (y in 1 until gridHeight - 1) {
            for (x in 1 until gridWidth - 1) {
                val index = x + y * gridWidth
                velocityX[index] -= 0.5f * (pressure[index + 1] - pressure[index - 1]) * gridWidth
                velocityY[index] -= 0.5f * (pressure[index + gridWidth] - pressure[index - gridWidth]) * gridWidth
            }
        }
        
        setBoundary(velocityX)
        setBoundary(velocityY)
    }
    
    private fun vorticityConfinement(deltaTime: Float) {
        // Calculate vorticity
        for (y in 1 until gridHeight - 1) {
            for (x in 1 until gridWidth - 1) {
                val index = x + y * gridWidth
                turbulenceField[index] = (velocityY[index + 1] - velocityY[index - 1]) * 0.5f -
                                       (velocityX[index + gridWidth] - velocityX[index - gridWidth]) * 0.5f
            }
        }
        
        // Apply vorticity confinement force
        for (y in 1 until gridHeight - 1) {
            for (x in 1 until gridWidth - 1) {
                val index = x + y * gridWidth
                
                val dwdx = (abs(turbulenceField[index + 1]) - abs(turbulenceField[index - 1])) * 0.5f
                val dwdy = (abs(turbulenceField[index + gridWidth]) - abs(turbulenceField[index - gridWidth])) * 0.5f
                
                val length = sqrt(dwdx * dwdx + dwdy * dwdy) + 1e-5f
                
                val fx = dwdy / length * turbulenceField[index] * vorticity * deltaTime
                val fy = -dwdx / length * turbulenceField[index] * vorticity * deltaTime
                
                velocityX[index] += fx
                velocityY[index] += fy
            }
        }
    }
    
    private fun setBoundary(field: FloatArray) {
        // Set boundary conditions (no-slip for walls)
        for (i in 1 until gridWidth - 1) {
            field[i] = field[i + gridWidth] // Top
            field[i + (gridHeight - 1) * gridWidth] = field[i + (gridHeight - 2) * gridWidth] // Bottom
        }
        
        for (j in 1 until gridHeight - 1) {
            field[j * gridWidth] = field[1 + j * gridWidth] // Left
            field[gridWidth - 1 + j * gridWidth] = field[gridWidth - 2 + j * gridWidth] // Right
        }
        
        // Corners
        field[0] = 0.5f * (field[1] + field[gridWidth])
        field[gridWidth - 1] = 0.5f * (field[gridWidth - 2] + field[2 * gridWidth - 1])
        field[(gridHeight - 1) * gridWidth] = 0.5f * (field[1 + (gridHeight - 1) * gridWidth] + field[(gridHeight - 2) * gridWidth])
        field[gridWidth - 1 + (gridHeight - 1) * gridWidth] = 0.5f * (field[gridWidth - 2 + (gridHeight - 1) * gridWidth] + field[gridWidth - 1 + (gridHeight - 2) * gridWidth])
    }
    
    private fun updateBoundaryRepresentation() {
        // Extract boundary from color field for rendering compatibility
        val centerX = gridWidth / 2
        
        for (i in 0 until boundaryResolution) {
            val y = i.toFloat() / (boundaryResolution - 1)
            val gridY = (y * (gridHeight - 1)).toInt().coerceIn(0, gridHeight - 1)
            
            // Find the boundary position by looking for the 0.5 color value
            var boundaryX = 0.5f
            for (x in 0 until gridWidth - 1) {
                val index1 = x + gridY * gridWidth
                val index2 = (x + 1) + gridY * gridWidth
                val color1 = colorField[index1]
                val color2 = colorField[index2]
                
                // Check if boundary crosses between these cells
                if ((color1 <= 0.5f && color2 >= 0.5f) || (color1 >= 0.5f && color2 <= 0.5f)) {
                    // Linear interpolation to find exact boundary position
                    val t = (0.5f - color1) / (color2 - color1)
                    boundaryX = (x + t) / gridWidth
                    break
                }
            }
            
            boundaryPoints[i * 2] = boundaryX
            boundaryPoints[i * 2 + 1] = y
        }
    }
    
    override fun applyTouchInput(x: Float, y: Float, pressure: Float) {
        val gridX = (x * gridWidth).toInt().coerceIn(0, gridWidth - 1)
        val gridY = (y * gridHeight).toInt().coerceIn(0, gridHeight - 1)
        
        // Calculate force direction (towards center for restoration effect)
        val centerX = gridWidth / 2f
        val forceX = (centerX - gridX) * 0.1f
        val forceY = 0f
        
        activeTouches.add(TouchForce(gridX, gridY, forceX, forceY, pressure * touchStrength))
        
        // Limit active touches for performance
        if (activeTouches.size > 15) {
            activeTouches.removeAt(0)
        }
    }
    
    override fun getBoundaryPoints(): FloatArray = boundaryPoints
    
    override fun getRestorationProgress(): Float {
        // Calculate mixing progress based on color field uniformity
        var totalVariance = 0f
        val targetColor = 0.5f
        
        for (i in colorField.indices) {
            val deviation = abs(colorField[i] - targetColor)
            totalVariance += deviation
        }
        
        val maxVariance = colorField.size * 0.5f
        return 1f - (totalVariance / maxVariance).coerceIn(0f, 1f)
    }
    
    override fun reset() {
        mixingIntensity = 0f
        globalTime = 0f
        activeTouches.clear()
        
        // Initialize color field: left half white (1.0), right half black (0.0)
        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val index = x + y * gridWidth
                colorField[index] = if (x < gridWidth / 2) 1f else 0f
                prevColorField[index] = colorField[index]
                
                velocityX[index] = 0f
                velocityY[index] = 0f
                prevVelocityX[index] = 0f
                prevVelocityY[index] = 0f
                pressure[index] = 0f
                divergence[index] = 0f
                turbulenceField[index] = 0f
            }
        }
        
        // Initialize boundary points
        for (i in 0 until boundaryResolution) {
            val y = i.toFloat() / (boundaryResolution - 1)
            boundaryPoints[i * 2] = 0.5f
            boundaryPoints[i * 2 + 1] = y
        }
    }
    
    override fun setScreenDimensions(width: Int, height: Int) {
        // Adjust grid resolution based on screen size while maintaining reasonable performance
        val targetCells = 3000 // Target total number of cells
        val aspectRatio = width.toFloat() / height.toFloat()
        
        gridHeight = sqrt(targetCells / aspectRatio).toInt().coerceIn(20, 80)
        gridWidth = (gridHeight * aspectRatio).toInt().coerceIn(20, 120)
        
        cellSize = width.toFloat() / gridWidth
        
        // Reinitialize arrays with new size
        initializeArrays()
        reset()
    }
    
    override fun getLeftRegionIntensity(x: Float, y: Float): Float {
        val gridX = (x * gridWidth).toInt().coerceIn(0, gridWidth - 1)
        val gridY = (y * gridHeight).toInt().coerceIn(0, gridHeight - 1)
        val index = gridX + gridY * gridWidth
        return colorField[index]
    }
    
    override fun getRightRegionIntensity(x: Float, y: Float): Float {
        val gridX = (x * gridWidth).toInt().coerceIn(0, gridWidth - 1)
        val gridY = (y * gridHeight).toInt().coerceIn(0, gridHeight - 1)
        val index = gridX + gridY * gridWidth
        return 1f - colorField[index]
    }
    
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
    
    fun getCurrentResolution(): ResolutionMode = currentResolution
    fun getCurrentFPS(): Float = currentFPS
}
