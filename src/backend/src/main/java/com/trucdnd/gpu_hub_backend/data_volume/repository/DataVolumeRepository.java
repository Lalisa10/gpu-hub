package com.trucdnd.gpu_hub_backend.data_volume.repository;

import com.trucdnd.gpu_hub_backend.data_volume.entity.DataVolume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataVolumeRepository extends JpaRepository<DataVolume, UUID> {

    List<DataVolume> findByTeam_Id(UUID teamId);

    List<DataVolume> findByTeam_IdAndCluster_Id(UUID teamId, UUID clusterId);

    List<DataVolume> findByTeam_IdIn(Collection<UUID> teamIds);

    @Query("select v.team.id from DataVolume v where v.id = :volumeId")
    Optional<UUID> findTeamIdByVolumeId(@Param("volumeId") UUID volumeId);
}
