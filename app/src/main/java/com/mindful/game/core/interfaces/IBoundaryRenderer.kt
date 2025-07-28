package com.mindful.game.core.interfaces

import android.graphics.Canvas

/**
 * Interface for rendering the boundary and fluid regions.
 * This abstraction allows different rendering approaches (simple, particle-based, etc.)
 */
interface IBoundaryRenderer {
    
    /**
     * Renders the entire game state to the canvas.
     * @param canvas The canvas to draw on
     * @param physicsEngine The physics engine providing the current state
     * @param width Canvas width
     * @param height Canvas height
     */
    fun render(canvas: Canvas, physicsEngine: IPhysicsEngine, width: Int, height: Int)
    
    /**
     * Sets the rendering quality level.
     * @param quality Quality level (0=low, 1=medium, 2=high)
     */
    fun setQuality(quality: Int)
    
    /**
     * Updates any animation states.
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun update(deltaTime: Float)
}
