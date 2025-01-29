package com.example.baseball

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.widget.Button
import android.widget.Toast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class RegisterActivity : AppCompatActivity() {
    private lateinit var teamRecyclerView: RecyclerView
    private lateinit var playerRecyclerView: RecyclerView
    private val selectedTeams = mutableSetOf<String>()
    private val selectedPlayers = mutableSetOf<String>()
    lateinit  var serverAddress :String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverAddress = getString(R.string.server_address)

        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        // Initialize RecyclerViews
        teamRecyclerView = findViewById(R.id.teamRecyclerView)
        playerRecyclerView = findViewById(R.id.playerRecyclerView)
        val user: EditText=findViewById(R.id.usernameInput)
        val password: EditText=findViewById(R.id.passwordInput)
        val email: EditText=findViewById(R.id.emailInput)

        // Sample data
        val teams = listOf(
            Team("Baltimore Orioles", R.drawable.baltimore_orioles),
            Team("Red Sox", R.drawable.red_sox),
            Team("Yankees", R.drawable.yankees),
            Team("Cincinnati Reds", R.drawable.cincinnati_reds),
            Team("Houston Astros", R.drawable.houston_astros),
            Team("Minnesota Twins", R.drawable.minnesota_twins),
            Team("Colorado Rockies", R.drawable.colorado_rockies),
            Team("Washington Nationals", R.drawable.washington_nationals)

        )
        val players = listOf(
            Player("Aaron Judge", R.drawable.aaron_judge),
            Player("Mookie Betts", R.drawable.mookie_betts),
            Player("Yordan Álvarez", R.drawable.yordan) ,
            Player("Greg Jones", R.drawable.greg_jones) ,

            Player("Kyle Stowers", R.drawable.kyle_stowers),
            Player("Jake Meyers", R.drawable.jake_meyers),
            Player("Wilyer Abreu", R.drawable.wilyer_abreu),

            Player("Trevor Larnach", R.drawable.trevor_larnach)



        )

        // Display Teams in RecyclerView
        val teamAdapter = TeamAdapter(teams) { team ->
            toggleSelection(selectedTeams, team)
        }
        teamRecyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns
        teamRecyclerView.adapter = teamAdapter

        // Display Players in RecyclerView
        val playerAdapter = PlayerAdapter(players) { player ->
            toggleSelection(selectedPlayers, player)
        }
        playerRecyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns
        playerRecyclerView.adapter = playerAdapter


        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            val username = user.text.toString().trim()
            val useremail = email.text.toString().trim()
            val userpassword = password.text.toString().trim()
            if (selectedTeams.isEmpty() || selectedPlayers.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please select at least one team and one player.",
                    Toast.LENGTH_SHORT
                ).show()
            }


            else if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username.", Toast.LENGTH_SHORT).show()

            }
            else if(useremail.isEmpty()){
                Toast.makeText(this, "Please enter a email.", Toast.LENGTH_SHORT).show()

            }
            else if(userpassword.isEmpty()){
                Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()

            }
            else {
                sendRegistrationData(username, useremail, userpassword)
            }
        }
    }


    private fun sendRegistrationData(
        username: String,
        email: String,
        password: String


    ) {
        // Create JSON object for the request body
        val jsonBody = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("preferences", JSONObject().apply { // Combine teams and players under 'preferences'
                put("teams", selectedTeams.toList()) // Convert set to list for JSON
                put("players", selectedPlayers.toList()) // Convert set to list for JSON
            })
        }


        val client = OkHttpClient()
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://$serverAddress:8000/api/auth/register/") // Use your Django server IP and port
            .post(requestBody)
            .build()

        // Use coroutine to run the network request on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    // Parse response JSON or handle it
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registered successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // Go back or move to the next activity
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration failed: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun toggleSelection(set: MutableSet<String>, item: String) {
        if (set.contains(item)) {
            set.remove(item)
        } else {
            set.add(item)
        }
    }
}






data class Team(val name: String, val imageResId: Int)
data class Player(val name: String, val imageResId: Int)



class PlayerAdapter(
    private val players: List<Player>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PlayerViewHolder {
        val itemView = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]
        holder.nameTextView.text = player.name
        holder.imageView.setImageResource(player.imageResId)

            holder.itemView.setBackgroundResource(
                if (position in selectedPositions) R.color.selected_background
                else android.R.color.white
            ) // Define this color in your colors.xml



        holder.itemView.setOnClickListener {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position) // Deselect
                onItemClick(player.name)
            } else {
                selectedPositions.add(position) // Select
                onItemClick(player.name)
            }

            notifyItemChanged(position) // Update the clicked item
        }
    }

    override fun getItemCount(): Int = players.size

    class PlayerViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.playerName)
        val imageView: ImageView = itemView.findViewById(R.id.playerImage)
    }
}


class TeamAdapter(
    private val teams: List<Team>,
    private val onItemClick: (String) -> Unit // Pass whether the item is selected or deselected
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>() // Tracks selected positions

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TeamViewHolder {
        val inflater = android.view.LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teams[position]

        with(holder) {
            nameTextView.text = team.name
            imageView.setImageResource(team.imageResId)

            // Update background based on selection
            itemView.setBackgroundResource(
                if (position in selectedPositions) R.color.selected_background
                else android.R.color.white
            )

            itemView.setOnClickListener {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position) // Deselect
                    onItemClick(team.name)
                } else {
                    selectedPositions.add(position) // Select
                    onItemClick(team.name)
                }

                notifyItemChanged(position) // Update the clicked item
            }
        }
    }

    override fun getItemCount(): Int = teams.size

    class TeamViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.teamName)
        val imageView: ImageView = itemView.findViewById(R.id.teamImage)
    }
}
