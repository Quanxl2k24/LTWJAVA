package org.example.feature.room.repository;

import org.example.feature.room.model.ExaminationRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExaminationRoomRepository extends JpaRepository<ExaminationRoom, Integer> {

    List<ExaminationRoom> findByIsActiveTrueOrderByRoomNumberAsc();

    @Query("SELECT DISTINCT r.building FROM ExaminationRoom r WHERE r.building IS NOT NULL AND r.building <> '' ORDER BY r.building ASC")
    List<String> findDistinctBuildings();

    boolean existsByRoomNumberAndBuilding(String roomNumber, String building);

    boolean existsByRoomNumberAndBuildingAndIdNot(String roomNumber, String building, Integer id);

    boolean existsByRoomNumberAndBuildingIsNull(String roomNumber);

    boolean existsByRoomNumberAndBuildingIsNullAndIdNot(String roomNumber, Integer id);

    @Query("SELECT r FROM ExaminationRoom r WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.building) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:building IS NULL OR :building = '' OR LOWER(r.building) = LOWER(:building)) AND " +
           "(:isActive IS NULL OR r.isActive = :isActive)")
    Page<ExaminationRoom> searchRooms(
            @Param("keyword") String keyword,
            @Param("building") String building,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}

