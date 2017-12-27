package org.teamtators.vision.vision

import io.reactivex.subscribers.DefaultSubscriber
import org.opencv.imgproc.Imgproc
import org.reactivestreams.Subscriber
import org.teamtators.vision.config.Config
import org.teamtators.vision.display.matToBufferedImage
import org.teamtators.vision.loggerFor
import java.awt.image.BufferedImage

class ImageResizer constructor(
        val config: Config,
        val observer: Subscriber<BufferedImage>
) : DefaultSubscriber<ProcessResult>() {
    override fun onNext(processResult: ProcessResult?) {
        val image = resizeImage(processResult!!)
        observer.onNext(image)
    }

    override fun onComplete() {
        observer.onComplete()
    }

    override fun onError(t: Throwable?) {
        observer.onError(t)
    }


    companion object {
        val logger = loggerFor<ImageResizer>()
    }

    fun resizeImage(result: ProcessResult): BufferedImage {
        val frame = result.frame
        val streamRes = config.vision.streamRes

        if (streamRes.width > 0 && streamRes.height > 0)
            Imgproc.resize(frame, frame, streamRes)

        return matToBufferedImage(frame)
    }
}
