package com.example.IncidentPulse.Repository;

import com.example.IncidentPulse.Model.Incident;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable, null-safe JPA Specifications for querying incidents.
 * Each factory returns {@code null} when its filter value is absent, which
 * Spring Data treats as "no constraint" when the specs are combined. This lets
 * the service build one query from any mix of optional filters.
 */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> hasStatus(Incident.status status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasSeverity(Incident.severity severity) {
        return (root, query, cb) -> severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    public static Specification<Incident> assignedToUsername(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) {
                return null;
            }
            return cb.equal(root.join("assignedTo", JoinType.LEFT).get("username"), username);
        };
    }
}
