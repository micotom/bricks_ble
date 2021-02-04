package com.example.leoboost

import com.funglejunk.bricksble.msgtypes.HubProperty
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun foo() {
        val b = byteArrayOf(
            0x10, 0x15, 0x37, 0x17
        )

        val n = HubProperty.VersionNumberPayload.decode(b)
        println(n)


        val number = HubProperty.VersionNumberPayload(
            // 1.7.37.1510
            HubProperty.VersionNumber(
                1, 7, 37, 1510
            )
        )
        println(number.encode().joinToString { "0x${com.funglejunk.bricksble.byteToHex(
            it
        )}" })

        // 0x0a, 0x00, 0x81, 0x10, 0x11, 0x07, 0x07, 0x64, 0x64, 0x03

        // 0x0b, 0x00, 0x81, 0x10, 0x11, 0x08, 0x00, 0x64, 0x00, 0x64, 0x03
    }

}