package com.zhongkong.app.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.zhongkong.app.R;
import com.zhongkong.app.model.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;
    private Context context;

    public DeviceAdapter(Context context, List<Device> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
    }

    public void updateData(List<Device> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        
        holder.deviceName.setText(device.getName());
        holder.phoneNumber.setText(device.getPhoneNumber());
        
        if (device.isConnected()) {
            holder.status.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.connected)));
            holder.actionButton.setText("断开");
            holder.actionButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.disconnect)));
            holder.btnProjectionScreen.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.disconnect)));
            holder.btnProjectionScreen.setClickable(true);
        } else {
            holder.status.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.disconnected)));
            holder.actionButton.setText("连接");
            holder.actionButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.connect)));
            holder.btnProjectionScreen.setClickable(false);
        }
        
        holder.actionButton.setOnClickListener(v -> {
            boolean newState = !device.isConnected();
            device.setConnected(newState);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView phoneNumber;
        ImageView status;
        Button actionButton;
        Button btnProjectionScreen;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            phoneNumber = itemView.findViewById(R.id.phone_number);
            status = itemView.findViewById(R.id.img_status);
            actionButton = itemView.findViewById(R.id.action_button);
            btnProjectionScreen = itemView.findViewById(R.id.btn_projection_screen);
        }
    }
}