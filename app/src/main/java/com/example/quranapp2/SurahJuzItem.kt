package com.example.quranapp2

import android.os.Parcel
import android.os.Parcelable

data class SurahJuzItem(
    val number: Int,
    val name: String?,
    val subtext: String?,
    val page: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(number)
        parcel.writeString(name)
        parcel.writeString(subtext)
        parcel.writeInt(page)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SurahJuzItem> {
        override fun createFromParcel(parcel: Parcel): SurahJuzItem {
            return SurahJuzItem(parcel)
        }

        override fun newArray(size: Int): Array<SurahJuzItem?> {
            return arrayOfNulls(size)
        }
    }
}