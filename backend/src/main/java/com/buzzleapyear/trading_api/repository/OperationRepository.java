package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {
    Optional<Operation> findByOperationName(String operationName);
    List<Operation> findByUserId(Long userId);
}
