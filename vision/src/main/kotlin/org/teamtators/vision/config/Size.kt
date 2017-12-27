package org.teamtators.vision.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.opencv.core.Size

import java.io.IOException

class SizeDeserializer : JsonDeserializer<Size>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Size {
        val array = p.readValueAs(DoubleArray::class.java)
        return Size(array)
    }
}

class SizeSerializer : JsonSerializer<Size>() {
    override fun serialize(value: Size?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value == null)
            gen!!.writeNull()
        else
            gen!!.writeArray(doubleArrayOf(value.width, value.height), 0, 2)
    }
}