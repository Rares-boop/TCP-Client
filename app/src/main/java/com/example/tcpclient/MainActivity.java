package com.example.tcpclient;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.List;

import chat.GroupChat;
import chat.GroupMember;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ConversationAdapter adapter;
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


        adapter = new ConversationAdapter(
                this,
                LocalStorage.getCurrentUserGroupChats(),
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

                //out.writeObject("OK");

                /*response = in.readObject();
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
                }*/
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

        adapter.setEnabled(false);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_with_spinner, null);

        EditText editGroupName = dialogView.findViewById(R.id.editGroupName);
        Spinner spinner = dialogView.findViewById(R.id.mySpinner);

        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Loading...", "Please wait"}
        );
        spinner.setAdapter(initialAdapter);

        List<Integer> userIds = new ArrayList<>();
        List<String> userNames = new ArrayList<>();

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Add a new conversation")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, w) -> d.cancel())
                .setPositiveButton("OK", (d, w) -> {

                    String groupName = editGroupName.getText().toString().trim();
                    if (groupName.isEmpty()) {
                        Toast.makeText(this, "Enter a group name!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int index = spinner.getSelectedItemPosition();
                    if (index < 0 || index >= userIds.size()) {
                        Toast.makeText(this, "No user selected!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedUserId = userIds.get(index);

                    new Thread(() -> {
                        try {
                            ObjectOutputStream out = TcpConnection.getOut();
                            ObjectInputStream in = TcpConnection.getIn();

                            String payload = selectedUserId + "," + groupName;
                            out.writeObject(payload);
                            out.flush();

                            Object response = in.readObject();

                            runOnUiThread(() -> {
                                if (response instanceof GroupChat) {
                                    GroupChat newGroup = (GroupChat) response;

                                    List<GroupChat> updatedList = LocalStorage.getCurrentUserGroupChats();
                                    updatedList.add(newGroup);
                                    LocalStorage.setCurrentUserGroupChats(updatedList);

                                    adapter.setGroupChats(updatedList);
                                    adapter.notifyDataSetChanged();

                                    adapter.setEnabled(true);

                                    Toast.makeText(this, "Created new group", Toast.LENGTH_SHORT).show();
                                } else {
                                    adapter.setEnabled(true);
                                    Toast.makeText(this, "Group already exists", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();

                }).create();

        dialog.show();

        new Thread(() -> {
            try {
                ObjectOutputStream out = TcpConnection.getOut();
                ObjectInputStream in = TcpConnection.getIn();

                out.writeObject("ADD_CONVERSATION");
                out.flush();

                List<String> listFromServer = (List<String>) in.readObject();

                userIds.clear();
                userNames.clear();
                for (String s : listFromServer) {
                    String[] parts = s.split(",");
                    userIds.add(Integer.parseInt(parts[0]));
                    userNames.add(parts[1]);
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> realAdapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            userNames
                    );
                    spinner.setAdapter(realAdapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
