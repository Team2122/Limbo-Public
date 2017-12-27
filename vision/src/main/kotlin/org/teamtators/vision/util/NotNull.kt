package org.teamtators.vision.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class NotNull<T: Any>(val message: String) : ReadWriteProperty<Any?, T> {
    constructor() :
            this("Property %s should be initialized before get.") {
    }

    private var value: T? = null

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException(message.format(property))
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}