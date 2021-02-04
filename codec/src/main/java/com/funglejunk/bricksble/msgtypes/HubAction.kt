package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage

data class HubAction(
    val type: ActionType
) : LegoMessage.Content.Downstream,
    LegoMessage.Content.Upstream {

    override val length: Int = 1
    override fun encode(): ByteArray = byteArrayOf(type.id)

    object Decoder : LegoMessage.Content.Upstream.Decoder<HubAction> {
        override fun decode(payload: ByteArray): HubAction =
            HubAction(
                ActionType.fromByte(
                    payload[0]
                )
            )
    }

    enum class ActionType(val id: Byte) {
        SWITCH_OFF_HUB(0x01.toByte()),
        DISCONNECT(0x02.toByte()),
        VCC_PORT_CONTROL_ON(0x03.toByte()),
        VCC_PORT_CONTROL_OFF(0x04.toByte()),
        ACTIVATE_BUSY_IND(0x05.toByte()),
        RESET_BUSY_IND(0x06.toByte()),
        SHUT_DOWN_FOREC(0x2f.toByte()),
        HUB_WILL_SWITCH_OFF(0x30.toByte()),
        HUB_WILL_DISCONNECT(0x31.toByte()),
        HUB_WILL_ENTER_BOOT_MODE(0x32.toByte()),
        INVALID(0xff.toByte());

        companion object {
            fun fromByte(byte: Byte): ActionType = values().find {
                byte == it.id
            } ?: {
                INVALID
            }()
        }
    }

}