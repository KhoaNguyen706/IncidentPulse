package com.example.IncidentPulse.Repository;

import com.example.IncidentPulse.Model.OnCallShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OnCallShiftRepository extends JpaRepository<OnCallShift,Long> {

    @Query("SELECT o FROM OnCallShift o WHERE :now BETWEEN o.startedAt AND o.endAt")
    Optional<OnCallShift> findCurrentOnCallShift(LocalDateTime now);
}
