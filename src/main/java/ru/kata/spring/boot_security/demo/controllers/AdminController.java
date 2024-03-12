package ru.kata.spring.boot_security.demo.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserService;


import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;

    }


    @GetMapping(value = "/admin")
    public String printUsers(Model model) {
        Map<User, String> usersWithRoles = userService
                .getAllUsers()
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toMap(
                        Function.identity(),
                        userService::getUserRoles,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));

        model.addAttribute("usersWithRoles", usersWithRoles);

        return "Admin";
    }


    @PostMapping(value = "/admin/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String lastname,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam List<String> roles) {
        User user = new User(username, lastname, email, password);
        userService.add(user);
        for (String role : roles) {
            userService.addRoleToUser(role, user);
        }

        return "redirect:/admin";
    }



    @PostMapping(value = "/admin/update")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String username,
                             @RequestParam String lastname,
                             @RequestParam String email,
                             @RequestParam String password) {
        User updateUser = userService.findById(id);

        if (updateUser != null) {
            updateUser.setUsername(username);
            updateUser.setLastname(lastname);
            updateUser.setEmail(email);
            updateUser.setPassword(password);
            userService.update(updateUser);
        }

        return "redirect:/admin";
    }


    @PostMapping(value = "/admin/delete")
    public String deleteUser(@RequestParam Long id) {
        userService.delete(id);

        return "redirect:/admin";
    }

}