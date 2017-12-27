package org.teamtators.vision.util

import org.slf4j.event.Level

fun runShell(name: String, script: String) {
    startShell(name, script).invoke()
}

/**
 * Runs a script
 * @param name The name of the logger the output of the script will outputed to
 */
fun startShell(name: String, script: String): () -> Unit {
    val runtime = Runtime.getRuntime()
    val process = runtime.exec(arrayOf("sh", "-c", script));

    val outputLogger = InputStreamLogger(name, process.inputStream)
    val errorLogger = InputStreamLogger(name, process.errorStream, Level.WARN)
    outputLogger.start()
    errorLogger.start()

    return {
        process.waitFor()
        outputLogger.join()
        errorLogger.join()
    }
}