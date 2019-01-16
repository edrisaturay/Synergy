package com.edrisa.zoko.synergy.models;

public class User {
    String userId;
    String userName;
    String userEmail;
    String userStatus;
    String userPhotoURL;

    public User(){}

    public User(String userId, String userName, String userEmail, String userStatus, String userPhotoURL) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userStatus = userStatus;
        this.userPhotoURL = userPhotoURL;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getUserPhotoURL() {
        return userPhotoURL;
    }

    public void setUserPhotoURL(String userPhotoURL) {
        this.userPhotoURL = userPhotoURL;
    }
}
