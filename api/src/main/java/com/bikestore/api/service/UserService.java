package com.bikestore.api.service;

import com.bikestore.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    void deactivateUser(User user);
    Page<User> getAllUsers(Pageable pageable);
}
