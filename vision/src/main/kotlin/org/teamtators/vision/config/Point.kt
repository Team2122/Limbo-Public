package org.teamtators.vision.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.opencv.core.Point
import org.opencv.core.Size

import java.io.IOException

class PointDeserializer : JsonDeserializer<Point>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Point {
        val array = p.readValueAs(DoubleArray::class.java)
        return Point(array)
    }
}

class PointSerializer : JsonSerializer<Point>() {
    override fun serialize(value: Point?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value == null)
            gen!!.writeNull()
        else
            gen!!.writeArray(doubleArrayOf(value.x, value.y), 0, 2)
    }
}