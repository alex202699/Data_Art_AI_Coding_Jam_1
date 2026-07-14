package com.dataart.ticketing.repository;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    /** All teams, ordered case-insensitively by name (matches the reference listing). */
    @Query("select t from Team t order by lower(t.name)")
    List<Team> findAllOrderByNameCi();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
