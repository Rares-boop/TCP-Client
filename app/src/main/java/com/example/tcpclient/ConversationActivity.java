package com.example.tcpclient;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import chat.User;

public class ConversationActivity extends AppCompatActivity {
    public static List<byte[]> sentMessages = new ArrayList<>();
    public static List<byte[]> receivedMessages = new ArrayList<>();
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
        View view = (View)findViewById(R.id.main);
        //handleMessageRecive(view);
        receiveMessage(view);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TcpConnection.close();
    }

    /*public void handleMessageRecive(View view) {
        EditText textBox = (EditText)findViewById(R.id.editTextMessage);

        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try{
            socket = TcpConnection.getSocket();
            out = TcpConnection.getOut();
            out.flush();
            in = TcpConnection.getIn();

            //thread for receiving messages
            ObjectInputStream finalIn = in;
            new Thread(()->{
                try{
                    while (true){
                        byte[] msg = (byte[]) finalIn.readObject();
                        String text = new String(msg);
                        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_LONG).show();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

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
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            try{
                socket = TcpConnection.getSocket();
                out = TcpConnection.getOut();
                in = TcpConnection.getIn();

                byte[] messageByte = message.getBytes();

                out.writeObject(messageByte);
                out.flush();

                runOnUiThread(() -> {
                    messageBox.setText("");
                    messageBox.requestFocus();
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void receiveMessage(View view){
        new Thread(()->{
            new Thread(() -> {
                try {
                    ObjectInputStream in = TcpConnection.getIn();
                    while (true) {
                        byte[] receivedMessageByte = (byte[]) in.readObject();
                        String messageText = new String(receivedMessageByte);
                        // handle message
                        runOnUiThread(()->{
                            Toast.makeText(getApplicationContext(),"Message received "+messageText,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        }).start();
    }
}

