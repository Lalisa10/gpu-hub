package com.trucdnd.gpu_hub_backend.data_source.repository;

import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

    List<DataSource> findByVolume_Id(UUID volumeId);

    List<DataSource> findByVolume_Team_IdIn(Collection<UUID> teamIds);

    @Query("select s.volume.team.id from DataSource s where s.id = :sourceId")
    Optional<UUID> findTeamIdBySourceId(@Param("sourceId") UUID sourceId);
}
