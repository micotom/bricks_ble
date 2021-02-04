package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage
import com.funglejunk.bricksble.byteToHex

data class GenericErrorMessage(
    val commandType: String,
    val errorCode: ErrorCode
) : LegoMessage.Content.Upstream {

    companion object Decoder : LegoMessage.Content.Upstream.Decoder<GenericErrorMessage> {
        override fun decode(payload: ByteArray): GenericErrorMessage = GenericErrorMessage(
            "0x${byteToHex(payload[0])}", ErrorCode.values().find {
                it.id == payload[1]
            } ?: ErrorCode.UNKNOWN
        )
    }

    enum class ErrorCode(val id: Byte) {
        ACK(0x1),
        MACK(0x2),
        BUFFER_OVERFLOW(0x3),
        TIMEOUT(0x4),
        COMMAND_NOT_RECOGNIZED(0x5),
        INVALID_USE(0x6),
        OVERCURRENT(0x7),
        INTERNAL_ERROR(0x8),
        UNKNOWN(0xff.toByte())
    }

}