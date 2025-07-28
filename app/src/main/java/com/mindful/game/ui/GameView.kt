package com.mindful.game.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.mindful.game.core.GameEngine
import com.mindful.game.input.TouchProcessor
import com.mindful.game.physics.NoiseBasedEngine
import com.mindful.game.rendering.SimpleRenderer

/**
 * Custom SurfaceView that displays the game and handles input.
 * Uses SurfaceView for better performance and frame rate control.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    
    private lateinit var gameEngine: GameEngine
    private var gameThread: GameThread? = null
    private var isGameInitialized = false
    
    init {
        // Initialize surface holder
        holder.addCallback(this)
        isFocusable = true
    }
    
    private fun initializeGame() {
        if (isGameInitialized) return
        
        // Create modular components - easy to swap implementations
        val physicsEngine = NoiseBasedEngine()
        val renderer = SimpleRenderer()
        val touchProcessor = TouchProcessor()
        
        // Create game engine with dependency injection
        gameEngine = GameEngine(physicsEngine, renderer, touchProcessor)
        
        isGameInitialized = true
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        initializeGame()
        gameEngine.start()
        
        // Start game thread
        gameThread = GameThread(holder)
        gameThread?.isRunning = true
        gameThread?.start()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (isGameInitialized) {
            gameEngine.setScreenDimensions(width, height)
        }
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameThread?.isRunning = false
        
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                // Will try again
            }
        }
        
        if (isGameInitialized) {
            gameEngine.stop()
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGameInitialized) return false
        
        return gameEngine.getTouchProcessor().processTouchEvent(
            event,
            gameEngine.getPhysicsEngine(),
            width,
            height
        )
    }
    
    /**
     * Resets the game to initial state.
     */
    fun resetGame() {
        if (isGameInitialized) {
            gameEngine.reset()
        }
    }
    
    /**
     * Gets the current restoration progress.
     */
    fun getProgress(): Float {
        return if (isGameInitialized) {
            gameEngine.getProgress()
        } else {
            0f
        }
    }
    
    /**
     * Sets the rendering quality.
     */
    fun setQuality(quality: Int) {
        if (isGameInitialized) {
            gameEngine.getRenderer().setQuality(quality)
        }
    }
    
    /**
     * Sets the touch sensitivity.
     */
    fun setSensitivity(sensitivity: Float) {
        if (isGameInitialized) {
            gameEngine.getTouchProcessor().setSensitivity(sensitivity)
        }
    }
    
    /**
     * Game loop thread that updates and renders the game.
     */
    private inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        
        var isRunning = false
        private val targetFPS = 60
        private val targetFrameTime = 1000 / targetFPS
        
        override fun run() {
            var lastFrameTime = System.currentTimeMillis()
            
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastFrameTime
                
                if (deltaTime >= targetFrameTime) {
                    var canvas: Canvas? = null
                    
                    try {
                        canvas = surfaceHolder.lockCanvas()
                        canvas?.let { c ->
                            // Update game state
                            gameEngine.update()
                            
                            // Render game
                            gameEngine.getRenderer().render(
                                c,
                                gameEngine.getPhysicsEngine(),
                                c.width,
                                c.height
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        canvas?.let { c ->
                            try {
                                surfaceHolder.unlockCanvasAndPost(c)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    lastFrameTime = currentTime
                } else {
                    // Sleep to maintain target frame rate
                    try {
                        sleep(1)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        }
    }
}
