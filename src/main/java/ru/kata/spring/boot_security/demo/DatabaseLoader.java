package ru.kata.spring.boot_security.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.kata.spring.boot_security.demo.dto.RoleDto;
import ru.kata.spring.boot_security.demo.dto.UserDto;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;


import java.util.HashSet;
import java.util.List;

@Component
public class DatabaseLoader implements CommandLineRunner {

    private final UserService userService;
    private final RoleService roleService;

    public DatabaseLoader(UserService userRepository, RoleService roleRepository) {
        this.userService = userRepository;
        this.roleService = roleRepository;
    }

    @Override
    public void run(String... strings) {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");
        RoleDto adminRoleDto =new RoleDto("ADMIN");
        RoleDto userRoleDto = new RoleDto("USER");


        this.roleService.save(adminRole);
        this.roleService.save(userRole);

        UserDto admin = new UserDto("admin", "Admin",21, "admin@mail.ru", "admin");
        admin.setRoles(new HashSet<>(List.of(adminRoleDto, userRoleDto)));

        UserDto user = new UserDto("user", "User",21,  "user@mail.ru","user");
        user.setRoles(new HashSet<>(List.of(userRoleDto)));

        this.userService.add(admin);
        this.userService.add(user);
    }
}