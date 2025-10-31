package com.example.tcpclient;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConversationActivity extends AppCompatActivity {

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
        handleMessageRecive(view);
    }

    public void handleMessageRecive(View view) {
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
    }

    public void handleMessage(View view) {
        Toast.makeText(getApplicationContext(),"Message logic",Toast.LENGTH_SHORT).show();
    }
}