package com.funglejunk.bricksble.msgtypes

import com.funglejunk.bricksble.LegoMessage
import com.funglejunk.bricksble.toInt16LE
import com.funglejunk.bricksble.toInt32LE
import kotlin.experimental.or

data class PortOutputCmd(
    val portId: Byte,
    val sacInfo: SaCInfo,
    val subCommand: SubCommand,
    val payload: Payload
) : LegoMessage.Content.Downstream {

    override val length = 3 + payload.size

    override fun encode(): ByteArray =
        byteArrayOf(
            portId,
            sacInfo.encode(),
            subCommand.id
        ) + payload.encode()

    data class SaCInfo(
        val startupFlag: Flags.Startup,
        val completionFlag: Flags.Completion
    ) {
        fun encode(): Byte = startupFlag.bits or completionFlag.bits

        sealed class Flags {
            abstract val bits: Byte

            sealed class Startup : Flags() {
                object BufferIfNecessary : Startup() {
                    override val bits: Byte = 0b0000 shl 4
                }

                object ExecuteImmediately : Startup() {
                    override val bits: Byte = (0b0001 shl 4).toByte()
                }
            }

            sealed class Completion : Flags() {
                object NoAction : Completion() {
                    override val bits: Byte = 0b0000
                }

                object CommandFeedback : Completion() {
                    override val bits: Byte = 0b0001
                }
            }
        }

    }

    enum class SubCommand(val id: Byte) {
        WRITE_DIRECT(0x50),
        WRITE_DIRECT_MODE(0x51),

        START_POWER(WRITE_DIRECT_MODE.id),
        START_POWER_1_2(0x02),
        SET_ACC_TIME(0x05),
        SET_DEC_TIME(0x06),
        START_SPEED_SPEED_MAXP_UP(0x07),
        START_SPEED_SPEED1_SPEED2_MAXP_UP(0x08),
        START_SPEED_FOR_TIME_1(0x09),
        START_SPEED_FOR_TIME_2(0x0A),
        START_SPEED_FOR_DEGREES_1(0x0B),
        START_SPEED_FOR_DEGREES_2(0x0C),
        GOTO_ABS_POSITION_1(0x0D),
        GOTO_ABS_POSITION_2(0x0E),
        PRESET_ENCODER(0x51),
        PRESET_ENCODER_2(0x14),
        TILT_IMPACT_PRESET(WRITE_DIRECT_MODE.id),
        TILT_CONFIG_ORIENTATION(WRITE_DIRECT_MODE.id),
        TILT_CONFIG_IMPACT(WRITE_DIRECT_MODE.id),
        TILT_FACTORY_CALIBRATION(WRITE_DIRECT.id),
        HW_RESET(WRITE_DIRECT.id),
        SET_COLOR(WRITE_DIRECT_MODE.id),
        SET_RGB_COLOR(WRITE_DIRECT_MODE.id)
    }

    interface Payload {
        val size: Int
        fun encode(): ByteArray
    }

    enum class EndState(val bits: Byte) {
        FLOAT(0), HOLD(126), BRAKE(127)
    }

    data class UseProfile(val flags: Set<Flags>) {
        enum class Flags(val bits: Byte) {
            USE_ACC_PROFILE(0b1),
            USE_DEC_PROFILE(0b10)
        }

        fun encode(): Byte = flags.fold(0x0.toByte()) { acc, new -> acc or new.bits }
    }

    data class StartSpeedForTime1(
        val timeMs: Int,
        val speedPerc: Byte,
        val maxPowerPerc: Byte,
        val endState: EndState,
        val useProfile: UseProfile = UseProfile(
            setOf(
                UseProfile.Flags.USE_ACC_PROFILE,
                UseProfile.Flags.USE_DEC_PROFILE
            )
        )
    ) : Payload {
        override val size: Int = 5
        override fun encode(): ByteArray =
            timeMs.toInt16LE() + byteArrayOf(
                speedPerc,
                maxPowerPerc,
                endState.bits,
                useProfile.encode()
            )
    }

    data class StartSpeedForDegrees1(
        val degrees: Int,
        val speed: Byte,
        val maxPower: Byte,
        val endState: EndState,
        val useProfile: UseProfile = UseProfile(
            setOf(
                UseProfile.Flags.USE_ACC_PROFILE,
                UseProfile.Flags.USE_DEC_PROFILE
            )
        )
    ) : Payload {
        override val size: Int = 8
        override fun encode(): ByteArray =
            degrees.toInt32LE() + byteArrayOf(
                speed, maxPower, endState.bits, useProfile.encode()
            )
    }

    data class StartPower(
        val power: Byte
    ) : Payload {
        override val size: Int = 3
        override fun encode(): ByteArray = byteArrayOf(
            0x51, 0x00, power
        )
    }

    data class StartSpeed1(
        val speed: Byte,
        val maxPower: Byte,
        val useProfile: UseProfile = UseProfile(
            setOf(
                UseProfile.Flags.USE_ACC_PROFILE,
                UseProfile.Flags.USE_DEC_PROFILE
            )
        )
    ) : Payload {
        override val size: Int = 4
        override fun encode(): ByteArray = byteArrayOf(
            speed, maxPower, useProfile.encode()
        )
    }

    data class StartSpeedLr(
        val speedLeft: Byte,
        val speedRight: Byte,
        val maxPower: Byte,
        val useProfile: UseProfile = UseProfile(
            setOf(
                UseProfile.Flags.USE_ACC_PROFILE,
                UseProfile.Flags.USE_DEC_PROFILE
            )
        )
    ) : Payload {
        override val size: Int = 5
        override fun encode(): ByteArray = byteArrayOf(
            speedLeft, speedRight, maxPower, useProfile.encode()
        )
    }

    enum class ColorByte(val id: Byte) {
        BLACK(0x0), BLUE(0x3), GREEN(0x5), YELLOW(0x7), RED(0x9), WHITE(0xA)
    }

    data class SetColor(
        val color: ColorByte
    ) : Payload {
        override val size: Int = 2
        override fun encode(): ByteArray = byteArrayOf(0x0, color.id)
    }

    data class SetColorRgb(
        val red: Byte, val green: Byte, val blue: Byte
    ) : Payload {
        override val size: Int = 6
        override fun encode(): ByteArray = byteArrayOf(
            0x0, 0x51, 0x01, red, green, blue
        )
    }

}