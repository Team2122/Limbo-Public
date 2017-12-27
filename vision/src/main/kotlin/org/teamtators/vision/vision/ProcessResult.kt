package org.teamtators.vision.vision

import org.opencv.core.Mat
import org.teamtators.vision.VisionData

data class ProcessResult(val frame: Mat, val visionData: VisionData) {
}