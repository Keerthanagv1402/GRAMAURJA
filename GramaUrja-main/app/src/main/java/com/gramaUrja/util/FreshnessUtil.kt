package com.gramaUrja.util

import com.gramaUrja.model.FreshnessInfo

object FreshnessUtil {

    /**
     * Returns a human-readable elapsed time label for the last update timestamp.
     * e.g. "Just now", "Updated 5 min ago", "Updated 2 hr ago"
     */
    fun getFreshnessLabel(updatedAt: Long): String {
        if (updatedAt == 0L) return "No data yet"
        val diffMs  = System.currentTimeMillis() - updatedAt
        val diffMin = diffMs / 60_000
        return when {
            diffMin < 1    -> "Just now"
            diffMin < 60   -> "Updated ${diffMin} min ago"
            diffMin < 1440 -> "Updated ${diffMin / 60} hr ago"
            else           -> "Updated ${diffMin / 1440} day(s) ago"
        }
    }

    /**
     * Returns hex color string based on data freshness:
     *  < 30 min  → Blue   (fresh)
     *  30–120min → Amber  (stale)
     *  > 120 min → Red    (very stale)
     */
    fun getFreshnessColorHex(updatedAt: Long): String {
        if (updatedAt == 0L) return "#9E9E9E"
        val diffMin = (System.currentTimeMillis() - updatedAt) / 60_000
        return when {
            diffMin < 30  -> "#1565C0"   // Blue  — Fresh
            diffMin < 120 -> "#E65100"   // Amber — Stale
            else          -> "#C62828"   // Red   — Very Stale
        }
    }

    fun getFreshnessInfo(updatedAt: Long): FreshnessInfo =
        FreshnessInfo(
            label    = getFreshnessLabel(updatedAt),
            colorHex = getFreshnessColorHex(updatedAt)
        )

    /** Returns true if data is so stale that a warning banner should be shown (> 2 hrs) */
    fun isDataVeryStale(updatedAt: Long): Boolean {
        if (updatedAt == 0L) return false
        val diffMin = (System.currentTimeMillis() - updatedAt) / 60_000
        return diffMin > 120
    }
}
