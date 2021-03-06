package com.rapidsos.emergencydatasdk.data.profile

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

data class Medication(@SerializedName("value") @Expose var value: List<String> = ArrayList(),
                      @SerializedName("type") @Expose var type: String = "",
                      @SerializedName("display_name") @Expose var displayName: String = "")