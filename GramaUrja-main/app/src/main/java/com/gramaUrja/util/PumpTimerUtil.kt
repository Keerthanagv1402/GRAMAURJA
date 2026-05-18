package com.gramaUrja.util

import com.gramaUrja.model.CropType

object PumpTimerUtil {

    // Average pump flow rate in liters per minute (submersible 1 HP pump)
    private const val PUMP_FLOW_RATE_LPM = 120

    /**
     * Calculates recommended pump runtime in minutes.
     * @param crop       CropType enum with irrigation norm (liters/acre)
     * @param areaAcres  Field area in acres
     * @return           Runtime in minutes
     */
    fun calculateRuntimeMinutes(crop: CropType, areaAcres: Double): Int {
        val totalLiters = crop.litersPerAcre * areaAcres
        return (totalLiters / PUMP_FLOW_RATE_LPM).toInt().coerceAtLeast(1)
    }

    /**
     * Formats minutes into "Xh Ym" display string.
     */
    fun formatDuration(minutes: Int): String {
        return if (minutes < 60) "${minutes} min"
        else "${minutes / 60}h ${minutes % 60}m"
    }

    /**
     * Formats remaining seconds into MM:SS countdown string.
     */
    fun formatCountdown(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }
}
