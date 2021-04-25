package plugins.parcelize

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize class ParcelableObject(val name: String) : Parcelable
