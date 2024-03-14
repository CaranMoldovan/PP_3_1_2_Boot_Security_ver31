package ru.kata.spring.boot_security.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private UserService userService;
    private RoleService roleService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("")
    public String getUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users";
    }

    @GetMapping("/{id}/edit")
    public String getEditForm(@PathVariable Long id, Model model) {
        User userEdit = userService.findById(id);
        List<Role> roles = roleService.findAll();
        model.addAttribute("allRoles", roles);
        model.addAttribute("user", userEdit);
        return "update";
    }

    @PostMapping("/adduser")
    public String addUser(@ModelAttribute("user")@Valid User user,BindingResult result,
                          @RequestParam("roles") List<String> roles
                          ) {
        if(result.hasErrors()) {
            return "add";
        }

        Set<Role> roleSet = roleService.getSetOfRoles(roles);
        user.setRoles(roleSet);
        userService.add(user);
        return "redirect:";
    }

    @PostMapping("update")
    public String updateUser(@ModelAttribute("user")@Valid User user,BindingResult result,
                             @RequestParam("roles") List<String> roles
                             ) {
        if(result.hasErrors()) {
            return "update";
        }
        Set<Role> roleSet = roleService.getSetOfRoles(roles);
        user.setRoles(roleSet);
        userService.update(user);
        return "redirect:/admin";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        List<Role> roles = roleService.findAll();
        model.addAttribute("allRoles", roles);
        return "add";
    }

    @GetMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        if(user != null) {
            userService.delete(id);
            model.addAttribute("messege", "user " + user.getUsername() + " succesfully deleted");
        } else {
            model.addAttribute("messege", "no such user");
        }

        return "redirect:/admin";
    }

}