package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var chatUserNameTv: TextView
    private lateinit var adapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private lateinit var db: AppDatabase
    private var senderId: String = ""
    private var receiverId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 1. Setup IDs
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        senderId = sharedPref.getString("current_userId", "") ?: ""
        
        receiverId = intent.getStringExtra("RECEIVER_ID") ?: ""
        val receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "Chat"

        if (senderId.isEmpty() || receiverId.isEmpty()) {
            Toast.makeText(this, "Error: User identification failed", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Init Views
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendBtn = findViewById(R.id.sendBtn)
        chatUserNameTv = findViewById(R.id.chatUserName)
        val backBtn = findViewById<ImageButton>(R.id.backBtn)

        chatUserNameTv.text = receiverName
        backBtn.setOnClickListener { finish() }

        // 3. Setup RecyclerView
        adapter = ChatAdapter(messageList, senderId)
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = adapter

        // 4. Init Room Database
        db = AppDatabase.getDatabase(this)

        // 5. Load Chat History using Flow
        lifecycleScope.launch {
            var isFirstLoad = true
            db.chatDao().getChatHistory(senderId, receiverId).collectLatest { messages ->
                val oldSize = messageList.size
                messageList.clear()
                messageList.addAll(messages)
                adapter.notifyDataSetChanged()
                
                if (messageList.isNotEmpty()) {
                    chatRecyclerView.smoothScrollToPosition(messageList.size - 1)
                    
                    // Show notification for received messages only
                    // We check if it's NOT the first load and the last message is from the other user
                    if (!isFirstLoad && messageList.size > oldSize) {
                        val lastMessage = messageList.last()
                        if (lastMessage.senderId != senderId) {
                            NotificationHelper.sendNotification(
                                this@ChatActivity,
                                "New Message from $receiverName",
                                lastMessage.messageText,
                                ChatActivity::class.java
                            )
                        }
                    }
                }
                isFirstLoad = false
            }
        }

        // 6. Send Message Logic
        sendBtn.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                val msg = ChatMessage(
                    senderId = senderId,
                    receiverId = receiverId,
                    messageText = text
                )
                
                lifecycleScope.launch {
                    db.chatDao().insertMessage(msg)
                    messageEditText.text.clear()
                    // Notification for sent message removed as requested
                }
            }
        }
    }

    class ChatAdapter(private val list: List<ChatMessage>, private val currentUserId: String) : 
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_SENT = 1
        private val VIEW_TYPE_RECEIVED = 2

        override fun getItemViewType(position: Int): Int {
            return if (list[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_SENT) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
                SentViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
                ReceivedViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = list[position]
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = timeFormat.format(Date(message.timestamp))

            if (holder is SentViewHolder) {
                holder.messageTv.text = message.messageText
                holder.timeTv.text = timeString
            } else if (holder is ReceivedViewHolder) {
                holder.messageTv.text = message.messageText
                holder.timeTv.text = timeString
            }
        }

        override fun getItemCount(): Int = list.size

        class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val messageTv: TextView = view.findViewById(R.id.messageText)
            val timeTv: TextView = view.findViewById(R.id.messageTime)
        }

        class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val messageTv: TextView = view.findViewById(R.id.messageText)
            val timeTv: TextView = view.findViewById(R.id.messageTime)
        }
    }
}
