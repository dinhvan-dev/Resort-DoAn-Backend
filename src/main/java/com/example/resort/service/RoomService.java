package com.example.resort.service;

import com.example.resort.aop.logging.Auditable;
import com.example.resort.dto.request.room.RoomCreateRequest;
import com.example.resort.dto.request.room.RoomUpdateRequest;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.dto.response.room.RoomResponse;
import com.example.resort.dto.response.room.RoomTypeAvailabilityResponse;
import com.example.resort.entity.room.Room;
import com.example.resort.enums.rooms.RoomType;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.RoomMapper;
import com.example.resort.repository.BookingRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomMapper roomMapper;

    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "Room",
            entityId = "#result.roomNumber",
            detail = "'Created room ' + #result.roomNumber + ' type ' + #result.type"
    )
    public RoomResponse createRoom(RoomCreateRequest request)
    {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber()))
        {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        Room room = roomMapper.toRoom(request);
        room.setFloor(extractFloor(request.getRoomNumber()));
        syncPrimaryImage(room);
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

    @Transactional(readOnly = true)
    public List<RoomTypeAvailabilityResponse> getRoomTypeAvailability(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer numberOfGuests,
            Integer quantity
    )
    {
        boolean hasDateRange = checkInDate != null && checkOutDate != null;
        return Arrays.stream(RoomType.values())
                .map(roomType -> toRoomTypeAvailability(roomType, checkInDate, checkOutDate))
                .filter(item -> item.getAvailableCount() >= (quantity == null ? 1 : quantity))
                .filter(item -> !hasDateRange || numberOfGuests == null || numberOfGuests <= item.getCapacity() * (quantity == null ? 1 : quantity))
                .toList();
    }

    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "DELETE",
            entity = "Room",
            entityId = "#p0",
            detail = "'Deleted room ' + #p0"
    )
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
    @Auditable(
            action = "UPDATE",
            entity = "Room",
            entityId = "#result.roomNumber",
            detail = "'Updated room ' + #result.roomNumber + ' status ' + #result.status"
    )
    public RoomResponse updateRoom(String roomNumber, RoomUpdateRequest request)
    {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        roomMapper.updateRoom(room, request);
        room.setFloor(extractFloor(room.getRoomNumber()));
        syncPrimaryImage(room);
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

    private void syncPrimaryImage(Room room) {
        if (room.getImageUrls() != null && !room.getImageUrls().isEmpty()) {
            room.setImageUrl(room.getImageUrls().getFirst());
        }
    }

    private RoomTypeAvailabilityResponse toRoomTypeAvailability(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate)
    {
        List<Room> rooms = roomRepository.findByTypeOrderByPrice(roomType);
        Long reservedQuantity = checkInDate != null && checkOutDate != null
                ? bookingRepository.sumReservedQuantityByType(roomType, checkInDate, checkOutDate, null)
                : 0L;
        int availableCount = Math.max(0, rooms.size() - reservedQuantity.intValue());
        Room representative = findRepresentativeRoom(roomType, checkInDate, checkOutDate, rooms);

        return RoomTypeAvailabilityResponse.builder()
                .roomType(roomType)
                .displayName(displayName(roomType))
                .representativeRoomNumber(representative == null ? null : representative.getRoomNumber())
                .availableCount(availableCount)
                .capacity(capacity(roomType))
                .pricePerNight(representative == null ? 0D : representative.getPricePerNight())
                .imageUrls(representative == null ? List.of() : copyImageUrls(representative))
                .highlights(highlights(roomType))
                .build();
    }

    private List<String> copyImageUrls(Room room)
    {
        if (room.getImageUrls() != null && !room.getImageUrls().isEmpty())
        {
            return new ArrayList<>(room.getImageUrls());
        }
        return room.getImageUrl() == null ? List.of() : List.of(room.getImageUrl());
    }

    private Room findRepresentativeRoom(
            RoomType roomType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            List<Room> rooms
    )
    {
        List<Room> candidateRooms = checkInDate != null && checkOutDate != null
                ? roomRepository.findAvailableRoomsByType(roomType, checkInDate, checkOutDate)
                : rooms;

        return candidateRooms.stream()
                .filter(this::hasRoomImage)
                .findFirst()
                .orElseGet(() -> candidateRooms.stream().findFirst().orElse(null));
    }

    private boolean hasRoomImage(Room room)
    {
        return room != null
                && ((room.getImageUrls() != null && !room.getImageUrls().isEmpty())
                || (room.getImageUrl() != null && !room.getImageUrl().isBlank()));
    }

    private String displayName(RoomType roomType)
    {
        return switch (roomType) {
            case SINGLE -> "Single Room";
            case DOUBLE -> "Double Room";
            case VIP -> "VIP Suite";
        };
    }

    private int capacity(RoomType roomType)
    {
        return switch (roomType) {
            case SINGLE -> 1;
            case DOUBLE -> 2;
            case VIP -> 4;
        };
    }

    private List<String> highlights(RoomType roomType)
    {
        return switch (roomType) {
            case SINGLE -> List.of("1 single bed", "Workspace", "Private bathroom");
            case DOUBLE -> List.of("1 double bed", "Balcony view", "Up to 2 adults");
            case VIP -> List.of("Premium view", "Bathtub", "Breakfast included");
        };
    }
}
