// package com.trucdnd.gpu_hub_backend.cluster.controller;

// import java.time.OffsetDateTime;
// import java.util.List;
// import java.util.UUID;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.trucdnd.gpu_hub_backend.cluster.dto.ClusterDto;
// import com.trucdnd.gpu_hub_backend.cluster.dto.JoinClusterRequest;
// import com.trucdnd.gpu_hub_backend.cluster.service.ClusterService;
// import com.trucdnd.gpu_hub_backend.common.constants.Cluster.Status;
// import com.trucdnd.gpu_hub_backend.config.SecurityConfig;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.context.annotation.Import;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest(ClusterController.class)
// @Import(SecurityConfig.class)
// class ClusterControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Autowired
//     private ObjectMapper objectMapper;

//     @MockBean
//     private ClusterService clusterService;

//     @Test
//     void getAll_returnsDtoList() throws Exception {
//         UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
//         OffsetDateTime now = OffsetDateTime.parse("2026-03-21T10:00:00Z");
//         ClusterDto dto = ClusterDto.builder()
//                 .id(id)
//                 .name("local")
//                 .description("Single node cluster")
//                 .apiEndpoint("https://127.0.0.1:6443")
//                 .status(Status.ACTIVE)
//                 .createdAt(now)
//                 .updatedAt(now)
//                 .build();
//         when(clusterService.findAll()).thenReturn(List.of(dto));

//         mockMvc.perform(get("/api/clusters"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$[0].id").value(id.toString()))
//                 .andExpect(jsonPath("$[0].status").value("ACTIVE"));
//     }

//     @Test
//     void getById_returnsDto() throws Exception {
//         UUID id = UUID.fromString("00000000-0000-0000-0000-000000000003");
//         OffsetDateTime now = OffsetDateTime.parse("2026-03-21T10:00:00Z");
//         ClusterDto dto = ClusterDto.builder()
//                 .id(id)
//                 .name("local")
//                 .description("Single node cluster")
//                 .apiEndpoint("https://127.0.0.1:6443")
//                 .status(Status.ACTIVE)
//                 .createdAt(now)
//                 .updatedAt(now)
//                 .build();
//         when(clusterService.findById(id)).thenReturn(dto);

//         mockMvc.perform(get("/api/clusters/{id}", id))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.id").value(id.toString()))
//                 .andExpect(jsonPath("$.name").value("local"));
//     }

//     @Test
//     void create_returnsCreatedWithLocation() throws Exception {
//         UUID id = UUID.fromString("00000000-0000-0000-0000-000000000004");
//         OffsetDateTime now = OffsetDateTime.parse("2026-03-21T10:00:00Z");
//         ClusterDto dto = ClusterDto.builder()
//                 .id(id)
//                 .name("local")
//                 .description("Single node cluster")
//                 .apiEndpoint("https://127.0.0.1:6443")
//                 .status(Status.ACTIVE)
//                 .createdAt(now)
//                 .updatedAt(now)
//                 .build();
//         JoinClusterRequest request = new JoinClusterRequest(
//                 "local",
//                 "Single node cluster",
//                 "https://127.0.0.1:6443",
//                 "/etc/rancher/k3s/k3s.yaml"
//         );
//         when(clusterService.save(any(JoinClusterRequest.class))).thenReturn(dto);

//         mockMvc.perform(post("/api/clusters")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isCreated())
//                 .andExpect(header().string("Location", "/api/clusters/" + id))
//                 .andExpect(jsonPath("$.id").value(id.toString()));
//     }
// }
