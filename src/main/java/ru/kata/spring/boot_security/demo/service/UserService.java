package ru.kata.spring.boot_security.demo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.kata.spring.boot_security.demo.dto.UserDto;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;
import java.util.Map;

public interface UserService extends UserDetailsService {
     void add(UserDto userDto);

    public void update(UserDto userDto);

    void delete(Long id);

     Map<User,String> getAllUsers();
     User findByEmail(String email);

    String getUserRoles(User user);



    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
