package com.example.baseball

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.MotionEvent

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var playPauseButton: ImageButton
    private lateinit var videoSeekBar: SeekBar
    private lateinit var elapsedTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var fullScreenButton: ImageButton
    private lateinit var skipForwardButton: ImageButton
    private lateinit var skipRewindButton: ImageButton
    private lateinit var videopauseplay : LinearLayout
    private lateinit var controllerLayout : LinearLayout
    private lateinit var videofullscreen : LinearLayout

    private var isPlaying = true
    private lateinit var mediaController: MediaController

    private val hidePlaybarTimeout = 3000L // 3 seconds
    private val handler = android.os.Handler()
    private lateinit var playbarRunnable: Runnable
    private var isPlaybarVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        supportActionBar?.hide()

        // Keep the screen on while video is playing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initializeUI()
        handleVideoPlayback()
        adjustUIForOrientation(resources.configuration.orientation)

        playbarRunnable = Runnable {
            hidePlaybar()
        }

    }

    private fun initializeUI() {



        videoView = findViewById(R.id.videoView)
        playPauseButton = findViewById(R.id.playPauseButton)
        videoSeekBar = findViewById(R.id.videoSeekBar)
        elapsedTime = findViewById(R.id.elapsedTime)
        totalTime = findViewById(R.id.totalTime)
        fullScreenButton = findViewById(R.id.fullScreenButton)
        skipForwardButton = findViewById(R.id.skipForwardButton)
        skipRewindButton = findViewById(R.id.skipRewindButton)
        videopauseplay = findViewById(R.id.videopauseplay_layout)
        controllerLayout = findViewById(R.id.controllerLayout)
        videofullscreen = findViewById(R.id.videofullscreen_layout)

    }

    private fun showPlaybar() {
        if (!isPlaybarVisible) {
            videopauseplay.visibility = View.VISIBLE
            controllerLayout.visibility = View.VISIBLE
            videofullscreen.visibility = View.VISIBLE
            isPlaybarVisible = true
        }
        resetPlaybarTimeout()
    }

    private fun hidePlaybar() {
        if (isPlaybarVisible) {
            videopauseplay.visibility = View.GONE
            controllerLayout.visibility = View.GONE
            videofullscreen.visibility = View.GONE
            isPlaybarVisible = false
        }
    }

    private fun resetPlaybarTimeout() {
        handler.removeCallbacks(playbarRunnable)
        handler.postDelayed(playbarRunnable, hidePlaybarTimeout)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (isPlaybarVisible){
                hidePlaybar()
                return super.onTouchEvent(event)
            }
            showPlaybar()
        }
        return super.onTouchEvent(event)
    }


    private fun handleVideoPlayback() {
        val videoUrl = intent.getStringExtra("videoUrl") ?: ""
        videoView.setVideoPath(videoUrl)

        mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        videoView.setOnPreparedListener { mediaPlayer ->
            videoSeekBar.max = mediaPlayer.duration
            totalTime.text = formatTime(mediaPlayer.duration)

            videoView.start()
            updateSeekBar()
            resetPlaybarTimeout() // Start the timeout
        }


        videoView.setOnCompletionListener {
            playPauseButton.setImageResource(R.drawable.ic_play)
            isPlaying = false
        }

        playPauseButton.setOnClickListener {
            if (isPlaying) {
                videoView.pause()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                videoView.start()
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
            isPlaying = !isPlaying
        }

        fullScreenButton.setOnClickListener {
            toggleFullScreen()
        }

        skipForwardButton.setOnClickListener {
            skip(10000) // Skip forward 10 seconds
        }

        skipRewindButton.setOnClickListener {
            skip(-10000) // Rewind 10 seconds
        }

        videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    elapsedTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateSeekBar() {
        videoSeekBar.progress = videoView.currentPosition
        elapsedTime.text = formatTime(videoView.currentPosition)
        videoSeekBar.postDelayed({ updateSeekBar() }, 500)
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = milliseconds / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun toggleFullScreen() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        resetPlaybarTimeout()
    }

    private fun adjustUIForOrientation(orientation: Int) {
        // Get display metrics
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Define constant height for buttons
        var buttonHeight = (screenHeight * 0.12f).toDouble()
        var margin=40.21
        // Adjust margins based on orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            margin=(screenWidth * 0.025f).toDouble()
            buttonHeight = (screenHeight * 0.055f).toDouble()


        // 5% of screen width for margins in portrait
        } else {
             margin=(screenHeight * 0.3f).toDouble()
        // 5% of screen height for margins in landscape
        }

        // Update button dimensions and margins
        updateButtonSizeWithMargin(videopauseplay, buttonHeight, margin*(1.5),margin)

//        updateButtonSizeWithMargin(videoplaybutton, buttonHeight, margin,margin)

        updateButtonSizeWithMargin(videofullscreen, buttonHeight, margin, 0.0) // No right margin

        // Handle fullscreen mode
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun updateButtonSizeWithMargin(
        button: View,
        height: Double,
        marginLeft: Double,
        marginRight: Double
    ) {
        val layoutParams = button.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.height = height.toInt()
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams.leftMargin = marginLeft.toInt()
        layoutParams.rightMargin = marginRight.toInt()
        button.layoutParams = layoutParams
    }

    private fun skip(milliseconds: Int) {
        val currentPosition = videoView.currentPosition
        val targetPosition = currentPosition + milliseconds
        videoView.seekTo(targetPosition)
        videoSeekBar.progress = targetPosition
        elapsedTime.text = formatTime(targetPosition)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("ConfigurationChange", "Orientation changed to ${newConfig.orientation}")
        adjustUIForOrientation(newConfig.orientation)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(playbarRunnable)
    }

}
