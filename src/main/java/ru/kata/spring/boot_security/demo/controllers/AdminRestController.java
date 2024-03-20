package ru.kata.spring.boot_security.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.dto.UserDto;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserService;
import ru.kata.spring.boot_security.demo.util.UserMapper;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {
    private final UserService userService;
    private final UserMapper userMapper;

    public AdminRestController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/current-user")
    public User getCurrentUser(Principal principal) {
        return userService.findByEmail(principal.getName());
    }

    @GetMapping("/all-users")
    public List<User> getAllUsersWithRoles() {
        return userService.getAllUsers().keySet().stream().collect(Collectors.toList());
    }

    @PostMapping("/add")
    public ResponseEntity<String> addUser(@RequestBody UserDto userDto) {
        userService.add(userMapper.toModel(userDto));
        return ResponseEntity.ok("User added successfully");
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateUser(@RequestBody UserDto userDto) {
        userService.update(userMapper.toModel(userDto));
        return ResponseEntity.ok("User updated successfully");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestBody UserDto userDto) {
        userService.delete(Long.getLong(userDto.getId()));
        return ResponseEntity.ok("User deleted successfully");
    }
}
