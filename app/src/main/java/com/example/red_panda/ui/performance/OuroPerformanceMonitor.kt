package com.example.red_panda.ui.performance

import android.util.Log
import androidx.metrics.performance.JankStats

class OuroPerformanceMonitor(window: android.view.Window) {
    val jankStats: JankStats = JankStats.createAndTrack(
        window,
        JankStats.OnFrameListener { frameData ->
            if (frameData.isJank) {
                Log.i(
                    TAG,
                    "jank durationMs=${frameData.frameDurationUiNanos / NANOS_PER_MILLISECOND} " +
                        "states=${frameData.states}"
                )
            }
        }
    )

    private companion object {
        const val TAG = "OuroPerformance"
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
