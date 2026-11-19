package com.lucanicoletti.wristotp.sync

interface IosWatchTransport {
    fun start(listener: IosWatchTransportListener)

    fun chooseDevice()

    fun send(message: Any, completion: (String?) -> Unit)
}

interface IosWatchTransportListener {
    fun onUnavailable(reason: String)

    fun onNoDevice()

    fun onDisconnected(deviceName: String)

    fun onChecking(deviceName: String)

    fun onAppNotInstalled(deviceName: String)

    fun onReady(deviceName: String)

    fun onMessage(payload: Any?)
}
