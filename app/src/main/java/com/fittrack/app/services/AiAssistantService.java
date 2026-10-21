
package com.fittrack.app.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fittrack.app.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiAssistantService {

    private static final String TAG = "AiAssistantService";
    private static final String API_URL =
            "https://fittrack-backend-ivg0.onrender.com/api/ai/chat";
    private static final int MAX_MESSAGES = 100;

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<JSONObject> history = new ArrayList<>();
    private boolean requestInProgress = false;

    public interface AiResponseCallback {
        void onResponse(String response);
    }

    public interface ConversationLoadCallback {
        void onLoaded(List<ChatMessage> messages);
        void onError(Exception error);
    }

    private com.google.firebase.firestore.CollectionReference chatCollection() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return null;
        }

        return db.collection("users")
                .document(user.getUid())
                .collection("ai_chat_messages");
    }

    public void loadConversation(ConversationLoadCallback callback) {
        com.google.firebase.firestore.CollectionReference collection =
                chatCollection();

        if (collection == null) {
            callback.onError(new Exception("Please log in again."));
            return;
        }

        collection.orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(MAX_MESSAGES)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ChatMessage> loaded = new ArrayList<>();
                    List<JSONObject> restoredHistory = new ArrayList<>();

                    try {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String text = doc.getString("text");
                            Long sender = doc.getLong("senderType");
                            Long time = doc.getLong("timestamp");

                            if (text == null || sender == null || time == null) {
                                continue;
                            }

                            int senderType = sender.intValue();

                            if (senderType != ChatMessage.TYPE_USER
                                    && senderType != ChatMessage.TYPE_AI) {
                                continue;
                            }

                            loaded.add(new ChatMessage(text, senderType, time));

                            JSONObject item = new JSONObject();
                            item.put("role",
                                    senderType == ChatMessage.TYPE_USER
                                            ? "user" : "assistant");
                            item.put("content", text);
                            restoredHistory.add(item);
                        }

                        synchronized (this) {
                            history.clear();
                            history.addAll(restoredHistory);
                            trimHistory();
                        }

                        callback.onLoaded(loaded);

                    } catch (Exception e) {
                        Log.e(TAG, "Could not restore chat history", e);
                        callback.onError(e);
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Firestore history load failed", error);
                    callback.onError(error);
                });
    }

    public synchronized void clearConversation() {
        history.clear();

        com.google.firebase.firestore.CollectionReference collection =
                chatCollection();

        if (collection == null) {
            return;
        }

        collection.get().addOnSuccessListener(snapshot -> {
            com.google.firebase.firestore.WriteBatch batch = db.batch();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                batch.delete(doc.getReference());
            }

            batch.commit()
                    .addOnFailureListener(error ->
                            Log.e(TAG, "Could not clear saved chat", error));
        }).addOnFailureListener(error ->
                Log.e(TAG, "Could not load chat for clearing", error));
    }

    public void getResponse(String userInput, AiResponseCallback callback) {
        final JSONArray messages;
        final long userTimestamp = System.currentTimeMillis();

        synchronized (this) {
            if (requestInProgress) {
                deliver(callback,
                        "Please wait for the current answer to finish.");
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                deliver(callback, "Please log in to use FitTrack AI.");
                return;
            }

            try {
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", userInput.trim());

                history.add(userMessage);
                trimHistory();

                messages = new JSONArray();

                for (JSONObject item : history) {
                    messages.put(new JSONObject(item.toString()));
                }

                requestInProgress = true;

            } catch (Exception e) {
                Log.e(TAG, "Could not prepare conversation", e);
                deliver(callback, "Could not prepare your message.");
                return;
            }

            user.getIdToken(false)
                    .addOnSuccessListener(tokenResult -> {
                        String token = tokenResult.getToken();

                        if (token == null || token.isEmpty()) {
                            rollbackUserMessage(userInput);
                            finishRequest();
                            deliver(callback,
                                    "Authentication failed. Please log in again.");
                            return;
                        }

                        sendMessage(messages, token, userInput,
                                userTimestamp, callback);
                    })
                    .addOnFailureListener(error -> {
                        Log.e(TAG, "Firebase token error", error);
                        rollbackUserMessage(userInput);
                        finishRequest();
                        deliver(callback,
                                "Authentication failed. Please try again.");
                    });
        }
    }

    private void sendMessage(
            JSONArray messages,
            String token,
            String userInput,
            long userTimestamp,
            AiResponseCallback callback) {

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("messages", messages);

            RequestBody body = RequestBody.create(
                    requestJson.toString(), JSON);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + token)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "AI network error", e);
                    rollbackUserMessage(userInput);
                    finishRequest();
                    deliver(callback,
                            "Unable to connect to FitTrack AI. Please try again.");
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (Response res = response) {
                        String bodyText = res.body() != null
                                ? res.body().string() : "";

                        if (!res.isSuccessful()) {
                            Log.e(TAG, "AI HTTP " + res.code() + ": " + bodyText);
                            rollbackUserMessage(userInput);
                            finishRequest();
                            deliver(callback,
                                    "FitTrack AI could not respond. Please try again.");
                            return;
                        }

                        JSONObject json = new JSONObject(bodyText);
                        String reply = json.optString("reply", "").trim();

                        if (reply.isEmpty()) {
                            rollbackUserMessage(userInput);
                            finishRequest();
                            deliver(callback,
                                    "AI returned an empty response. Please try again.");
                            return;
                        }

                        long assistantTimestamp = System.currentTimeMillis();

                        synchronized (AiAssistantService.this) {
                            JSONObject assistantMessage = new JSONObject();
                            assistantMessage.put("role", "assistant");
                            assistantMessage.put("content", reply);
                            history.add(assistantMessage);
                            trimHistory();
                            requestInProgress = false;
                        }

                        saveMessage(userInput, ChatMessage.TYPE_USER, userTimestamp);
                        saveMessage(reply, ChatMessage.TYPE_AI, assistantTimestamp);

                        deliver(callback, reply);

                    } catch (Exception e) {
                        Log.e(TAG, "AI response parsing error", e);
                        rollbackUserMessage(userInput);
                        finishRequest();
                        deliver(callback,
                                "Could not read the AI response. Please try again.");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Could not create AI request", e);
            rollbackUserMessage(userInput);
            finishRequest();
            deliver(callback, "Unable to send your message.");
        }
    }

    private void saveMessage(String text, int senderType, long timestamp) {
        com.google.firebase.firestore.CollectionReference collection =
                chatCollection();

        if (collection == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("senderType", senderType);
        data.put("timestamp", timestamp);

        collection.add(data).addOnFailureListener(error ->
                Log.e(TAG, "Could not save chat message", error));
    }

    private synchronized void rollbackUserMessage(String userInput) {
        if (!history.isEmpty()) {
            JSONObject last = history.get(history.size() - 1);

            if ("user".equals(last.optString("role"))
                    && userInput.trim().equals(last.optString("content"))) {
                history.remove(history.size() - 1);
            }
        }
    }

    private synchronized void trimHistory() {
        while (history.size() > MAX_MESSAGES) {
            history.remove(0);
        }

        while (!history.isEmpty()
                && !"user".equals(history.get(0).optString("role"))) {
            history.remove(0);
        }
    }

    private synchronized void finishRequest() {
        requestInProgress = false;
    }

    private void deliver(AiResponseCallback callback, String message) {
        mainHandler.post(() -> callback.onResponse(message));
    }
}
