package org.teamtators.agitator.dashboard

import javafx.beans.property.Property
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyDelegate<T>(val fxProp: Property<T>) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return fxProp.value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        fxProp.value = value
    }

}