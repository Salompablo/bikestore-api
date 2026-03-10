package com.bikestore.api.service;

import com.bikestore.api.dto.request.ForgotPasswordRequest;
import com.bikestore.api.dto.request.LoginRequest;
import com.bikestore.api.dto.request.RegisterRequest;
import com.bikestore.api.dto.request.ResetPasswordRequest;
import com.bikestore.api.dto.response.AuthResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.VerificationToken;
import com.bikestore.api.entity.enums.AuthProvider;
import com.bikestore.api.entity.enums.Role;
import com.bikestore.api.exception.AccountDeactivatedException;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.UserMapper;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.repository.VerificationTokenRepository;
import com.bikestore.api.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final VerificationTokenService tokenService;

    @Value("${google.client.id}")
    private String googleClientId;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email is already in use");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, encodedPassword);
        user = userRepository.save(user);

        String verificationCode = tokenService.generateAndSaveVerificationToken(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        return new AuthResponse("", "User registered successfully. Please check your email for the verification code.");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getIsActive()) {
            throw new AccountDeactivatedException("Your account has been deactivated. Please contact support.");
        }

        if (!user.getIsEmailVerified()) {
            throw new AccountDeactivatedException("Please verify your email before logging in.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, "Login successful");
    }

    @Transactional
    public AuthResponse loginWithGoogle(String googleToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null && !user.getIsActive()) {
                throw new DisabledException("Account is deactivated");
            }

            if (user == null) {
                user = userMapper.toGoogleUserEntity(email, firstName, lastName);
                user = userRepository.save(user);
            }

            String jwtToken = jwtService.generateToken(user);
            return new AuthResponse(jwtToken, "Google login successful");

        } catch (Exception e) {
            log.error("Error verifying Google token", e);
            throw new RuntimeException("Failed to authenticate with Google");
        }
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        User user = tokenService.validateAndDeleteToken(token);

        user.setIsEmailVerified(true);
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, "Email verified successfully");
    }

    @Transactional
    public void requestAccountReactivation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsActive()) {
            throw new ConflictException("Your account is already active.");
        }

        String reactivationCode = tokenService.generateAndSaveVerificationToken(user);

        emailService.sendReactivationEmail(user.getEmail(), reactivationCode);
    }

    @Transactional
    public AuthResponse processReactivation(String token) {
        User user = tokenService.validateAndDeleteToken(token);

        user.setIsActive(true);
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, "Account reactivated successfully. Welcome back!");
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (!user.getIsActive()) {
            throw new ConflictException("Account is deactivated");
        }

        String resetCode = String.format("%06d", new Random().nextInt(999999));

        user.setResetPasswordCode(resetCode);
        user.setResetPasswordExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), resetCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (user.getResetPasswordCode() == null || !user.getResetPasswordCode().equals(request.code())) {
            throw new ConflictException("Invalid reset code");
        }

        if (user.getResetPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Reset code has expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordCode(null);
        user.setResetPasswordExpiresAt(null);

        userRepository.save(user);
    }
}
