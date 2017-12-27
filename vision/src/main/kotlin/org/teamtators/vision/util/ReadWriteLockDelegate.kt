package org.teamtators.vision.util

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReadWriteLockDelegate<T : Any>(val readWriteLock: ReentrantReadWriteLock, initial: T) : ReadWriteProperty<Any?, T> {
    var value: T = initial

    operator override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
            readWriteLock.write {
                this.value = value
            }


    operator override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            readWriteLock.read {
                this.value
            }

}