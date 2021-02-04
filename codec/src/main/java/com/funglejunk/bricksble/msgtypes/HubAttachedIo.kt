package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage
import com.funglejunk.bricksble.toUint16
import com.funglejunk.bricksble.toUint8

data class HubAttachedIo(
    val portId: Int,
    val eventType: EventType,
    val ioType: IoType?,
    val subPortIds: Pair<Int, Int>?
) : LegoMessage.Content.Upstream {

    enum class EventType(val id: Byte) {
        DETACHED_IO(0x00),
        ATTACHED_IO(0x01),
        ATTACHED_VIRTUAL_IO(0x02),
        INVALID(0xff.toByte());

        companion object {
            fun fromByte(byte: Byte): EventType = values().find {
                it.id == byte
            } ?: INVALID
        }
    }

    enum class IoType(val id: Int) {
        MOTOR(0x0001),
        SYSTEM_TRAIN_MONITOR(0x0002),
        BUTTON(0x0005),
        LED_LIGHT(0x0008),
        VOLTAGE(0x0014),
        CURRENT(0x0015),
        PIEZO_TONE(0x0016),
        RGB_LIGHT(0x0017),
        EXTERNAL_TILT_SENSOR(0x0022),
        MOTION_SENSOR(0x0023),
        VISION_SENSOR(0x0025),
        EXT_MOTOR_W_TACHO(0x0026),
        INT_MOTOR_W_TACHO(0x0027),
        INT_TILT(0x0028),
        INVALID(0xffff);

        companion object {
            fun fromBytes(bytes: ByteArray): IoType = bytes.toUint16(bigEndian = true).run {
                values().find {
                    it.id == this
                } ?: {
                    INVALID
                }()
            }
        }
    }

    object Decoder : LegoMessage.Content.Upstream.Decoder<HubAttachedIo> {
        override fun decode(payload: ByteArray): HubAttachedIo {
            var offset = 0
            val portId = payload[offset++].toUint8()
            val eventType =
                EventType.fromByte(
                    payload[offset++]
                )
            val ioTypeId =
                if (eventType == EventType.ATTACHED_IO || eventType == EventType.ATTACHED_VIRTUAL_IO) {
                    IoType.fromBytes(
                        payload.copyOfRange(offset, offset + 2)
                    ).also {
                        offset += 2
                    }
                } else {
                    null
                }
            // ignore refs
            if (eventType == EventType.ATTACHED_IO) {
                offset += 8
            }
            val subPortIds = if (eventType == EventType.ATTACHED_VIRTUAL_IO) {
                payload[offset++].toUint8() to payload[offset].toUint8()
            } else {
                null
            }
            return HubAttachedIo(
                portId,
                eventType,
                ioTypeId,
                subPortIds
            )
        }
    }
}