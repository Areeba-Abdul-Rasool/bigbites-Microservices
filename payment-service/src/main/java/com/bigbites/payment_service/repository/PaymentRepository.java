package com.bigbites.payment_service.repository;
import com.bigbites.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bigbites.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByTransactionRef(String transactionRef);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double getTotalSuccessfulRevenue();

    List<Payment> findByMethod(String method);
}