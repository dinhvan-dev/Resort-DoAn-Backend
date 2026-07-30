package com.example.resort.entity.room;

import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.enums.rooms.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomId")
    private Long roomId;

    @Column(name = "roomNumber", nullable = false, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    @Column(name = "pricePerNight", nullable = false)
    private Double pricePerNight;

    @Column(name = "imageUrl", length = 1024)
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "roomId"))
    @OrderColumn(name = "sortOrder")
    @Column(name = "imageUrl", nullable = false, length = 1024)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Column(name = "floor")
    private Integer floor;
}
