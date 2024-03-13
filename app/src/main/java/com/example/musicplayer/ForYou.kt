package com.example.musicplayer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.FragmentForYouBinding
import com.example.musicplayer.databinding.FragmentNowPlayingBinding

class ForYou : Fragment() {

    private lateinit var binding: FragmentForYouBinding
    private lateinit var adapter: ForYouAdapter

    companion object{
        var favouriteSongs: ArrayList<Music> = ArrayList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_for_you, container, false)
        binding = FragmentForYouBinding.bind(view)
        favouriteSongs = checkPlaylist(favouriteSongs)
        binding.forRV.setHasFixedSize(true)
        binding.forRV.setItemViewCacheSize(13)
        binding.forRV.layoutManager = LinearLayoutManager(context)
        adapter = ForYouAdapter(requireContext(), FavouriteActivity.favouriteSongs)
        binding.forRV.adapter = adapter
        return view
    }
}