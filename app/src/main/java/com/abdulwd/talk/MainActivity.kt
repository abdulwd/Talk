package com.abdulwd.talk

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : Activity() {

    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null
    private val requestBluetoothEnable: Int = 1
    var device: BluetoothDevice? = null
    private val uuidReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                /*device?.createRfcommSocketToServiceRecord(device?.uuids?.get(0)?.uuid)
                ?.let { connectSocket(it) }*/
                val uuid = intent?.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                uuid?.forEach { Log.d("UUID bluetooth", it.toString()) }
                Toast.makeText(applicationContext, "Unable to connect", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_UUID)
        registerReceiver(uuidReceiver, intentFilter)

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

    @Throws(IOException::class)
    private fun init() {
        val blueAdapter = BluetoothAdapter.getDefaultAdapter()
        if (blueAdapter != null) {
            if (blueAdapter.isEnabled) {
                showDialog(blueAdapter.bondedDevices.toList())
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, requestBluetoothEnable)
                showDialog(blueAdapter.bondedDevices.toList())
            }
        }
    }

    @Throws(IOException::class)
    fun write(s: String) {
        outputStream!!.write(s.toByteArray())
    }

    private fun connect() {
        try {
            device?.createRfcommSocketToServiceRecord(device?.uuids?.get(0)?.uuid)
                ?.let { connectSocket(it) }
        } catch (e: java.lang.Exception) {
            device?.fetchUuidsWithSdp()
        }
    }

    private fun showDialog(devices: List<BluetoothDevice>) {
        this.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.select_device)
                .setItems(devices.map { device -> device.name }
                    .toTypedArray()) { _, which ->
                    device = devices[which]
                    connect()
                }
            builder.create()
        }.show()
    }

    private fun connectSocket(socket: BluetoothSocket) {
        socket.connect()
        outputStream = socket.outputStream
        inStream = socket.inputStream
        Toast.makeText(applicationContext, "Connected", Toast.LENGTH_SHORT).show()
    }
}