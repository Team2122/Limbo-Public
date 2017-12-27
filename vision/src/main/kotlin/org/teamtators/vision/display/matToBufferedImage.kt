package org.teamtators.vision.display

import org.opencv.core.Mat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

fun matToBufferedImage(m: Mat): BufferedImage {
    // source: http://answers.opencv.org/question/10344/opencv-java-load-image-to-gui/
    // Fastest code
    // The output can be assigned either to a BufferedImage or to an Image

    var type = BufferedImage.TYPE_BYTE_GRAY
    if (m.channels() > 1) {
        type = BufferedImage.TYPE_3BYTE_BGR
    }
    val bufferSize = m.channels() * m.cols() * m.rows()
    val b = ByteArray(bufferSize)
    m.get(0, 0, b) // get all the pixels
    val image = BufferedImage(m.cols(), m.rows(), type)
    val targetPixels = (image.raster.dataBuffer as DataBufferByte).data
    System.arraycopy(b, 0, targetPixels, 0, b.size)
    return image
}