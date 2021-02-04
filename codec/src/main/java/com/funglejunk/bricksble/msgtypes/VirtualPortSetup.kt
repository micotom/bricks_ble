package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage

abstract class VirtualPortSetup : LegoMessage.Content.Downstream {

    abstract val id: Byte

    data class Connect(val portIdA: Byte, val portIdB: Byte) : VirtualPortSetup() {
        override val id: Byte = SubCommand.CONNECTED.id
        override val length: Int = 3
        override fun encode(): ByteArray = byteArrayOf(
            id, portIdA, portIdB
        )
    }

    data class Disconnect(val portId: Byte) : VirtualPortSetup() {
        override val id: Byte = SubCommand.DISCONNECTED.id
        override val length: Int = 2
        override fun encode(): ByteArray = byteArrayOf(
            id, portId
        )
    }

    private enum class SubCommand(val id: Byte) {
        DISCONNECTED(0x0),
        CONNECTED(0x1)
    }

}