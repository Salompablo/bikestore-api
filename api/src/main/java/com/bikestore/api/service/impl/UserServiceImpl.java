package com.bikestore.api.service.impl;

import com.bikestore.api.dto.response.UserResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.UserMapper;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void deactivateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        existingUser.setIsActive(false);
        userRepository.save(existingUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void updateDefaultPhone(User user, String phone) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        existingUser.setDefaultPhone(phone);
        userRepository.save(existingUser);
    }

    @Override
    public UserResponse getMyProfile(User user) {
        return userMapper.toResponse(user);
    }
}
