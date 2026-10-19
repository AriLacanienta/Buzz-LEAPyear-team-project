package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
	List<LoginAttempt> findAllByOrderByAttemptedAtDesc();

	List<LoginAttempt> findByEmailIgnoreCaseOrderByAttemptedAtDesc(String email);
}