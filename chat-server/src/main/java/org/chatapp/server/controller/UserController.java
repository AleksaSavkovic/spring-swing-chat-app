package org.chatapp.server.controller;

import lombok.RequiredArgsConstructor;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class UserController {

    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_ID = USER_PATH + "/{userId}";

    private final UserService userService;

    // Method that returns user for the requested userId
    @GetMapping(USER_PATH_ID)
    public UserDTO findUserById(@PathVariable UUID userId){
        return userService.findUserById(userId);
    }

}
