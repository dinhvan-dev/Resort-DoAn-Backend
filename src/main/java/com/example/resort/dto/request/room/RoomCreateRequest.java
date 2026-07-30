package com.example.resort.dto.request.room;

import com.example.resort.enums.rooms.RoomType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomCreateRequest {
    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType type;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per night must be greater than 0")
    private Double pricePerNight;

    @Size(max = 1024, message = "Image URL must not exceed 1024 characters")
    private String imageUrl;

    @NotEmpty(message = "Room images are required")
    @Size(min = 3, max = 5, message = "Room must have between 3 and 5 images")
    private List<@Size(max = 1024, message = "Image URL must not exceed 1024 characters") String> imageUrls;
}
