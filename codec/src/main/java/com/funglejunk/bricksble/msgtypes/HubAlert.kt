package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage

data class HubAlert(
    val type: AlertType,
    val operation: AlertOperation,
    val payload: AlertPayload?
) : LegoMessage.Content.Upstream,
    LegoMessage.Content.Downstream {

    override val length: Int = if (payload != null) 6 else 5

    companion object Decoder : LegoMessage.Content.Upstream.Decoder<HubAlert> {
        override fun decode(payload: ByteArray) = HubAlert(
            AlertType.fromByte(payload[0]),
            AlertOperation.fromByte(payload[1]),
            null
        )
    }

    override fun encode(): ByteArray = byteArrayOf(
        type.id,
        operation.id
    ) + (payload?.let {
        byteArrayOf(it.id)
    } ?: byteArrayOf())

    enum class AlertType(val id: Byte) {
        LOW_VOLTAGE(0x1),
        HIGH_CURRENT(0x2),
        LOW_SIGNAL_STRENGTH(0x3),
        OVER_POWER_CONDITION(0x4),
        INVALID(0xff.toByte())
        ;

        companion object {
            fun fromByte(byte: Byte): AlertType = values().find {
                it.id == byte
            } ?: INVALID
        }
    }

    enum class AlertOperation(val id: Byte) {
        ENABLE_UPDATES(0x1), // downstream only
        DISABLE_UPDATES(0x2), // downstream only
        REQ_UPDATES(0x3), // downstream only
        UPDATE(0x4), // upstream only
        INVALID(0xff.toByte())
        ;

        companion object {
            fun fromByte(byte: Byte): AlertOperation = values().find {
                it.id == byte
            } ?: INVALID
        }
    }

    // Downstream only
    enum class AlertPayload(val id: Byte) {
        STATUS_OK(0x0),
        ALERT(0xff.toByte())
    }
}