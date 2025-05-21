package com.example.apppurificadora.models

data class NgrokResponse(
    val message: String,
    val data: List<Int>,
    val ngrok_url: String
)