package org.teamtators.agitator.dashboard

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

fun <T : Any> loggerFor(clazz: Class<T>): Logger = LoggerFactory.getLogger(unwrapCompanionClass(clazz))

fun <T : Any> loggerFor(klass: KClass<T>): Logger = loggerFor(klass.java)

inline fun <reified T : Any> loggerFor(): Logger = loggerFor(T::class)

fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null
            && ofClass.enclosingClass.kotlin.objectInstance?.javaClass == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

fun loggerFactory(): ReadOnlyProperty<Any, Logger> = LoggerFactoryDelegate()

private class LoggerFactoryDelegate : ReadOnlyProperty<Any, Logger> {
    var logger: Logger? = null

    override operator fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        val l = logger
        return if (l !== null) {
            l
        } else {
            val initialized = loggerFor(thisRef.javaClass)
            logger = initialized
            initialized
        }
    }
}

//val Any.logger: Logger by loggerFactory()
