package com.example.apppurificadora.models

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("name") val name: String,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("current_password") val currentPassword: String?,
    @SerializedName("new_password") val newPassword: String?,
    @SerializedName("new_password_confirmation") val confirmPassword: String?
)
