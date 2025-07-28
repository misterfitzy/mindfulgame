package com.mindful.game.rendering

import android.graphics.*
import com.mindful.game.core.interfaces.IBoundaryRenderer
import com.mindful.game.core.interfaces.IPhysicsEngine

/**
 * Simple implementation of boundary renderer using Canvas drawing.
 * This can be replaced with OpenGL or more advanced rendering techniques.
 */
class SimpleRenderer : IBoundaryRenderer {
    
    private val whitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val blackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val boundaryPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val progressBarPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val progressBarBackgroundPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val path = Path()
    private var quality = 1 // 0=low, 1=medium, 2=high
    
    override fun render(canvas: Canvas, physicsEngine: IPhysicsEngine, width: Int, height: Int) {
        // Clear canvas with background
        canvas.drawColor(Color.WHITE)
        
        val boundaryPoints = physicsEngine.getBoundaryPoints()
        val numPoints = boundaryPoints.size / 2
        
        // Draw left region (white with gradient to boundary)
        renderLeftRegion(canvas, physicsEngine, width, height, boundaryPoints)
        
        // Draw right region (black with gradient from boundary)
        renderRightRegion(canvas, physicsEngine, width, height, boundaryPoints)
        
        // Draw boundary line for visual clarity
        if (quality > 0) {
            renderBoundaryLine(canvas, width, height, boundaryPoints)
        }
        
        // Draw progress bar
        renderProgressBar(canvas, width, height, physicsEngine.getRestorationProgress())
    }
    
    private fun renderLeftRegion(
        canvas: Canvas,
        physicsEngine: IPhysicsEngine,
        width: Int,
        height: Int,
        boundaryPoints: FloatArray
    ) {
        val numPoints = boundaryPoints.size / 2
        path.reset()
        
        // Start from top-left corner
        path.moveTo(0f, 0f)
        path.lineTo(boundaryPoints[0] * width, 0f)
        
        // Follow boundary curve
        for (i in 0 until numPoints) {
            val x = boundaryPoints[i * 2] * width
            val y = boundaryPoints[i * 2 + 1] * height
            path.lineTo(x, y)
        }
        
        // Close path to bottom-left corner
        path.lineTo(0f, height.toFloat())
        path.close()
        
        canvas.drawPath(path, whitePaint)
    }
    
    private fun renderRightRegion(
        canvas: Canvas,
        physicsEngine: IPhysicsEngine,
        width: Int,
        height: Int,
        boundaryPoints: FloatArray
    ) {
        val numPoints = boundaryPoints.size / 2
        path.reset()
        
        // Start from top boundary point
        path.moveTo(boundaryPoints[0] * width, 0f)
        path.lineTo(width.toFloat(), 0f)
        path.lineTo(width.toFloat(), height.toFloat())
        
        // Follow boundary curve in reverse
        for (i in (numPoints - 1) downTo 0) {
            val x = boundaryPoints[i * 2] * width
            val y = boundaryPoints[i * 2 + 1] * height
            path.lineTo(x, y)
        }
        
        path.close()
        canvas.drawPath(path, blackPaint)
    }
    
    private fun renderBoundaryLine(
        canvas: Canvas,
        width: Int,
        height: Int,
        boundaryPoints: FloatArray
    ) {
        val numPoints = boundaryPoints.size / 2
        
        for (i in 0 until numPoints - 1) {
            val x1 = boundaryPoints[i * 2] * width
            val y1 = boundaryPoints[i * 2 + 1] * height
            val x2 = boundaryPoints[(i + 1) * 2] * width
            val y2 = boundaryPoints[(i + 1) * 2 + 1] * height
            
            canvas.drawLine(x1, y1, x2, y2, boundaryPaint)
        }
    }
    
    private fun renderProgressBar(canvas: Canvas, width: Int, height: Int, progress: Float) {
        val barWidth = width * 0.8f
        val barHeight = 20f
        val barX = (width - barWidth) / 2
        val barY = 50f
        
        // Draw background
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, progressBarBackgroundPaint)
        
        // Draw progress
        canvas.drawRect(
            barX, 
            barY, 
            barX + barWidth * progress, 
            barY + barHeight, 
            progressBarPaint
        )
        
        // Draw border
        boundaryPaint.style = Paint.Style.STROKE
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, boundaryPaint)
        boundaryPaint.style = Paint.Style.FILL
    }
    
    override fun setQuality(quality: Int) {
        this.quality = quality.coerceIn(0, 2)
        
        // Adjust anti-aliasing based on quality
        val antiAlias = quality > 0
        whitePaint.isAntiAlias = antiAlias
        blackPaint.isAntiAlias = antiAlias
        boundaryPaint.isAntiAlias = antiAlias
        progressBarPaint.isAntiAlias = antiAlias
    }
    
    override fun update(deltaTime: Float) {
        // No animations in simple renderer
        // Future renderers might animate particles, effects, etc.
    }
}
