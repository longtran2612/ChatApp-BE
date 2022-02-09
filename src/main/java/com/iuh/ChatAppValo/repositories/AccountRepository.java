package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.Account;
import com.iuh.ChatAppValo.services.AccountService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String>, AccountService {
    Optional<Account> findByUsername(String username);
    Boolean existsByUsername(String username);
}
