package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage
import kotlin.experimental.and

data class PortOutputCommandFeedback(
    val portId: Byte,
    val message: Array<FeedbackMessage>,
    val secodPortId: Byte?,
    val secondMessage: Array<FeedbackMessage>?,
    val thirdPortId: Byte?,
    val thirdMessage: Array<FeedbackMessage>?
) : LegoMessage.Content.Upstream {

    companion object Decoder : LegoMessage.Content.Upstream.Decoder<PortOutputCommandFeedback> {
        override fun decode(payload: ByteArray): PortOutputCommandFeedback =
            PortOutputCommandFeedback(
                payload[0],
                FeedbackMessage.fromByte(payload[1]),
                if (payload.size > 2) {
                    payload[2]
                } else null,
                if (payload.size > 2) {
                    FeedbackMessage.fromByte(payload[3])
                } else null,
                if (payload.size > 4) {
                    payload[4]
                } else null,
                if (payload.size > 4) {
                    FeedbackMessage.fromByte(payload[5])
                } else null
            )

    }

    enum class FeedbackMessage(val mask: Byte) {
        BUFFER_EMPTY_CMD_IN_PROGRESS(0b0000_0001),
        BUFFER_EMPTY_CMD_COMPLETED(0b0000_0010),
        CURRENT_COMMAND_DISCARDED(0b0000_0100),
        IDLE(0b0000_1000),
        BUSY_FULL(0b0001_0000)
        ;

        companion object {
            fun fromByte(byte: Byte): Array<FeedbackMessage> = values().filter {
                byte and it.mask == it.mask
            }.toTypedArray()
        }
    }

}