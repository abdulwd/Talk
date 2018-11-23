package com.abdulwd.talk

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.gotev.speech.Speech
import net.gotev.speech.SpeechDelegate
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null
    private val requestBluetoothEnable = 1
    private val requestAudioPermission = 2
    private val tag = "AbdulWadood"
    private val words = LinkedList<String>()
    private val displayText = StringBuilder()
    private val displayMaxHeight = 4
    private val displayMaxWidth = 21
    private val delay = 1000L
    private val stopListening = "Stop Listening"
    private val displaySize = displayMaxHeight * displayMaxWidth
    private val defaultUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val speechDelegate: SpeechDelegate = object : SpeechDelegate {
        override fun onStartOfSpeech() {

        }

        override fun onSpeechPartialResults(results: MutableList<String>?) {

        }

        override fun onSpeechRmsChanged(value: Float) {

        }

        override fun onSpeechResult(result: String?) {
            result?.let {
                it.split(" ").forEach { word ->
                    if (word.isNotEmpty())
                        words.add(word)
                }
                addToTextView(it)
            }

            if (result != null && result.isNotEmpty() &&
                (disposable == null || disposable?.isDisposed == true)
            ) {
                Observable
                    .interval(0, delay, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Long> {
                        override fun onComplete() {
                            Log.d(tag, "on Complete")
                        }

                        override fun onSubscribe(d: Disposable) {
                            disposable = d
                        }

                        override fun onNext(l: Long) {
                            if (words.isEmpty()) {
                                return
                            }
                            if (displayText.length + words.peek().length > displaySize) {
                                displayText.setLength(0)
                            }
                            displayText.append(words.poll())
                            displayText.append(' ')
                            Log.d(tag, displayText.toString())
                            write(displayText.toString())
                        }

                        override fun onError(e: Throwable) {

                        }
                    })
            }

            if (flag && startButton.text.toString().equals(stopListening, true)) {
                Handler().post { startVoiceRecognition() }
            }
        }
    }
    private var flag = false
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectBluetoothDevice()
        Speech.init(this, packageName)
        sendText.setOnClickListener {
            inputEdiText?.editableText?.toString()?.let { it1 ->
                addToTextView(it1)
                write(it1)
            }
        }
        startButton.setOnClickListener {
            if (getString(R.string.start_listening).equals(startButton.text.toString(), true)) {
                startVoiceRecognition()
            } else {
                stopVoiceRecognition()
            }
        }
    }

    fun addToTextView(inputText: CharSequence) {
        if (inputText.isNotEmpty()) {
            inputEdiText.setText("")
            val output = if (outputTextView.text != null)
                outputTextView.text.toString() + inputText.toString() + "\n"
            else
                inputText.toString() + "\n"
            outputTextView.text = output
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            showDialog(BluetoothAdapter.getDefaultAdapter().bondedDevices.toTypedArray())
        } else {
            Toast.makeText(this, "Turn on bluetooth to use this app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (!grantResults.isNotEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectBluetoothDevice() {
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
            checkAudioPermission()
        } catch (e: java.io.IOException) {
            Toast.makeText(this, "Unable to connect", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestAudioPermission
            )
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

    private fun startVoiceRecognition() {
        flag = true
        Speech.getInstance().startListening(speechProgressView, speechDelegate)
        startButton.text = stopListening
    }

    private fun stopVoiceRecognition() {
        flag = false
        Speech.getInstance().stopListening()
        startButton.text = getString(R.string.start_listening)
    }

    override fun onDestroy() {
        stopVoiceRecognition()
        super.onDestroy()
    }
}