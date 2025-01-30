package com.example.baseball



import android.util.Log
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import android.content.pm.ActivityInfo
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class FullscreenVideoActivity : AppCompatActivity() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var fullScreenButton: ImageButton
    private lateinit var controllerLayout : LinearLayout
    private var webSocket: WebSocket? = null
    lateinit  var serverAddress :String




    private var isPlaybarVisible = true
    private val handler = android.os.Handler()
    private val hidePlaybarTimeout = 3000L
    private lateinit var playbarRunnable: Runnable





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverAddress = getString(R.string.server_address)

        setContentView(R.layout.activity_fullscreen_video)

        supportActionBar?.hide()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playbarRunnable = Runnable {
            hidePlaybar()
        }

        // Force landscape mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val livePlayerView: com.google.android.exoplayer2.ui.PlayerView = findViewById(R.id.liveexoView)
        val videoUrl = intent.getStringExtra("videoUrl") ?: return
        val curposition: Long = intent.getLongExtra("time", 10000L)



        fullScreenButton = findViewById(R.id.fullScreenButton)
        controllerLayout = findViewById(R.id.controllerLayout)

        fullScreenButton.setOnClickListener {
            finish();
        }



        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        livePlayerView.player = exoPlayer

        livePlayerView.useController = false
        livePlayerView.visibility = View.VISIBLE


        // Load and play the video
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)

        exoPlayer.seekTo(curposition)

        exoPlayer.prepare()
        exoPlayer.play()

        connectWebSocket(serverAddress)

    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, null)

        exoPlayer.release()
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

    private fun showPlaybar() {
        if (!isPlaybarVisible) {

            controllerLayout.visibility = View.VISIBLE
            isPlaybarVisible = true
        }
        resetPlaybarTimeout()
    }

    private fun hidePlaybar() {
        if (isPlaybarVisible) {

            controllerLayout.visibility = View.GONE
            isPlaybarVisible = false
        }
    }

    private fun resetPlaybarTimeout() {
        handler.removeCallbacks(playbarRunnable)
        handler.postDelayed(playbarRunnable, hidePlaybarTimeout)
    }


    private fun connectWebSocket(laptopIp: String) {

        val sharedPreferences = getSharedPreferences("BaseballAppPrefs", MODE_PRIVATE)

        // Retrieve preferences from SharedPreferences
        val langcode = sharedPreferences.getString("lang", "en")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://$laptopIp:8001/ws/updates/")
            .addHeader("lang", langcode?:"en")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    Toast.makeText(this@FullscreenVideoActivity, "WebSocket connected!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    try {
                        val data = JSONObject(text)
                        val message = data.getJSONObject("message") // Extract the message object

                        // Extract individual fields from the message
                        val nextEvent = message.getString("next_event")
                        val description = message.getString("description")
                        val selectedEvents = message.getJSONArray("selected_events")
                        val selectedEventsList = mutableListOf<String>()
                        for (i in 0 until selectedEvents.length()) {
                            val event = selectedEvents.getString(i)
                            selectedEventsList.add(event)
                            Log.d("SelectedEvent", "Event $i: $event")
                        }


                        val dialog = OptionsDialogFragment.newInstance(selectedEventsList,description,nextEvent)
                        dialog.show(supportFragmentManager, "OptionsDialog")
                        // Example: Updating a TextView with one of the extracted values

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }


            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                runOnUiThread {
                    Toast.makeText(this@FullscreenVideoActivity, "WebSocket error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    Toast.makeText(this@FullscreenVideoActivity, "WebSocket disconnected", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


}
