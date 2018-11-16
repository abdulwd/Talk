package com.abdulwd.talk

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : Activity() {

    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null
    private val requestBluetoothEnable: Int = 1
    private val defaultUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
        sendText.setOnClickListener {
            val inputText = inputEdiText.editableText
            if (inputText.isNotEmpty()) {
                write(inputText.toString())
                inputEdiText.setText("")
                val output = if (outputTextView.text != null)
                    outputTextView.text.toString() + inputText.toString() + "\n"
                else
                    inputText.toString() + "\n"
                outputTextView.text = output
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            showDialog(BluetoothAdapter.getDefaultAdapter().bondedDevices.toTypedArray())
        } else {
            Toast.makeText(this, "Turn on bluetooth to use this app", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(IOException::class)
    private fun init() {
        val blueAdapter = BluetoothAdapter.getDefaultAdapter()
        if (blueAdapter.isEnabled) {
            showDialog(blueAdapter.bondedDevices.toTypedArray())
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, requestBluetoothEnable)
        }
    }

    private fun write(s: String) {
        outputStream?.write(s.toByteArray())
    }

    private fun connect(device: BluetoothDevice) {
        try {
            val socket = device.createRfcommSocketToServiceRecord(defaultUUID)
            socket.connect()
            outputStream = socket.outputStream
            inStream = socket.inputStream
            Toast.makeText(applicationContext, "Connected", Toast.LENGTH_SHORT).show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "Unable to connect", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDialog(devices: Array<BluetoothDevice>) {
        this.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.select_device)
                .setItems(devices.map { device -> device.name }
                    .toTypedArray()) { d, which ->
                    d.dismiss()
                    connect(devices[which])
                }
            builder.create()
        }.show()
    }
}