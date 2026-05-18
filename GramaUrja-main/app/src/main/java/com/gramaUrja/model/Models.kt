package com.gramaUrja.model

data class Zone(
    val id: String = "",
    val name: String = "",
    val district: String = "",
    val taluk: String = "",
    val village: String = ""
)

data class PowerStatus(
    val zoneId: String = "",
    val status: String = "UNKNOWN",   // "ON" | "OFF" | "UNKNOWN"
    val updatedAt: Long = 0L,
    val reporterId: String = "",
    val confirmCount: Int = 0
) {
    val isOn: Boolean get() = status == "ON"
    val isOff: Boolean get() = status == "OFF"
}

data class PowerHistoryEntry(
    val id: String = "",
    val zoneId: String = "",
    val status: String = "",
    val timestamp: Long = 0L,
    val reporterId: String = ""
)

data class FreshnessInfo(
    val label: String,
    val colorHex: String   // "#1565C0" | "#E65100" | "#C62828"
)

enum class CropType(val displayName: String, val litersPerAcre: Int) {
    RICE("Rice / Paddy", 1200),
    WHEAT("Wheat", 450),
    SUGARCANE("Sugarcane", 1800),
    COTTON("Cotton", 600),
    VEGETABLES("Vegetables", 400),
    MAIZE("Maize / Corn", 500),
    GROUNDNUT("Groundnut", 350);
}
