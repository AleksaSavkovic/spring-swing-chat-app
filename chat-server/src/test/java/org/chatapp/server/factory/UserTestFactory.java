package org.chatapp.server.factory;

import org.chatapp.server.entities.User;


public class UserTestFactory {

    public static User createUser(){
        return User.builder()
                .username("test-user")
                .password("password")
                .build();
    }

    public static User createUser(String username){
        return User.builder()
                .username(username)
                .password("password")
                .build();
    }
}
