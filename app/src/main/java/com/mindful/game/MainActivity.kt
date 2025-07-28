package com.mindful.game

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.mindful.game.ui.GameView

/**
 * Main activity that hosts the game and provides simple UI controls.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var gameView: GameView
    private lateinit var resetButton: Button
    private lateinit var progressText: TextView
    private lateinit var sensitivitySeekBar: SeekBar
    private lateinit var qualitySeekBar: SeekBar
    private lateinit var controlsContainer: ConstraintLayout
    
    private var controlsVisible = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set fullscreen flags for immersive experience
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide action bar
        supportActionBar?.hide()
        
        setupUI()
        setupControls()
    }
    
    private fun setupUI() {
        // Create the main layout programmatically for better control
        val mainLayout = ConstraintLayout(this).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create game view
        gameView = GameView(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create controls container
        controlsContainer = ConstraintLayout(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0x80000000.toInt()) // Semi-transparent black
        }
        
        // Create reset button
        resetButton = Button(this).apply {
            id = View.generateViewId()
            text = "Reset"
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        
        // Create progress text
        progressText = TextView(this).apply {
            id = View.generateViewId()
            text = "Progress: 0%"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        
        // Create sensitivity label and seekbar
        val sensitivityLabel = TextView(this).apply {
            id = View.generateViewId()
            text = "Touch Sensitivity"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = resetButton.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 32
            }
        }
        
        sensitivitySeekBar = SeekBar(this).apply {
            id = View.generateViewId()
            max = 100
            progress = 50 // Default sensitivity
            layoutParams = ConstraintLayout.LayoutParams(
                200,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = sensitivityLabel.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 8
            }
        }
        
        // Create quality label and seekbar
        val qualityLabel = TextView(this).apply {
            id = View.generateViewId()
            text = "Rendering Quality"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = resetButton.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 32
            }
        }
        
        qualitySeekBar = SeekBar(this).apply {
            id = View.generateViewId()
            max = 2
            progress = 1 // Default medium quality
            layoutParams = ConstraintLayout.LayoutParams(
                200,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = qualityLabel.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 8
            }
        }
        
        // Add views to containers
        controlsContainer.addView(resetButton)
        controlsContainer.addView(progressText)
        controlsContainer.addView(sensitivityLabel)
        controlsContainer.addView(sensitivitySeekBar)
        controlsContainer.addView(qualityLabel)
        controlsContainer.addView(qualitySeekBar)
        
        mainLayout.addView(gameView)
        mainLayout.addView(controlsContainer)
        
        setContentView(mainLayout)
    }
    
    private fun setupControls() {
        // Reset button
        resetButton.setOnClickListener {
            gameView.resetGame()
        }
        
        // Sensitivity control
        sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = 0.1f + (progress / 100f) * 1.9f // Range: 0.1 to 2.0
                gameView.setSensitivity(sensitivity)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Quality control
        qualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gameView.setQuality(progress)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Toggle controls visibility on game view tap
        gameView.setOnClickListener {
            toggleControlsVisibility()
        }
        
        // Update progress periodically
        startProgressUpdater()
    }
    
    private fun toggleControlsVisibility() {
        controlsVisible = !controlsVisible
        controlsContainer.visibility = if (controlsVisible) View.VISIBLE else View.GONE
    }
    
    private fun startProgressUpdater() {
        val progressRunnable = object : Runnable {
            override fun run() {
                val progress = (gameView.getProgress() * 100).toInt()
                progressText.text = "Progress: $progress%"
                
                // Schedule next update
                progressText.postDelayed(this, 100)
            }
        }
        progressText.post(progressRunnable)
    }
    
    override fun onResume() {
        super.onResume()
        // Apply initial settings
        val sensitivity = 0.1f + (sensitivitySeekBar.progress / 100f) * 1.9f
        gameView.setSensitivity(sensitivity)
        gameView.setQuality(qualitySeekBar.progress)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Enable immersive mode
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}
