package org.teamtators.vision.vision

class FpsCounter {
    private var lastFpsTime: Long = 0
    private var frames: Long = 0

    companion object {
        private val S_IN_NS = 1000000000
    }

    fun reset() {
        lastFpsTime = 0
        frames = 0
    }

    fun getFps() : Long? {
        val nowNs = System.nanoTime()
        if (lastFpsTime <= nowNs - S_IN_NS) {
            val fps = frames
            frames = 0
            lastFpsTime = nowNs
            return fps
        } else {
            frames++
            return null
        }
    }
}