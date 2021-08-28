package com.example.emojistatus

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*

data class User(
    val displayName: String = "",
    val emojis: String = ""
)

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


class MainActivity : AppCompatActivity() {
    private companion object{
        private  const val TAG="MainActivity"
    }
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.apply {
            title="Home"    //to chane the name of the action bar

            setBackgroundDrawable(ColorDrawable(Color.parseColor("#666666")))  //To change the color of the action bar
        }


        auth = Firebase.auth    //FireBase Authentication
        // Access a Cloud Firestore instance from your Activity
        val db = Firebase.firestore
        val query = db.collection("users")
        val options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java)
            .setLifecycleOwner(this).build()
        val adapter = object: FirestoreRecyclerAdapter<User, UserViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                val view= LayoutInflater.from(this@MainActivity).inflate(android.R.layout.simple_list_item_2, parent,false)
                return UserViewHolder(view)
            }

            override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvEmojis: TextView = holder.itemView.findViewById(android.R.id.text2)
                tvName.text = model.displayName
                tvEmojis.text = model.emojis
            }

        }
        rvUsers.adapter = adapter
        rvUsers.layoutManager = LinearLayoutManager(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==R.id.miLogout){
            Log.i(TAG,"Logout")
            //user logout
            auth.signOut()
            val logoutIntent = Intent(this, Login_activity::class.java)
            logoutIntent.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)

        }else if (item.itemId == R.id.miEdit) {
            Log.i(TAG,"Show alert dialog to Edit Status")
            showAlertDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    inner class EmojiFilter : InputFilter {
        override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int):
                CharSequence {
            if(source==null || source.isBlank()){
                return ""
            }
            Log.i(TAG,"Added text $source, it has length ${source.length} characters")
            val validCharTypes = listOf(Character.SURROGATE,Character.NON_SPACING_MARK,Character.OTHER_SYMBOL).map {it.toInt() }
            for (inputChar in source) {
                val type=Character.getType(inputChar)
                Log.i(TAG,"character type $type")
                if (!validCharTypes.contains(type)) {
                    Toast.makeText(this@MainActivity,"only emojis are allowed show your creativity", Toast.LENGTH_SHORT).show()
                    return ""
                }
            }
            return source
        }


    }

    private fun showAlertDialog() {
        val editText = EditText(this)

        val emojiFilter = EmojiFilter()      //to restrict input to only emojis
        val lengthFilter = InputFilter.LengthFilter(9)  //TO RESTRICT THE EMOJIS TO LENGTH 4
        editText.filters = arrayOf(lengthFilter,emojiFilter)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Update Your Emojis")
            .setView(editText)
            .setNegativeButton("cancel", null)
            .setPositiveButton("OK", null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            Log.i(TAG, "Clicked on positive button!")
            val emojisEntered = editText.text.toString()
            if(emojisEntered.isBlank()) {
                Toast.makeText(this,"cannot submit empty text",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUser = auth.currentUser
            if(currentUser == null) {
                Toast.makeText(this,"No signed in user",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //update firestore with the new emojis
            val db = Firebase.firestore
            db.collection("users").document(currentUser.uid)
                .update("emojis", emojisEntered)
            dialog.dismiss()
        }
    }
}