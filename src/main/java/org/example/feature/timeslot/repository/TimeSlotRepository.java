package org.example.feature.timeslot.repository;

import org.example.feature.timeslot.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {

    List<TimeSlot> findAllByOrderByStartTimeAsc();

    List<TimeSlot> findBySessionOrderByStartTimeAsc(String session);

    boolean existsByStartTimeAndEndTime(LocalTime startTime, LocalTime endTime);

    boolean existsByStartTimeAndEndTimeAndIdNot(LocalTime startTime, LocalTime endTime, Integer id);

    @Query("SELECT COUNT(s) FROM TimeSlot s WHERE " +
           "s.startTime < :endTime AND s.endTime > :startTime AND " +
           "(:id IS NULL OR s.id <> :id)")
    long countOverlappingSlots(
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("id") Integer id
    );
}

