package com.bikestore.api.service;

import com.bikestore.api.entity.User;
import com.bikestore.api.entity.VerificationToken;
import com.bikestore.api.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;

    public String generateAndSaveVerificationToken(User user) {
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        VerificationToken verificationToken = VerificationToken.builder()
                .token(verificationCode)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        verificationTokenRepository.save(verificationToken);

        return verificationCode;
    }

    public User validateAndDeleteToken(String tokenString) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();

        verificationTokenRepository.delete(verificationToken);

        return user;
    }
}