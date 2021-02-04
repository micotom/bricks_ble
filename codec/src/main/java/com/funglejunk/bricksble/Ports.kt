package com.funglejunk.bricksble

// example ports - should be read from notifications after connections via HUB_ATTACHED_IO messages
enum class Ports(val id: Byte) {
    INT_MOTOR_W_TACHO_1(0x0),
    INT_MOTOR_W_TACHO_2(0x1),
    VISION_SENSOR(0x2),
    EXT_MOTOR_W_TACHO(0x3),
    INT_MOTOR_W_TACHO_VIRT(0x10),
    RGB_LIGHT(0x32),
    INT_TILT(0x3a),
    CURRENT(0x3b),
    VOLTAGE(0x3c)
}