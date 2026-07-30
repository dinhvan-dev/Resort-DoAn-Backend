package com.example.resort.mapper;

import com.example.resort.dto.request.room.RoomCreateRequest;
import com.example.resort.dto.request.room.RoomUpdateRequest;
import com.example.resort.dto.response.room.RoomResponse;
import com.example.resort.entity.room.Room;
import com.example.resort.enums.rooms.RoomStatus;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T16:51:51+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class RoomMapperImpl implements RoomMapper {

    @Override
    public Room toRoom(RoomCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Room.RoomBuilder room = Room.builder();

        room.roomNumber( request.getRoomNumber() );
        room.type( request.getType() );
        room.pricePerNight( request.getPricePerNight() );
        room.imageUrl( request.getImageUrl() );
        List<String> list = request.getImageUrls();
        if ( list != null ) {
            room.imageUrls( new ArrayList<String>( list ) );
        }

        room.status( RoomStatus.AVAILABLE );

        return room.build();
    }

    @Override
    public void updateRoom(Room room, RoomUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.getType() != null ) {
            room.setType( request.getType() );
        }
        if ( request.getPricePerNight() != null ) {
            room.setPricePerNight( request.getPricePerNight() );
        }
        if ( request.getImageUrl() != null ) {
            room.setImageUrl( request.getImageUrl() );
        }
        if ( room.getImageUrls() != null ) {
            List<String> list = request.getImageUrls();
            if ( list != null ) {
                room.getImageUrls().clear();
                room.getImageUrls().addAll( list );
            }
        }
        else {
            List<String> list = request.getImageUrls();
            if ( list != null ) {
                room.setImageUrls( new ArrayList<String>( list ) );
            }
        }
        if ( request.getStatus() != null ) {
            room.setStatus( request.getStatus() );
        }
    }

    @Override
    public RoomResponse toRoomResponse(Room room) {
        if ( room == null ) {
            return null;
        }

        RoomResponse.RoomResponseBuilder roomResponse = RoomResponse.builder();

        roomResponse.roomId( room.getRoomId() );
        roomResponse.roomNumber( room.getRoomNumber() );
        roomResponse.type( room.getType() );
        roomResponse.status( room.getStatus() );
        roomResponse.pricePerNight( room.getPricePerNight() );
        roomResponse.imageUrl( room.getImageUrl() );
        List<String> list = room.getImageUrls();
        if ( list != null ) {
            roomResponse.imageUrls( new ArrayList<String>( list ) );
        }
        roomResponse.floor( room.getFloor() );

        return roomResponse.build();
    }
}
