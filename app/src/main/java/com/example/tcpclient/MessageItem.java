package com.example.tcpclient;

import chat.Message;

public class MessageItem {
    public boolean isDateSeparator;
    public String dateText; // pentru separator
    public Message message; // pentru mesaj normal

    public MessageItem(String dateText) {
        this.isDateSeparator = true;
        this.dateText = dateText;
    }

    public MessageItem(Message message) {
        this.isDateSeparator = false;
        this.message = message;
    }
}


