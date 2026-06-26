package com.example.IncidentPulse.Repository;

import com.example.IncidentPulse.Model.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {

    // JOIN FETCH the actor so the response can read actor fields without a
    // LazyInitializationException after the transaction closes.
    @Query("SELECT h FROM IncidentHistory h LEFT JOIN FETCH h.actor " +
            "WHERE h.incident.id = :incidentId ORDER BY h.createdAt ASC")
    List<IncidentHistory> findHistoryForIncident(@Param("incidentId") Long incidentId);
}
