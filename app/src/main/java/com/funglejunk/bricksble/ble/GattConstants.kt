package com.funglejunk.bricksble.ble

import java.util.*

fun Int.gattConnectionStatusString(): String = gattConnectionStatusMap[this] ?: "UNKNOWN CONNECTION STATUS"

fun Int.gattStatusString(): String = gattStatusMap[this] ?: "UNKNOWN STATUS (${this.toHexString()})"

private val gattConnectionStatusMap = mapOf(
    0 to "DISCONNECTED",
    1 to "CONNECTING",
    2 to "CONNECTED",
    3 to "DISCONNECTING"
)

private val gattStatusMap = mapOf(
    0x0000 to "GATT_SUCCESS / GATT_ENCRYPED_MITM",
    0x0001 to "GATT_INVALID_HANDLE",
    0x0002 to "GATT_READ_NOT_PERMIT",
    0x0003 to "GATT_WRITE_NOT_PERMIT",
    0x0004 to "GATT_INVALID_PDU",
    0x0005 to "GATT_INSUF_AUTHENTICATION",
    0x0006 to "GATT_REQ_NOT_SUPPORTED",
    0x0007 to "GATT_INVALID_OFFSET",
    0x0008 to "GATT_INSUF_AUTHORIZATION",
    0x0009 to "GATT_PREPARE_Q_FULL",
    0x000a to "GATT_NOT_FOUND",
    0x000b to "GATT_NOT_LONG",
    0x000c to "GATT_INSUF_KEY_SIZE",
    0x000d to "GATT_INVALID_ATTR_LEN",
    0x000e to "GATT_ERR_UNLIKELY",
    0x000f to "GATT_INSUF_ENCRYPTION",
    0x0010 to "GATT_UNSUPPORT_GRP_TYPE",
    0x0011 to "GATT_INSUF_RESOURCE",
    0x0087 to "GATT_ILLEGAL_PARAMETER",
    0x0080 to "GATT_NO_RESOURCES",
    0x0081 to "GATT_INTERNAL_ERROR",
    0x0082  to "GATT_WRONG_STATE",
    0x0083 to "GATT_DB_FULL",
    0x0084 to "GATT_BUSY",
    0x0085 to "GATT_ERROR",
    0x0086 to "GATT_CMD_STARTED",
    0x0088 to "GATT_PENDING",
    0x0089 to "GATT_AUTH_FAIL",
    0x008a to "GATT_MORE",
    0x008b to "GATT_INVALID_CFG",
    0x008c to "GATT_SERVICE_STARTED",
    0x008d to "GATT_ENCRYPED_NO_MITM",
    0x008e to "GATT_NOT_ENCRYPTED"
)

val legoCustomServiceUuid = UUID.fromString("00001623-1212-efde-1623-785feabcd123")

val legoCharacteristicUuid = UUID.fromString("00001624-1212-efde-1623-785feabcd123")

fun Int.toHexString() = "0x${this.toUInt().toString(8)}"