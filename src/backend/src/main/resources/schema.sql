-- ============================================================
-- CLUSTERS
-- ============================================================
CREATE TABLE clusters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    api_endpoint    VARCHAR(255) NOT NULL,          -- K8s API server URL
    kubeconfig_ref  VARCHAR(255),                   -- ref đến secret store (Vault, K8s Secret)
    status          VARCHAR(20) NOT NULL DEFAULT 'active'
                        CHECK (status IN ('active', 'inactive', 'maintenance')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- POLICIES (thuộc về cluster, tái sử dụng cho team và project)
-- ============================================================
CREATE TABLE policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cluster_id      UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,

    -- Max priority một workload trong scope này được phép đặt
    -- KAI: priority >= 100 → non-preemptible, < 100 → preemptible
    max_priority    INTEGER NOT NULL DEFAULT 50,

    -- GPU Quota (KAI: deservedGPUs)
    gpu_quota       NUMERIC(10,2),
    cpu_quota       NUMERIC(10,2),          -- cores
    memory_quota    BIGINT,                 -- MiB

    -- Hard limit per single workload
    gpu_limit_per_workload      NUMERIC(10,2),
    cpu_limit_per_workload      NUMERIC(10,2),
    memory_limit_per_workload   BIGINT,

    -- Over-quota borrowing weight (KAI: overQuotaWeight)
    over_quota_weight   NUMERIC(5,2) NOT NULL DEFAULT 1.0,

    -- Node constraints
    node_affinity   JSONB,
    gpu_types       TEXT[],                 -- VD: ['A100', 'H100'], NULL = any

    UNIQUE (cluster_id, name),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAMS
-- ============================================================
CREATE TABLE teams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAM ↔ CLUSTER  (kèm policy của cluster đó)
-- ============================================================
CREATE TABLE team_clusters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    cluster_id      UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    policy_id       UUID NOT NULL REFERENCES policies(id),
    -- K8s namespace được tạo cho team này trên cluster này
    namespace       VARCHAR(63) NOT NULL,

    UNIQUE (team_id, cluster_id),

    -- Đảm bảo policy phải thuộc đúng cluster
    CONSTRAINT fk_policy_cluster CHECK (
        -- Enforced ở application layer hoặc trigger
        true
    ),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trigger đảm bảo policy.cluster_id = team_clusters.cluster_id
CREATE OR REPLACE FUNCTION check_team_cluster_policy()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM policies
        WHERE id = NEW.policy_id
          AND cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'Policy % không thuộc cluster %',
            NEW.policy_id, NEW.cluster_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_team_cluster_policy
BEFORE INSERT OR UPDATE ON team_clusters
FOR EACH ROW EXECUTE FUNCTION check_team_cluster_policy();

-- ============================================================
-- PROJECTS (thuộc team, policy riêng trên từng cluster)
-- ============================================================
CREATE TABLE projects (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id                 UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name                    VARCHAR(100) NOT NULL,
    description             TEXT,
    mlflow_experiment_id    VARCHAR(255),
    minio_prefix            VARCHAR(255),  -- VD: "team-nlp/project-alpha/"
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (team_id, name)
);

-- Policy của project trên từng cluster
-- (sub-quota của team_clusters policy)
CREATE TABLE project_cluster_policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    cluster_id      UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    policy_id       UUID NOT NULL REFERENCES policies(id),

    UNIQUE (project_id, cluster_id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trigger: policy phải thuộc cluster, và project phải thuộc
-- team đang có mặt trên cluster đó
CREATE OR REPLACE FUNCTION check_project_cluster_policy()
RETURNS TRIGGER AS $$
BEGIN
    -- Policy phải thuộc đúng cluster
    IF NOT EXISTS (
        SELECT 1 FROM policies
        WHERE id = NEW.policy_id AND cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'Policy không thuộc cluster này';
    END IF;

    -- Team của project phải có trong cluster đó
    IF NOT EXISTS (
        SELECT 1
        FROM projects p
        JOIN team_clusters tc ON tc.team_id = p.team_id
        WHERE p.id = NEW.project_id
          AND tc.cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'Team của project chưa được gán vào cluster này';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_project_cluster_policy
BEFORE INSERT OR UPDATE ON project_cluster_policies
FOR EACH ROW EXECUTE FUNCTION check_project_cluster_policy();

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    global_role     VARCHAR(20)  NOT NULL DEFAULT 'USER'
                        CHECK (global_role IN ('ADMIN', 'USER')),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAM_MEMBERS: đơn giản - chỉ thể hiện user thuộc team nào
-- ============================================================
CREATE TABLE team_members (
    user_id     UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    team_id     UUID NOT NULL REFERENCES teams(id)  ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, team_id)      -- composite PK, không cần cột id riêng
);

-- Các loại workload hệ thống hỗ trợ
-- VD: jupyter_notebook, vllm_deployment, training_job, pipeline...
CREATE TABLE workload_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,   -- VD: 'jupyter_notebook'
    display_name    VARCHAR(100) NOT NULL,           -- VD: 'Jupyter Notebook'
    description     TEXT,

    -- Template K8s resource mặc định cho loại này
    default_gpu     NUMERIC(10,2),
    default_cpu     NUMERIC(10,2),
    default_memory  BIGINT,                         -- MiB

    -- Loại này có support multi-GPU không
    supports_multi_gpu  BOOLEAN NOT NULL DEFAULT false,

    -- Workload này chạy liên tục (notebook, vllm)
    -- hay chạy xong thì dừng (training job)
    is_service      BOOLEAN NOT NULL DEFAULT false,

    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workloads (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Liên kết nghiệp vụ
    project_id          UUID NOT NULL REFERENCES projects(id),
    cluster_id          UUID NOT NULL REFERENCES clusters(id),
    submitted_by        UUID NOT NULL REFERENCES users(id),
    workload_type_id    UUID NOT NULL REFERENCES workload_types(id),

    -- Tên hiển thị do user đặt
    name                VARCHAR(255) NOT NULL,

    -- Priority tại thời điểm submit (phải <= max_priority của policy)
    priority            INTEGER NOT NULL,

    -- Tài nguyên yêu cầu
    requested_gpu       NUMERIC(10,2) NOT NULL DEFAULT 0,
    requested_cpu       NUMERIC(10,2) NOT NULL DEFAULT 0,
    requested_memory    BIGINT NOT NULL DEFAULT 0,  -- MiB

    -- Trạng thái vòng đời
    status  VARCHAR(30) NOT NULL DEFAULT 'pending'
                CHECK (status IN (
                    'pending',      -- Đang chờ scheduler
                    'queued',       -- KAI đã nhận, đang queue
                    'running',      -- Đang chạy trên cluster
                    'succeeded',    -- Hoàn thành thành công
                    'failed',       -- Thất bại
                    'preempted',    -- Bị preempt bởi workload ưu tiên cao hơn
                    'cancelled'     -- User chủ động huỷ
                )),

    -- Tham chiếu sang K8s
    k8s_namespace       VARCHAR(63),
    k8s_resource_name   VARCHAR(255),   -- tên object trên K8s
    k8s_resource_kind   VARCHAR(100),   -- VD: 'TrainingJob', 'Deployment'

    -- Thời gian
    queued_at       TIMESTAMPTZ,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,

    -- Metadata mở rộng (VD: mlflow_run_id, jupyter_url, vllm_endpoint...)
    extra           JSONB,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Constraint: priority không được vượt max_priority của policy
    -- (enforce ở application layer vì cần join qua project→cluster→policy)
    CONSTRAINT chk_priority_positive CHECK (priority >= 0)
);




-- Tra cứu policy theo cluster
CREATE INDEX idx_policies_cluster ON policies(cluster_id);

-- Tra cứu team trên cluster
CREATE INDEX idx_team_clusters_team    ON team_clusters(team_id);
CREATE INDEX idx_team_clusters_cluster ON team_clusters(cluster_id);
CREATE INDEX idx_team_clusters_policy  ON team_clusters(policy_id);

-- Tra cứu project theo team
CREATE INDEX idx_projects_team ON projects(team_id);

-- Tra cứu project-cluster policy
CREATE INDEX idx_pcp_project   ON project_cluster_policies(project_id);
CREATE INDEX idx_pcp_cluster   ON project_cluster_policies(cluster_id);

-- Tra cứu member theo team/user
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_team_members_team ON team_members(team_id);

-- Workloads hay được query theo status, project, user
CREATE INDEX idx_workloads_project     ON workloads(project_id);
CREATE INDEX idx_workloads_cluster     ON workloads(cluster_id);
CREATE INDEX idx_workloads_submitted_by ON workloads(submitted_by);
CREATE INDEX idx_workloads_status      ON workloads(status);
CREATE INDEX idx_workloads_type        ON workloads(workload_type_id);

-- Query workloads đang active (running + queued) thường xuyên
CREATE INDEX idx_workloads_active ON workloads(cluster_id, status)
    WHERE status IN ('pending', 'queued', 'running');
---


-- Refresh token rotation: mỗi lần refresh → token cũ bị revoke, cấp token mới
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,   -- SHA-256 của token thực
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT false,
    revoked_at      TIMESTAMPTZ,
    -- Rotation chain: biết token này được sinh ra từ token nào
    parent_id       UUID REFERENCES refresh_tokens(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash    ON refresh_tokens(token_hash);




