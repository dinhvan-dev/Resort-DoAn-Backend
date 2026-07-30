package com.example.resort.mapper;

import com.example.resort.dto.request.customer.CustomerCreateRequest;
import com.example.resort.dto.request.customer.CustomerUpdateRequest;
import com.example.resort.dto.response.customer.CustomerResponse;
import com.example.resort.entity.customer.Customer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "user", ignore = true)
    Customer toCustomer(CustomerCreateRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCustomer(@MappingTarget Customer customer, CustomerUpdateRequest request);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(target = "identityMasked", expression = "java(maskIdentity(customer))")
    CustomerResponse toCustomerResponse(Customer customer);

    default String maskIdentity(Customer customer) {
        String identityNumber = customer == null ? null : customer.getIdentityNumber();
        if (identityNumber == null || identityNumber.isBlank()) {
            return null;
        }

        String digits = identityNumber.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "*".repeat(digits.length());
        }

        return "*".repeat(Math.max(0, digits.length() - 4)) + digits.substring(digits.length() - 4);
    }
}
