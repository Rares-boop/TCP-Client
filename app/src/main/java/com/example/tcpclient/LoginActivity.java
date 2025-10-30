package com.example.tcpclient;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import chat.*;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void handleLogin(View view) {
        ConstraintLayout layout = findViewById(R.id.loginLayout);
        EditText usernameField = findViewById(R.id.textInputEditText);
        EditText passwordField = findViewById(R.id.editTextTextPassword);

        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if(username.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please enter both values", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        Log.e("####1","ENTER HANDLE LOGIN");

        new Thread(() -> {
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            Log.e("####1","ENTER THREAD");
            try{
                TcpConnection.connect("192.168.1.132",15555);
                socket = TcpConnection.getSocket();
                out = TcpConnection.getOut();
                in = TcpConnection.getIn();

                Log.e("####1","SOCKET ESTABLISHED");

                // Trim username/password și trimitem serverului
                String loginCredentials = username + "," + password;
                out.writeObject(loginCredentials);
                out.flush();

                Log.e("####1","CREDENTIALS SENT");

                Object response = in.readObject();

                Log.e("####1","RESPONSE RECEIVED");

                runOnUiThread(() -> {
                    if(response instanceof User){
                        Intent intent = new Intent(LoginActivity.this, ConversationActivity.class);
                        startActivity(intent);
                    }
                    else if(response instanceof String){
                        TextView responseTextView = new TextView(LoginActivity.this);
                        responseTextView.setText((String) response);
                        responseTextView.setTextColor(Color.RED);
                        layout.addView(responseTextView);
                    }

                    // Curățăm câmpurile după login
                    usernameField.setText("");
                    passwordField.setText("");
                    usernameField.requestFocus();
                });

            } catch (UnknownHostException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Host not found", Toast.LENGTH_LONG).show()
                );
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Unexpected response from server", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}