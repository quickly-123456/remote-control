package com.zhongkong.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Device implements Parcelable {
    private String name;
    private String phoneNumber;
    private boolean connected;

    public Device(String name, String phoneNumber, boolean connected) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.connected = connected;
    }

    protected Device(Parcel in) {
        this.name = in.readString();;
        this.phoneNumber = in.readString();;
        this.connected = in.readByte() != 0;
    }

    // 创建 Creator
    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phoneNumber);
        dest.writeByte((byte) (isConnected() ? 1 : 0));
    }
}