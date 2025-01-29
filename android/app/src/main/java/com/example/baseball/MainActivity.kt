package com.example.baseball

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import org.json.JSONException





class MainActivity : AppCompatActivity() {
    private lateinit var videoRecyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private val videoList = mutableListOf<Video>()
    lateinit  var serverAddress :String
    private lateinit var exoPlayer: ExoPlayer

    var counter: Int =0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

         serverAddress=getString(R.string.server_address)

        exoPlayer = ExoPlayer.Builder(this).build()



        // Check login status
        val sharedPreferences = getSharedPreferences("BaseballAppPrefs", MODE_PRIVATE)
        if (!sharedPreferences.getBoolean("isLoggedIn", false)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity
            return
        }


        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val logout: Button = findViewById(R.id.logout)

        logout.setOnClickListener {
            // Clear user data from SharedPreferences
            sharedPreferences.edit()
                .clear() // Clears all data stored in SharedPreferences
                .apply()

            // Redirect the user to the LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finish the current activity to prevent going back to it
        }


        // Initialize RecyclerView
        videoRecyclerView = findViewById(R.id.videoList)
        videoRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load video data (populate videoList)
        loadVideoData()

        // Set up adapter
        adapter = VideoAdapter(videoList) { video ->
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("videoUrl", video.url)
            startActivity(intent)
        }
        videoRecyclerView.adapter = adapter

        // Initialize ExoPlayer
        setupExoPlayer()



        val recyclerView = findViewById<RecyclerView>(R.id.videoList)
        val progressBar = findViewById<ProgressBar>(R.id.loadingProgressBar)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Check if the RecyclerView is scrolled to the bottom
                if (!recyclerView.canScrollVertically(1)) {
                    if(counter>8){
                        return
                    }
                    else{
                        counter+=3
                    }
                    // RecyclerView reached the bottom
                    progressBar.visibility = View.VISIBLE

                    loadVideoData()

                }
            }
        })

    }

    private fun setupExoPlayer() {
        // Find PlayerView in the layout
        val livePlayerView: com.google.android.exoplayer2.ui.PlayerView = findViewById(R.id.livePlayerView)

        // Create an instance of ExoPlayer

        // Attach ExoPlayer to the PlayerView
        livePlayerView.player = exoPlayer
        livePlayerView.useController = false // Disable player controls (optional)

        // Create a MediaItem for the live video
        val liveVideoUrl = "http://$serverAddress:8000/api/video/1/" // Replace with your live video URL
        val mediaItem = MediaItem.fromUri(liveVideoUrl)

        // Set the MediaItem to ExoPlayer
        exoPlayer.setMediaItem(mediaItem)

        // Mute the live video by default
        exoPlayer.volume = 0f

        exoPlayer.seekTo(759000)

        // Add a listener for playback state changes
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Video is ready to play
                        livePlayerView.visibility = View.VISIBLE
                    }
                    Player.STATE_BUFFERING -> {
                        // Video is buffering
                        livePlayerView.visibility = View.VISIBLE
                    }
                    Player.STATE_ENDED -> {
                        // Video playback ended
                    }
                    Player.STATE_IDLE -> {
                        // ExoPlayer is idle
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                // Handle playback errors
                Log.e("ExoPlayer", "Error: ${error.message}")
            }
        })

        // Handle livePlayerView click for fullscreen and sound toggle
        livePlayerView.setOnClickListener {
            val currentPosition: Long = exoPlayer.getCurrentPosition()
            val intent = Intent(this, FullscreenVideoActivity::class.java)
            val liveVideoUrl = "http://$serverAddress:8000/api/video/1/" // Live video URL from server
          // Optional thumbnail URL

            // Pass the live video URL and thumbnail URL to the new activity
            intent.putExtra("videoUrl", liveVideoUrl)
            intent.putExtra("time", currentPosition)


            // Start the VideoPlayerActivity
            startActivity(intent)
        }

        // Prepare and start playback
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }



    override fun onDestroy() {
        super.onDestroy()
        // Release ExoPlayer resources
        exoPlayer.release()
    }

    private fun generateThumbnail(videoUrl: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoUrl, HashMap())
            retriever.getFrameAtTime(1000000) // Frame at 1 second
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun loadVideoData() {

        val serverUrl = "http://$serverAddress:8000/recommendation/homerun-urls/" // Ensure trailing slash
        val sharedPreferences = getSharedPreferences("BaseballAppPrefs", MODE_PRIVATE)

        // Retrieve preferences from SharedPreferences
        val preferences = sharedPreferences.getString("preferences", "")

// Build the form body and include preferences in the query
        val formBody = FormBody.Builder()
            .add("query", "your search description") // Replace with the actual query
            .add("preferences", preferences ?: "")  // Add preferences from SharedPreferences
            .add("top_n", "3") // Number of top matches to retrieve
            .build()


        val request = Request.Builder()
            .url(serverUrl)
            .post(formBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("loadVideoData", "Failed to connect to server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        try {
                            // Parse the response as a JSONObject
                            val jsonObject = JSONObject(responseBody)

                            // Extract "top_matches" JSONArray
                            val topMatches = jsonObject.getJSONArray("top_matches")


                            // Iterate through the array
                            for (i in 0 until topMatches.length()) {
                                val match = topMatches.getJSONObject(i)

                                val description = match.getString("description")

                                val url = match.getString("url")
                                val thumbnail = generateThumbnail(url)

//
//                                // Add video with thumbnail to the list
                                videoList.add(Video(title = description, url = url, thumbnail = thumbnail))
                                Log.e("loadVideoData", "done")

                            }
                            runOnUiThread {
                                adapter.notifyDataSetChanged()
                                val progressBar = findViewById<ProgressBar>(R.id.loadingProgressBar)
                                videoRecyclerView.adapter?.let { adapter ->
                                    if (adapter.itemCount > 0) {
                                        videoRecyclerView.scrollToPosition(adapter.itemCount - 2)
                                    }
                                }

                                progressBar.visibility = View.GONE
                            }

                            // Update UI (run on main thread)

                        } catch (e: JSONException) {
                            Log.e("loadVideoData", "Error parsing video data: ${e.message}")
                        }
                    }
                } else {
                    Log.e("loadVideoData", "Server returned an error: ${response.code}")
                }
            }
        })
    }







}
