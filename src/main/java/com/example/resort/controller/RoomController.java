package com.example.resort.controller;

import com.example.resort.dto.request.room.RoomCreateRequest;
import com.example.resort.dto.request.room.RoomUpdateRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.dto.response.room.RoomResponse;
import com.example.resort.dto.response.room.RoomTypeAvailabilityResponse;
import com.example.resort.service.RoomImageStorageService;
import com.example.resort.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDate;

@RequestMapping("/rooms")
@RestController
@RequiredArgsConstructor
public class RoomController {

    private final  RoomService roomService;
    private final RoomImageStorageService roomImageStorageService;

    @PostMapping
    ApiResponse<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request)
    {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.createRoom(request))
                .build();
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<List<String>> uploadRoomImages(@RequestPart("images") List<MultipartFile> images)
    {
        return ApiResponse.<List<String>>builder()
                .result(roomImageStorageService.storeRoomImages(images))
                .build();
    }

    @GetMapping
    ApiResponse<PageResponse<RoomResponse>> getAllRoom(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size)
    {
        return ApiResponse.<PageResponse<RoomResponse>> builder()
                .result(roomService.getAllRoom(page, size))
                .build();
    }

    @GetMapping("/availability")
    ApiResponse<List<RoomTypeAvailabilityResponse>> getRoomTypeAvailability(
            @RequestParam(required = false) LocalDate checkInDate,
            @RequestParam(required = false) LocalDate checkOutDate,
            @RequestParam(required = false) Integer numberOfGuests,
            @RequestParam(defaultValue = "1") Integer quantity)
    {
        return ApiResponse.<List<RoomTypeAvailabilityResponse>>builder()
                .result(roomService.getRoomTypeAvailability(checkInDate, checkOutDate, numberOfGuests, quantity))
                .build();
    }

    @GetMapping("/{roomNumber}")
    ApiResponse<RoomResponse> getRoomNumber(@PathVariable String roomNumber)
    {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.getRoomNumber(roomNumber))
                .build();
    }

    @DeleteMapping("/{roomNumber}")
    ApiResponse<Void> deleteRoom(@PathVariable("roomNumber") String roomNumber)
    {
        roomService.deleteRoom(roomNumber);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PutMapping("/{roomNumber}")
    ApiResponse<RoomResponse> updateRoom(@PathVariable String roomNumber, @Valid @RequestBody RoomUpdateRequest request)
    {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.updateRoom(roomNumber, request))
                .build();
    }
}
