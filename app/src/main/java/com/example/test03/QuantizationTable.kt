package com.example.test03

data class QuantizationTable(
    val tableId: Int,
    val precision: Int, // 0 = 8-bit, 1 = 16-bit
    val values: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as QuantizationTable
        if (tableId != other.tableId) return false
        if (precision != other.precision) return false
        if (!values.contentEquals(other.values)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tableId
        result = 31 * result + precision
        result = 31 * result + values.contentHashCode()
        return result
    }
}

data class DQTExtractResult(
    val tables: List<QuantizationTable>,
    val rawData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DQTExtractResult

        if (tables != other.tables) return false
        if (!rawData.contentEquals(other.rawData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tables.hashCode()
        result = 31 * result + rawData.contentHashCode()
        return result
    }
}