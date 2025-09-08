package com.rotacerta.api.repository;

import com.rotacerta.api.model.entities.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
}
