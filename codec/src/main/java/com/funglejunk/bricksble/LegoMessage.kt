package com.funglejunk.bricksble

import com.funglejunk.bricksble.msgtypes.*


abstract class LegoMessage {

    abstract val header: com.funglejunk.bricksble.Header
    abstract val content: Content?

    interface Content {
        interface Upstream : Content {
            interface Decoder<T : Upstream> {
                fun decode(payload: ByteArray): T
            }
        }

        interface Downstream : Content {
            val length: Int
            fun encode(): ByteArray
        }
    }

    data class UpstreamMessage(
        override val header: com.funglejunk.bricksble.Header,
        override val content: Content.Upstream?
    ) :
        LegoMessage() {

        inline fun <reified T : Content.Upstream> getContentTyped(): T? = content as? T

        companion object {

            fun decode(bytes: ByteArray): UpstreamMessage {
                val header = com.funglejunk.bricksble.Header.Decoder.decode(bytes)
                val payloadBytes = bytes.copyOfRange(header.headerLength, header.packetLength)
                val decoder: Content.Upstream.Decoder<*>? = when (header.messageType) {
                    com.funglejunk.bricksble.Header.HubMessageType.HUB_ATTACHED_IO -> HubAttachedIo.Decoder
                    com.funglejunk.bricksble.Header.HubMessageType.HUB_ACTIONS -> HubAction.Decoder
                    com.funglejunk.bricksble.Header.HubMessageType.HUB_PROPERTIES -> HubProperty.Decoder
                    com.funglejunk.bricksble.Header.HubMessageType.GENERIC_ERROR_MSG -> GenericErrorMessage.Decoder
                    com.funglejunk.bricksble.Header.HubMessageType.HUB_ALERTS -> HubAlert.Decoder
                    com.funglejunk.bricksble.Header.HubMessageType.PORT_OUTPUT_COMMAND_FEEDBACK -> PortOutputCommandFeedback.Decoder
                    else -> null
                }
                return UpstreamMessage(
                    header,
                    decoder?.decode(payloadBytes)
                )
            }
        }

        inline fun <reified T : HubProperty.Payload<*>> onHubPropertyMessage(
            property: HubProperty.Property,
            onHubPropertyMessage: HubProperty.Payload<*>.() -> Unit
        ) {
            if (header.messageType == com.funglejunk.bricksble.Header.HubMessageType.HUB_PROPERTIES &&
                (content as HubProperty<*>).property == property
            ) {
                onHubPropertyMessage(content.payload as HubProperty.Payload<*>)
            }
        }

    }

    data class DownstreamMessage(
        override val header: com.funglejunk.bricksble.Header,
        override val content: Content.Downstream?
    ) :
        LegoMessage() {

        fun encode(): ByteArray =
            content?.let { content ->
                val headerBytes = com.funglejunk.bricksble.Header.Builder.encode(header, content.length)
                val contentBytes = content.encode()
                headerBytes + contentBytes
            } ?: throw IllegalArgumentException("No content provided")

    }

}