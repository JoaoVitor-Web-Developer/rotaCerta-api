package com.rotacerta.api.repository;

import com.rotacerta.api.model.entities.OptimizedRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OptimizedRouteRepository extends JpaRepository<OptimizedRoute, UUID> {
}
