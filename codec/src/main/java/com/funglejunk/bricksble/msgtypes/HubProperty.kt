package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage
import com.funglejunk.bricksble.toUint16
import com.funglejunk.bricksble.toUint8
import kotlin.experimental.and
import kotlin.experimental.or

data class HubProperty<T>(
    val property: Property,
    val operation: Operation,
    val payload: Payload<T>?
) : LegoMessage.Content.Upstream,
    LegoMessage.Content.Downstream {

    override val length: Int = 2 + (payload?.encode()?.size ?: 0)

    override fun encode(): ByteArray = byteArrayOf(
        property.id, operation.id
    ) + (payload?.encode() ?: byteArrayOf())

    object Decoder : LegoMessage.Content.Upstream.Decoder<HubProperty<*>> {
        override fun decode(payload: ByteArray): HubProperty<*> {
            var offset = 0
            val property =
                Property.fromByte(
                    payload[offset]
                )
            offset += 1
            val operation =
                Operation.fromByte(
                    payload[offset]
                )
            offset += 1
            val payloadBytes = payload.copyOfRange(
                offset, payload.size
            )
            val payload: Payload<*> = when (property) {
                Property.ADVERTISING_NAME -> StringPayload.parse(
                    payloadBytes
                )
                Property.BUTTON -> BooleanPayload.parse(
                    payloadBytes
                )
                Property.FW_VERSION -> StringPayload.parse(
                    payloadBytes
                )
                Property.HW_VERSION -> StringPayload.parse(
                    payloadBytes
                )
                Property.RSSI -> Int8Payload.parse(
                    payloadBytes
                )
                Property.BATTERY_VOLTAGE -> Uint8Payload.parse(
                    payloadBytes
                )
                Property.BATTERY_TYPE -> BatteryTypePayload.parse(
                    payloadBytes
                )
                Property.MANUFACTURER_NAME -> StringPayload.parse(
                    payloadBytes
                )
                Property.RADIO_FW_VERSION -> StringPayload.parse(
                    payloadBytes
                )
                Property.PROTOCOL_VERSION -> StringPayload.parse(
                    payloadBytes
                )
                Property.SYSTEM_TYPE_ID -> StringPayload.parse(
                    payloadBytes
                )
                Property.HW_NETWORK_ID -> StringPayload.parse(
                    payloadBytes
                )
                Property.PRIMARY_MAC -> StringPayload.parse(
                    payloadBytes
                )
                Property.SECONDARY_MAC -> StringPayload.parse(
                    payloadBytes
                )
                Property.HW_NET_FAMILY -> StringPayload.parse(
                    payloadBytes
                )
                Property.INVALID -> StringPayload(
                    "INVALID"
                )
            }
            return HubProperty(
                property, operation, payload
            )
        }
    }

    interface Payload<T> {
        val content: T
        fun encode(): ByteArray
    }

    data class StringPayload(override val content: String) :
        Payload<String> {
        companion object {
            fun parse(bytes: ByteArray): Payload<String> =
                StringPayload(
                    String(bytes)
                )
        }

        override fun encode(): ByteArray = content.toByteArray()
    }

    data class BooleanPayload(override val content: Boolean) :
        Payload<Boolean> {
        companion object {
            fun parse(bytes: ByteArray): Payload<Boolean> =
                BooleanPayload(
                    when (bytes[0]) {
                        0x0.toByte() -> false
                        else -> true
                    }
                )
        }

        override fun encode(): ByteArray = byteArrayOf()
    }

    data class Int8Payload(override val content: Byte) : Payload<Byte> {
        companion object {
            fun parse(bytes: ByteArray): Payload<Byte> =
                Int8Payload(bytes[0])
        }

        override fun encode(): ByteArray = byteArrayOf()
    }

    data class Uint8Payload(override val content: Int) : Payload<Int> {
        companion object {
            fun parse(bytes: ByteArray): Payload<Int> =
                Uint8Payload(bytes[0].toUint8())
        }

        override fun encode(): ByteArray = byteArrayOf()
    }

    enum class BatteryType {
        NORMAL, RECHARGEABLE;
    }

    data class BatteryTypePayload(override val content: BatteryType) : Payload<BatteryType> {
        companion object {
            fun parse(bytes: ByteArray): Payload<BatteryType> =
                BatteryTypePayload(
                    when (bytes[0]) {
                        0x0.toByte() -> BatteryType.NORMAL
                        else -> BatteryType.RECHARGEABLE
                    }
                )
        }

        override fun encode(): ByteArray = byteArrayOf()
    }

    data class VersionNumber(val major: Int, val minor: Int, val bugFixing: Int, val build: Int)

    data class VersionNumberPayload(override val content: VersionNumber) : Payload<VersionNumber> {
        companion object {
            fun decode(bytes: ByteArray): Payload<VersionNumber> =
                VersionNumberPayload(
                    VersionNumber(
                        bytes[3].toUint8() shr 4,
                        (bytes[3] and 0xf.toByte()).toUint8(),
                        bytes[2].toUint8(),
                        byteArrayOf(bytes[1], bytes[0]).toUint16()
                    )
                )
        }

        override fun encode(): ByteArray =
            byteArrayOf(
                (content.build and 0xff).toByte(),
                (content.build shr 8 and 0xff).toByte()
            ) + byteArrayOf(
                (content.bugFixing and 0xff).toByte(),
                (content.minor and 0xf).toByte() or (content.major shl 4).toByte()
            )
    }

    enum class Property(val id: Byte, val supportedOperations: Set<Operation>) {
        ADVERTISING_NAME(
            0x01,
            setOf(
                Operation.SET,
                Operation.ENABLE_UPDATE,
                Operation.DISABLE_UPDATE,
                Operation.RESET,
                Operation.REQ_UPDATE,
                Operation.UPDATE
            )
        ),
        BUTTON(
            0x02,
            setOf(
                Operation.ENABLE_UPDATE,
                Operation.DISABLE_UPDATE,
                Operation.REQ_UPDATE,
                Operation.UPDATE
            )
        ),
        FW_VERSION(0x03, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        HW_VERSION(0x04, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        RSSI(
            0x05,
            setOf(
                Operation.ENABLE_UPDATE,
                Operation.DISABLE_UPDATE,
                Operation.REQ_UPDATE,
                Operation.UPDATE
            )
        ),
        BATTERY_VOLTAGE(
            0x06,
            setOf(
                Operation.ENABLE_UPDATE,
                Operation.DISABLE_UPDATE,
                Operation.REQ_UPDATE,
                Operation.UPDATE
            )
        ),
        BATTERY_TYPE(0x07, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        MANUFACTURER_NAME(0x08, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        RADIO_FW_VERSION(0x09, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        PROTOCOL_VERSION(0x0A, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        SYSTEM_TYPE_ID(0x0B, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        HW_NETWORK_ID(
            0x0C,
            setOf(Operation.SET, Operation.RESET, Operation.REQ_UPDATE, Operation.UPDATE)
        ),
        PRIMARY_MAC(0x0D, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        SECONDARY_MAC(0x0E, setOf(Operation.REQ_UPDATE, Operation.UPDATE)),
        HW_NET_FAMILY(0x0F, setOf(Operation.SET, Operation.REQ_UPDATE, Operation.UPDATE)),
        INVALID(0xFF.toByte(), emptySet())
        ;

        companion object {
            fun fromByte(byte: Byte): Property = values()
                .find {
                    byte == it.id
                } ?: INVALID
        }
    }

    enum class Operation(val id: Byte) {
        SET(0x01),
        ENABLE_UPDATE(0x02),
        DISABLE_UPDATE(0x03),
        RESET(0x04),
        REQ_UPDATE(0x05),
        UPDATE(0x06),
        INVALID(0xFF.toByte());

        companion object {
            fun fromByte(byte: Byte): Operation = values().find {
                byte == it.id
            } ?: INVALID
        }
    }

}