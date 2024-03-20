package ru.kata.spring.boot_security.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dto.RoleDto;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImp implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImp(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }



    @Override
    @Transactional
    public void save(Role role) {
        roleRepository.save(role);
    }



    @Override
    @Transactional(readOnly = true)
    public Set<Role> getSetOfRoles(Set<RoleDto> rolesName){
        Set<Role> roleSet = new HashSet<>();
        for (RoleDto roleDto: rolesName) {
            roleSet.add(roleRepository.findByName(roleDto.getName()));
        }
        return roleSet;
    }


}