package com.mindful.game.core.interfaces

/**
 * Interface for physics engines that handle boundary simulation and interaction.
 * This abstraction allows easy swapping between different physics implementations.
 */
interface IPhysicsEngine {
    
    /**
     * Updates the boundary simulation for the given time delta.
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun updateBoundary(deltaTime: Float)
    
    /**
     * Applies touch input to influence the boundary restoration.
     * @param x Touch X coordinate (normalized 0-1)
     * @param y Touch Y coordinate (normalized 0-1) 
     * @param pressure Touch pressure (0-1, where 1 is maximum pressure)
     */
    fun applyTouchInput(x: Float, y: Float, pressure: Float)
    
    /**
     * Gets the current boundary points for rendering.
     * @return Array of alternating x,y coordinates representing the boundary
     */
    fun getBoundaryPoints(): FloatArray
    
    /**
     * Gets the current restoration progress.
     * @return Progress value between 0.0 (fully degraded) and 1.0 (fully restored)
     */
    fun getRestorationProgress(): Float
    
    /**
     * Resets the simulation to initial state.
     */
    fun reset()
    
    /**
     * Sets the screen dimensions for coordinate normalization.
     * @param width Screen width in pixels
     * @param height Screen height in pixels
     */
    fun setScreenDimensions(width: Int, height: Int)
    
    /**
     * Gets the left rectangle color intensity at given position.
     * @param x X coordinate (0-1)
     * @param y Y coordinate (0-1)
     * @return Color intensity (0-1)
     */
    fun getLeftRegionIntensity(x: Float, y: Float): Float
    
    /**
     * Gets the right rectangle color intensity at given position.
     * @param x X coordinate (0-1)
     * @param y Y coordinate (0-1)
     * @return Color intensity (0-1)
     */
    fun getRightRegionIntensity(x: Float, y: Float): Float
}
