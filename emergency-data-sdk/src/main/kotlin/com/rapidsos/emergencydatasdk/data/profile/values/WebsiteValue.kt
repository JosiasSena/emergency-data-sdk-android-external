package com.rapidsos.emergencydatasdk.data.profile.values

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WebsiteValue(@SerializedName("label") @Expose var label: String = "",
                        @SerializedName("url") @Expose var url: String = "",
                        @SerializedName("note") @Expose var note: String = "")