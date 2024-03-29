package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        lateinit var musicListPA: ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying:Boolean = false
        var musicService: MusicService? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding
        var repeat:Boolean = false
        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MusicPlayer)
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val coverImageBitmap = BitmapFactory.decodeResource(resources, R.drawable.cover_size)

        val dominantColor = getDominantColor(coverImageBitmap)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(dominantColor, dominantColor)
        )

        val layout = findViewById<LinearLayout>(R.id.linear_Lay)
        layout.background = gradientDrawable

        initializeLayout()
        binding.backBtnPA.setOnClickListener {
            finish()
        }
        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()
        }
        binding.previousBtnPA.setOnClickListener {
            prevNextSong(increment = false)
        }
        binding.nextBtnPA.setOnClickListener {
            prevNextSong(increment = true)
        }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.repeatBtnPA.setOnClickListener {
            if (!repeat){
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.gray))
            } else {
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.white))
            }
        }
        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult(eqIntent, 13)
            } catch (e: Exception){
                Toast.makeText(this, "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.timerBtnPA.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes"){ _, _ ->
                        min15 = false
                        min30 = false
                        min60 = false
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.white))
                    }
                    .setNegativeButton("No"){dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
            }
        }
        binding.shareBtnPA.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
        }
        binding.favouriteBtnPA.setOnClickListener {
            if (isFavourite){
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.empty_fav)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            }
            else {
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.fav)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
        }
    }

    private fun setLayout(){
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(this)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.cover).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title
        if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.gray))
        if (min15 || min30 || min60) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.gray))
        if (isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.fav)
        else binding.favouriteBtnPA.setImageResource(R.drawable.empty_fav)
    }

    private fun createMediaPlayer(){
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            musicService!!.mediaPlayer!!.start()
            isPlaying = true
            binding.playPauseBtnPA.setIconResource(R.drawable.pause_song)
            musicService!!.showNotification(R.drawable.pause_song)
            binding.tvSeekBarState.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
        } catch (e: Exception){
            return
        }
    }
    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index",0)
        when(intent.getStringExtra("class")){
            "FavouriteAdapter"-> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                setLayout()
            }
            "NowPlaying"->{
                setLayout()
                binding.tvSeekBarState.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if (isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.pause_song)
                else binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch"->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.musicListSearch)
                setLayout()
            }
            "MusicAdapter" ->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                setLayout()
            }
            "MainActivity"->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                musicListPA.shuffle()
                setLayout()
            }
            "FavouriteShuffle" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                musicListPA.shuffle()
                setLayout()
            }
            "PlaylistDetailsAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist)
                setLayout()
            }
            "PlaylistDetailsShuffle" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist)
                musicListPA.shuffle()
                setLayout()
            }
        }
    }
    private fun playMusic(){
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_song)
        musicService!!.showNotification(R.drawable.pause_song)
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
    }
    private fun pauseMusic(){
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
    }
    private fun prevNextSong(increment: Boolean){
        if (increment)
        {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        }
        else{
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.MyBinder
        musicService = binder.currentService()
        createMediaPlayer()
        musicService!!.seekBarSetup()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        try {
            setLayout()
        } catch (e:Exception){
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener{
            Toast.makeText(baseContext, "Music will stop after 15 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.gray))
            min15 = true
            Thread{Thread.sleep((15 * 60000).toLong())
            if (min15) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener{
            Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.gray))
            min30 = true
            Thread{Thread.sleep((30 * 60000).toLong())
                if (min30) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener{
            Toast.makeText(baseContext, "Music will stop after 60 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.gray))
            min60 = true
            Thread{Thread.sleep((60 * 60000).toLong())
                if (min60) exitApplication()}.start()
            dialog.dismiss()
        }
    }

    private fun getDominantColor(bitmap: Bitmap): Int {
        // Here you need to implement your own algorithm to extract the dominant color from the bitmap
        // This can be done using techniques like palette analysis or color quantization
        // For simplicity, you can use a basic approach like sampling the center pixel
        return bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
    }
}