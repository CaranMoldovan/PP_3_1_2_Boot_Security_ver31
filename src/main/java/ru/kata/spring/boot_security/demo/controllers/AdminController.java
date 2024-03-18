package ru.kata.spring.boot_security.demo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.dto.UserDto;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;
import ru.kata.spring.boot_security.demo.util.UserMapper;

import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;

    private final UserMapper userMapper;
    private final RoleService roleService;

    public AdminController(UserService userService,
                               UserMapper userMapper,
                           RoleService roleService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.roleService= roleService;
    }


    @GetMapping(value = "")
    public String printUsersAndCurrentUser(Model model, Principal principal) {
        User currentUser = userService.findByEmail(principal.getName());
        String userRoles = userService.getUserRoles(currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRoles", userRoles);

        Map<User, String> usersWithRoles = userService.getAllUsers();
        model.addAttribute("usersWithRoles", usersWithRoles);

        return "adminPage";
    }


    @PostMapping(value = "/add")
    public String addUser(@ModelAttribute UserDto userDto, @RequestParam List<String> role) {
        User user = userMapper.toModel(userDto);
        Set<Role> roles = roleService.getSetOfRoles(role);
        user.setRoles(roles);
        userService.add( user);

        return "redirect:/admin";
    }


    @PostMapping(value = "/update")
    public String updateUser(@ModelAttribute UserDto userDto,
                             @RequestParam List<String> role) {
        User user = userMapper.toModel(userDto);
        user.setRoles(roleService.getSetOfRoles(role));
        userService.update(user);

        return "redirect:/admin";
    }


    @PostMapping(value = "/delete")
    public String deleteUser(@RequestParam Long id) {
        userService.delete(id);

        return "redirect:/admin";
    }
}