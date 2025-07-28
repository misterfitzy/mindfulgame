package com.mindful.game.core

import com.mindful.game.core.interfaces.IBoundaryRenderer
import com.mindful.game.core.interfaces.IPhysicsEngine
import com.mindful.game.core.interfaces.ITouchProcessor

/**
 * Central game engine that orchestrates all game components.
 * This class follows dependency injection pattern for easy component swapping.
 */
class GameEngine(
    private val physicsEngine: IPhysicsEngine,
    private val renderer: IBoundaryRenderer,
    private val touchProcessor: ITouchProcessor
) {
    
    private var lastUpdateTime = System.nanoTime()
    private var isRunning = false
    
    /**
     * Starts the game engine.
     */
    fun start() {
        isRunning = true
        lastUpdateTime = System.nanoTime()
    }
    
    /**
     * Stops the game engine.
     */
    fun stop() {
        isRunning = false
    }
    
    /**
     * Updates the game state. Should be called from the main game loop.
     */
    fun update() {
        if (!isRunning) return
        
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000f
        lastUpdateTime = currentTime
        
        // Update physics simulation
        physicsEngine.updateBoundary(deltaTime)
        
        // Update renderer animations
        renderer.update(deltaTime)
    }
    
    /**
     * Resets the game to initial state.
     */
    fun reset() {
        physicsEngine.reset()
    }
    
    /**
     * Sets screen dimensions for all components.
     */
    fun setScreenDimensions(width: Int, height: Int) {
        physicsEngine.setScreenDimensions(width, height)
    }
    
    /**
     * Gets the current restoration progress.
     */
    fun getProgress(): Float {
        return physicsEngine.getRestorationProgress()
    }
    
    /**
     * Gets the physics engine (for direct access if needed).
     */
    fun getPhysicsEngine(): IPhysicsEngine = physicsEngine
    
    /**
     * Gets the renderer (for direct access if needed).
     */
    fun getRenderer(): IBoundaryRenderer = renderer
    
    /**
     * Gets the touch processor (for direct access if needed).
     */
    fun getTouchProcessor(): ITouchProcessor = touchProcessor
    
    /**
     * Checks if the game is currently running.
     */
    fun isRunning(): Boolean = isRunning
}
