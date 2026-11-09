package com.lucanicoletti.wristotp.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

private const val REPEAT_WINDOW_SECONDS = 2.0

@Composable
actual fun QrScanner(onScanned: (String) -> Unit, modifier: Modifier) {
    var status by remember { mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) }
    LaunchedEffect(Unit) {
        if (status != AVAuthorizationStatusAuthorized) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                }
            }
        }
    }

    val hasCamera = remember { AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null }
    if (!hasCamera) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No camera available. Import an image of the QR code instead.")
        }
    } else if (status == AVAuthorizationStatusAuthorized) {
        CameraPreview(onScanned, modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                if (status == AVAuthorizationStatusDenied || status == AVAuthorizationStatusRestricted) {
                    "Camera access is off. Enable it for WristOTP in Settings to scan QR codes."
                } else {
                    "Waiting for camera access…"
                },
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun CameraPreview(onScanned: (String) -> Unit, modifier: Modifier) {
    val currentOnScanned by rememberUpdatedState(onScanned)
    val delegate = remember { MetadataDelegate { currentOnScanned(it) } }
    val session = remember { AVCaptureSession() }

    DisposableEffect(Unit) {
        val camera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = camera?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        }
        val output = AVCaptureMetadataOutput()
        if (session.inputs.isNotEmpty() && session.canAddOutput(output)) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
            if (output.availableMetadataObjectTypes.contains(AVMetadataObjectTypeQRCode)) {
                output.metadataObjectTypes = listOfNotNull(AVMetadataObjectTypeQRCode)
            }
        }
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            session.startRunning()
        }
        onDispose {
            session.stopRunning()
        }
    }

    UIKitView(factory = { PreviewView(session) }, modifier = modifier)
}

private class MetadataDelegate(private val onValue: (String) -> Unit) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    private var lastValue: String? = null
    private var lastSeenAt = 0.0

    override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
        val value = didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstNotNullOfOrNull { it.stringValue } ?: return
        val now = NSDate().timeIntervalSince1970
        if (value != lastValue || now - lastSeenAt > REPEAT_WINDOW_SECONDS) {
            onValue(value)
        }
        lastValue = value
        lastSeenAt = now
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PreviewView(session: AVCaptureSession) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.setFrame(bounds)
    }
}
