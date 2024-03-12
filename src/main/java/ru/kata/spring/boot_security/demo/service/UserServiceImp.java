package ru.kata.spring.boot_security.demo.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;


    @Service
    public class UserServiceImp implements UserService, UserDetailsService {

        private final UserRepository userRepository;

        private final RoleService roleService;

        public UserServiceImp(UserRepository userRepository, RoleService roleService) {
            this.userRepository = userRepository;
            this.roleService = roleService;
        }

        @Transactional
        @Override
        public void add(User user) {
            userRepository.save(user);
        }

        @Transactional
        @Override
        public void update(User user) {
            userRepository.save(user);
        }

        @Transactional
        @Override
        public void delete(Long id) {
            userRepository.deleteById(id);
        }

        @Transactional(readOnly = true)
        @Override
        public List<User> getAllUsers() {
            return userRepository.findAll(Sort.by("id"));
        }


        @Transactional(readOnly = true)
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            User user = userRepository.findByUsername(username);

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
            return userRepository.findByUsername(username);
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

