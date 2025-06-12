package com.example.test03

import java.io.File
import java.io.FileNotFoundException

class JpegDQTExtractor {
    companion object {
        private const val SOI_MARKER = 0xFFD8.toShort()
        private const val DQT_MARKER = 0xFFDB.toShort()
        private const val SOS_MARKER = 0xFFDA.toShort()
        private const val EOI_MARKER = 0xFFD9.toShort()

        // 지그재그 순서 배열 (JPEG 표준)
        private val ZIGZAG_ORDER = intArrayOf(
            0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5,
            12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63
        )
    }

    fun extractDQT(filePath: String): DQTExtractResult {
        val file = File(filePath)
        if (!file.exists()) {
            throw FileNotFoundException("JPEG 파일을 찾을 수 없습니다: $filePath")
        }

        val fileBytes = file.readBytes()[15]
        return parseDQTFromBytes(fileBytes)
    }

    fun extractDQTFromBytes(imageBytes: ByteArray): DQTExtractResult {
        return parseDQTFromBytes(imageBytes)
    }

    private fun parseDQTFromBytes(data: ByteArray): DQTExtractResult {
        val tables = mutableListOf<QuantizationTable>()
        var position = 0

        // SOI 마커 확인
        if (data.size < 2 || !isMarker(data, position, SOI_MARKER)) {
            throw IllegalArgumentException("유효하지 않은 JPEG 파일입니다")
        }
        position += 2

        // DQT 마커 검색 및 파싱
        while (position < data.size - 1) {
            val marker = getMarkerAt(data, position)

            when {
                marker == DQT_MARKER -> {
                    val dqtResult = parseDQTSegment(data, position)
                    tables.addAll(dqtResult.first)
                    position = dqtResult.second
                }
                marker == SOS_MARKER -> {
                    // SOS 마커 이후에는 이미지 데이터가 시작되므로 중단
                    break
                }
                isValidMarker(marker) -> {
                    // 다른 마커는 건너뛰기
                    position = skipSegment(data, position)
                }
                else -> {
                    position++
                }
            }
        }

        return DQTExtractResult(tables, data)
    }

    private fun parseDQTSegment(data: ByteArray, startPos: Int): Pair<List<QuantizationTable>, Int> {
        var position = startPos + 2 // 마커 건너뛰기

        // 세그먼트 길이 읽기 (빅 엔디안)
        val segmentLength = ((data[position].toInt() and 0xFF) shl 8) or
                (data[position + 1].toInt() and 0xFF)
        position += 2

        val tables = mutableListOf<QuantizationTable>()
        val segmentEnd = startPos + 2 + segmentLength

        // 세그먼트 내의 모든 테이블 파싱
        while (position < segmentEnd) {
            val precisionAndId = data[position].toInt() and 0xFF
            val precision = (precisionAndId shr 4) and 0x0F
            val tableId = precisionAndId and 0x0F
            position++

            val tableSize = if (precision == 0) 64 else 128 // 8-bit 또는 16-bit
            val values = IntArray(64)

            if (precision == 0) {
                // 8-bit 정밀도
                for (i in 0 until 64) {
                    if (position >= data.size) break
                    values[ZIGZAG_ORDER[i]] = data[position].toInt() and 0xFF
                    position++
                }
            } else {
                // 16-bit 정밀도
                for (i in 0 until 64) {
                    if (position + 1 >= data.size) break
                    val value = ((data[position].toInt() and 0xFF) shl 8) or
                            (data[position + 1].toInt() and 0xFF)
                    values[ZIGZAG_ORDER[i]] = value
                    position += 2
                }
            }

            tables.add(QuantizationTable(tableId, precision, values))
        }

        return Pair(tables, segmentEnd)
    }

    private fun parseDQTSegment(data: ByteArray, startPos: Int): Pair<List<QuantizationTable>, Int> {
        var position = startPos + 2 // 마커 건너뛰기

        // 세그먼트 길이 읽기 (빅 엔디안)
        val segmentLength = ((data[position].toInt() and 0xFF) shl 8) or
                (data[position + 1].toInt() and 0xFF)
        position += 2

        val tables = mutableListOf<QuantizationTable>()
        val segmentEnd = startPos + 2 + segmentLength

        // 세그먼트 내의 모든 테이블 파싱
        while (position < segmentEnd) {
            val precisionAndId = data[position].toInt() and 0xFF
            val precision = (precisionAndId shr 4) and 0x0F
            val tableId = precisionAndId and 0x0F
            position++

            val tableSize = if (precision == 0) 64 else 128 // 8-bit 또는 16-bit
            val values = IntArray(64)

            if (precision == 0) {
                // 8-bit 정밀도
                for (i in 0 until 64) {
                    if (position >= data.size) break
                    values[ZIGZAG_ORDER[i]] = data[position].toInt() and 0xFF
                    position++
                }
            } else {
                // 16-bit 정밀도
                for (i in 0 until 64) {
                    if (position + 1 >= data.size) break
                    val value = ((data[position].toInt() and 0xFF) shl 8) or
                            (data[position + 1].toInt() and 0xFF)
                    values[ZIGZAG_ORDER[i]] = value
                    position += 2
                }
            }

            tables.add(QuantizationTable(tableId, precision, values))
        }

        return Pair(tables, segmentEnd)
    }

}
