package com.example.resort.dto.response.room;

import com.example.resort.enums.rooms.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomTypeAvailabilityResponse {
    private RoomType roomType;
    private String displayName;
    private String representativeRoomNumber;
    private Integer availableCount;
    private Integer capacity;
    private Double pricePerNight;
    private List<String> imageUrls;
    private List<String> highlights;
}
