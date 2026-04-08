/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.amnezia.awg.util

import android.os.Process
import androidx.collection.CircularArray
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Date

object LogCapture {
    private const val MAX_LINES = (1 shl 14) - 1

    private val lines = CircularArray<LogLine>(MAX_LINES + 1)
    private val _newLines = MutableSharedFlow<LogLine>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val newLines = _newLines.asSharedFlow()

    data class LogLine(
        val pid: Int,
        val tid: Int,
        val time: Date,
        val level: String,
        val tag: String,
        val msg: String
    )

    @Synchronized
    fun log(level: String, tag: String, msg: String) {
        val line = LogLine(
            pid = Process.myPid(),
            tid = Process.myTid(),
            time = Date(),
            level = level,
            tag = tag,
            msg = msg
        )
        if (lines.size() >= MAX_LINES) lines.popFirst()
        lines.addLast(line)
        _newLines.tryEmit(line)
    }

    @Synchronized
    fun snapshot(): List<LogLine> {
        val result = ArrayList<LogLine>(lines.size())
        for (i in 0 until lines.size()) result.add(lines[i])
        return result
    }

    fun v(tag: String, msg: String) = log("V", tag, msg)
    fun d(tag: String, msg: String) = log("D", tag, msg)
    fun i(tag: String, msg: String) = log("I", tag, msg)
    fun w(tag: String, msg: String) = log("W", tag, msg)
    fun e(tag: String, msg: String) = log("E", tag, msg)
}
