package com.example.resort.service;

import com.example.resort.aop.logging.Auditable;
import com.example.resort.dto.request.customer.CustomerCreateRequest;
import com.example.resort.dto.request.customer.CustomerUpdateRequest;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.dto.response.customer.CustomerResponse;
import com.example.resort.entity.User;
import com.example.resort.entity.customer.Customer;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.CustomerMapper;
import com.example.resort.repository.CustomerRepository;
import com.example.resort.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository;

    @CacheEvict(value = {"customers", "customer"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "Customer",
            entityId = "#result.customerId",
            detail = "'Created customer profile ' + #result.customerId"
    )
    public CustomerResponse createCustomer(CustomerCreateRequest request)
    {
        if (hasRole("ROLE_USER")) {
            return createCustomerProfileForCurrentUser(request);
        }

        validateWalkInCustomerRequest(request);
        validateCustomerUniqueness(
                request.getEmail(),
                request.getPhoneNumber(),
                request.getIdentityNumber(),
                null
        );

        Customer customer = customerMapper.toCustomer(request);
        return toMaskedResponse(customerRepository.save(customer));
    }

    @Cacheable(value = "customers", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<Customer> customerPage = customerRepository.findAllActive(pageable);

        List<CustomerResponse> data = customerPage.getContent()
                .stream()
                .map(this::toMaskedResponse)
                .toList();

        return PageResponse.<CustomerResponse> builder()
                .data(data)
                .currentPage(customerPage.getNumber())
                .pageSize(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .build();

    }

    @Cacheable(value = "customer", key = "#customerId")
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(String customerId)
    {
        Customer customer = customerRepository.findActiveByCustomerId(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return toMaskedResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile()
    {
        Customer customer = customerRepository.findActiveByUsername(currentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return toOwnerResponse(customer);
    }

    @CacheEvict(value = {"customers", "customer"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "DELETE",
            entity = "Customer",
            entityId = "#p0",
            detail = "'Deleted customer ' + #p0"
    )
    public void deleteCustomer(String customerId)
    {
        Customer customer = customerRepository.findActiveByCustomerId(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        customer.setActive(false);
        customerRepository.save(customer);
    }

    @CacheEvict(value = {"customers", "customer"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "UPDATE",
            entity = "Customer",
            entityId = "#result.customerId",
            detail = "'Updated customer ' + #result.customerId"
    )
    public CustomerResponse updateCustomer(String customerId, CustomerUpdateRequest request)
    {
        Customer customer = customerRepository.findActiveByCustomerId(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        validateWalkInCustomerUpdateRequest(request);
        validateCustomerUniqueness(
                request.getEmail(),
                request.getPhoneNumber(),
                request.getIdentityNumber(),
                customerId
        );
        customerMapper.updateCustomer(customer, request);
        return toMaskedResponse(customerRepository.save(customer));
    }

    @CacheEvict(value = {"customers", "customer"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "UPDATE",
            entity = "Customer",
            entityId = "#result.customerId",
            detail = "'Updated current customer profile ' + #result.customerId"
    )
    public CustomerResponse updateMyProfile(CustomerUpdateRequest request)
    {
        Customer customer = customerRepository.findActiveByUsername(currentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        User user = findCurrentUser();

        validateCustomerProfileUpdateRequest(request);
        validateCustomerUniqueness(
                request.getEmail(),
                request.getPhoneNumber(),
                request.getIdentityNumber(),
                customer.getCustomerId()
        );
        validateUserUniqueness(request.getEmail(), request.getPhoneNumber(), user.getUserId());

        customerMapper.updateCustomer(customer, request);
        syncUserFromCustomer(user, customer);
        userRepository.save(user);
        return toOwnerResponse(customerRepository.save(customer));
    }

    private CustomerResponse createCustomerProfileForCurrentUser(CustomerCreateRequest request)
    {
        String username = currentUsername();
        if (customerRepository.existsActiveByUsername(username)) {
            throw new AppException(ErrorCode.CUSTOMER_PROFILE_EXISTS);
        }

        User user = findCurrentUser();
        validateCustomerProfileCreateRequest(request);
        String nextFullName = firstPresent(request.getFullName(), user.getFullName());
        String nextPhoneNumber = firstPresent(request.getPhoneNumber(), user.getPhoneNumber());
        String nextEmail = firstPresent(request.getEmail(), user.getEmail());

        validateRequired(nextFullName);
        validateRequired(nextPhoneNumber);
        validateRequired(nextEmail);
        validateRequired(request.getIdentityNumber());
        validateCustomerUniqueness(
                nextEmail,
                nextPhoneNumber,
                request.getIdentityNumber(),
                null
        );
        validateUserUniqueness(nextEmail, nextPhoneNumber, user.getUserId());

        user.setFullName(nextFullName);
        user.setPhoneNumber(nextPhoneNumber);
        user.setEmail(nextEmail);
        userRepository.save(user);

        Customer customer = customerMapper.toCustomer(request);
        syncCustomerFromUser(customer, user);
        customer.setUser(user);

        return toOwnerResponse(customerRepository.save(customer));
    }

    private void validateWalkInCustomerRequest(CustomerCreateRequest request)
    {
        validateRequired(request.getFullName());
        validateRequired(request.getPhoneNumber());
        validateRequired(request.getEmail());
        validateRequired(request.getIdentityNumber());
    }

    private void validateWalkInCustomerUpdateRequest(CustomerUpdateRequest request)
    {
        validateOptionalText(request.getFullName());
        validateOptionalText(request.getPhoneNumber());
        validateOptionalText(request.getEmail());
        validateOptionalText(request.getIdentityNumber());
    }

    private void validateCustomerProfileCreateRequest(CustomerCreateRequest request)
    {
        validateOptionalText(request.getFullName());
        validateOptionalText(request.getPhoneNumber());
        validateOptionalText(request.getEmail());
    }

    private void validateCustomerProfileUpdateRequest(CustomerUpdateRequest request)
    {
        validateOptionalText(request.getFullName());
        validateOptionalText(request.getPhoneNumber());
        validateOptionalText(request.getEmail());
        validateOptionalText(request.getIdentityNumber());
    }

    private void validateUserUniqueness(String email, String phoneNumber, String ignoredUserId)
    {
        if (email != null && userRepository.existsByEmailAndUserIdNot(email, ignoredUserId)) {
            throw new AppException(ErrorCode.USER_EMAIL_EXISTS);
        }
        if (phoneNumber != null && userRepository.existsByPhoneNumberAndUserIdNot(phoneNumber, ignoredUserId)) {
            throw new AppException(ErrorCode.USER_PHONE_EXISTS);
        }
    }

    private void validateCustomerUniqueness(String email, String phoneNumber, String identityNumber, String ignoredCustomerId)
    {
        if (ignoredCustomerId == null) {
            if (email != null && customerRepository.existsByEmail(email)) {
                throw new AppException(ErrorCode.CUSTOMER_EMAIL_EXISTS);
            }
            if (phoneNumber != null && customerRepository.existsByPhoneNumber(phoneNumber)) {
                throw new AppException(ErrorCode.CUSTOMER_PHONE_EXISTS);
            }
            if (identityNumber != null && customerRepository.existsByIdentityNumber(identityNumber)) {
                throw new AppException(ErrorCode.CUSTOMER_IDENTITY_EXISTS);
            }
            return;
        }

        if (email != null && customerRepository.existsByEmailAndCustomerIdNot(email, ignoredCustomerId)) {
            throw new AppException(ErrorCode.CUSTOMER_EMAIL_EXISTS);
        }
        if (phoneNumber != null && customerRepository.existsByPhoneNumberAndCustomerIdNot(phoneNumber, ignoredCustomerId)) {
            throw new AppException(ErrorCode.CUSTOMER_PHONE_EXISTS);
        }
        if (identityNumber != null && customerRepository.existsByIdentityNumberAndCustomerIdNot(identityNumber, ignoredCustomerId)) {
            throw new AppException(ErrorCode.CUSTOMER_IDENTITY_EXISTS);
        }
    }

    private void syncCustomerFromUser(Customer customer, User user)
    {
        customer.setFullName(user.getFullName());
        customer.setPhoneNumber(user.getPhoneNumber());
        customer.setEmail(user.getEmail());
    }

    private void syncUserFromCustomer(User user, Customer customer)
    {
        user.setFullName(customer.getFullName());
        user.setPhoneNumber(customer.getPhoneNumber());
        user.setEmail(customer.getEmail());
    }

    private CustomerResponse toOwnerResponse(Customer customer)
    {
        return customerMapper.toCustomerResponse(customer);
    }

    private CustomerResponse toMaskedResponse(Customer customer)
    {
        CustomerResponse response = customerMapper.toCustomerResponse(customer);
        response.setIdentityNumber(null);
        return response;
    }

    private User findCurrentUser()
    {
        return userRepository.findActiveByUsername(currentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateRequired(String value)
    {
        if (isBlank(value)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateOptionalText(String value)
    {
        if (value != null && isBlank(value)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private String firstPresent(String preferred, String fallback)
    {
        return isBlank(preferred) ? fallback : preferred;
    }

    private String currentUsername()
    {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean hasRole(String role)
    {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
