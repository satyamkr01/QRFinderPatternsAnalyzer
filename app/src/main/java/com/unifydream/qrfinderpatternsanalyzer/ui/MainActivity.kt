package com.unifydream.qrfinderpatternsanalyzer.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.unifydream.qrfinderpatternsanalyzer.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanQR.setOnClickListener {
            startActivity(Intent(this, QRScanActivity::class.java))
        }
    }
}
