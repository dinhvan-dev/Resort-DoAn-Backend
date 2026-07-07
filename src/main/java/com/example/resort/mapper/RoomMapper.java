package com.example.resort.mapper;

import com.example.resort.dto.request.room.RoomCreateRequest;
import com.example.resort.dto.request.room.RoomUpdateRequest;
import com.example.resort.dto.response.room.RoomResponse;
import com.example.resort.entity.room.Room;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(target = "status", constant = "AVAILABLE")
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "floor", ignore = true)
    Room toRoom(RoomCreateRequest request);

    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "floor", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRoom(@MappingTarget Room room, RoomUpdateRequest request);

    RoomResponse toRoomResponse(Room room);
}
