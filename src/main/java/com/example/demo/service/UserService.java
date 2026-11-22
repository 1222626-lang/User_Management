package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;

import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.Company;
import com.example.demo.entity.CompanyStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.UserMapper;

import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            CompanyRepository companyRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String normalize(String value) {
        return value == null
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()
                || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters");
        }
    }

    private void validateUnique(
            String username,
            String email,
            Long currentId) {

        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);

        if (normalizedUsername == null
                || normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (normalizedEmail == null
                || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        boolean usernameExists = currentId == null
                ? userRepository.existsByUsernameIgnoreCase(normalizedUsername)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(
                normalizedUsername, currentId);

        boolean emailExists = currentId == null
                ? userRepository.existsByEmailIgnoreCase(normalizedEmail)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(
                normalizedEmail, currentId);

        if (usernameExists) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (emailExists) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private Company getActiveCompany(Long companyId) {

        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Company", companyId));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Cannot assign user to a disabled company");
        }

        return company;
    }

    private User getActiveUser(Long id) {
        return userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));
    }

    private Role getRole(String roleName) {

        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        return roleRepository.findByNameIgnoreCase(roleName.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role", roleName));
    }

    // PUBLIC REGISTRATION
    // The public user cannot choose their role.
    public UserResponse register(RegisterRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Registration request is required");
        }

        validateUnique(
                request.username(),
                request.email(),
                null
        );

        validatePassword(request.password());

        if (request.firstName() == null
                || request.firstName().isBlank()
                || request.lastName() == null
                || request.lastName().isBlank()) {
            throw new IllegalArgumentException(
                    "First name and last name are required");
        }

        Company company = getActiveCompany(request.companyId());

        Role userRole = getRole("USER");

        User user = new User();

        user.setUsername(normalize(request.username()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalize(request.email()));
        user.setPassword(passwordEncoder.encode(request.password()));

        user.setRole(userRole);
        user.setStatus(AccountStatus.PENDING);
        user.setCompany(company);

        return userMapper.toResponse(userRepository.save(user));
    }

    // GET PENDING REGISTRATIONS
    @Transactional(readOnly = true)
    public Page<UserResponse> getPendingRegistrations(
            Pageable pageable) {

        return userRepository.findAllByStatusAndIsDeletedFalse(
                        AccountStatus.PENDING,
                        pageable
                )
                .map(userMapper::toResponse);
    }

    // APPROVE REGISTRATION
    public UserResponse approveRegistration(Long id) {

        User user = getActiveUser(id);

        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending registrations can be approved");
        }

        if (user.getCompany() == null
                || user.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot approve user because the company is disabled");
        }

        user.setStatus(AccountStatus.APPROVED);

        return userMapper.toResponse(userRepository.save(user));
    }

    // REJECT REGISTRATION
    public UserResponse rejectRegistration(Long id) {

        User user = getActiveUser(id);

        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending registrations can be rejected");
        }

        user.setStatus(AccountStatus.REJECTED);

        return userMapper.toResponse(userRepository.save(user));
    }

    // GET ALL ACTIVE USERS
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {

        return userRepository.findAllByIsDeletedFalse(pageable)
                .map(userMapper::toResponse);
    }

    // ADMIN: CREATE USER
    public UserResponse create(UserRequest request) {

        validateUnique(
                request.username(),
                request.email(),
                null
        );

        validatePassword(request.password());

        if (request.firstName() == null
                || request.firstName().isBlank()
                || request.lastName() == null
                || request.lastName().isBlank()) {
            throw new IllegalArgumentException(
                    "First name and last name are required");
        }

        Company company = getActiveCompany(request.companyId());
        Role role = getRole(request.role());

        User user = new User();

        user.setUsername(normalize(request.username()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalize(request.email()));
        user.setPassword(passwordEncoder.encode(request.password()));

        user.setRole(role);
        user.setStatus(AccountStatus.APPROVED);
        user.setCompany(company);

        return userMapper.toResponse(userRepository.save(user));
    }

    // GET USER BY ID
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(getActiveUser(id));
    }

    // UPDATE USER
    public UserResponse update(Long id, UserRequest request) {

        User user = getActiveUser(id);

        validateUnique(
                request.username(),
                request.email(),
                id
        );

        if (request.firstName() == null
                || request.firstName().isBlank()
                || request.lastName() == null
                || request.lastName().isBlank()) {
            throw new IllegalArgumentException(
                    "First name and last name are required");
        }

        Role role = getRole(request.role());

        user.setUsername(normalize(request.username()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalize(request.email()));
        user.setRole(role);
        user.setCompany(getActiveCompany(request.companyId()));

        if (request.password() != null
                && !request.password().isBlank()) {

            validatePassword(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    // SOFT DELETE USER
    public void delete(Long id) {

        User user = getActiveUser(id);
        user.softDelete();
        userRepository.save(user);
    }

    // GET DELETED USERS
    @Transactional(readOnly = true)
    public Page<UserResponse> getDeletedUsers(Pageable pageable) {

        return userRepository.findAllByIsDeletedTrue(pageable)
                .map(userMapper::toResponse);
    }

    // GET DELETED USER BY ID
    @Transactional(readOnly = true)
    public UserResponse getDeletedUserById(Long id) {

        User user = userRepository.findByIdAndIsDeletedTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Deleted user", id));

        return userMapper.toResponse(user);
    }

    // RESTORE DELETED USER
    public UserResponse restore(Long id) {

        User user = userRepository.findByIdAndIsDeletedTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Deleted user", id));

        if (user.getCompany() == null
                || user.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Cannot restore user because the company is disabled");
        }

        user.restore();

        return userMapper.toResponse(userRepository.save(user));
    }
}