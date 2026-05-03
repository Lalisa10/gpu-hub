-- ============================================================
-- CLUSTERS
-- ============================================================
CREATE TABLE clusters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    kubeconfig_ref  VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'active'
                        CHECK (status IN ('active', 'inactive', 'maintenance')),
    juicefs_metaurl VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- POLICIES (thuộc về một cluster cụ thể)
-- ============================================================
CREATE TABLE policies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cluster_id          UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    priority            INTEGER NOT NULL DEFAULT 100,
    gpu_quota           NUMERIC(10,2),
    cpu_quota           NUMERIC(10,2),
    memory_quota        BIGINT,             -- MiB
    gpu_limit           NUMERIC(10,2),
    cpu_limit           NUMERIC(10,2),
    memory_limit        BIGINT,             -- MiB
    gpu_over_quota_weight       INTEGER DEFAULT 1,
    cpu_over_quota_weight       INTEGER DEFAULT 1,
    memory_over_quota_weight    INTEGER DEFAULT 1,
    node_affinity       JSONB,
    gpu_types           TEXT[],
    UNIQUE (cluster_id, name),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAMS
-- ============================================================
CREATE TABLE teams (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAM ↔ CLUSTER  (level team, policy là node con của root)
-- ============================================================
CREATE TABLE team_clusters (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID NOT NULL REFERENCES teams(id)    ON DELETE CASCADE,
    cluster_id  UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    policy_id   UUID NOT NULL REFERENCES policies(id),
    namespace   VARCHAR(63) NOT NULL,       -- K8s namespace của team trên cluster này
    UNIQUE (team_id, cluster_id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trigger: policy phải thuộc đúng cluster
CREATE OR REPLACE FUNCTION check_team_cluster_policy()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM policies
        WHERE id = NEW.policy_id AND cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'Policy % không thuộc cluster %', NEW.policy_id, NEW.cluster_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_team_cluster_policy
BEFORE INSERT OR UPDATE ON team_clusters
FOR EACH ROW EXECUTE FUNCTION check_team_cluster_policy();

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
-- TEAM_MEMBERS
-- ============================================================
CREATE TABLE team_members (
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id     UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
                    CHECK (role IN ('MEMBER', 'TEAM_LEAD')),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, team_id)
);

-- ============================================================
-- PROJECTS
-- Mỗi project thuộc đúng 1 team + 1 cluster + 1 policy
-- Policy này là node con của policy team trong cây KAI-scheduler
-- ============================================================
CREATE TABLE projects (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id              UUID        NOT NULL REFERENCES teams(id)    ON DELETE CASCADE,
    cluster_id           UUID        NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    policy_id            UUID        NOT NULL REFERENCES policies(id),
    name                 VARCHAR(100) NOT NULL,
    description          TEXT,
    UNIQUE (team_id, name),                  -- tên project unique trong team
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trigger: đảm bảo policy thuộc đúng cluster,
--          và team đã được gán vào cluster đó
CREATE OR REPLACE FUNCTION check_project_policy()
RETURNS TRIGGER AS $$
BEGIN
    -- Policy phải thuộc đúng cluster của project
    IF NOT EXISTS (
        SELECT 1 FROM policies
        WHERE id = NEW.policy_id AND cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'Policy % không thuộc cluster %', NEW.policy_id, NEW.cluster_id;
    END IF;

    -- Team phải đã được gán vào cluster đó qua team_clusters
    IF NOT EXISTS (
        SELECT 1 FROM team_clusters
        WHERE team_id = NEW.team_id AND cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'Team % chưa được gán vào cluster %', NEW.team_id, NEW.cluster_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_project_policy
BEFORE INSERT OR UPDATE ON projects
FOR EACH ROW EXECUTE FUNCTION check_project_policy();

-- ============================================================
-- WORKLOADS
-- ============================================================
CREATE TABLE workloads (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID NOT NULL REFERENCES projects(id),
    cluster_id          UUID NOT NULL REFERENCES clusters(id),
    submitted_by        UUID NOT NULL REFERENCES users(id),
    workload_type    	VARCHAR(20) NOT NULL
							CHECK (workload_type IN (
								'notebook', 'llm_inference'
							)),
	priority_class      VARCHAR(20) NOT NULL DEFAULT 'train'
                            CHECK (priority_class IN ('train', 'build-preemptible', 'build', 'inference')),
	image				VARCHAR(100) NOT NULL, 
    name                VARCHAR(255) NOT NULL,
    requested_gpu       NUMERIC(10,2) NOT NULL DEFAULT 0,
    requested_cpu       NUMERIC(10,2) NOT NULL DEFAULT 0,
    requested_memory    BIGINT        NOT NULL DEFAULT 0,   -- MiB
    status              VARCHAR(30)   NOT NULL DEFAULT 'pending'
                            CHECK (status IN (
                                'pending', 'running',
                                'succeeded', 'failed', 'preempted', 'cancelled'
                            )),
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    extra               JSONB,          -- mlflow_run_id, jupyter_url, vllm_endpoint...
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- DATA_VOLUMES
-- ============================================================
CREATE TABLE data_volumes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id             UUID NOT NULL REFERENCES teams(id),
    cluster_id          UUID NOT NULL REFERENCES clusters(id),
    created_by        	UUID NOT NULL REFERENCES users(id),
    pvc_name            VARCHAR(255) NOT NULL,
    volume_type			VARCHAR(30) NOT NULL
							CHECK (volume_type IN (
								'dynamic', 'source'
							)),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- DATA_SOURCES
-- ============================================================
CREATE TABLE data_source (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by        	UUID NOT NULL REFERENCES users(id),
    volume_id			UUID NOT NULL REFERENCES data_volumes(id),
    status              VARCHAR(30)   NOT NULL DEFAULT 'formating'
                            CHECK (status IN (
                                'formating', 'formated'
                            )),
	bucket_url			VARCHAR(255) NOT NULL,
	access_key			VARCHAR(255) NOT NULL,
	secret_key			VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- WORKLOAD_VOLUMES
-- ============================================================
CREATE TABLE workload_volumes (
    workload_id     	UUID NOT NULL REFERENCES workloads(id) ON DELETE CASCADE,
    volume_id     		UUID NOT NULL REFERENCES data_volumes(id) ON DELETE CASCADE,
    mount_path			VARCHAR(255) NOT NULL,
    PRIMARY KEY (workload_id, volume_id)
);

-- Trigger: cluster của workload phải khớp cluster của project
CREATE OR REPLACE FUNCTION check_workload_cluster()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM projects
        WHERE id = NEW.project_id AND cluster_id = NEW.cluster_id
    ) THEN
        RAISE EXCEPTION 'cluster_id của workload không khớp với cluster_id của project %', NEW.project_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_workload_cluster
BEFORE INSERT OR UPDATE ON workloads
FOR EACH ROW EXECUTE FUNCTION check_workload_cluster();

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT false,
    revoked_at  TIMESTAMPTZ,
    parent_id   UUID REFERENCES refresh_tokens(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_policies_cluster          ON policies(cluster_id);
CREATE INDEX idx_team_clusters_team        ON team_clusters(team_id);
CREATE INDEX idx_team_clusters_cluster     ON team_clusters(cluster_id);
CREATE INDEX idx_team_clusters_policy      ON team_clusters(policy_id);
CREATE INDEX idx_team_members_user         ON team_members(user_id);
CREATE INDEX idx_team_members_team         ON team_members(team_id);
CREATE INDEX idx_projects_team             ON projects(team_id);
CREATE INDEX idx_projects_cluster          ON projects(cluster_id);
CREATE INDEX idx_projects_policy           ON projects(policy_id);
CREATE INDEX idx_workloads_project         ON workloads(project_id);
CREATE INDEX idx_workloads_cluster         ON workloads(cluster_id);
CREATE INDEX idx_workloads_submitted_by    ON workloads(submitted_by);
CREATE INDEX idx_workloads_status          ON workloads(status);
CREATE INDEX idx_workloads_active          ON workloads(cluster_id, status)
    WHERE status IN ('pending', 'queued', 'running');
CREATE INDEX idx_refresh_tokens_user       ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash       ON refresh_tokens(token_hash);