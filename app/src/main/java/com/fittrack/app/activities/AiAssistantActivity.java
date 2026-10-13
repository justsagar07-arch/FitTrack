package com.fittrack.app.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fittrack.app.R;
import com.fittrack.app.adapters.AiMessageAdapter;
import com.fittrack.app.models.ChatMessage;
import com.fittrack.app.services.AiAssistantService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AiAssistantActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AiMessageAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText editMessage;
    private FloatingActionButton btnSend;
    private LinearLayout layoutTyping;
    private AiAssistantService aiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        initViews();
        setupChat();
        
        // Add welcome message
        addMessage(new ChatMessage("Hi there! I'm your AI Fitness Assistant. How can I help you today?", ChatMessage.TYPE_AI, System.currentTimeMillis()));
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_chat);
        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        layoutTyping = findViewById(R.id.layout_typing);

        aiService = new AiAssistantService();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupChat() {
        messageList = new ArrayList<>();
        adapter = new AiMessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        // Add user message
        addMessage(new ChatMessage(text, ChatMessage.TYPE_USER, System.currentTimeMillis()));
        editMessage.setText("");

        // Show typing indicator
        layoutTyping.setVisibility(View.VISIBLE);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);

        // Get AI response
        aiService.getResponse(text, response -> {
            layoutTyping.setVisibility(View.GONE);
            addMessage(new ChatMessage(response, ChatMessage.TYPE_AI, System.currentTimeMillis()));
        });
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ai_assistant, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_clear_chat) {
            clearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearChat() {
        messageList.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
        // Add welcome message again
        addMessage(new ChatMessage("Hi there! I'm your AI Fitness Assistant. How can I help you today?", ChatMessage.TYPE_AI, System.currentTimeMillis()));
    }
}
