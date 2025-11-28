package com.example.tcpclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
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

import chat.*;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class LoginActivity extends AppCompatActivity {
    private ConfigReader config;
    SharedPreferences preferences;
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

        config = new ConfigReader(this);
        preferences = SecureStorage.getEncryptedPrefs(getApplicationContext());

        CheckBox checkBox = findViewById(R.id.checkBoxKeepSignedIn);
        if (preferences.contains("username")) {
            checkBox.setChecked(true);
        }

        checkAutoLogin();
    }

    public void handleLogin(View view) {
        ConstraintLayout layout = findViewById(R.id.loginLayout);
        EditText usernameField = findViewById(R.id.textInputEditText);
        EditText passwordField = findViewById(R.id.editTextTextPassword);
        CheckBox checkBox = findViewById(R.id.checkBoxKeepSignedIn);

        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if(username.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please enter both values", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            try{
                TcpConnection.connect(config.getServerIp(),config.getServerPort());
                socket = TcpConnection.getSocket();
                out = TcpConnection.getOut();
                in = TcpConnection.getIn();

                //request login
                String request = "GET_LOGIN";

                out.writeObject(request);
                out.flush();

                // Trim username/password și trimitem serverului
                String loginCredentials = username + "," + password;
                out.writeObject(loginCredentials);
                out.flush();

                Object response = in.readObject();

                runOnUiThread(() -> {

                    View textView = layout.findViewWithTag("responseText");
                    if(textView!=null){
                        layout.removeView(textView);
                    }

                    if(response instanceof User){

                        TcpConnection.setCurrentUser((User)response);
                        TcpConnection.setCurrentUserId(((User) response).getId());

                        if(checkBox.isChecked()){
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.apply();
                        }
                        else{
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.clear();
                            editor.apply();
                        }

                        //Intent intent = new Intent(LoginActivity.this, ConversationActivity.class);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    else if(response instanceof String){
                        TextView responseTextView = new TextView(LoginActivity.this);
                        responseTextView.setTag("responseText");
                        responseTextView.setText((String) response);
                        responseTextView.setTextColor(Color.RED);
                        responseTextView.setId(View.generateViewId());
                        layout.addView(responseTextView);

                        ConstraintSet set = new ConstraintSet();
                        set.clone(layout);
                        set.connect(responseTextView.getId(), ConstraintSet.TOP, passwordField.getId(), ConstraintSet.BOTTOM, 20);
                        set.connect(responseTextView.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START, 0);
                        set.connect(responseTextView.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END, 0);
                        set.setHorizontalBias(responseTextView.getId(), 0.5f);
                        set.applyTo(layout);
                    }

                    usernameField.setText("");
                    passwordField.setText("");
                    usernameField.requestFocus();
                });

            } catch (Exception e) {
                e.printStackTrace();
                TcpConnection.close();

                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Connection Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    public void handleRegister(View view) {
        Intent newActivity = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(newActivity);
    }

    private void checkAutoLogin() {
        String savedUser = preferences.getString("username", null);
        String savedPass = preferences.getString("password", null);

        if (savedUser == null || savedPass == null) {
            return;
        }

        Toast.makeText(this, "Auto-logging in...", Toast.LENGTH_SHORT).show();

        findViewById(R.id.button).setEnabled(false);
        findViewById(R.id.button2).setEnabled(false);

        new Thread(() -> {
            try {
                TcpConnection.connect(config.getServerIp(), config.getServerPort());
                ObjectOutputStream out = TcpConnection.getOut();
                ObjectInputStream in = TcpConnection.getIn();

                out.writeObject("GET_LOGIN");
                out.flush();

                out.writeObject(savedUser + "," + savedPass);
                out.flush();

                Object response = in.readObject();

                if (response instanceof User) {
                    TcpConnection.setCurrentUser((User) response);
                    TcpConnection.setCurrentUserId(((User) response).getId());

                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    TcpConnection.close();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Auto-login failed. Please sign in again.", Toast.LENGTH_SHORT).show();
                        findViewById(R.id.button).setEnabled(true);
                        findViewById(R.id.button2).setEnabled(true);
                        // Opțional: ștergem datele invalide
                        preferences.edit().clear().apply();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                TcpConnection.close();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection failed during auto-login", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.button).setEnabled(true);
                    findViewById(R.id.button2).setEnabled(true);
                });
            }
        }).start();
    }
}