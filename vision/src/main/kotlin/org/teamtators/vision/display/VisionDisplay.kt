package org.teamtators.vision.display

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import org.teamtators.vision.config.Config
import org.teamtators.vision.loggerFor
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.WindowConstants

class VisionDisplay constructor(
        _config: Config,
        val displayImages: Flowable<BufferedImage>
) : JFrame("TatorVision") {
    companion object {
        val logger = loggerFor<VisionDisplay>()
    }

    val config = _config.vision
    val imageDisplay = OpenCVDisplay()

    init {
        this.add(imageDisplay)
        this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }

    private var subscription: Disposable? = null

    fun start() {
        logger.info("Starting image display")

        val initialFrame = BufferedImage(config.streamRes.width.toInt(),
                config.streamRes.height.toInt(),
                BufferedImage.TYPE_3BYTE_BGR)
        imageDisplay.updateImage(initialFrame)
        this.pack()

        this.isVisible = true
        if (subscription?.isDisposed ?: true)
            subscription = displayImages.subscribe({ displayImage(it) })
    }

    fun stop() {
        isVisible = false
        subscription?.dispose()
        subscription = null
    }

    fun displayImage(image: BufferedImage) {
        imageDisplay.updateImage(image)
    }
}