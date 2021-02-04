package com.funglejunk.bricksble

data class Header(
    val packetLength: Int = 0,
    val hubId: Int = 0,
    val messageType: HubMessageType,
    val headerLength: Int = 0
) {

    object Builder {
        fun encode(header: Header, contentLength: Int): ByteArray =
            (contentLength + 3 > 0x7f).let { isFourByteHeader ->
                val lengthBytes = when (isFourByteHeader) {
                    true -> (4 + contentLength).toInt16BE()
                    false -> byteArrayOf((3 + contentLength).toByte())
                }
                val hubId = 0x0.toByte()
                val messageTypeByte = header.messageType.id
                when (isFourByteHeader) {
                    true -> byteArrayOf(lengthBytes[0], lengthBytes[1], hubId, messageTypeByte)
                    false -> byteArrayOf(lengthBytes[0], hubId, messageTypeByte)
                }
            }
    }

    object Decoder {
        fun decode(bytes: ByteArray): Header {
            var offset = 0
            val lengthBytesCount = when (bytes[offset].msbSet()) {
                true -> 2
                else -> 1
            }
            val length = when (lengthBytesCount) {
                1 -> bytes[offset].toUint8()
                else -> byteArrayOf(bytes[0].msbMasked(), bytes[1]).toUint16(bigEndian = true)
            }
            offset += lengthBytesCount
            val hubId = 0x0
            offset += 1
            val messageType =
                HubMessageType.fromByte(
                    bytes[offset]
                )
            return Header(
                length,
                hubId,
                messageType,
                lengthBytesCount + 2
            )
        }
    }

    enum class HubMessageType(val id: Byte) {
        HUB_PROPERTIES(0x01),
        HUB_ACTIONS(0x02),
        HUB_ALERTS(0x03),
        HUB_ATTACHED_IO(0x04),
        GENERIC_ERROR_MSG(0x05),
        HW_NETWORK_COMMANDS(0x08),
        FW_UPDATE_INTO_BOOT_MODE(0x10),
        FW_UPDATE_LOCK_MEMORY(0x11),
        FW_UPDATE_LOCK_STATUS_REQ(0x12),
        FW_LOCK_STATUS(0x13),
        // port related
        PORT_INFO_REQ(0x21),
        PORT_MODE_INFO_REQ(0x22),
        PORT_INPUT_FORMAT_SETUP_SINGLE(0x41),
        PORT_INPUT_FORMAT_SETUP_COMBINED(0x42),
        PORT_INFO(0x43),
        PORT_MODE_INFO(0x44),
        PORT_VALUE_SINGLE(0x45),
        PORT_VALUE_COMBINED(0x46),
        PORT_INPUT_FORMAT_SINGLE(0x47),
        PORT_INPUT_FORMAT_COMBINED(0x48),
        VIRTUAL_PORT_SETUP(0x61),
        PORT_OUTPUT_COMMAND(0x81.toByte()),
        PORT_OUTPUT_COMMAND_FEEDBACK(0x82.toByte()),
        INVALID(0xff.toByte());

        companion object {
            fun fromByte(byte: Byte): HubMessageType = values().find {
                it.id == byte
            } ?: {
                INVALID
            }()
        }
    }

}