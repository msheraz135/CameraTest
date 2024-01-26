package com.example.cameratest

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    var capture: ImageButton? = null
    private var previewView: PreviewView? = null

    // launcher to get result for permissions
    private val activityResultLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            startCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.cameraPreview)
        capture = findViewById(R.id.capture)

        // check permissions
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
    }

    fun startCamera() {
        val aspectRatio = aspectRatio(previewView!!.width, previewView!!.height)
        val listenableFuture = ProcessCameraProvider.getInstance(this)
        listenableFuture.addListener({
            try {
                val cameraProvider =
                    listenableFuture.get() as ProcessCameraProvider
                val preview =
                    Preview.Builder().setTargetAspectRatio(aspectRatio).build()
                val imageCapture =
                    ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(windowManager.defaultDisplay.rotation).build()
                cameraProvider.unbindAll()
                capture!!.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        activityResultLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    takePicture(imageCapture)
                }
                preview.setSurfaceProvider(previewView!!.surfaceProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun takePicture(imageCapture: ImageCapture) {
        val file = File(getExternalFilesDir(null), System.currentTimeMillis().toString() + ".jpg")
        val outputFileOptions = OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputFileOptions,
            Executors.newCachedThreadPool(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: OutputFileResults) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Image saved at: " + file.path,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    startCamera()
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to save: " + exception.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    startCamera()
                }
            })
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = Math.max(width, height).toDouble() / Math.min(width, height)
        return if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else AspectRatio.RATIO_16_9
    }
}