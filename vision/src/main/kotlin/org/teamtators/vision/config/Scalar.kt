package org.teamtators.vision.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.opencv.core.Scalar
import java.io.IOException

class ScalarDeserializer : JsonDeserializer<Scalar>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Scalar? {
        val array = p.readValueAs(DoubleArray::class.java)
        return Scalar(array)
    }
}

class ScalarSerializer : JsonSerializer<Scalar>() {
    override fun serialize(value: Scalar?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value == null)
            gen!!.writeNull()
        else
            gen!!.writeArray(value.`val`, 0, value.`val`.size)
    }
}