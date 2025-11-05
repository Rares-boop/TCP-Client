package com.example.tcpclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import chat.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Message> messages;
    private final int currentUserId;
    private final Context context;

    private static final int VIEW_TYPE_MINE = 1;
    private static final int VIEW_TYPE_FRIEND = 2;

    public MessageAdapter(Context context, List<Message> messages, int currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return (message.getSenderId() == currentUserId) ? VIEW_TYPE_MINE : VIEW_TYPE_FRIEND;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MINE) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_right, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_left, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.messageText.setText(new String(message.getContent()));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }
    }
}
