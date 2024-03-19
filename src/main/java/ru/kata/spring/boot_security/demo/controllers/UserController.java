package ru.kata.spring.boot_security.demo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserService;

import java.security.Principal;




@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/user")
    public String getUserPage(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        String userRoles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("userRoles", userRoles);
        return "userPage";
    }


}