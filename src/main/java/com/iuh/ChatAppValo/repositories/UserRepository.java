package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.User;
import com.iuh.ChatAppValo.services.UserService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String>, UserService {
    @Override
    boolean existsById(String s);

    Optional<User> findDistinctByPhone(String s);

    List<User> findAllByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByNameAsc(String name, String phone);

    List<User> findAllByAddressContainingIgnoreCase(String address);

    List<User> findAllByStatusContainingIgnoreCase(String status);
}
