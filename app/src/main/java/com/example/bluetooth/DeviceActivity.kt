package com.example.bluetooth

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import java.io.IOException

class DeviceActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var title: TextView
    private lateinit var editText: EditText
    private lateinit var sendCommand: MaterialButton
    private lateinit var open1: MaterialButton
    private lateinit var open2: MaterialButton
    private lateinit var open3: MaterialButton
    private lateinit var open4: MaterialButton
    private lateinit var open5: MaterialButton
    private lateinit var open6: MaterialButton
    private lateinit var open7: MaterialButton
    private lateinit var open8: MaterialButton
    private lateinit var open9: MaterialButton
    private lateinit var open10: MaterialButton

    private var thread: ConnectThread? = null
    private var threadHandler: Handler? = null
    private var progressDialog: ProgressDialog? = null

    private val mainHandler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("MissingPermission", "SetTextI18n")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                CONNECT_SUCCESS -> {
                    progressDialog?.dismiss()
                    scrollView.isVisible = true
                    val device = intent.getParcelableExtra<BluetoothDevice>(DEVICE)
                    val deviceName = device?.name ?: "unknown"
                    title.text = "Подключено: $deviceName"
                    Toast.makeText(this@DeviceActivity, "Устройство успешно подключено", Toast.LENGTH_LONG).show()
                }
                CONNECT_ERROR -> {
                    progressDialog?.dismiss()
                    val exception = msg.obj as Exception
                    Toast.makeText(this@DeviceActivity, exception.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                WRITE_ERROR_NOT_CONNECTED -> {
                    val message = "Отправка данных невозможна, соединение потеряно. Попробуйте заново"
                    Toast.makeText(this@DeviceActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                }
                WRITE_SUCCESS -> {
                    val text = msg.obj as String
                    val message = "Команда $text успешно отправлена"
                    Toast.makeText(this@DeviceActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var pinCode = "1234"

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            Log.e("TEST", "intent.action = ${intent.action}")
            when(intent.action) {
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    try {
                        //Toast.makeText(this@DeviceActivity, "ACTION_PAIRING_REQUEST", Toast.LENGTH_LONG).show()
                        val bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        bluetoothDevice?.setPin(pinCode.toByteArray())
                        //bluetoothDevice?.setPairingConfirmation(true)
                        bluetoothDevice?.createBond()
                    } catch (ex: Exception) {
                        //Toast.makeText(this@DeviceActivity, "ACTION_PAIRING_REQUEST: ${ex.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    //private val U_STR = "0000112f-0000-1000-8000-00805f9b34fb"
    private val U_STR = "00001101-0000-1000-8000-00805F9B34FB"
    private val uuid = ParcelUuid.fromString(U_STR).uuid

    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        pinCode = intent.getStringExtra(PIN_CODE) ?: "1234"

        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        registerReceiver(receiver, filter)

        scrollView = findViewById(R.id.scroll_view)
        title = findViewById(R.id.title)
        editText = findViewById(R.id.edit_text)
        sendCommand = findViewById(R.id.send_command)
        open1 = findViewById(R.id.door_1)
        open2 = findViewById(R.id.door_2)
        open3 = findViewById(R.id.door_3)
        open4 = findViewById(R.id.door_4)
        open5 = findViewById(R.id.door_5)
        open6 = findViewById(R.id.door_6)
        open7 = findViewById(R.id.door_7)
        open8 = findViewById(R.id.door_8)
        open9 = findViewById(R.id.door_9)
        open10 = findViewById(R.id.door_10)

        sendCommand.setOnClickListener {
            val text = editText.text.toString()
            writeData(text)
        }
        open1.setOnClickListener { writeData("open:1\n") }
        open2.setOnClickListener { writeData("open:2\n") }
        open3.setOnClickListener { writeData("open:3\n") }
        open4.setOnClickListener { writeData("open:4\n") }
        open5.setOnClickListener { writeData("open:5\n") }
        open6.setOnClickListener { writeData("open:6\n") }
        open7.setOnClickListener { writeData("open:7\n") }
        open8.setOnClickListener { writeData("open:8\n") }
        open9.setOnClickListener { writeData("open:9\n") }
        open10.setOnClickListener { writeData("open:10\n") }

        val device = intent.getParcelableExtra<BluetoothDevice>(DEVICE)

        scrollView.isVisible = false
        val deviceName = device?.name ?: "unknown"
        title.text = "Попытка подключения к $deviceName"

        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle("Подключение")
        progressDialog?.setMessage("Подождите идет подключение к $deviceName")
        progressDialog?.setCancelable(false)
        progressDialog?.show()

        thread?.quitSafely()
        thread = ConnectThread("blueToothThread") { handler ->
            threadHandler = handler
            val message = threadHandler?.obtainMessage(TRY_CONNECT, device)
            message?.let { threadHandler?.sendMessage(it) }
        }
        thread?.start()
    }

    private fun writeData(command: String) {
        val message = threadHandler?.obtainMessage(WRITE_DATA, command.toByteArray())
        message?.let { threadHandler?.sendMessage(it) }
    }

    private fun closeSocket() {
        val message = threadHandler?.obtainMessage(CLOSE_SOCKET)
        message?.let { threadHandler?.sendMessage(it) }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(
        name: String,
        private val onHandlerReady: (Handler) -> Unit
    ) : HandlerThread(name) {

        lateinit var handler: Handler

        private lateinit var device: BluetoothDevice

        private val socket by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        private val outputStream by lazy(LazyThreadSafetyMode.NONE) {
            socket.outputStream
        }

        private var connected = false

        override fun onLooperPrepared() {
            handler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        TRY_CONNECT -> {
                            device = msg.obj as BluetoothDevice
                            tryConnect()
                        }
                        WRITE_DATA -> {
                            val data = msg.obj as ByteArray
                            write(data)
                        }
                        CLOSE_SOCKET -> closeSocket()
                    }
                }
            }
            onHandlerReady(handler)
        }

        private fun tryConnect() {
            connected = try {
                socket.connect()
                val message = mainHandler.obtainMessage(CONNECT_SUCCESS)
                mainHandler.sendMessage(message)
                true
            } catch (ex: IOException) {
                val message = mainHandler.obtainMessage(CONNECT_ERROR, ex)
                mainHandler.sendMessage(message)
                false
            }
        }

        private fun write(data: ByteArray) {
            try {
                if (connected) {
                    outputStream.write(data)
                    val text  = data.toString(Charsets.UTF_8)
                    val message = mainHandler.obtainMessage(WRITE_SUCCESS, text)
                    mainHandler.sendMessage(message)
                } else {
                    val message = mainHandler.obtainMessage(WRITE_ERROR_NOT_CONNECTED)
                    mainHandler.sendMessage(message)
                }
            } catch (ex: Exception) {
                Log.e("TEST", "ConnectThread write + ${ex.message.toString()}")
                val message = mainHandler.obtainMessage(WRITE_ERROR, ex)
                mainHandler.sendMessage(message)
            }
        }

        private fun closeSocket() {
            try {
                if (socket.isConnected) {
                    socket.close()
                }
            } catch (ex: Exception) {
                Log.e("TEST", "ConnectThread write + ${ex.message.toString()}")
            } finally {
                quitSafely()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        closeSocket()
        super.onDestroy()
        thread = null
        threadHandler = null
    }

    companion object {
        private const val CONNECT_ERROR = 100
        private const val CONNECT_SUCCESS = 101
        private const val WRITE_ERROR = 102
        private const val TRY_CONNECT = 103
        private const val WRITE_DATA = 104
        private const val WRITE_ERROR_NOT_CONNECTED = 105
        private const val WRITE_SUCCESS = 106
        private const val CLOSE_SOCKET = 107

        private const val DEVICE = "device"
        private const val PIN_CODE = "pin_code"

        fun startActivity(activity: AppCompatActivity, device: BluetoothDevice, pinCode: String) {
            val intent = Intent(activity, DeviceActivity::class.java)
            intent.putExtra(DEVICE, device)
            intent.putExtra(PIN_CODE, pinCode)
            activity.startActivity(intent)
        }
    }
}