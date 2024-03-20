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
import ru.kata.spring.boot_security.demo.dto.RoleDto;
import ru.kata.spring.boot_security.demo.dto.UserDto;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import ru.kata.spring.boot_security.demo.repository.UserRepository;
import ru.kata.spring.boot_security.demo.util.UserMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class UserServiceImp implements UserService, UserDetailsService {

    private final UserRepository userRepository;

    private final RoleService roleService;
    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserServiceImp(UserRepository userRepository, RoleService roleService, @Lazy PasswordEncoder encoder, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.encoder = encoder;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    @Override
    public void add(UserDto userDto) {
        User user = userMapper.toModel(userDto);
        user.setPassword(encoder.encode(user.getPassword()));
        setRolesToUser(user,userDto.getRoles());

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    @Override
    public void update(UserDto userDto) {
        User existingUser = userRepository.findById(userDto.getId()).orElse(null);
        setRolesToUser(existingUser,userDto.getRoles());

        if (existingUser != null) {
            existingUser.setFirstname(userDto.getFirstname());
            existingUser.setLastname(userDto.getLastname());
            existingUser.setAge(userDto.getAge());
            existingUser.setEmail(userDto.getEmail());
            existingUser.setPassword(encoder.encode(userDto.getPassword()));
            roleService.getSetOfRoles(userDto.getRoles());
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
    public Map<User, String> getAllUsers() {
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





    private void setRolesToUser(User user, Set<RoleDto> roleDtoSet) {
        Set<Role> roles = roleDtoSet.stream()
                .map(roleDto -> roleRepository.findByName("ROLE_" + roleDto.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        user.setRoles(roles);
    }

}

