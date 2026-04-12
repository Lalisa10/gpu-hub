// package com.trucdnd.gpu_hub_backend.cluster.service;

// import java.time.OffsetDateTime;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// import com.trucdnd.gpu_hub_backend.cluster.dto.ClusterDto;
// import com.trucdnd.gpu_hub_backend.cluster.dto.JoinClusterRequest;
// import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
// import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
// import com.trucdnd.gpu_hub_backend.common.constants.Cluster.Status;
// import jakarta.persistence.EntityNotFoundException;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Captor;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import static org.mockito.Mockito.any;

// @ExtendWith(MockitoExtension.class)
// class ClusterServiceTest {

//     @Mock
//     private ClusterRepository clusterRepository;

//     @Captor
//     private ArgumentCaptor<Cluster> clusterCaptor;

//     private ClusterService clusterService;

//     @BeforeEach
//     void setUp() {
//         clusterService = new ClusterService(clusterRepository);
//     }

//     @Test
//     void findAll_returnsDtos() {
//         Cluster cluster = sampleCluster();
//         when(clusterRepository.findAll()).thenReturn(List.of(cluster));

//         List<ClusterDto> dtos = clusterService.findAll();

//         assertEquals(1, dtos.size());
//         ClusterDto dto = dtos.get(0);
//         assertEquals(cluster.getId(), dto.id());
//         assertEquals(cluster.getName(), dto.name());
//         assertEquals(Status.ACTIVE, dto.status());
//     }

//     @Test
//     void findById_returnsDtoWhenPresent() {
//         Cluster cluster = sampleCluster();
//         when(clusterRepository.findById(cluster.getId())).thenReturn(Optional.of(cluster));

//         ClusterDto dto = clusterService.findById(cluster.getId());

//         assertEquals(cluster.getId(), dto.id());
//     }

//     @Test
//     void findById_throwsWhenMissing() {
//         UUID missingId = UUID.randomUUID();
//         when(clusterRepository.findById(missingId)).thenReturn(Optional.empty());

//         assertThrows(EntityNotFoundException.class, () -> clusterService.findById(missingId));
//     }

//     @Test
//     void save_setsActiveStatusAndReturnsDto() {
//         JoinClusterRequest request = new JoinClusterRequest(
//                 "local",
//                 "Local cluster",
//                 "/etc/rancher/k3s/k3s.yaml"
//         );
//         Cluster savedCluster = sampleCluster();
//         when(clusterRepository.save(any(Cluster.class))).thenReturn(savedCluster);

//         ClusterDto dto = clusterService.save(request);

//         verify(clusterRepository).save(clusterCaptor.capture());
//         Cluster captured = clusterCaptor.getValue();
//         assertEquals(Status.ACTIVE, captured.getStatus());
//         assertEquals(request.name(), captured.getName());
//         assertEquals(savedCluster.getId(), dto.id());
//     }

//     private Cluster sampleCluster() {
//         OffsetDateTime now = OffsetDateTime.parse("2026-03-21T10:00:00Z");
//         Cluster cluster = Cluster.builder()
//                 .name("local")
//                 .description("Single node cluster")
//                 .kubeconfigRef("/etc/rancher/k3s/k3s.yaml")
//                 .status(Status.ACTIVE)
//                 .build();
//         cluster.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
//         cluster.setCreatedAt(now);
//         cluster.setUpdatedAt(now);
//         return cluster;
//     }
// }
