package com.example.IncidentPulse.Repository;

import com.example.IncidentPulse.Model.OnCallShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OnCallShiftRepository extends JpaRepository<OnCallShift, Long> {

    /**
     * Active shifts for {@code now}. Overlapping windows are allowed in the DB;
     * callers pick the most recently created row as the effective on-call engineer.
     */
    @Query("""
            SELECT o FROM OnCallShift o
            WHERE :now BETWEEN o.startedAt AND o.endAt
            ORDER BY o.createdAt DESC
            """)
    List<OnCallShift> findActiveOnCallShifts(@Param("now") LocalDateTime now);

    default Optional<OnCallShift> findCurrentOnCallShift(LocalDateTime now) {
        List<OnCallShift> shifts = findActiveOnCallShifts(now);
        return shifts.isEmpty() ? Optional.empty() : Optional.of(shifts.getFirst());
    }

    @Query("""
            SELECT o FROM OnCallShift o
            JOIN FETCH o.user_id
            ORDER BY o.startedAt DESC
            """)
    List<OnCallShift> findAllWithUserOrderByStartedAtDesc();

    @Query("""
            SELECT o FROM OnCallShift o
            WHERE o.user_id.id = :userId AND :now BETWEEN o.startedAt AND o.endAt
            ORDER BY o.createdAt DESC
            """)
    List<OnCallShift> findActiveShiftsForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
