package org.teamtators.vision.config

import com.fasterxml.jackson.databind.module.SimpleModule
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size

class OpenCVModule : SimpleModule {
    constructor() : super() {
        addDeserializer(Size::class.java, SizeDeserializer())
        addSerializer(Size::class.java, SizeSerializer())
        addDeserializer(Scalar::class.java, ScalarDeserializer())
        addSerializer(Scalar::class.java, ScalarSerializer())
        addDeserializer(Point::class.java, PointDeserializer())
        addSerializer(Point::class.java, PointSerializer())
    }
}