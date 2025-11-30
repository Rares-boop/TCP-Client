package com.example.tcpclient;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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
import java.util.Arrays;
import java.util.List;

import chat.Message;

public class ConversationActivity extends AppCompatActivity {
    public List<Message> messages = new ArrayList<>();
    RecyclerView recyclerView;
    MessageAdapter messageAdapter;
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    private volatile boolean isRunning = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_conversation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBackPress
            );
        }

        recyclerView = (RecyclerView)findViewById(R.id.recyclerViewMessages);
        messageAdapter = new MessageAdapter(this, messages, TcpConnection.getCurrentUserId(),
                new MessageAdapter.OnMessageLongClickListener() {
                    @Override
                    public void onMessageLongClick(Message message) {
                        handleLongMessageClick(message);
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        View view = (View)findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            view.setPadding(0, 0, 0, imeHeight + 4);
            return insets;
        });

        socket = TcpConnection.getSocket();
        out = TcpConnection.getOut();
        in = TcpConnection.getIn();

        new Thread(() -> {
            try {
                synchronized (out) {
                    out.writeObject("GET_MESSAGES");
                    out.flush();
                }

                Object response = in.readObject();
                if(response != null) {
                    Log.e("#2345", response.toString());
                }
                else{
                    Log.e("#2345","GET_MESSAGES ERROR ");
                }
                if (response instanceof List) {
                    List<?> list = (List<?>) response;
                    if (!list.isEmpty() && list.get(0) instanceof Message) {
                        messages.clear();
                        messages.addAll((List<Message>) list);

                        runOnUiThread(() -> messageAdapter.notifyDataSetChanged());

                        messages.stream().forEach(message ->
                                runOnUiThread( ()-> Toast.makeText(this, message.toString(),Toast.LENGTH_SHORT)));
                    }
                }

                while (isRunning) {
                    try{
                        Object incoming = in.readObject();

                        if (incoming instanceof Message) {
                            Message receivedMessage = (Message) incoming;

                            runOnUiThread(() -> {
                                messages.add(receivedMessage);
                                messageAdapter.notifyItemInserted(messages.size() - 1);
                                recyclerView.scrollToPosition(messages.size() - 1);
                            });
                        }
                        else if (incoming instanceof String) {
                            String command = (String) incoming;
                            if (command.equals("STOPPED_LISTENING")) {
                                isRunning = false;
                                break;
                            }
                            else if (command.startsWith("BCST_EDIT")){
                                int idEdit = Integer.parseInt(command.split(",")[1]);

                                Object newContent = in.readObject();

                                if(newContent instanceof byte[]){
                                    byte[] newByteContent = (byte[])newContent;

                                    runOnUiThread(()->{
                                        for(int i=0;i<messages.size();i++){
                                            if(messages.get(i).getId() == idEdit){
                                                messages.get(i).setContent(newByteContent);
                                                messageAdapter.notifyItemChanged(i);

                                                break;
                                            }
                                        }
                                    });
                                }
                            }
                            else if(command.startsWith("BCST_DELETE")){
                                int idDelete = Integer.parseInt(command.split(",")[1]);

                                runOnUiThread(()->{
                                    for(int i=0;i<messages.size();i++){
                                        if(messages.get(i).getId() == idDelete){
                                            messages.remove(i);
                                            messageAdapter.notifyItemRemoved(i);

                                            break;
                                        }
                                    }
                                });
                            }
                        }
                    }catch (Exception e){
                        isRunning = false;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        View btnBack = findViewById(R.id.btnBackArrow);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackPress();
            }
        });

        TextView txtChatName = (TextView)findViewById(R.id.txtChatName);
        Intent intent = getIntent();

        String chatName = intent.getStringExtra("CHAT_NAME");
        txtChatName.setText(chatName);
        }

    @SuppressLint({"GestureBackNavigation", "MissingSuperCall"})
    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    public void handleMessage(View view) {
        //Toast.makeText(getApplicationContext(),"Message logic",Toast.LENGTH_SHORT).show();
        ConstraintLayout layout = findViewById(R.id.main);
        EditText messageBox = findViewById(R.id.editTextMessage);

        String message = messageBox.getText().toString().trim();

        if(message.isEmpty()){
            return;
        }

        Toast.makeText(getApplicationContext(),"Sending message...",Toast.LENGTH_SHORT).show();

        new Thread(()->{
            try{
                byte[] messageByte = message.getBytes();

                synchronized (out) {
                    out.writeObject(messageByte);
                    out.flush();
                }

                runOnUiThread(() -> {
                    messageBox.setText("");
                    messageBox.requestFocus();
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ConversationActivity.this, "Failed to send", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    public void handleBackPress() {
        Toast.makeText(this, "Leaving conversation...", Toast.LENGTH_SHORT).show();

        new Thread(()->{
            ObjectOutputStream out = null;
            try{
                out = TcpConnection.getOut();

                if(out != null) {
                    synchronized (out) {
                        out.writeObject("PAUSE_CONVERSATION");
                        out.flush();
                    }
                }

                int attempts = 0;
                while (isRunning && attempts < 20) {
                    Thread.sleep(100);
                    attempts++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runOnUiThread(this::finish);
            }
        }).start();
    }

    public void handleLongMessageClick(Message message){
        //Toast.makeText(this, message.toString(), Toast.LENGTH_SHORT).show();
        AlertDialog alertDialog = new AlertDialog.Builder(ConversationActivity.this)
                .setTitle("Modify or delete message ")
                .setItems(new String[]{"Modify", "Delete"},((dialog, which) -> {
                    if(which == 0){
                        modifyMessage(message);
                    }
                    else{
                        deleteMessage(message);
                    }
                })).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();

        alertDialog.show();
    }

    public void modifyMessage(Message message){
        //Toast.makeText(this,"UPDATE " + message.getId(),Toast.LENGTH_SHORT).show();
        EditText input = new EditText(this);
        String messageContent = new String(message.getContent());

        input.setText(messageContent);
        input.setSelection(messageContent.length());

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Modify message ")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newText = input.getText().toString();

                        if(newText.isEmpty()){
                            Toast.makeText(ConversationActivity.this, "Message cannot be empty ",Toast.LENGTH_SHORT).show();
                        }
                        else if(newText.equals(messageContent)){
                            Toast.makeText(ConversationActivity.this, "No changes made", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            new Thread(()->{
                                try{
                                    byte[] newContentBytes = newText.getBytes();

                                    if(out != null){
                                        synchronized (out){
                                            out.writeObject("CMD_EDIT,"+message.getId());
                                            out.flush();

                                            out.writeObject(newContentBytes);
                                            out.flush();
                                        }
                                    }
                                } catch (Exception e) {
//                                    throw new RuntimeException(e);
                                    e.printStackTrace();
                                    runOnUiThread(() ->
                                            Toast.makeText(ConversationActivity.this, "Failed to edit: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }).start();
                        }

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        alertDialog.show();
    }

    public void deleteMessage(Message message){
        //Toast.makeText(this,"DELETE " + message.getId(),Toast.LENGTH_SHORT).show();
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Delete chat ")
                .setMessage("Are you sure you want to delete \"" + new String(message.getContent()) + "\"?")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(()->{
                            try{
                                if (out != null) {
                                    synchronized (out) {
                                        out.writeObject("CMD_DELETE," + message.getId());
                                        out.flush();
                                    }
                                }
                            } catch (IOException e) {
//                            throw new RuntimeException(e);
                                e.printStackTrace();
                                runOnUiThread(() ->
                                        Toast.makeText(ConversationActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            }
                        }).start();
                    }
                }).setNegativeButton("Cancel ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        alertDialog.show();
    }
}

