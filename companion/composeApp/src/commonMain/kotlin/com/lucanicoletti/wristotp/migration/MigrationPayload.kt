package com.lucanicoletti.wristotp.migration

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class MigrationPayload(
    @ProtoNumber(1) val otpParameters: List<OtpParameters> = emptyList(),
    @ProtoNumber(2) val version: Int = 0,
    @ProtoNumber(3) val batchSize: Int = 0,
    @ProtoNumber(4) val batchIndex: Int = 0,
    @ProtoNumber(5) val batchId: Int = 0,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class OtpParameters(
    @ProtoNumber(1) val secret: ByteArray = ByteArray(0),
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val issuer: String = "",
    @ProtoNumber(4) val algorithm: Int = 0,
    @ProtoNumber(5) val digits: Int = 0,
    @ProtoNumber(6) val type: Int = 0,
    @ProtoNumber(7) val counter: Long = 0,
)
