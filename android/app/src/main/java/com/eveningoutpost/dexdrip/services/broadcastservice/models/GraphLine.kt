package com.eveningoutpost.dexdrip.services.broadcastservice.models

import android.os.Parcel
import android.os.Parcelable

class GraphLine : Parcelable {

    var values: MutableList<GraphPoint> = mutableListOf()

    var color: Int = 0

    constructor()

    constructor(parcel: Parcel) {
        values = parcel.readArrayList(GraphPoint::class.java.classLoader) as? MutableList<GraphPoint>
            ?: mutableListOf()
        color = parcel.readInt()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeList(values)
        parcel.writeInt(color)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GraphLine> = object : Parcelable.Creator<GraphLine> {
            override fun createFromParcel(parcel: Parcel): GraphLine = GraphLine(parcel)

            override fun newArray(size: Int): Array<GraphLine?> = arrayOfNulls(size)
        }
    }
}
