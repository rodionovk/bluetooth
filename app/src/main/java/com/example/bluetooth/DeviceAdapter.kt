package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val onClick: (Int) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<BluetoothDevice>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<BluetoothDevice>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DeviceHolder) {
            holder.bind(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    private class DeviceHolder(view: View, onClick: (Int) -> Unit) : RecyclerView.ViewHolder(view) {

        private val name = itemView.findViewById<TextView>(R.id.name)
        private val address = itemView.findViewById<TextView>(R.id.address)

        init {
            itemView.setOnClickListener {
                onClick(adapterPosition)
            }
        }

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice) {
            name.text = device.name ?: "unknown"
            address.text = device.address
        }
    }
}