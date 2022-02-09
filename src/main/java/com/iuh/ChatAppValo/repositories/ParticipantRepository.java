package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.Participant;
import com.iuh.ChatAppValo.services.ParticipantService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends MongoRepository<Participant, String>, ParticipantService {
}
