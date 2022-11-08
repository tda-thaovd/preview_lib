package com.demo.lib.preview_camera_lib

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.view.TextureRegistry
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner

class PreviewCamera(private val activity: Activity, private val textureRegistry: TextureRegistry) :
    MethodChannel.MethodCallHandler,
    PluginRegistry.RequestPermissionsResultListener {
    companion object {
        /**
         * When the application's activity is [androidx.fragment.app.FragmentActivity], requestCode can only use the lower 16 bits.
         * @see androidx.fragment.app.FragmentActivity.validateRequestPermissionsRequestCode
         */
        private const val REQUEST_CODE = 0x0786
        private val TAG = PreviewCamera::class.java.simpleName
    }

    private var listener: PluginRegistry.RequestPermissionsResultListener? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return listener?.onRequestPermissionsResult(requestCode, permissions, grantResults) ?: false
    }

    private fun checkPermission(result: MethodChannel.Result) {
        // Can't get exact denied or not_determined state without request. Just return not_determined when state isn't authorized
        val state =
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) 1
            else 0
        result.success(state)
    }

    private fun requestPermission(result: MethodChannel.Result) {
        listener = PluginRegistry.RequestPermissionsResultListener { requestCode, _, grantResults ->
            if (requestCode != REQUEST_CODE) {
                false
            } else {
                val authorized = grantResults[0] == PackageManager.PERMISSION_GRANTED
                result.success(authorized)
                listener = null
                true
            }
        }
        val permissions = arrayOf(Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "state" -> checkPermission(result)
            "request" -> requestPermission(result)
            "start" -> start(call, result)
        }
    }

    private fun start(call: MethodCall, result: MethodChannel.Result) {
        if (preview != null && textureEntry != null) {
            val resolution = preview!!.resolutionInfo!!.resolution
            val portrait = camera!!.cameraInfo.sensorRotationDegrees % 180 == 0
            val width = resolution.width.toDouble()
            val height = resolution.height.toDouble()
            val size = if (portrait) mapOf(
                "width" to width,
                "height" to height
            ) else mapOf("width" to height, "height" to width)
            val answer = mapOf(
                "textureId" to textureEntry!!.id(),
                "size" to size,
            )
            result.success(answer)
        } else {
            val future = ProcessCameraProvider.getInstance(activity)
            val executor = ContextCompat.getMainExecutor(activity)
            future.addListener({
                cameraProvider = future.get()
                if (cameraProvider == null) {
                    result.error("cameraProvider", "cameraProvider is null", null)
                    return@addListener
                }
                cameraProvider!!.unbindAll()
                textureEntry = textureRegistry.createSurfaceTexture()
                if (textureEntry == null) {
                    result.error("textureEntry", "textureEntry is null", null)
                    return@addListener
                }

                // Preview
                val surfaceProvider = Preview.SurfaceProvider { request ->
                    val texture = textureEntry!!.surfaceTexture()
                    texture.setDefaultBufferSize(
                        request.resolution.width,
                        request.resolution.height
                    )
                    val surface = Surface(texture)
                    request.provideSurface(surface, executor) { }
                }

                val previewBuilder = Preview.Builder()
                preview = previewBuilder.build().apply { setSurfaceProvider(surfaceProvider) }

                camera = cameraProvider!!.bindToLifecycle(
                    activity as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
                val previewSize = preview!!.resolutionInfo?.resolution ?: Size(0, 0)
                Log.i(TAG, "start: $previewSize")
                if (camera == null) {
                    result.error("camera", "camera is null", null)
                    return@addListener
                }
                val resolution = preview!!.resolutionInfo!!.resolution
                val portrait = camera!!.cameraInfo.sensorRotationDegrees % 180 == 0
                val width = resolution.width.toDouble()
                val height = resolution.height.toDouble()
                val size = if (portrait) mapOf(
                    "width" to width,
                    "height" to height
                ) else mapOf("width" to height, "height" to width)
                val answer = mapOf(
                    "textureId" to textureEntry!!.id(),
                    "size" to size,
                )
                result.success(answer)
            }, executor)
        }
    }
}