package ru.kata.spring.boot_security.demo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;
import java.util.Map;

public interface UserService extends UserDetailsService {
    void add(User user);

    void update(User user);

    void delete(Long id);

    public Map<User,String> getAllUsers();
    public User findByEmail(String email);

    String getUserRoles(User user);

    void addRoleToUser(String roleName, User user);

    User findByUsername(String username);

    User findById(Long id);

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
