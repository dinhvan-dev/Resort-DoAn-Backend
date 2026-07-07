package com.example.resort.service;

import com.example.resort.dto.request.room.RoomCreateRequest;
import com.example.resort.dto.request.room.RoomUpdateRequest;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.dto.response.room.RoomResponse;
import com.example.resort.entity.room.Room;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.RoomMapper;
import com.example.resort.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request)
    {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber()))
        {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        Room room = roomMapper.toRoom(request);
        room.setFloor(extractFloor(request.getRoomNumber()));
        return roomMapper.toRoomResponse(roomRepository.save(room));
    }

    @Cacheable(value = "rooms", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getAllRoom(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("roomId").ascending());
        Page<Room> roomPage = roomRepository.findAll(pageable);
        List<RoomResponse> data = roomPage.getContent()
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();

        return PageResponse.<RoomResponse> builder()
                .data(data)
                .currentPage(roomPage.getNumber())
                .pageSize(roomPage.getSize())
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .first(roomPage.isFirst())
                .last(roomPage.isLast())
                .build();
    }

    @Cacheable(value = "room", key = "#roomNumber")
    @Transactional(readOnly = true)
    public RoomResponse getRoomNumber(String roomNumber)
    {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        return roomMapper.toRoomResponse(room);
    }

    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    @Transactional
    public void deleteRoom(String roomNumber)
    {
        if(!roomRepository.existsByRoomNumber(roomNumber))
        {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
        roomRepository.deleteByRoomNumber(roomNumber);
    }

    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    @Transactional
    public RoomResponse updateRoom(String roomNumber, RoomUpdateRequest request)
    {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        roomMapper.updateRoom(room, request);
        room.setFloor(extractFloor(room.getRoomNumber()));
        return roomMapper.toRoomResponse(roomRepository.save(room));
    }

    private int extractFloor(String roomNumber)
    {
        if (roomNumber == null || !roomNumber.matches("\\d{3,}"))
        {
            throw new AppException(ErrorCode.INVALID_ROOM_NUMBER);
        }

        int floor = Integer.parseInt(roomNumber.substring(0, roomNumber.length() - 2));
        if (floor < 1)
        {
            throw new AppException(ErrorCode.INVALID_ROOM_NUMBER);
        }

        return floor;
    }
}
