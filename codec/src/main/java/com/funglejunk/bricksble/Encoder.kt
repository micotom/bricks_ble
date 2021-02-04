package com.funglejunk.bricksble

import com.funglejunk.bricksble.msgtypes.HubProperty
import com.funglejunk.bricksble.msgtypes.PortOutputCmd

object Encoder {

    private val standardSacInfo = PortOutputCmd.SaCInfo(
        PortOutputCmd.SaCInfo.Flags.Startup.ExecuteImmediately,
        PortOutputCmd.SaCInfo.Flags.Completion.CommandFeedback
    )

    object Updates {

        fun <T> getFor(property: HubProperty.Property): ByteArray =
            if (property.supportedOperations.contains(HubProperty.Operation.ENABLE_UPDATE)) {
                LegoMessage.DownstreamMessage(
                    Header(messageType = Header.HubMessageType.HUB_PROPERTIES),
                    HubProperty<T>(
                        property,
                        HubProperty.Operation.ENABLE_UPDATE,
                        null
                    )
                ).encode()
            } else {
                throw UnsupportedOperationException("$property does not allow updates")
            }

        fun <T> stop(property: HubProperty.Property): ByteArray =
            if (property.supportedOperations.contains(HubProperty.Operation.ENABLE_UPDATE)) {
                LegoMessage.DownstreamMessage(
                    Header(messageType = Header.HubMessageType.HUB_PROPERTIES),
                    HubProperty<T>(
                        property,
                        HubProperty.Operation.DISABLE_UPDATE,
                        null
                    )
                ).encode()
            } else {
                throw UnsupportedOperationException("$property does not allow updates")
            }

    }

    object Motor {

        fun startSpeed(
            port: Ports,
            speed: Byte,
            maxPower: Byte
        ): ByteArray =
            LegoMessage.DownstreamMessage(
                Header(messageType = Header.HubMessageType.PORT_OUTPUT_COMMAND),
                PortOutputCmd(
                    port.id,
                    standardSacInfo,
                    PortOutputCmd.SubCommand.START_SPEED_SPEED_MAXP_UP,
                    PortOutputCmd.StartSpeed1(
                        speed,
                        maxPower
                    )
                )
            ).encode()

        fun startSpeedForTime(
            port: Ports,
            speed: Byte,
            maxPower: Byte,
            time: Short
        ): ByteArray =
            LegoMessage.DownstreamMessage(
                Header(messageType = Header.HubMessageType.PORT_OUTPUT_COMMAND),
                PortOutputCmd(
                    port.id,
                    standardSacInfo,
                    PortOutputCmd.SubCommand.START_SPEED_FOR_TIME_1,
                    PortOutputCmd.StartSpeedForTime1(
                        time.toInt(),
                        speed,
                        maxPower,
                        PortOutputCmd.EndState.BRAKE
                    )
                )
            ).encode()

        fun startSpeedForDegrees(
            port: Ports,
            degrees: Int,
            speed: Byte,
            maxPower: Byte
        ) : ByteArray = LegoMessage.DownstreamMessage(
            Header(messageType = Header.HubMessageType.PORT_OUTPUT_COMMAND),
            PortOutputCmd(
                port.id,
                standardSacInfo,
                PortOutputCmd.SubCommand.START_SPEED_FOR_DEGREES_1,
                PortOutputCmd.StartSpeedForDegrees1(
                    degrees, speed, maxPower, PortOutputCmd.EndState.BRAKE
                )
            )
        ).encode()

        fun startSpeedLr(
            port: Ports,
            speedLeft: Byte,
            speedRight: Byte,
            maxPower: Byte
        ) : ByteArray = LegoMessage.DownstreamMessage(
            Header(messageType = Header.HubMessageType.PORT_OUTPUT_COMMAND),
            PortOutputCmd(
                port.id,
                standardSacInfo,
                PortOutputCmd.SubCommand.START_SPEED_SPEED1_SPEED2_MAXP_UP,
                PortOutputCmd.StartSpeedLr(
                    speedLeft, speedRight, maxPower
                )
            )
        ).encode()
    }

    object Led {

        fun setColor(color: PortOutputCmd.ColorByte): ByteArray = LegoMessage.DownstreamMessage(
            Header(messageType = Header.HubMessageType.PORT_OUTPUT_COMMAND),
            PortOutputCmd(
                Ports.RGB_LIGHT.id,
                PortOutputCmd.SaCInfo(
                    PortOutputCmd.SaCInfo.Flags.Startup.ExecuteImmediately,
                    PortOutputCmd.SaCInfo.Flags.Completion.NoAction
                ),
                PortOutputCmd.SubCommand.SET_COLOR,
                PortOutputCmd.SetColor(color)
            )
        ).encode()

    }

}