package com.example.musicplayer

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("items/songs")
    fun getSongs(): Call<List<Song>>
}

data class Song(
    val id: Int,
    val title: String,
    val album: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val artUri: String
)
