package org.teamtators.vision.vision

import org.opencv.core.Mat
import org.teamtators.vision.RobotData

data class MatCaptureData(val frame : Mat, val robotData: RobotData)