package com.example.howareyou.model

import com.google.gson.annotations.SerializedName

data class PostingDTO(
    @SerializedName("email") val email: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("author") val author: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("header") val header: String,
    @SerializedName("code") val code: String
    //@SerializedName("image") val image: List<Image>
)

