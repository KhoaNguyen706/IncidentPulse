package com.example.IncidentPulse.Repository;


import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident,Long> {
    
    // Prevent N+1 query problem by fetching users with incidents
    @Query("SELECT i FROM Incident i LEFT JOIN FETCH i.createdBy LEFT JOIN FETCH i.assignedTo")
    List<Incident> findAllWithUsers();

    Incident findIncidentByAssignedTo(User assignedTo); 
}
