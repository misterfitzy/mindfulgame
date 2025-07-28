package com.mindful.game.core.interfaces

import android.view.MotionEvent

/**
 * Interface for processing touch input and converting it to game interactions.
 * This abstraction allows different touch handling strategies.
 */
interface ITouchProcessor {
    
    /**
     * Processes a touch event and applies it to the physics engine.
     * @param event The motion event to process
     * @param physicsEngine The physics engine to apply the input to
     * @param viewWidth The width of the touch view
     * @param viewHeight The height of the touch view
     * @return True if the event was handled, false otherwise
     */
    fun processTouchEvent(
        event: MotionEvent, 
        physicsEngine: IPhysicsEngine, 
        viewWidth: Int, 
        viewHeight: Int
    ): Boolean
    
    /**
     * Sets the touch sensitivity.
     * @param sensitivity Sensitivity multiplier (0.1 to 2.0)
     */
    fun setSensitivity(sensitivity: Float)
    
    /**
     * Gets whether touch input is currently active.
     * @return True if actively touching, false otherwise
     */
    fun isTouchActive(): Boolean
}
