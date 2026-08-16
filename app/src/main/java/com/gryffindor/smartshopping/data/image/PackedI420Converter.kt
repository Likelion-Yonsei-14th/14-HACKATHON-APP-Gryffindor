package com.gryffindor.smartshopping.data.image

/**
 * Converts the packed I420 buffer emitted by the uncompressed Meta DAT VideoFrame
 * to the NV21 layout required by Android's YuvImage.
 *
 * Source layout: Y plane, U plane, V plane.
 * Destination layout: Y plane, interleaved VU chroma plane.
 *
 * DAT exposes one ByteBuffer, not ImageProxy planes, so rowStride/pixelStride are
 * not available here. The conversion therefore requires tightly packed planes.
 */
internal object PackedI420Converter {

    fun expectedSize(width: Int, height: Int): Int? {
        if (width <= 0 || height <= 0 || width % 2 != 0 || height % 2 != 0) {
            return null
        }
        return width * height * 3 / 2
    }

    fun toNv21(data: ByteArray, width: Int, height: Int): ByteArray? {
        val expectedSize = expectedSize(width, height) ?: return null
        val output = ByteArray(expectedSize)
        return if (copyToNv21(data, width, height, output)) output else null
    }

    fun copyToNv21(
        data: ByteArray,
        width: Int,
        height: Int,
        destination: ByteArray
    ): Boolean {
        val expectedSize = expectedSize(width, height) ?: return false
        if (data.size != expectedSize || destination.size < expectedSize) return false

        val yPlaneSize = width * height
        val chromaPlaneSize = yPlaneSize / 4
        data.copyInto(destination, destinationOffset = 0, startIndex = 0, endIndex = yPlaneSize)

        val uPlaneOffset = yPlaneSize
        val vPlaneOffset = yPlaneSize + chromaPlaneSize
        var destinationOffset = yPlaneSize
        for (index in 0 until chromaPlaneSize) {
            destination[destinationOffset++] = data[vPlaneOffset + index]
            destination[destinationOffset++] = data[uPlaneOffset + index]
        }
        return true
    }
}
