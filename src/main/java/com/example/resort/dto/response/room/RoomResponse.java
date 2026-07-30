package com.example.resort.dto.response.room;

import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.enums.rooms.RoomType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
    private Long roomId;
    private String roomNumber;
    private RoomType type;
    private RoomStatus status;
    private Double pricePerNight;
    private String imageUrl;
    private List<String> imageUrls;
    private Integer floor;
}
