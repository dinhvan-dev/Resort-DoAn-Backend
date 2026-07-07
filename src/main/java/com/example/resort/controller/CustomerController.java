package com.example.resort.controller;

import com.example.resort.dto.request.customer.CustomerCreateRequest;
import com.example.resort.dto.request.customer.CustomerUpdateRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.dto.response.customer.CustomerResponse;
import com.example.resort.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/customers", "/Customer"})
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    @PostMapping
    ApiResponse<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request)
    {
        return ApiResponse.<CustomerResponse>builder()
                .result(customerService.createCustomer(request))
                .build();
    }
    @GetMapping
    ApiResponse<PageResponse<CustomerResponse>> getAllCustomers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size)
    {
        return ApiResponse.<PageResponse<CustomerResponse>> builder()
                .result(customerService.getAllCustomers(page, size))
                .build();
    }

    @GetMapping("/my-profile")
    ApiResponse<CustomerResponse> getMyProfile()
    {
        return ApiResponse.<CustomerResponse>builder()
                .result(customerService.getMyProfile())
                .build();
    }

    @PutMapping("/my-profile")
    ApiResponse<CustomerResponse> updateMyProfile(@Valid @RequestBody CustomerUpdateRequest request)
    {
        return ApiResponse.<CustomerResponse>builder()
                .result(customerService.updateMyProfile(request))
                .build();
    }

    @GetMapping("/{customerId}")
    ApiResponse<CustomerResponse> getCustomerById(@PathVariable String customerId)
    {
        return ApiResponse.<CustomerResponse>builder()
                .result(customerService.getCustomerById(customerId))
                .build();
    }

    @DeleteMapping("/{customerId}")
    ApiResponse<Void> deleteCustomer(@PathVariable("customerId") String customerId)
    {
        customerService.deleteCustomer(customerId);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PutMapping("/{customerId}")
    ApiResponse<CustomerResponse> updateCustomer(@PathVariable String customerId, @Valid @RequestBody CustomerUpdateRequest request) {
        return ApiResponse.<CustomerResponse>builder()
                .result(customerService.updateCustomer(customerId, request))
                .build();
    }
}
