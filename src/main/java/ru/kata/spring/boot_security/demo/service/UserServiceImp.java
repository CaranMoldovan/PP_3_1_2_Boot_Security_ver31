package ru.kata.spring.boot_security.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.UserRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class UserServiceImp implements UserService, UserDetailsService {

    private final UserRepository userRepository;

    private final RoleService roleService;
    private final PasswordEncoder encoder;

    @Autowired
    public UserServiceImp(UserRepository userRepository, RoleService roleService, @Lazy PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.encoder = encoder;
    }

    @Transactional
    @Override
    public void add(User user) {
        User userToSave = new User();
        userToSave.setFirstname(user.getFirstname());
        userToSave.setLastname(user.getLastname());
        userToSave.setEmail(user.getEmail());
        userToSave.setAge(user.getAge());
        userToSave.setPassword(encoder.encode(user.getPassword()));
        userToSave.setRoles(user.getRoles());
        userRepository.save(userToSave);
    }

    @Transactional(readOnly = true)
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    @Transactional
    @Override
    public void update(User user) {
        // Получаем пользователя из базы данных по его идентификатору
        Optional<User> optionalUser = userRepository.findById(user.getId());
        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();

            // Обновляем поля пользователя на основе переданных значений
            existingUser.setFirstname(user.getFirstname());
            existingUser.setLastname(user.getLastname());
            existingUser.setEmail(user.getEmail());
            existingUser.setAge(user.getAge());
            existingUser.setPassword(encoder.encode(user.getPassword()));
            existingUser.getRoles().clear();

            // Добавляем новые роли
            for (Role role : user.getRoles()) {
                existingUser.getRoles().add(role);
            }

            // Сохраняем обновленного пользователя с новыми ролями и полями
            userRepository.save(existingUser);
        }
    }


    @Transactional
    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<User,String> getAllUsers() {
        return userRepository
                .findAll(Sort.by("id"))
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getUserRoles,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }



    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.
                User(user.getUsername(), user.getPassword(), authorities);
    }



    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByEmail(username);
    }


    @Transactional(readOnly = true)
    @Override
    public String getUserRoles(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(", "));
    }

    @Transactional
    @Override
    public void addRoleToUser(String roleName, User user) {
        Role role = roleService.findByName(roleName);
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

}

