package org.example.feature.room.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "examination_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExaminationRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(length = 50)
    private String building;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

