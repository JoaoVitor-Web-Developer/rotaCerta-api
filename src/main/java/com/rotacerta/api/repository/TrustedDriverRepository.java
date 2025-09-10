package com.rotacerta.api.repository;

import com.rotacerta.api.model.entities.TrustedDriver;
import com.rotacerta.api.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrustedDriverRepository extends JpaRepository<TrustedDriver, UUID> {
	List<TrustedDriver> findByUser(User user);
}
