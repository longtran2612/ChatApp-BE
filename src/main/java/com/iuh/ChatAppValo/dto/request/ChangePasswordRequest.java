package com.iuh.ChatAppValo.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String phoneNumber;
    private String newPassword;
}
