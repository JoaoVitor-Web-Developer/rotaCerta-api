package com.rotacerta.api.repository;

import com.rotacerta.api.model.entities.Subscription;
import com.rotacerta.api.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
	Optional<Subscription> findByUser(User user);
}
