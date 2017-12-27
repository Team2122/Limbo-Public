package org.teamtators.vision.vision

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DefaultSubscriber
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import org.reactivestreams.Subscriber
import org.slf4j.LoggerFactory
import org.slf4j.profiler.Profiler
import org.teamtators.vision.RobotData
import org.teamtators.vision.VisionMode
import org.teamtators.vision.config.Config
import org.teamtators.vision.util.runShell
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

val scheduler = Schedulers.io()

class OpenCVCapturer(
        val _config: Config,
        val matSource: Flowable<Mat>,
        val robotData: Flowable<RobotData>,
        val capturedFrames: Subscriber<MatCaptureData>
) {
    companion object {
        val logger = LoggerFactory.getLogger(OpenCVCapturer::class.java)
    }

    private val config = _config.vision

    private val currentRobotData = AtomicReference<RobotData?>()
    private val capturingSubscriber = CapturingSubscriber()
    private val fuelCapture = VideoCapture()
    private val gearCapture = VideoCapture()
    private var lastMode = VisionMode.NONE;

    init {
        robotData
                .observeOn(scheduler)
                .subscribe({
                    currentRobotData.lazySet(it)
                })
    }

    fun start() {
        logger.info("Starting OpenCV capturer")
        configureVideoCapture(fuelCapture, VisionMode.FUEL)
        configureVideoCapture(gearCapture, VisionMode.GEAR)

        matSource
                .subscribeOn(scheduler)
                .subscribe(capturingSubscriber)
    }

    fun stop() {
        capturingSubscriber.stop()
        try {
            logger.debug("Releasing VideoCaptures")
            releaseCapture(fuelCapture)
            releaseCapture(gearCapture)
        } catch (e: Throwable) {
            logger.error("Error releasing VideoCapture", e)
        }
    }

    private fun releaseCapture(videoCapture: VideoCapture) {
        synchronized(videoCapture) {
            if (videoCapture.isOpened)
                videoCapture.release()
        }
    }

    private fun configureVideoCapture(videoCapture: VideoCapture, mode: VisionMode) {
        val modeConfig: VisionModeConfig = config.modes[mode] ?: return
        try {
            logger.debug("Opening vision camera for $mode (${modeConfig.cameraIndex})")
            videoCapture.open(modeConfig.cameraIndex)//Initialize Video Capture

            val inputRes = config.inputRes
            if (inputRes.width > 0 && inputRes.height > 0) {
                logger.debug("Setting capture resolution to {}x{}", inputRes.width, inputRes.height)
                videoCapture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, inputRes.width)
                videoCapture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, inputRes.height)
            }
            videoCapture.set(Videoio.CV_CAP_PROP_BUFFERSIZE, 1.0)
            if (!videoCapture.isOpened) {
                throw RuntimeException("Error opening OpenCV camera " + modeConfig.cameraIndex)
            }

            Observable.interval(2, 10, TimeUnit.SECONDS)
                    .subscribe {
                        configureCamera(mode, modeConfig)
                    }

            logger.info("Opened OpenCV camera {}. Starting capturer", modeConfig.cameraIndex)
        } catch(e: Exception) {
            throw RuntimeException("Unhandled exception initializing VideoCapture", e)
        }
    }

    private fun configureCamera(mode: VisionMode, modeConfig: VisionModeConfig) {
        logger.debug("Configuring vision camera for $mode (${modeConfig.cameraIndex})")
        try {
            runShell("VisionConfig", modeConfig.startVisionScript)
        } catch(e: IOException) {
            logger.warn("Could not configure camera. Is v4l2-ctl installed?", e)
        }
    }

    inner class CapturingSubscriber : DefaultSubscriber<Mat>() {
        override fun onStart() {
            this.request(1)
        }

        override fun onNext(mat: Mat?) {
            val robotData = currentRobotData.get() ?: RobotData()
            val visionMode = when (robotData.visionMode) {
                VisionMode.NONE, VisionMode.FUEL, null -> VisionMode.FUEL
                VisionMode.GEAR -> VisionMode.GEAR
            }
            if (visionMode != lastMode) {
                config.modes[visionMode]?.let { configureCamera(visionMode, it) }
                lastMode = visionMode
            }
            val cap = when (visionMode) {
                VisionMode.FUEL -> fuelCapture
                VisionMode.GEAR -> gearCapture
                else -> null
            }
            mat?.let {
                doCapture(cap, it, visionMode, robotData)
            }
            this.request(1)
        }

        fun pushBlankMat(mat: Mat, robotData: RobotData) {
            mat.setTo(Scalar(0.0, 0.0, 0.0))
            capturedFrames.onNext(MatCaptureData(mat, robotData))
        }

        private fun doCapture(cap: VideoCapture?, mat: Mat, visionMode: VisionMode, robotData: RobotData) {
            if (cap == null) {
                pushBlankMat(mat, robotData)
                return
            }
            synchronized(cap) {
                if (cap.isOpened) {
                    try {
                        val modeConfig: VisionModeConfig? = config.modes[visionMode];
                        if (modeConfig == null) {
                            logger.warn("No config for mode $visionMode")
                            pushBlankMat(mat, robotData)
                            return
                        }
                        capture(cap, mat, modeConfig)
                        val data = MatCaptureData(mat, robotData)
                        capturedFrames.onNext(data)
                        logger.trace("Captured frame: $data")

                    } catch (e: Throwable) {
                        logger.error("Unhandled exception while capturing frame", e)
                    }
                } else {
                    pushBlankMat(mat, robotData)
                }
            }
        }

        private fun capture(videoCapture: VideoCapture, frame: Mat, modeConfig: VisionModeConfig) {
            val inputRes = config.inputRes
            val profiler = Profiler("OpenCVCapturer")
            profiler.logger = logger

            profiler.start("CAPTURE")
            videoCapture.grab()

            videoCapture.retrieve(frame)
            profiler.start("PROCESS")
            if (inputRes.width > 0 && inputRes.height > 0) {
                Imgproc.resize(frame, frame, inputRes)
            }
            if (modeConfig.upsideDown) {
                Core.flip(frame, frame, -1)
            }
            if (_config.profile) {
                profiler.log()
            }
        }

        override fun onComplete() {
            capturedFrames.onComplete()
        }

        override fun onError(t: Throwable) {
            capturedFrames.onError(t)
        }

        fun stop() {
            cancel()
        }
    };
}