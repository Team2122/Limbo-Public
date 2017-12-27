package org.teamtators.vision.vision

import org.opencv.core.Scalar
import org.opencv.core.Size
import org.teamtators.vision.VisionMode

enum class VisionDisplay {
    NONE, INPUT, THRESHOLD, CONTOURS
}

class VisionModeConfig {
    var cameraIndex: Int = 0
    var maxFPS: Int = 30

    var upsideDown: Boolean = false

    var lowerThreshold: Scalar = Scalar(60.0, 150.0, 20.0)
    var upperThreshold: Scalar = Scalar(100.0, 255.0, 255.0)
    var minArea: Double = 0.0
    var maxArea: Double = 1.0
    var arcLengthPercentage: Double = 0.01

    var startVisionScript: String = "true"
}

class VisionConfig {
    var display: VisionDisplay = VisionDisplay.CONTOURS
    var debug: Boolean = false

    var inputRes: Size = Size()
    var streamRes: Size = Size()

    var modes: Map<VisionMode, VisionModeConfig> = mutableMapOf()
    var visionMode: VisionMode = VisionMode.FUEL
    val currentMode: VisionModeConfig? = modes[visionMode]
}