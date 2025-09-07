package com.rotacerta.api.repository;

import com.rotacerta.api.model.entities.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
	Optional<Plan> findByStripePriceId(String stripePriceId);
	Optional<Plan> findByName(String name);
}
