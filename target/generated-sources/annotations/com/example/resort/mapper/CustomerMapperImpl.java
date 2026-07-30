package com.example.resort.mapper;

import com.example.resort.dto.request.customer.CustomerCreateRequest;
import com.example.resort.dto.request.customer.CustomerUpdateRequest;
import com.example.resort.dto.response.customer.CustomerResponse;
import com.example.resort.entity.User;
import com.example.resort.entity.customer.Customer;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T16:51:52+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public Customer toCustomer(CustomerCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Customer.CustomerBuilder<?, ?> customer = Customer.builder();

        customer.fullName( request.getFullName() );
        customer.phoneNumber( request.getPhoneNumber() );
        customer.email( request.getEmail() );
        customer.identityNumber( request.getIdentityNumber() );
        customer.dateOfBirth( request.getDateOfBirth() );

        return customer.build();
    }

    @Override
    public void updateCustomer(Customer customer, CustomerUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.getFullName() != null ) {
            customer.setFullName( request.getFullName() );
        }
        if ( request.getPhoneNumber() != null ) {
            customer.setPhoneNumber( request.getPhoneNumber() );
        }
        if ( request.getEmail() != null ) {
            customer.setEmail( request.getEmail() );
        }
        if ( request.getIdentityNumber() != null ) {
            customer.setIdentityNumber( request.getIdentityNumber() );
        }
        if ( request.getDateOfBirth() != null ) {
            customer.setDateOfBirth( request.getDateOfBirth() );
        }
    }

    @Override
    public CustomerResponse toCustomerResponse(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerResponse.CustomerResponseBuilder customerResponse = CustomerResponse.builder();

        customerResponse.userId( customerUserUserId( customer ) );
        customerResponse.customerId( customer.getCustomerId() );
        customerResponse.fullName( customer.getFullName() );
        customerResponse.phoneNumber( customer.getPhoneNumber() );
        customerResponse.email( customer.getEmail() );
        customerResponse.identityNumber( customer.getIdentityNumber() );
        customerResponse.dateOfBirth( customer.getDateOfBirth() );
        customerResponse.createdAt( customer.getCreatedAt() );

        customerResponse.identityMasked( maskIdentity(customer) );

        return customerResponse.build();
    }

    private String customerUserUserId(Customer customer) {
        User user = customer.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getUserId();
    }
}
