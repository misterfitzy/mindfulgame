package com.mindful.game.utils

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Generic object pool for efficient memory management and reduced garbage collection.
 */
class ObjectPool<T> private constructor(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val maxSize: Int
) {
    private val pool = ConcurrentLinkedQueue<T>()
    
    fun acquire(): T {
        return pool.poll() ?: factory()
    }
    
    fun release(obj: T) {
        if (pool.size < maxSize) {
            reset(obj)
            pool.offer(obj)
        }
    }
    
    fun clear() {
        pool.clear()
    }
    
    companion object {
        fun <T> create(factory: () -> T, reset: (T) -> Unit, maxSize: Int = 10): ObjectPool<T> {
            return ObjectPool(factory, reset, maxSize)
        }
    }
}

/**
 * Specialized bitmap pool for FluidRenderer memory optimization.
 */
class BitmapPool(private val maxSize: Int = 5) {
    private val pool = mutableMapOf<String, ConcurrentLinkedQueue<Bitmap>>()
    
    fun acquire(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val key = "${width}x${height}_${config}"
        val queue = pool.getOrPut(key) { ConcurrentLinkedQueue() }
        
        return queue.poll()?.takeIf { !it.isRecycled } 
            ?: Bitmap.createBitmap(width, height, config)
    }
    
    fun release(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            val key = "${bitmap.width}x${bitmap.height}_${bitmap.config}"
            val queue = pool.getOrPut(key) { ConcurrentLinkedQueue() }
            
            if (queue.size < maxSize) {
                queue.offer(bitmap)
            } else {
                bitmap.recycle()
            }
        }
    }
    
    fun cleanup() {
        pool.values.forEach { queue ->
            queue.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            queue.clear()
        }
        pool.clear()
    }
    
    fun getPoolStats(): Map<String, Int> {
        return pool.mapValues { it.value.size }
    }
}

/**
 * Array pool for efficient reuse of pixel arrays and other large arrays.
 */
class ArrayPool {
    private val intArrayPool = ObjectPool.create(
        factory = { IntArray(0) },
        reset = { /* Arrays are replaced, no reset needed */ },
        maxSize = 3
    )
    
    private val floatArrayPool = ObjectPool.create(
        factory = { FloatArray(0) },
        reset = { /* Arrays are replaced, no reset needed */ },
        maxSize = 3
    )
    
    fun acquireIntArray(size: Int): IntArray {
        val array = intArrayPool.acquire()
        return if (array.size == size) array else IntArray(size)
    }
    
    fun releaseIntArray(array: IntArray) {
        intArrayPool.release(array)
    }
    
    fun acquireFloatArray(size: Int): FloatArray {
        val array = floatArrayPool.acquire()
        return if (array.size == size) array else FloatArray(size)
    }
    
    fun releaseFloatArray(array: FloatArray) {
        floatArrayPool.release(array)
    }
    
    fun cleanup() {
        intArrayPool.clear()
        floatArrayPool.clear()
    }
}

/**
 * Performance monitoring utility for tracking FPS and memory usage.
 */
class PerformanceMonitor {
    private val frameTimeHistory = mutableListOf<Long>()
    private val maxHistorySize = 60
    private var lastFrameTime = System.currentTimeMillis()
    
    fun onFrameStart() {
        val currentTime = System.currentTimeMillis()
        val frameTime = currentTime - lastFrameTime
        lastFrameTime = currentTime
        
        frameTimeHistory.add(frameTime)
        if (frameTimeHistory.size > maxHistorySize) {
            frameTimeHistory.removeAt(0)
        }
    }
    
    fun getCurrentFPS(): Float {
        return if (frameTimeHistory.isNotEmpty()) {
            val averageFrameTime = frameTimeHistory.average()
            (1000f / averageFrameTime).toFloat()
        } else 60f
    }
    
    fun getAverageFrameTime(): Float {
        return frameTimeHistory.average().toFloat()
    }
    
    fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    fun getMaxMemory(): Long {
        return Runtime.getRuntime().maxMemory()
    }
    
    fun getMemoryPressure(): Float {
        return getMemoryUsage().toFloat() / getMaxMemory().toFloat()
    }
    
    fun reset() {
        frameTimeHistory.clear()
        lastFrameTime = System.currentTimeMillis()
    }
}
