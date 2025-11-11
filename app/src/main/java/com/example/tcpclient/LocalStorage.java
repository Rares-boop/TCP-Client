package com.example.tcpclient;

import java.util.ArrayList;
import java.util.List;

import chat.GroupChat;
import chat.GroupMember;

public class LocalStorage {
    public static List<GroupChat> currentUserGroupChats = new ArrayList<>();
    public static List<GroupMember> currentUserGroupMembers = new ArrayList<>();

    public static List<GroupChat> getCurrentUserGroupChats() {
        return currentUserGroupChats;
    }

    public static void setCurrentUserGroupChats(List<GroupChat> currentUserGroupChats) {
        LocalStorage.currentUserGroupChats = currentUserGroupChats;
    }

    public static List<GroupMember> getCurrentUserGroupMembers() {
        return currentUserGroupMembers;
    }

    public static void setCurrentUserGroupMembers(List<GroupMember> currentUserGroupMembers) {
        LocalStorage.currentUserGroupMembers = currentUserGroupMembers;
    }
}
