package com.example.resort.repository;

import com.example.resort.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
        boolean existsByEmail(String email);

        boolean existsByPhoneNumber(String phoneNumber);
        boolean existsByIdentityNumber(String identityNumber);

        boolean existsByEmailAndCustomerIdNot(String email, String customerId);
        boolean existsByPhoneNumberAndCustomerIdNot(String phoneNumber, String customerId);
        boolean existsByIdentityNumberAndCustomerIdNot(String identityNumber, String customerId);

        @Query("SELECT c FROM Customer c WHERE c.isActive = true")
        Page<Customer> findAllActive(Pageable pageable);

        @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId AND c.isActive = true")
        Optional<Customer> findActiveByCustomerId(@Param("customerId") String customerId);

        @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.customerId = :customerId AND c.isActive = true")
        boolean existsActiveByCustomerId(@Param("customerId") String customerId);

        @Query("SELECT c FROM Customer c WHERE c.user.username = :username AND c.isActive = true")
        Optional<Customer> findActiveByUsername(@Param("username") String username);

        @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.user.username = :username AND c.isActive = true")
        boolean existsActiveByUsername(@Param("username") String username);

        @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true")
        long countActiveCustomers();
}
