package org.teamtators.vision.display

import org.opencv.core.Mat
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JLabel

class OpenCVDisplay() : JLabel() {
    private val imageIcon: ImageIcon = ImageIcon()

    init {
        icon = imageIcon
    }

    constructor(img: BufferedImage) : this() {
        updateImage(img)
    }

    constructor(mat: Mat) : this() {
        updateImage(mat)
    }

    fun updateImage(mat: Mat) {
        updateImage(matToBufferedImage(mat))
    }

    fun updateImage(image: BufferedImage) {
        imageIcon.image = image
        this.updateUI()
    }
}
