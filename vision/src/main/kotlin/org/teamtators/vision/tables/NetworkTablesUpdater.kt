package org.teamtators.vision.tables

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.vision.ProcessResult

class NetworkTablesUpdater constructor(
        _config: Config,
        val processResults: Flowable<ProcessResult>
) {
    val logger = LoggerFactory.getLogger(javaClass)
    private val config = _config.tables
    //    private var visionTable: NetworkTable? = null
    private var frameNumber: Int = 0

    private var subscriber: Disposable? = null

    fun start() {
        frameNumber = 0
        //        NetworkTable.setClientMode()
        //        NetworkTable.setIPAddress(config.host)
        logger.debug("Attempting to connect to NT server at \"{}\"", config.host)
        //        NetworkTable.initialize()
        //        if (subscriber?.isDisposed ?: true)
        //            subscriber = processResults.subscribe({ updateNetworkTable(it) })
    }

    fun stop() {
        logger.info("Disconnecting from NT server")
        //        NetworkTable.shutdown()
        subscriber?.dispose()
        subscriber = null
    }

    fun getTurretAngle(): Double {
        //        val visionTable = this.visionTable
        return /* visionTable?.getNumber("turretAngle", Double.NaN) ?:*/ Double.NaN
    }

    /*
    @Subscribe
    fun updateNetworkTable(result: ProcessResult) {
        if (NetworkTable.connections().isNotEmpty()) {
            if (visionTable == null) {
                logger.info("Connected to NT server")
                visionTable = NetworkTable.getTable(config.tableName)
            }
        } else {
            visionTable = null
        }

        if (visionTable != null) {
            val x = result.target?.x ?: Double.NaN
            val y = result.target?.y ?: Double.NaN
            val distance = result.distance ?: Double.NaN
            val offsetAngle = result.offsetAngle ?: Double.NaN
            val newAngle = result.newAngle ?: Double.NaN
            visionTable?.putNumber("x", x);
            visionTable?.putNumber("y", y);
            visionTable?.putNumber("distance", distance);
            visionTable?.putNumber("offsetAngle", offsetAngle);
            visionTable?.putNumber("newAngle", newAngle);
            visionTable?.putNumber("frameNumber", frameNumber.toDouble())
        }
        frameNumber++
    }
    */
}