package com.eveningoutpost.dexdrip.services.broadcastservice.models

import android.os.Parcel
import android.os.Parcelable

class Settings : Parcelable {

    var graphStart: Long = 0

    var graphEnd: Long = 0

    var apkName: String? = null

    var displayGraph: Boolean = false

    constructor() : super()

    constructor(parcel: Parcel) {
        apkName = parcel.readString()
        graphStart = parcel.readLong()
        graphEnd = parcel.readLong()
        displayGraph = parcel.readInt() == 1
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(apkName)
        parcel.writeLong(graphStart)
        parcel.writeLong(graphEnd)
        parcel.writeInt(if (displayGraph) 1 else 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Settings> = object : Parcelable.Creator<Settings> {
            override fun createFromParcel(parcel: Parcel): Settings = Settings(parcel)

            override fun newArray(size: Int): Array<Settings?> = arrayOfNulls(size)
        }
    }
}
