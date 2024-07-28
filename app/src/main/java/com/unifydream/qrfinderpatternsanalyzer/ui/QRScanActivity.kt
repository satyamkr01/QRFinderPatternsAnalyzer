package com.unifydream.qrfinderpatternsanalyzer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.unifydream.qrfinderpatternsanalyzer.databinding.ActivityQrscanBinding
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executors

class QRScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrscanBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeOpenCV()

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)


        if (allPermissionsGranted()) {
            cameraProvider()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun initializeOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Log.d("MainActivity", "OpenCV Loaded Successfully")
        } else {
            Log.e("MainActivity", "OpenCV Load Failed")
        }
    }

    private fun cameraProvider() {
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            startCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val preview = Preview.Builder().apply {
            setTargetResolution(Size(binding.previewView.width, binding.previewView.height))
        }.build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(binding.previewView.width, binding.previewView.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { result ->
                    binding.previewView.post {
                        Log.d("QRScanActivity", "QR code scanned: ${result.text}")
                    }
                })
            }

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraProvider()
            } else {
                Toast.makeText(this, "Please grant Camera Permissions.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}