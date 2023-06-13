package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    private val requestConnectPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            checkAdapterEnabled()
        }
    }

    private val blueToothEnablesResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            checkScanPermission()
        }
    }

    private val requestScanPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            checkCoarseLocationPermission()
        }
    }

    private val requestCoarsePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            checkGpsEnabled()
        } else {
            Toast.makeText(this, "Для поиска устройств нужно предоставить доступ к местоположению", Toast.LENGTH_LONG).show()
        }
    }

    private val requestGpsEnabledLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val locationManager: LocationManager  = getSystemService(LocationManager::class.java)
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsEnabled) {
            findDevices()
        } else {
            Toast.makeText(this, "Для поиска устройств нужно предоставить доступ к местоположению", Toast.LENGTH_LONG).show()
        }
    }

    private val devicesList = mutableListOf<BluetoothDevice>()

    private lateinit var editText: EditText

    @SuppressLint("MissingPermission")
    private val deviceAdapter = DeviceAdapter() {
        val device = devicesList[it]

        val deviceName = device.name ?: "unknown"
        MaterialAlertDialogBuilder(this)
            .setTitle("Подключение")
            .setMessage("Вы действительно хотите подключить $deviceName?")
            .setNegativeButton("Нет", null)
            .setPositiveButton("Да") { dialog, which ->
                //tryConnectToDevice(device)
                getPreferences(MODE_PRIVATE).edit().putString("PIN", editText.text.toString()).apply()
                bluetoothAdapter!!.cancelDiscovery()
                DeviceActivity.startActivity(this, device, editText.text.toString())
            }
            .show()

    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            Log.e("TEST", "intent.action = ${intent.action}")
            when(intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    devicesList.clear()
                    //Toast.makeText(this@MainActivity, "ACTION_DISCOVERY_STARTED", Toast.LENGTH_LONG).show()
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    Log.e("TEST", "DEVICE NAME = $deviceName, DEVICE ADDR = $deviceHardwareAddress")
                    devicesList.add(device!!)
                    deviceAdapter.setItems(devicesList)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    //Toast.makeText(this@MainActivity, "ACTION_DISCOVERY_FINISHED", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        findViewById<Button>(R.id.find_button).setOnClickListener {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null) {
                checkConnectPermission()
            }
        }
        editText = findViewById<TextInputEditText>(R.id.edit_text)
        val pin = getPreferences(MODE_PRIVATE).getString("PIN", "1234")
        editText.setText(pin)
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }

    private fun findDevices() {
        findBlueToothDevices()
    }

    @SuppressLint("MissingPermission")
    private fun findBlueToothDevices() {
        bluetoothAdapter!!.bondedDevices.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.e("TEST", "DEVICE NAME = $deviceName, DEVICE ADDR = $deviceHardwareAddress")
        }
        val result = bluetoothAdapter!!.startDiscovery()
        Log.e("TEST", "startDiscovery result = $result")
    }

    private fun checkConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val check = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            if (check != PackageManager.PERMISSION_GRANTED) {
                requestConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                checkAdapterEnabled()
            }
        } else {
            checkAdapterEnabled()
        }
    }

    private fun checkAdapterEnabled() {
        if (bluetoothAdapter!!.isEnabled) {
            checkScanPermission()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            blueToothEnablesResult.launch(enableBtIntent)
        }
    }

    private fun checkScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val check = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            if (check != PackageManager.PERMISSION_GRANTED) {
                requestScanPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                checkCoarseLocationPermission()
            }
        } else {
            checkCoarseLocationPermission()
        }
    }

    private fun checkCoarseLocationPermission() {
        val check = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (check != PackageManager.PERMISSION_GRANTED) {
            requestCoarsePermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkGpsEnabled()
        }
    }

    private fun checkGpsEnabled() {
        val locationManager: LocationManager  = getSystemService(LocationManager::class.java)
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            requestGpsEnabledLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            findDevices()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}