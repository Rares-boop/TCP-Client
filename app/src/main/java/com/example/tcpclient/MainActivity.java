package com.example.tcpclient;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

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

    AlertDialog dialog;
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
                chat -> handleChatClick(chat),
                chat -> handleLongChatClick(chat)
        );

        recyclerView.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedCallback callback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    if (dialog != null && dialog.isShowing()) {
                        Toast.makeText(MainActivity.this, "Finalizează acțiunea înainte de a ieși!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    handleLogout();
                }
            };

            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    callback
            );
        }

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

    @SuppressLint({"GestureBackNavigation", "MissingSuperCall"})
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (dialog != null && dialog.isShowing()) {
            Toast.makeText(this, "Finalizează acțiunea înainte de a ieși!", Toast.LENGTH_SHORT).show();
            return;
        }
        handleLogout();
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

    public void handleLongChatClick(GroupChat chat){
        //Toast.makeText(this, chat.toString(), Toast.LENGTH_SHORT).show();
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(chat.getName())
                .setItems(new String[]{"Rename ", "Delete "},(dialog, which)->{
                    if(which == 0){
                        renameChat(chat);
                    }
                    else{
                        deleteChat(chat);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        alertDialog.show();
    }

    public void renameChat(GroupChat chat){
        //Toast.makeText(this, String.valueOf(chat.getName()), Toast.LENGTH_SHORT).show();
        EditText input = new EditText(this);
        input.setHint("Enter the new name ");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Rename "+chat.getName())
                .setView(input)
                .setPositiveButton("Save ",(dialog, which)->{
                    String newName = input.getText().toString().trim();
                    if(!newName.isEmpty()){
                        new Thread(()->{

                            Socket socket = null;
                            ObjectOutputStream out = null;
                            ObjectInputStream in = null;
                            try{
                                socket = TcpConnection.getSocket();
                                out = TcpConnection.getOut();
                                in = TcpConnection.getIn();

                                String request = "UPDATE" +","+chat.getId() + ","+newName;

                                out.writeObject(request);
                                out.flush();

                                String response = (String)in.readObject();

                                if(response.equalsIgnoreCase("OK")){
                                    List<GroupChat> currentGroupChats = LocalStorage.getCurrentUserGroupChats();
                                    currentGroupChats.stream()
                                            .filter(c -> c.getId() == chat.getId())
                                            .findFirst()
                                            .ifPresent(c -> c.setName(newName));

                                    LocalStorage.setCurrentUserGroupChats(currentGroupChats);

                                    //update adapter
                                    runOnUiThread(()->adapter.notifyDataSetChanged());
                                }
                                else if(response.equalsIgnoreCase("NOT_FOUND")){
                                    runOnUiThread(()->
                                            Toast.makeText(this, chat.getName() + " was not found ",
                                                    Toast.LENGTH_SHORT).show());
                                }

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }

                        }).start();
                    }
                })
                .setNegativeButton("Cancel ",(dialog, which)->dialog.cancel())
                .create().show();
    }

    public void deleteChat(GroupChat chat){
        //Toast.makeText(this, chat.getId(), Toast.LENGTH_SHORT).show();
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete chat ")
                .setMessage("Are you sure you want to delete \"" + chat.getName() + "\"?")
                .setPositiveButton("Delete ",(dialog, which)->{
                    new Thread(()->{
                        Socket socket = null;
                        ObjectOutputStream out = null;
                        ObjectInputStream in =null;

                        try{
                            socket = TcpConnection.getSocket();
                            out = TcpConnection.getOut();
                            in = TcpConnection.getIn();

                            String request = "DELETE" + "," + chat.getId();

                            out.writeObject(request);
                            out.flush();

                            String response = (String)in.readObject();

                            if(response.equalsIgnoreCase("OK")){
                                List<GroupChat> currentGroupChats = LocalStorage.getCurrentUserGroupChats();
                                int index = currentGroupChats.indexOf(chat);

                                currentGroupChats.remove(index);
                                LocalStorage.setCurrentUserGroupChats(currentGroupChats);

                                //update adapter
                                runOnUiThread(()->adapter.notifyDataSetChanged());
                            }
                            else{
                                runOnUiThread(()->
                                        Toast.makeText(this, chat.getName() + " was not found ",
                                                Toast.LENGTH_SHORT).show());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }

                    }).start();
                }).setNegativeButton("Cancel ",(dialog, which)->dialog.cancel())
                .create().show();
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

         dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Add a new conversation")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, w) ->{
                    sendCancelDialogMessage();
                    d.cancel();
                })
                .setPositiveButton("OK", (d, w) -> {

                    String groupName = editGroupName.getText().toString().trim();
                    if (groupName.isEmpty()) {
                        Toast.makeText(this, "Enter a group name!", Toast.LENGTH_SHORT).show();

                        sendCancelDialogMessage();

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

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        dialog.getWindow().getDecorView().setOnTouchListener((v, event)->{
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //Toast.makeText(MainActivity.this, "Please complete this first!", Toast.LENGTH_SHORT).show();
                sendCancelDialogMessage();
                dialog.cancel();
            }
            return true;
        });

        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                //Toast.makeText(MainActivity.this, "Finalizează acțiunea înainte de a ieși!", Toast.LENGTH_SHORT).show();
                sendCancelDialogMessage();
                dialog.cancel();
                return true;
            }
            return false;
        });

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

    public void handleLogout(){
        //Toast.makeText(this,"Back press logic... ",Toast.LENGTH_SHORT).show();

        if(adapter != null){
            adapter.setEnabled(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Do you wish to logout ")
                .setNegativeButton("NO ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {

                        dialogInterface.cancel();
                    }
                })
                .setPositiveButton("YES ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(()->{
                            try {
                                ObjectOutputStream out = TcpConnection.getOut();

                                if (out != null) {
                                    out.writeObject("LOGOUT");
                                    out.flush();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            TcpConnection.stopListening();
                            TcpConnection.close();

                            runOnUiThread(() -> {
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                                startActivity(intent);
                                finish();
                            });

                        }).start();
                    }
                }).create();

        dialog.show();
    }

    public void sendCancelDialogMessage(){
        new Thread(()->{
            try {
                ObjectOutputStream out = TcpConnection.getOut();
                ObjectInputStream in = TcpConnection.getIn();

                out.writeObject("RST");
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
