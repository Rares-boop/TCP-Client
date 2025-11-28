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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import chat.User;

public class RegisterActivity extends AppCompatActivity {
    private ConfigReader config;
    SharedPreferences preferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        config = new ConfigReader(this);
        preferences = SecureStorage.getEncryptedPrefs(getApplicationContext());
    }

    public void handleAccount(View view) {
        ConstraintLayout layout = findViewById(R.id.main);
        EditText usernameField = findViewById(R.id.editTextText);
        EditText passwordField = findViewById(R.id.editTextTextPassword2);
        EditText confirmedPasswordField = findViewById(R.id.editTextTextPassword3);
        CheckBox checkBox = findViewById(R.id.checkBoxKeepSignedInRegister);

        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmedPassword = confirmedPasswordField.getText().toString().trim();

        if(username.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty()){
            Toast.makeText(this, "Please enter all the values ",Toast.LENGTH_SHORT).show();
            return;
        }

        if(!password.equals(confirmedPassword)){
            Toast.makeText(this, "Passwords need to match ",Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(()->{
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            try{
                TcpConnection.connect(config.getServerIp(),config.getServerPort());
                socket = TcpConnection.getSocket();
                out = TcpConnection.getOut();
                in = TcpConnection.getIn();

                //request login
                String request = "GET_REGISTER";

                out.writeObject(request);
                out.flush();

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

                        //Intent intent = new Intent(RegisterActivity.this, ConversationActivity.class);
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    else if(response instanceof String){
                        TextView responseTextView = new TextView(RegisterActivity.this);
                        responseTextView.setTag("responseText");
                        responseTextView.setText((String) response);
                        responseTextView.setTextColor(Color.RED);
                        responseTextView.setId(View.generateViewId());
                        layout.addView(responseTextView);

                        ConstraintSet set = new ConstraintSet();
                        set.clone(layout);
                        set.connect(responseTextView.getId(), ConstraintSet.TOP, confirmedPasswordField.getId(), ConstraintSet.BOTTOM, 20);
                        set.connect(responseTextView.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START, 0);
                        set.connect(responseTextView.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END, 0);
                        set.setHorizontalBias(responseTextView.getId(), 0.5f);
                        set.applyTo(layout);
                    }

                    usernameField.setText("");
                    passwordField.setText("");
                    confirmedPasswordField.setText("");
                    usernameField.requestFocus();
                });

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                TcpConnection.close();

                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "Connection Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
