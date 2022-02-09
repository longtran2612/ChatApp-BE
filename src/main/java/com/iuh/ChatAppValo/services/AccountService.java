package com.iuh.ChatAppValo.services;

public interface AccountService {
    void setRefreshToken(String accountId, String refreshToken);
    void setOnlineStatus(String username);
    void setOfflineStatus(String username);
}
