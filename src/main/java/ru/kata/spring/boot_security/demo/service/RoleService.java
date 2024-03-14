package ru.kata.spring.boot_security.demo.service;

import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;
import java.util.Set;

public interface RoleService {

    Role findByName(String name);
    void save(Role role);
    List<Role> findAll();
    Set<Role> getSetOfRoles(List<String> rolesId);
}