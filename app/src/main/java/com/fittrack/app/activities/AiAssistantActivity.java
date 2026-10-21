
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

    private boolean historyLoaded = false;

    private static final String WELCOME_MESSAGE =
            "Hi there! I'm your AI Fitness Assistant. How can I help you today?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        initViews();
        setupChat();
        loadSavedConversation();
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

        btnSend.setEnabled(false);
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupChat() {
        messageList = new ArrayList<>();
        adapter = new AiMessageAdapter(messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadSavedConversation() {
        layoutTyping.setVisibility(View.VISIBLE);

        aiService.loadConversation(new AiAssistantService.ConversationLoadCallback() {
            @Override
            public void onLoaded(List<ChatMessage> messages) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                messageList.clear();
                messageList.addAll(messages);

                if (messageList.isEmpty()) {
                    addMessage(new ChatMessage(
                            WELCOME_MESSAGE,
                            ChatMessage.TYPE_AI,
                            System.currentTimeMillis()
                    ));
                } else {
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }

                historyLoaded = true;
                btnSend.setEnabled(true);
                layoutTyping.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                android.util.Log.e(
                        "AiAssistantActivity",
                        "Could not load saved conversation",
                        error
                );

                if (messageList.isEmpty()) {
                    addMessage(new ChatMessage(
                            WELCOME_MESSAGE,
                            ChatMessage.TYPE_AI,
                            System.currentTimeMillis()
                    ));
                }

                historyLoaded = true;
                btnSend.setEnabled(true);
                layoutTyping.setVisibility(View.GONE);

                Toast.makeText(
                        AiAssistantActivity.this,
                        "Couldn't load saved chat. Check your connection.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void sendMessage() {
        if (!historyLoaded) {
            Toast.makeText(this,
                    "Please wait for chat history to load.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String text = editMessage.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        addMessage(new ChatMessage(
                text,
                ChatMessage.TYPE_USER,
                System.currentTimeMillis()
        ));

        editMessage.setText("");
        btnSend.setEnabled(false);
        layoutTyping.setVisibility(View.VISIBLE);

        aiService.getResponse(text, response -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            layoutTyping.setVisibility(View.GONE);

            addMessage(new ChatMessage(
                    response,
                    ChatMessage.TYPE_AI,
                    System.currentTimeMillis()
            ));

            btnSend.setEnabled(true);
        });
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);

        recyclerView.post(() ->
                recyclerView.smoothScrollToPosition(messageList.size() - 1));
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
        aiService.clearConversation();

        messageList.clear();
        adapter.notifyDataSetChanged();

        addMessage(new ChatMessage(
                WELCOME_MESSAGE,
                ChatMessage.TYPE_AI,
                System.currentTimeMillis()
        ));

        Toast.makeText(
                this,
                "Chat history cleared",
                Toast.LENGTH_SHORT
        ).show();
    }
}
