package com.eveningoutpost.dexdrip.services.broadcastservice.models

import android.os.Parcel
import android.os.Parcelable

class GraphPoint : Parcelable {

    var x: Float = 0f

    var y: Float = 0f

    constructor()

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    constructor(parcel: Parcel) {
        x = parcel.readFloat()
        y = parcel.readFloat()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(x)
        parcel.writeFloat(y)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GraphPoint> = object : Parcelable.Creator<GraphPoint> {
            override fun createFromParcel(parcel: Parcel): GraphPoint = GraphPoint(parcel)

            override fun newArray(size: Int): Array<GraphPoint?> = arrayOfNulls(size)
        }
    }
}
