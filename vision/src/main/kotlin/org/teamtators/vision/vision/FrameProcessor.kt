package org.teamtators.vision.vision

import io.reactivex.subscribers.DefaultSubscriber
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.reactivestreams.Subscriber
import org.slf4j.Logger
import org.slf4j.profiler.Profiler
import org.teamtators.vision.VisionData
import org.teamtators.vision.VisionMode
import org.teamtators.vision.config.Config
import org.teamtators.vision.loggerFactory
import java.util.*

private val overlayColor = Scalar(255.0, 255.0, 255.0)

class FrameProcessor constructor(
        val _config: Config,
        val processResults: Subscriber<ProcessResult>,
        val matReturn: Subscriber<Mat>
) : DefaultSubscriber<MatCaptureData>() {
    override fun onNext(captureData: MatCaptureData?) {
        val result = process(captureData!!)
        processResults.onNext(result)
    }

    override fun onComplete() {
        processResults.onComplete()
    }

    override fun onError(t: Throwable?) {
        processResults.onError(t)
    }

    private val config: VisionConfig = _config.vision

    private val fpsCounter: FpsCounter = FpsCounter()
    private var fps: Long = 0

    companion object {
        val logger: Logger by loggerFactory()
    }

    private val erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, Size(2.0, 2.0))
    private val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, Size(2.0, 2.0))

    class ContourInfo(
            val contour: MatOfPoint,
            val contour2f: MatOfPoint2f,
            val minAreaRect: RotatedRect,
            val area: Double,
            val center: Point,
            val size: Size
    )

    private fun getContourInfo(contour: MatOfPoint, mode: VisionModeConfig): ContourInfo {
        val contour2f = MatOfPoint2f()
        contour.convertTo(contour2f, CvType.CV_32FC2)
        val epsilon = Imgproc.arcLength(contour2f, true) * mode.arcLengthPercentage
        Imgproc.approxPolyDP(contour2f, contour2f, epsilon, true)
        val minAreaRect = Imgproc.minAreaRect(contour2f)
        val area = minAreaRect.size.area() / config.inputRes.area()
        val moments = Imgproc.moments(contour)
        val c = moments.center
        val center = Point((c.x / config.inputRes.width * 2) - 1, (c.y / config.inputRes.height * 2) - 1)
        val s = contour.size()
        val size = Size(s.width / config.inputRes.width, s.height / config.inputRes.height)
        return ContourInfo(contour, contour2f, minAreaRect, area, center, size)
    }

    val displayMat = Mat.zeros(config.inputRes, CvType.CV_8UC3)
    val hsvMat = Mat.zeros(config.inputRes, CvType.CV_8UC3)
    val thresholdMat = Mat.zeros(config.inputRes, CvType.CV_8UC1)

    fun process(captureData: MatCaptureData): ProcessResult {
        val inputMat = captureData.frame
        val visionMode = captureData.robotData.visionMode
        val mode = config.modes.get(visionMode)
        if (visionMode == null || mode == null) {
            inputMat.copyTo(displayMat)
            matReturn.onNext(inputMat)
            return ProcessResult(displayMat, VisionData(captureData.robotData, null, null, null, null, null, null))
        }
        val profiler = Profiler("FrameProcessor")
        profiler.logger = logger

        if (config.display == VisionDisplay.INPUT || config.display == VisionDisplay.CONTOURS)
            inputMat.copyTo(displayMat)

        profiler.start("ERODE_DILATE")
//        Imgproc.erode(inputMat, hsvMat, erodeKernel, Point(), 3);
//        Imgproc.dilate(hsvMat, hsvMat, dilateKernel, Point(), 2);

        profiler.start("THRESHOLD")
        Imgproc.cvtColor(inputMat, hsvMat, Imgproc.COLOR_BGR2HSV)
        Core.inRange(hsvMat, mode.lowerThreshold, mode.upperThreshold, thresholdMat)
        if (config.display == VisionDisplay.THRESHOLD)
            thresholdMat.copyTo(displayMat)

        profiler.start("FIND_CONTOURS")
        val hierarchy = Mat()
        val rawContours = ArrayList<MatOfPoint>()
        Imgproc.findContours(thresholdMat, rawContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1)

        profiler.start("FILTER_CONTOURS")
        // Get information on all contours and filter by area range, and sort by area
        val contours = rawContours
                .map { getContourInfo(it, mode) }
                .filter {
                    it.area >= mode.minArea
                            && it.area <= mode.maxArea
                }
                .sortedByDescending { it.area }

        profiler.start("DRAW")
        // Draw all contours, with the largest one in a larger thickness
        if (config.display == VisionDisplay.CONTOURS) {
            if (config.debug)
                displayMat.drawContours(rawContours, color = Scalar(0.0, 0.0, 255.0), thickness = 1, hierarchy = hierarchy)
            contours.forEachIndexed { i, contour ->
                val thickness = if (i == 0 || (i == 1 && config.visionMode == VisionMode.GEAR)) 3 else 1;
                displayMat.drawContour(contour.contour, color = Scalar(0.0, 255.0, 0.0), thickness = thickness)
                displayMat.drawCircle(contour.center, 2, Scalar(255.0, 0.0, 0.0))
                displayMat.drawText(contour.area.round().toString(), contour.center,
                        fontFace = Core.FONT_HERSHEY_SIMPLEX, fontScale = 0.5, color = Scalar(255.0, 0.0, 0.0))
            }
        }

        // Draw crosshair
        drawCrosshair(displayMat)

        val fps = fpsCounter.getFps()
        if (fps != null) {
            this.fps = fps
            if (config.debug)
                logger.trace("Process FPS: {}", fps)
        }

        displayMat.drawText("${this.fps}", Point(5.0, 30.0), Core.FONT_HERSHEY_SIMPLEX, 1.0,
                overlayColor)

        profiler.start("CALCULATE")

        // Calculate and draw largest contour position
        val visionData: VisionData
        when (captureData.robotData.visionMode ?: VisionMode.FUEL) {
            VisionMode.GEAR -> {
                val largest = contours.getOrNull(0)
                val second = contours.getOrNull(1)
                val x : Double?
                val y : Double?
                val area : Double?
                val separation: Double?
                if (largest != null && second != null) {
                    x = (largest.center.x + second.center.x) / 2
                    y = (largest.center.y + second.center.y) / 2
                    area = largest.area + second.area
                    separation = largest.center.dist(second.center)
                    displayMat.drawCircle(center = Point((x / 2.0 + .5) * displayMat.width(), (y / 2.0 + .5) * displayMat.height()),
                            color = Scalar(255.0, 0.0, 0.0), radius = 2)
                } else {
                    x = null
                    y = null
                    area = null
                    separation = null
                }

                visionData = VisionData(captureData.robotData, x, y, area, largest?.size?.width, largest?.size?.height,
                        separation)
            }
            else -> { // FUEL
                val largestContour = contours.firstOrNull()
                val target = largestContour?.center
                val size = largestContour?.size

                visionData = VisionData(captureData.robotData, target?.x, target?.y, largestContour?.area,
                        size?.width, size?.height, null)
            }
        }

        if (_config.profile) {
            profiler.log()
        }

        matReturn.onNext(captureData.frame)
        logger.trace("Processed vision: $visionData")
        return ProcessResult(displayMat, visionData)
    }

    private fun drawCrosshair(mat: Mat) {
        val center = mat.size().toPoint() / 2.0
//        val width = mat.size().width
        val height = mat.size().height
        mat.drawCenterRect(center, 25, 25, overlayColor)   //to be replaced with corrected targeting values
//        mat.drawLine(Point(0.0, center.y), Point(width, center.y), overlayColor)
        mat.drawLine(Point(center.x, 0.0), Point(center.x, height), overlayColor)
    }
}