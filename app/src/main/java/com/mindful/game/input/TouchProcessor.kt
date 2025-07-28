package com.mindful.game.input

import android.view.MotionEvent
import com.mindful.game.core.interfaces.IPhysicsEngine
import com.mindful.game.core.interfaces.ITouchProcessor

/**
 * Basic implementation of touch processor for MVP.
 * Handles single and multi-touch input and converts to normalized coordinates.
 */
class TouchProcessor : ITouchProcessor {
    
    private var sensitivity = 1.0f
    private var isTouchActive = false
    private val activeTouches = mutableMapOf<Int, TouchState>()
    
    data class TouchState(
        var x: Float,
        var y: Float,
        var pressure: Float,
        var startTime: Long
    )
    
    override fun processTouchEvent(
        event: MotionEvent,
        physicsEngine: IPhysicsEngine,
        viewWidth: Int,
        viewHeight: Int
    ): Boolean {
        
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                handleTouchDown(event, pointerIndex, pointerId, physicsEngine, viewWidth, viewHeight)
            }
            
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(event, physicsEngine, viewWidth, viewHeight)
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handleTouchUp(pointerId)
            }
            
            MotionEvent.ACTION_CANCEL -> {
                handleTouchCancel()
            }
        }
        
        isTouchActive = activeTouches.isNotEmpty()
        return true
    }
    
    private fun handleTouchDown(
        event: MotionEvent,
        pointerIndex: Int,
        pointerId: Int,
        physicsEngine: IPhysicsEngine,
        viewWidth: Int,
        viewHeight: Int
    ) {
        val x = event.getX(pointerIndex) / viewWidth
        val y = event.getY(pointerIndex) / viewHeight
        val pressure = event.getPressure(pointerIndex) * sensitivity
        
        // Store touch state
        activeTouches[pointerId] = TouchState(
            x = x,
            y = y,
            pressure = pressure.coerceIn(0f, 1f),
            startTime = System.currentTimeMillis()
        )
        
        // Apply to physics engine
        physicsEngine.applyTouchInput(x, y, pressure.coerceIn(0f, 1f))
    }
    
    private fun handleTouchMove(
        event: MotionEvent,
        physicsEngine: IPhysicsEngine,
        viewWidth: Int,
        viewHeight: Int
    ) {
        for (i in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(i)
            val touchState = activeTouches[pointerId] ?: continue
            
            val x = event.getX(i) / viewWidth
            val y = event.getY(i) / viewHeight
            val pressure = event.getPressure(i) * sensitivity
            
            // Update touch state
            touchState.x = x
            touchState.y = y
            touchState.pressure = pressure.coerceIn(0f, 1f)
            
            // Apply to physics engine
            physicsEngine.applyTouchInput(x, y, pressure.coerceIn(0f, 1f))
        }
    }
    
    private fun handleTouchUp(pointerId: Int) {
        activeTouches.remove(pointerId)
    }
    
    private fun handleTouchCancel() {
        activeTouches.clear()
    }
    
    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.1f, 2.0f)
    }
    
    override fun isTouchActive(): Boolean {
        return isTouchActive
    }
    
    /**
     * Gets the number of active touches.
     */
    fun getActiveTouchCount(): Int {
        return activeTouches.size
    }
    
    /**
     * Gets the average position of all active touches.
     */
    fun getAverageTouchPosition(): Pair<Float, Float>? {
        if (activeTouches.isEmpty()) return null
        
        var sumX = 0f
        var sumY = 0f
        
        for (touch in activeTouches.values) {
            sumX += touch.x
            sumY += touch.y
        }
        
        return Pair(sumX / activeTouches.size, sumY / activeTouches.size)
    }
    
    /**
     * Gets the total pressure from all active touches.
     */
    fun getTotalPressure(): Float {
        return activeTouches.values.sumOf { it.pressure.toDouble() }.toFloat()
    }
}
