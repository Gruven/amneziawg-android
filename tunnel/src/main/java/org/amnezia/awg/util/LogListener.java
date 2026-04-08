/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.amnezia.awg.util;

import android.util.Log;

public final class LogListener {
    public interface Callback {
        void onLog(String level, String tag, String msg);
    }

    private static volatile Callback callback;

    public static void setCallback(final Callback cb) {
        callback = cb;
    }

    private static void log(final String level, final String tag, final String msg) {
        final Callback cb = callback;
        if (cb != null) cb.onLog(level, tag, msg);
    }

    public static void v(final String tag, final String msg) {
        Log.v(tag, msg);
        log("V", tag, msg);
    }

    public static void d(final String tag, final String msg) {
        Log.d(tag, msg);
        log("D", tag, msg);
    }

    public static void d(final String tag, final String msg, final Throwable tr) {
        Log.d(tag, msg, tr);
        log("D", tag, msg + ": " + tr.getMessage());
    }

    public static void i(final String tag, final String msg) {
        Log.i(tag, msg);
        log("I", tag, msg);
    }

    public static void w(final String tag, final String msg) {
        Log.w(tag, msg);
        log("W", tag, msg);
    }

    public static void e(final String tag, final String msg) {
        Log.e(tag, msg);
        log("E", tag, msg);
    }

    public static void w(final String tag, final String msg, final Throwable tr) {
        Log.w(tag, msg, tr);
        log("W", tag, msg + ": " + tr.getMessage());
    }

    public static void e(final String tag, final String msg, final Throwable tr) {
        Log.e(tag, msg, tr);
        log("E", tag, msg + ": " + tr.getMessage());
    }
}
