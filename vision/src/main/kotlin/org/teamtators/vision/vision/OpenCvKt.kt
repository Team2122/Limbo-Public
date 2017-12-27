package org.teamtators.vision.vision

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Moments
import java.util.*


operator fun Point.plus(other: Point): Point = Point(x + other.x, y + other.y)

operator fun Point.minus(other: Point): Point = Point(x - other.x, y - other.y)

operator fun Point.times(d: Double): Point = Point(x * d, y * d)

operator fun Point.div(d: Double): Point = Point(x / d, y / d)

fun Point.dist(other: Point): Double? = Math.sqrt(Math.pow(this.x - other.x, 2.0) + Math.pow(this.y - other.y, 2.0))

fun Point.toSize(): Size = Size(x, y)

operator fun Size.div(d: Double): Size = Size(width / d, height / d)

fun Size.toPoint(): Point = Point(width, height)

fun Mat.drawLine(point1: Point, point2: Point, color: Scalar, thickness: Int = 1) {
    Imgproc.line(this, point1, point2, color, thickness, 8, 0)
}

fun Mat.drawCenterRect(center: Point, width: Int, height: Int, color: Scalar, thickness: Int = 1) {
    val upperLeft = Point(center.x - width / 2, center.y - height / 2)
    val upperRight = Point(center.x + width / 2, center.y - height / 2)
    val lowerRight = Point(center.x + width / 2, center.y + height / 2)
    val lowerLeft = Point(center.x - width / 2, center.y + height / 2)
    this.drawLine(upperLeft, upperRight, color, thickness)
    this.drawLine(upperRight, lowerRight, color, thickness)
    this.drawLine(lowerRight, lowerLeft, color, thickness)
    this.drawLine(lowerLeft, upperLeft, color, thickness)
}

fun Mat.drawCircle(center: Point, radius: Int, color: Scalar, thickness: Int = 1) {
    Imgproc.circle(this, center, radius, color, thickness, 8, 0)
}

fun Mat.drawText(text: String, origin: Point, fontFace: Int, fontScale: Double, color: Scalar, thickness: Int = 1) {
    Imgproc.putText(this, text, origin, fontFace, fontScale, color, thickness)
}

fun Mat.drawContours(contours: List<MatOfPoint>, color: Scalar, thickness: Int = 1, hierarchy: Mat = Mat(),
                     maxLevel: Int = Int.MAX_VALUE, offset: Point = Point()) {
    Imgproc.drawContours(this, contours, -1, color, thickness, 8, hierarchy, maxLevel, offset)
}

fun Mat.drawContour(contour: MatOfPoint, color: Scalar, thickness: Int = 1, offset: Point = Point()) {
    this.drawContours(Collections.singletonList(contour), color, thickness, offset = offset)
}


val Moments.center: Point
    get() = Point(m10 / m00, m01 / m00)

fun Double.round(): Long = Math.round(this)

fun Double.toDegrees(): Double = Math.toDegrees(this)

fun Double.toRadians(): Double = Math.toRadians(this)