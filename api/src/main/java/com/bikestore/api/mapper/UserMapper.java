package com.bikestore.api.mapper;

import com.bikestore.api.dto.request.RegisterRequest;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.AuthProvider;
import com.bikestore.api.entity.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request, String encodedPassword) {
        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(encodedPassword)
                .role(Role.CUSTOMER)
                .provider(AuthProvider.LOCAL)
                .build();
    }

    public User toGoogleUserEntity(String email, String firstName, String lastName) {
        return User.builder()
                .firstName(firstName != null ? firstName : "Usuario")
                .lastName(lastName != null ? lastName : "")
                .email(email)
                .role(Role.CUSTOMER)
                .provider(AuthProvider.GOOGLE)
                .isEmailVerified(true)
                .build();
    }
}
