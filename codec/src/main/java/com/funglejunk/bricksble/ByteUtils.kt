package com.funglejunk.bricksble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

private const val MSB: Byte = 0x80.toByte()

private const val NON_MSB_MASK: Byte = 0x7f.toByte()

fun Int.hasFlag(flag: Int) = this and flag == flag

fun Byte.toUint8() = toInt() and 0xff

fun Byte.msbSet(): Boolean = this and MSB == MSB

fun Byte.msbMasked(): Byte = this and NON_MSB_MASK

fun ByteArray.toUint16(bigEndian: Boolean = false): Int {
    return if (bigEndian) {
        (this[0].toInt() and 0xff) or ((this[1].toInt() and 0xff) shl 8)
    } else {
        (this[1].toInt() and 0xff) or ((this[0].toInt() and 0xff) shl 8)
    }
}

fun byteToHex(num: Byte): String {
    val hexDigits = CharArray(2)
    hexDigits[0] = Character.forDigit(num.toInt() shr 4 and 0xF, 16)
    hexDigits[1] = Character.forDigit((num and 0xF).toInt(), 16)
    return String(hexDigits)
}

fun ByteArray.toHexString(): String {
    val hexStringBuffer = StringBuffer()
    for (i in indices) {
        hexStringBuffer.append(byteToHex(this[i]))
    }
    return hexStringBuffer.toString()
}

fun Int.toInt16BE(): ByteArray =
    ByteBuffer.allocate(2).apply {
        order(ByteOrder.BIG_ENDIAN)
        putShort(this@toInt16BE.toShort())
    }.run {
        array()
    }

fun Int.toInt16LE(): ByteArray =
    ByteBuffer.allocate(2).apply {
        order(ByteOrder.LITTLE_ENDIAN)
        putShort(this@toInt16LE.toShort())
    }.run {
        array()
    }

fun Int.toInt32LE(): ByteArray =
    ByteBuffer.allocate(4).apply {
        order(ByteOrder.LITTLE_ENDIAN)
        putInt(this@toInt32LE)
    }.run {
        array()
    }