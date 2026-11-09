import ComposeApp
import ConnectIQ
import Foundation

final class GarminTransport: NSObject, IosWatchTransport, IQDeviceEventDelegate, IQAppMessageDelegate {
    static let shared = GarminTransport()
    static let urlScheme = "wristotp-ciq"

    private static let editions: [(UUID, WatchEdition)] = [
        (UUID(uuidString: "5bdabd0d-e07e-48e1-a7c1-03ee7a3d006c")!, .full),
        (UUID(uuidString: "c4cc401e-ebcc-4cae-ab76-3d52f85af9af")!, .free),
    ]
    private static let devicesKey = "connectiq.devices"

    private var listener: IosWatchTransportListener?
    private var device: IQDevice?
    private var app: IQApp?

    func start(listener: IosWatchTransportListener) {
        self.listener = listener
        ConnectIQ.sharedInstance().initialize(withUrlScheme: Self.urlScheme, uiOverrideDelegate: nil)
        if let cached = loadDevices().first {
            use(cached)
        } else {
            listener.onNoDevice()
        }
    }

    func chooseDevice() {
        ConnectIQ.sharedInstance().showDeviceSelection()
    }

    func send(message: Any, completion: @escaping (String?) -> Void) {
        guard let app else {
            completion("No watch connected")
            return
        }
        ConnectIQ.sharedInstance().sendMessage(message, to: app, progress: nil) { result in
            completion(result == .success ? nil : NSStringFromSendMessageResult(result))
        }
    }

    func handle(url: URL) -> Bool {
        guard url.scheme == Self.urlScheme,
              let devices = ConnectIQ.sharedInstance().parseDeviceSelectionResponse(from: url) as? [IQDevice] else {
            return false
        }
        saveDevices(devices)
        if let first = devices.first {
            use(first)
        } else {
            listener?.onNoDevice()
        }
        return true
    }

    func deviceStatusChanged(_ device: IQDevice!, status: IQDeviceStatus) {
        guard let device, device.uuid == self.device?.uuid else { return }
        switch status {
        case .connected:
            checkApp(on: device)
        case .bluetoothNotReady:
            app = nil
            listener?.onUnavailable(reason: "Bluetooth is off")
        default:
            app = nil
            listener?.onDisconnected(deviceName: device.friendlyName)
        }
    }

    func receivedMessage(_ message: Any!, from app: IQApp!) {
        listener?.onMessage(payload: message)
    }

    private func use(_ device: IQDevice) {
        ConnectIQ.sharedInstance().unregister(forAllDeviceEvents: self)
        ConnectIQ.sharedInstance().unregister(forAllAppMessages: self)
        self.device = device
        app = nil
        ConnectIQ.sharedInstance().register(forDeviceEvents: device, delegate: self)
        deviceStatusChanged(device, status: ConnectIQ.sharedInstance().getDeviceStatus(device))
    }

    private func checkApp(on device: IQDevice) {
        listener?.onChecking(deviceName: device.friendlyName)
        findApp(on: device, remaining: Self.editions, unknown: nil)
    }

    private func findApp(on device: IQDevice, remaining: [(UUID, WatchEdition)], unknown: (IQApp, WatchEdition)?) {
        guard let next = remaining.first else {
            if let unknown {
                select(unknown.0, edition: unknown.1, on: device)
            } else {
                listener?.onAppNotInstalled(deviceName: device.friendlyName)
            }
            return
        }
        let (uuid, edition) = next
        let candidate = IQApp(uuid: uuid, store: uuid, device: device)!
        ConnectIQ.sharedInstance().getAppStatus(candidate) { [weak self] status in
            guard let self, self.device?.uuid == device.uuid else { return }
            if let status, status.isInstalled {
                self.select(candidate, edition: edition, on: device)
                return
            }
            self.findApp(on: device, remaining: Array(remaining.dropFirst()), unknown: unknown ?? (status == nil ? (candidate, edition) : nil))
        }
    }

    private func select(_ candidate: IQApp, edition: WatchEdition, on device: IQDevice) {
        app = candidate
        ConnectIQ.sharedInstance().register(forAppMessages: candidate, delegate: self)
        listener?.onReady(deviceName: device.friendlyName, edition: edition)
    }

    private func saveDevices(_ devices: [IQDevice]) {
        let data = try? NSKeyedArchiver.archivedData(withRootObject: devices, requiringSecureCoding: true)
        UserDefaults.standard.set(data, forKey: Self.devicesKey)
    }

    private func loadDevices() -> [IQDevice] {
        guard let data = UserDefaults.standard.data(forKey: Self.devicesKey) else { return [] }
        return (try? NSKeyedUnarchiver.unarchivedArrayOfObjects(ofClass: IQDevice.self, from: data)) ?? []
    }
}
