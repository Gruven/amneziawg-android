/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.amnezia.awg.util

import android.os.Process
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogCapture {
    private const val MAX_FILE_SIZE = 2L * 1024 * 1024 // 2 MB
    private const val LOG_FILE_NAME = "awg.log"
    private const val OLD_LOG_FILE_NAME = "awg.log.1"

    private var logDir: File? = null
    private var writer: BufferedWriter? = null
    private val df = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    private val _newLines = MutableSharedFlow<String>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val newLines = _newLines.asSharedFlow()

    @Synchronized
    fun init(dir: File) {
        logDir = dir
        openWriter()
    }

    @Synchronized
    private fun openWriter() {
        val dir = logDir ?: return
        writer = BufferedWriter(FileWriter(File(dir, LOG_FILE_NAME), true))
    }

    @Synchronized
    fun log(level: String, tag: String, msg: String) {
        val w = writer ?: return
        val pid = Process.myPid()
        val tid = Process.myTid()
        val time = df.format(Date())
        val line = "$time $pid $tid $level $tag: $msg"
        try {
            w.write(line)
            w.newLine()
            w.flush()
            rotateIfNeeded()
        } catch (_: Exception) {
        }
        _newLines.tryEmit(line)
    }

    @Synchronized
    private fun rotateIfNeeded() {
        val dir = logDir ?: return
        val file = File(dir, LOG_FILE_NAME)
        if (file.length() > MAX_FILE_SIZE) {
            writer?.close()
            val oldFile = File(dir, OLD_LOG_FILE_NAME)
            oldFile.delete()
            file.renameTo(oldFile)
            openWriter()
        }
    }

    @Synchronized
    fun readAll(): List<String> {
        val dir = logDir ?: return emptyList()
        try {
            writer?.flush()
        } catch (_: Exception) {
        }
        val result = mutableListOf<String>()
        val oldFile = File(dir, OLD_LOG_FILE_NAME)
        val curFile = File(dir, LOG_FILE_NAME)
        for (f in listOf(oldFile, curFile)) {
            if (f.exists()) {
                try {
                    f.bufferedReader().useLines { lines ->
                        lines.forEach { result.add(it) }
                    }
                } catch (_: Exception) {
                }
            }
        }
        return result
    }

    fun v(tag: String, msg: String) = log("V", tag, msg)
    fun d(tag: String, msg: String) = log("D", tag, msg)
    fun i(tag: String, msg: String) = log("I", tag, msg)
    fun w(tag: String, msg: String) = log("W", tag, msg)
    fun e(tag: String, msg: String) = log("E", tag, msg)
}
