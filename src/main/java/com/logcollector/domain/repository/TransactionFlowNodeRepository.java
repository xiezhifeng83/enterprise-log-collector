package com.logcollector.domain.repository;

import com.logcollector.domain.model.TransactionFlowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TransactionFlowNode entity.
 */
@Repository
public interface TransactionFlowNodeRepository extends JpaRepository<TransactionFlowNode, Long> {

    List<TransactionFlowNode> findByTransactionIdOrderBySequenceOrderAsc(String transactionId);

    List<TransactionFlowNode> findByTransactionIdOrderByTimestampAsc(String transactionId);

    @Query("SELECT f FROM TransactionFlowNode f WHERE f.transactionId = :transactionId AND f.systemName = :system ORDER BY f.sequenceOrder ASC")
    List<TransactionFlowNode> findByTransactionAndSystem(
            @Param("transactionId") String transactionId,
            @Param("system") String system);

    void deleteByTransactionId(String transactionId);
}
