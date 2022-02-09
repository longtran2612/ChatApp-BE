package com.iuh.ChatAppValo.services;

import com.iuh.ChatAppValo.entity.User;

public interface UserService {
    void partialUpdate(String userId, String fieldName, Object fieldValue);
}
