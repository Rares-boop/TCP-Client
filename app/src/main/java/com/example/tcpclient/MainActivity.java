package com.example.tcpclient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import chat.GroupChat;
import chat.GroupMember;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ConversationAdapter adapter;
    List<GroupChat> groupChats;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerViewConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupChats = LocalStorage.getCurrentUserGroupChats();

        adapter = new ConversationAdapter(
                this,
                groupChats,
                chat -> handleChatClick(chat)
        );

        recyclerView.setAdapter(adapter);

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onStart() {
        super.onStart();
        new Thread(()->{
            Socket socket = TcpConnection.getSocket();
            ObjectOutputStream out = TcpConnection.getOut();
            ObjectInputStream in = TcpConnection.getIn();
            try{
                out.writeObject("GET_CONVERSATIONS");
                out.flush();

                Object response = in.readObject();

                if (response instanceof List) {
                    List<?> list = (List<?>) response;
                    if (!list.isEmpty() && list.get(0) instanceof GroupChat) {
                        @SuppressWarnings("unchecked")
                        List<GroupChat> groupChats = (List<GroupChat>) list;
                        LocalStorage.setCurrentUserGroupChats(groupChats);
                        //System.out.println("Received group chats: " + groupChats.size());
                        runOnUiThread(()->{
                            Toast.makeText(this, "Chats: " + groupChats.size(), Toast.LENGTH_SHORT).show();
                            adapter.setGroupChats(groupChats); // update adapter's data
                            adapter.notifyDataSetChanged();
                        });
                    }
                }

                out.writeObject("OK");

                response = in.readObject();
                if (response instanceof List) {
                    List<?> list = (List<?>) response;
                    if (!list.isEmpty() && list.get(0) instanceof GroupMember) {
                        @SuppressWarnings("unchecked")
                        List<GroupMember> groupMembers = (List<GroupMember>) list;
                        LocalStorage.setCurrentUserGroupMembers(groupMembers);
                        //System.out.println("Received group members: " + groupMembers.size());
                        runOnUiThread(()->{
                            Toast.makeText(this,"Members " + groupMembers.size(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void handleChatClick(GroupChat chat) {
        new Thread(() -> {
            try {
                Socket socket = TcpConnection.getSocket();
                ObjectOutputStream out = TcpConnection.getOut();
                ObjectInputStream in = TcpConnection.getIn();

                String request = "GET_CONVERSATION," + chat.getId();
                Log.e("#1234", request);

                out.writeObject(request);
                out.flush();

                Object response = in.readObject();
                Log.e("#1234", response.toString());
                String[] parts = ((String) response).split(",");

                runOnUiThread(() -> {
                    if (parts[0].equalsIgnoreCase("NOT_FOUND")) {
                        Toast.makeText(this, parts[1] + " group not found", Toast.LENGTH_SHORT).show();
                    } else if (parts[0].equalsIgnoreCase("GROUP_NOT_ALLOWED")) {
                        Toast.makeText(this, parts[1] + " group not allowed", Toast.LENGTH_SHORT).show();
                    } else if (parts[0].equalsIgnoreCase("OK")) {
                        Intent intent = new Intent(this, ConversationActivity.class);
                        startActivity(intent);
                    }
                });

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    public void handleAddConversation(View view) {
        Toast.makeText(MainActivity.this, "Add conversation logic...",Toast.LENGTH_SHORT).show();
    }
}