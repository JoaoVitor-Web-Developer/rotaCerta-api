package com.rotacerta.api.repository;

import com.rotacerta.api.model.entities.Quote;
import com.rotacerta.api.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
	List<Quote> findByUserOrderByCreatedAtDesc(User user);
}
