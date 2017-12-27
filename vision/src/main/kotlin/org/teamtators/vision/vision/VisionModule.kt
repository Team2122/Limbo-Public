package org.teamtators.vision.vision

import io.reactivex.functions.Consumer
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.teamtators.vision.RobotData
import org.teamtators.vision.*
import java.awt.image.BufferedImage

class VisionModule(val mainModule: MainModule) : Module {
    companion object {
        val logger = loggerFor<VisionModule>()
        private val MAT_BUFFER_SIZE = 3
    }

    val mats: FlowableProcessor<Mat> = BehaviorProcessor.create<Mat>()
    val capturer: OpenCVCapturer
    val frameProcessor: FrameProcessor
    val imageResizer: ImageResizer

    val capturedFrames: FlowableProcessor<MatCaptureData> = BehaviorProcessor.create()
    val robotData: FlowableProcessor<RobotData> = BehaviorProcessor.create()
    val processResults: FlowableProcessor<ProcessResult> = BehaviorProcessor.create()
    val displayImages: FlowableProcessor<BufferedImage> = BehaviorProcessor.create()

    init {
        capturer = OpenCVCapturer(mainModule.config, mats
                .onBackpressureBuffer(MAT_BUFFER_SIZE)
                .observeOn(Schedulers.io()), robotData, capturedFrames)
        frameProcessor = FrameProcessor(mainModule.config, processResults, mats)
        imageResizer = ImageResizer(mainModule.config, displayImages)

        robotData.subscribe { robotData ->
            logger.trace("Received robot data: $robotData")
        }

        capturedFrames
                .onBackpressureDrop(Consumer { data ->
                    mats.onNext(data.frame)
                })
                .observeOn(Schedulers.computation(), false, 1)
                .subscribe(frameProcessor)

        processResults.subscribe({ result ->
            logger.trace("Process result: $result")
        })
        processResults.subscribe(imageResizer)
    }

    override fun start() {
        capturer.start()

        Main.logger.trace("Creating $MAT_BUFFER_SIZE initial Mats for capturing")
        for (i in 0..MAT_BUFFER_SIZE) {
            mats.onNext(Mat.zeros(mainModule.config.vision.inputRes, CvType.CV_8UC3))
        }
    }

    override fun stop() {
        capturer.stop()
        // TODO Properly stop vision
    }
}

