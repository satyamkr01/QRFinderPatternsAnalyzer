package com.unifydream.qrfinderpatternsanalyzer.ui

import android.graphics.ImageFormat.YUV_420_888
import android.graphics.ImageFormat.YUV_422_888
import android.graphics.ImageFormat.YUV_444_888
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

class QRCodeAnalyzer(private val onQrCodesDetected: (qrCode: Result) -> Unit) : ImageAnalysis.Analyzer {

    private val yuvFormats = mutableListOf(YUV_420_888)

    init {
        yuvFormats.addAll(listOf(YUV_422_888, YUV_444_888))
    }

    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
        )
        setHints(map)
    }

    override fun analyze(image: ImageProxy) {
        if (image.format !in yuvFormats) {
            Log.e("QRCodeAnalyzer", "Expected YUV, now = ${image.format}")
            return
        }

        val data = image.planes[0].buffer.toByteArray()

        val source = PlanarYUVLuminanceSource(data, image.width, image.height, 0, 0,
            image.width, image.height, false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = reader.decode(binaryBitmap)
            Log.d("QRCodeAnalyzer", "QR code found: ${result.text}")

            // Convert image to OpenCV Mat
            val yuvMat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
            yuvMat.put(0, 0, data)
            val rgbMat = Mat()
            Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3)

            // Draw rectangle around QR code
            val points = result.resultPoints
            if (points.size >= 4) {
                val x = points.map { it.x.toInt() }
                val y = points.map { it.y.toInt() }
                val minX = x.minOrNull() ?: 0
                val maxX = x.maxOrNull() ?: 0
                val minY = y.minOrNull() ?: 0
                val maxY = y.maxOrNull() ?: 0

                val rect = Rect(Point(minX.toDouble(), minY.toDouble()), Point(maxX.toDouble(), maxY.toDouble()))
                Imgproc.rectangle(rgbMat, rect, Scalar(255.0, 0.0, 0.0), 5)
            }

            // Update UI with the result
            onQrCodesDetected(result)
        } catch (e: NotFoundException) {
            Log.e("QRCodeAnalyzer", "QR code not found", e)
        }
        image.close()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}

