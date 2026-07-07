package com.example.resort.controller;

import com.example.resort.dto.request.room.RoomCreateRequest;
import com.example.resort.dto.request.room.RoomUpdateRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.dto.response.room.RoomResponse;
import com.example.resort.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/rooms")
@RestController
@RequiredArgsConstructor
public class RoomController {

    private final  RoomService roomService;

    @PostMapping
    ApiResponse<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request)
    {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.createRoom(request))
                .build();
    }

    @GetMapping
    ApiResponse<PageResponse<RoomResponse>> getAllRoom(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size)
    {
        return ApiResponse.<PageResponse<RoomResponse>> builder()
                .result(roomService.getAllRoom(page, size))
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
