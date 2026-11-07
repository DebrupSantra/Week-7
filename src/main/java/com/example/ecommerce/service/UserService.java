package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProfileUpdateRequest;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.AppUser;
import com.example.ecommerce.entity.ShoppingCart;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.ShoppingCartRepository;
import com.example.ecommerce.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ShoppingCartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            ShoppingCartRepository cartRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }
        AppUser user = userRepository.save(new AppUser(
                email, passwordEncoder.encode(request.password()), request.fullName().trim()));
        cartRepository.save(new ShoppingCart(user));
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser requireByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    public AppUser updateProfile(String email, ProfileUpdateRequest request) {
        AppUser user = requireByEmail(email);
        user.setFullName(request.fullName().trim());
        return userRepository.save(user);
    }
}
