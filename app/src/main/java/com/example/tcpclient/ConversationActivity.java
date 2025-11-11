package com.example.tcpclient;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import java.util.List;

import chat.Message;
import chat.User;

public class ConversationActivity extends AppCompatActivity {
    public List<Message> messages = new ArrayList<>();
    RecyclerView recyclerView;
    MessageAdapter messageAdapter;
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
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

        recyclerView = (RecyclerView)findViewById(R.id.recyclerViewMessages);
        messageAdapter = new MessageAdapter(this, messages, TcpConnection.getCurrentUserId());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        View view = (View)findViewById(R.id.main);

        //test tastastatura ridicata
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
                out.writeObject("GET_MESSAGES");
                out.flush();

                Object response = in.readObject();
                if (response instanceof List) {
                    List<?> list = (List<?>) response;
                    if (!list.isEmpty() && list.get(0) instanceof Message) {
                        messages.clear();
                        messages.addAll((List<Message>) list);

                        runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
                    }
                }

                while (true) {
                    byte[] receivedMessageByte = (byte[]) in.readObject();
                    String messageText = new String(receivedMessageByte);
                    // creezi obiect Message pentru prieten
                    Message received = new Message(0, receivedMessageByte,
                            System.currentTimeMillis(), /* senderId  */ 999, /* groupId */ 0);

                    runOnUiThread(() -> {
                        messages.add(received);
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                    });
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();

        //receiveMessage();
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

                out.writeObject(messageByte);
                out.flush();

                runOnUiThread(() -> {
                    //la messages unde este 0 primul este id message si al doilea id group
                    messages.add(new Message(0, messageByte, System.currentTimeMillis(), TcpConnection.getCurrentUserId(), 0));
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);

                    messageBox.setText("");
                    messageBox.requestFocus();
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /*public void receiveMessage(){
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] receivedMessageByte = (byte[]) in.readObject();
                        String messageText = new String(receivedMessageByte);
                        // creezi obiect Message pentru prieten
                        Message received = new Message(0, receivedMessageByte,
                                System.currentTimeMillis(),  senderId  999, groupId 0);

                        runOnUiThread(() -> {
                            messages.add(received);
                            messageAdapter.notifyItemInserted(messages.size() - 1);
                            recyclerView.scrollToPosition(messages.size() - 1);
                        });
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
    }*/
}

