package com.example.apppurificadora.models

import com.google.gson.annotations.SerializedName

data class DataResponse(
    val message: String,
    val data: List<Int>,
    val ngrok_url: String
)
