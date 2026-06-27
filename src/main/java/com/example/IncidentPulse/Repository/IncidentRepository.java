package com.example.IncidentPulse.Repository;


import com.example.IncidentPulse.Model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident,Long>, JpaSpecificationExecutor<Incident> {
    
    // Prevent N+1 query problem by fetching users with incidents
    @Query("SELECT i FROM Incident i LEFT JOIN FETCH i.createdBy LEFT JOIN FETCH i.assignedTo")
    List<Incident> findAllWithUsers();

    List<Incident> findByAssignedTo_IdOrderByUpdatedAtDesc(Long assignedToId);

    // Used by the webhook ingestion path to deduplicate alerts from the same monitor.
    Optional<Incident> findFirstBySourceAndExternalId(String source, String externalId);
}
