# UX Requirements — GPU-Hub Frontend

Tài liệu mô tả các màn hình, luồng nghiệp vụ, và yêu cầu tương tác từ góc độ người dùng cuối. **Tách biệt hoàn toàn với chi tiết kỹ thuật (API, database, code) và styling (màu sắc, font, spacing).**

---

## Personas

### Admin
- Quản lý toàn bộ hệ thống
- Tạo/xóa team, user, cluster, policy
- Xem tất cả workload, resource usage của hệ thống
- Manage permissions, role assignment

### Team Lead
- Quản lý thành viên của team
- Tạo/xóa project trong team
- Xem workload, resource usage của team
- Cấp access cho project members

### User
- Thành viên trong một hoặc nhiều team
- Tạo workload (Notebook, LLM Inference)
- Xem workload của mình, pod logs
- Sử dụng GPU resource được cấp phát

---

## Core Domains

### 1. Authentication

#### 1.1 Login Screen

**Goal:** Người dùng nhập credentials để truy cập hệ thống.

**Elements:**
- Username field (text input)
- Password field (masked input)
- Login button
- Error message display (if login fails)
- Loading indicator (while submitting)

**Flow:**
1. User nhập username + password
2. User click Login
3. System validates credentials
4. On success: navigate to dashboard
5. On failure: show error message (e.g., "Invalid username/password")

**Constraints:**
- Require both username and password
- Show which field is invalid if error occurs
- Password must be masked

---

#### 1.2 Logout

**Goal:** User thoát khỏi hệ thống.

**Flow:**
1. User click Logout (từ sidebar/menu)
2. System clear token, session
3. Redirect to Login screen

---

#### 1.3 Token Refresh (Background)

**Goal:** Maintain user session while using the app.

**Flow:**
1. Access token expires (15 min)
2. System automatically refresh token in background
3. If refresh fails → redirect to login
4. User continues without interruption (ideally)

---

### 2. User Management (Admin Only)

#### 2.1 Users List

**Goal:** Admin view, search, manage all users.

**Elements:**
- Search/filter bar (by username, email)
- User table with columns:
  - Username
  - Email
  - Global Role (ADMIN, USER)
  - Created Date
  - Actions (Edit, Delete, Reset Password)
- Create User button
- Pagination (if many users)

**Interactions:**
- Click row → User detail page
- Click Create User → New user form
- Click Edit → Edit user form (change role, email)
- Click Delete → Confirmation dialog → Delete
- Click Reset Password → Send password reset email

---

#### 2.2 Create/Edit User Form

**Elements:**
- Username field (required, unique)
- Email field (required, valid email)
- Global Role dropdown (ADMIN, USER)
- Password field (only on create, or reset flow)
- Save button
- Cancel button

**Validation:**
- Username: 3-32 chars, alphanumeric + underscore
- Email: valid format
- Role: select from dropdown
- Show error message if validation fails

---

### 3. Team Management

#### 3.1 Teams List (Admin)

**Goal:** Admin view and manage all teams.

**Elements:**
- Team table with columns:
  - Team Name
  - Description
  - Member Count
  - Created Date
  - Actions (View, Edit, Delete)
- Create Team button
- Search/filter by name

**Interactions:**
- Click row → Team detail page
- Click Create Team → New team form
- Click Edit → Edit team form
- Click Delete → Confirmation → Delete team

---

#### 3.2 Create/Edit Team Form

**Elements:**
- Team Name (required, unique)
- Description (optional)
- Save button
- Cancel button

**Validation:**
- Name: 3-50 chars, alphanumeric + spaces/hyphens
- Check uniqueness on backend

---

#### 3.3 Team Detail Page (Team Lead + Admin)

**Goal:** Team lead manage team members and projects.

**Layout:**
- Team info header (name, description, member count)
- Two tabs: **Members**, **Projects**

##### Tab: Members

**Elements:**
- Members table with columns:
  - Username
  - Email
  - Role in Team (MEMBER, TEAM_LEAD)
  - Joined Date
  - Actions (Change Role, Remove)
- Add Member button (search + select user)
- Remove Member confirmation dialog

**Interactions:**
- Click Add Member → Search modal → Select user → Set role → Add
- Click Change Role → Dropdown (MEMBER ↔ TEAM_LEAD) → Save
- Click Remove → Confirmation → Remove

**Constraints:**
- Cannot remove last team lead
- Require team lead role to manage members

##### Tab: Projects

**Elements:**
- Projects table with columns:
  - Project Name
  - Cluster
  - Resource Policy
  - Workload Count
  - Created Date
  - Actions (View, Edit, Delete)
- Create Project button

**Interactions:**
- Click row → Project detail page
- Click Create Project → Project form
- Click Edit → Edit project form
- Click Delete → Confirmation → Delete

---

#### 3.4 My Teams Page (User)

**Goal:** Users see teams they belong to, access team resources.

**Elements:**
- Team cards showing:
  - Team Name
  - My Role (MEMBER, TEAM_LEAD)
  - Project Count
  - Last Activity
  - Click to enter team

**Interactions:**
- Click team card → Team detail page

---

### 4. Cluster Management

#### 4.1 Clusters List (Admin + Team Lead)

**Goal:** View all clusters (admin) or team-assigned clusters (team lead).

**Elements:**
- Cluster table with columns:
  - Cluster Name
  - Status (Online, Offline, Unknown)
  - Node Count
  - GPU Count
  - Total CPU / Memory capacity
  - Last Health Check
  - Actions (View Details, Register, Assign to Team, Edit, Delete)
- Register New Cluster button

**Interactions:**
- Click row → Cluster detail page
- Click Register → Cluster registration form
- Click Edit → Edit cluster form
- Click Delete → Confirmation → Delete
- Click Assign to Team → Modal (select team, select policy) → Assign

---

#### 4.2 Cluster Registration Form

**Goal:** Admin register a new Kubernetes cluster.

**Elements:**
- Cluster Name (required, unique)
- Cluster Description (optional)
- Kubeconfig file upload (required)
  - File input or paste-text area
  - Validate kubeconfig format
- Test Connection button
- Create button
- Cancel button

**Flow:**
1. Admin upload/paste kubeconfig
2. Click Test Connection → verify cluster reachable
3. If success, show "Connection OK" message
4. Click Create → Register cluster
5. Redirect to cluster detail page

---

#### 4.3 Cluster Detail Page

**Goal:** View cluster status, metrics, resource allocation, nodes, workloads.

**Layout:**
- Cluster info header (name, status, created date)
- Multiple tabs/sections:

##### Section: Status & Health

**Elements:**
- Overall status badge (Online / Offline / Unknown)
- Last health check timestamp
- Connection error (if offline) — human-readable message

##### Section: Resource Summary

**Elements:**
- Total GPU count (grouped by GPU model)
  - GPU Model (e.g., "NVIDIA A100", "NVIDIA H100")
  - Total GPUs in cluster
  - Free / In Use count
- Total CPU capacity (cores) vs allocatable
- Total Memory capacity vs allocatable
- Resource charts (% utilization) — if data available

**Note:** Percentage computed at frontend (not backend). Show raw numbers + calculated %.

##### Section: GPU Inventory

**Elements:**
- GPU slots table with columns:
  - GPU Model
  - Node Name
  - Current Status (Idle / In Use)
  - Current Workload (if In Use, show workload name/user)
- Filter by GPU model, status

**Update Strategy:** Poll every 30 seconds OR receive push updates (SSE).

##### Section: Nodes

**Elements:**
- Nodes table with columns:
  - Node Name
  - Status (Ready, NotReady, Unknown)
  - CPU Allocatable / Capacity
  - Memory Allocatable / Capacity
  - GPU Count
  - Age

##### Section: Active Workloads Summary

**Elements:**
- Workload summary by status:
  - Running count
  - Pending count
  - Queued count
  - Succeeded count
  - Failed count
- Link to Workloads page filtered by cluster

---

#### 4.4 Assign Cluster to Team

**Goal:** Admin assign cluster + policy to team, creating team namespace.

**Dialog:**
- Select Team (dropdown)
- Select Policy (dropdown, filtered to cluster's policies)
- Assign button
- Cancel button

**On Success:**
- Show "Team assigned to cluster" message
- Update teams list on cluster detail
- Auto-create team namespace (backend)

**On Error:**
- Show error message (e.g., "Cluster already assigned to team")

---

### 5. Policy Management (Admin)

#### 5.1 Policies List

**Goal:** Admin create, edit, delete resource quotas/limits per cluster.

**Elements:**
- Policies table with columns:
  - Policy Name
  - Cluster
  - GPU Quota
  - CPU Quota (cores)
  - Memory Quota (GB)
  - Created Date
  - Actions (View, Edit, Delete)
- Create Policy button
- Search by cluster, name

**Interactions:**
- Click Create Policy → Policy form
- Click Edit → Edit policy form
- Click Delete → Confirmation → Delete
- Click row → Policy detail page

---

#### 5.2 Create/Edit Policy Form

**Elements:**
- Policy Name (required, unique per cluster)
- Cluster (required, dropdown)
- GPU Quota (required, integer ≥ 1)
- CPU Quota in cores (required, integer ≥ 1)
- Memory Quota in GB (required, integer ≥ 1)
- Node Affinity Rules (optional, key-value pairs)
  - E.g., key="zone", value="gpu-zone-1"
- Save button
- Cancel button

**Validation:**
- All quotas > 0
- Policy name unique in cluster
- Show validation errors

---

### 6. Project Management

#### 6.1 Projects List (Admin view)

**Goal:** Admin view all projects.

**Elements:**
- Projects table with columns:
  - Project Name
  - Team
  - Cluster
  - Policy
  - Workload Count
  - Created Date
  - Actions (View, Edit, Delete)
- Create Project button
- Filter by team, cluster

---

#### 6.2 Create/Edit Project Form (Team Lead)

**Goal:** Team lead create/edit team's projects on assigned clusters.

**Elements:**
- Project Name (required, unique in team)
- Cluster (required, dropdown — only assigned clusters)
- Policy (required, dropdown — policies for selected cluster)
- Description (optional)
- Save button
- Cancel button

**Validation:**
- Name unique in team
- Cluster and Policy must be on same cluster
- Show errors

**Note:** Team lead can only create projects for their team on assigned clusters.

---

#### 6.3 Project Detail Page (Team Lead)

**Goal:** Team lead view project resources, workloads, members.

**Layout:**
- Project info header (name, cluster, policy, team)
- Tabs: **Overview**, **Members**, **Workloads**

##### Tab: Overview

**Elements:**
- Resource quota display (GPU, CPU, Memory)
  - Total quota vs current usage
  - Progress bars or % displays
- Recent workloads list (last 5-10)

##### Tab: Members

**Elements:**
- Members table with columns:
  - Username
  - Role (User, Lead)
  - Workload Count
- Add Member button
- Remove Member (with confirmation)

##### Tab: Workloads

**Elements:**
- Workloads table (same as workload list, but filtered to project)

---

### 7. Workload Management

#### 7.1 Workload List (User / Team Lead / Admin)

**Goal:** View all user's, team's, or system's workloads with status.

**Elements:**
- Workloads table with columns:
  - Workload Name
  - Type (Notebook, LLM Inference)
  - Project
  - Status (Pending, Queued, Running, Succeeded, Failed, Preempted, Cancelled)
  - GPU Request
  - Submitted By
  - Submitted Date
  - Actions (View, Cancel, Delete, View Logs)
- Create Workload button
- Filter/search by:
  - Status
  - Type
  - Project
  - User (admin only)
- Status badge with distinct indicators

**Interactions:**
- Click row → Workload detail page
- Click Create Workload → Submit workload form
- Click Cancel → Confirmation → Cancel running workload
- Click Delete → Confirmation → Delete workload
- Click View Logs → Pod logs viewer

---

#### 7.2 Workload Submit Form

**Goal:** User submit a new workload (Notebook or LLM Inference).

**Elements:**

##### Common Fields
- Workload Name (required, unique in project)
- Workload Type (required, radio or dropdown):
  - Notebook
  - LLM Inference
- Project (required, dropdown — only user's accessible projects)
- Docker Image (required, text input)
  - E.g., "nvcr.io/nvidia/pytorch:24.04-py3"
  - Validate format (registry/namespace:tag)
- GPU Request (required, integer ≥ 1)
- CPU Request (optional, integer)
- Memory Request (optional, text input, e.g., "32Gi")

##### Type-Specific Fields (Conditional)

**If Type = Notebook:**
- PVC Size (optional, text, e.g., "10Gi")
- GPU Type (optional, text, e.g., "nvidia.com/gpu")

**If Type = LLM Inference:**
- Model Source (required, text, e.g., "meta-llama/Llama-2-7b-hf")
- vLLM Parameters (optional, text area)
  - Free-form parameters for vllm server
  - E.g., "--dtype float16 --max-model-len 4096"
- Replica Count (optional, integer ≥ 1, default 1)
- Environment Variables (optional, key-value pairs)
  - Add/remove rows dynamically

**Buttons:**
- Submit button (disabled if required fields empty)
- Cancel button
- Clear button (optional)

**Validation:**
- Name unique in project
- Image format valid
- GPU/CPU/Memory > 0
- Show validation errors next to fields
- Disable submit until all required fields valid

**Flow:**
1. User fill form
2. Click Submit
3. Show loading state
4. On success: 
   - Show success message "Workload submitted"
   - Redirect to workload detail page
5. On error:
   - Show error message (e.g., "Insufficient GPU quota")
   - Highlight error field

---

#### 7.3 Workload Detail Page

**Goal:** Monitor workload status, view pods, logs, resource usage.

**Layout:**
- Workload info header (name, type, project, status, submitted by, submitted date)
- Real-time status updates (via SSE)
- Tabs: **Status**, **Pods & Logs**, **Details**

##### Tab: Status

**Elements:**
- Status badge (large) + last updated timestamp
- Status timeline (Pending → Queued → Running → Succeeded/Failed/Preempted)
  - Show current status, completed steps
  - Timestamps for each transition
- Resource request display:
  - GPU: X
  - CPU: X cores
  - Memory: X
- Resource limit display (if applicable)
- Error message (if failed/preempted/cancelled)
  - Human-readable reason

**Update Strategy:** Poll every 5 seconds OR push via SSE (status change)

**Actions:**
- Cancel button (only if running/pending/queued)
- Delete button (only if terminal status)
- Refresh button (manual)

---

##### Tab: Pods & Logs

**Elements:**

**Pods List:**
- Pods table with columns:
  - Pod Name
  - Status (Pending, Running, Succeeded, Failed)
  - Restarts
  - Age
  - Actions (View Logs)
- Refresh button

**Pod Logs Viewer:**
- Log viewer (scrollable text area or log panel)
  - Show full logs when pod selected
  - Auto-scroll to bottom option
  - Copy-all, download logs buttons (optional)
  - Search/filter in logs (optional)

**Update Strategy:** 
- Pods list: poll every 5 seconds OR push via SSE
- Logs: poll every 5 seconds (stream latest)

---

##### Tab: Details

**Elements:**
- Workload metadata:
  - ID
  - Type
  - Project
  - Cluster
  - Team
  - Created At
  - Finished At (if terminal)
  - Duration (if finished)
- Resource configuration (GPU, CPU, Memory, image, etc.)
- Type-specific details:
  - If Notebook: PVC size, GPU type
  - If LLM: Model source, vLLM parameters, replicas, env vars
- K8s resource info (if applicable):
  - K8s name (auto-generated)
  - Namespace
  - Resource kind (Notebook CR, Deployment)

---

#### 7.4 Workload Cancel Dialog

**Goal:** Confirm workload cancellation.

**Dialog:**
- Title: "Cancel Workload?"
- Message: "Cancelling will stop the workload. This action cannot be undone."
- Workload name shown
- Cancel button
- Confirm button (prominent, possibly destructive styling)

**On Confirm:**
- Show loading state
- On success: Close dialog, update workload status to CANCELLED, show success message
- On error: Show error message

---

### 8. Dashboard Pages

#### 8.1 Admin Dashboard

**Goal:** System overview for admin.

**Sections:**

##### System Health
- Cluster count (online / offline)
- Total nodes
- Total GPUs
- Total GPU utilization %

##### Quick Stats
- Total users
- Total teams
- Total projects
- Total workloads (by status)

##### Recent Activity
- Recent workload submissions (user, time, status)
- Recent cluster changes
- Recent user/team changes

##### Alerts (Optional)
- Cluster offline warnings
- High resource usage warnings
- Failed workload count

---

#### 8.2 Team Lead Dashboard

**Goal:** Team resources overview for team lead.

**Sections:**

##### Team Overview
- Team name, member count
- Assigned clusters count
- Projects count

##### Resource Usage
- Per-cluster resource summary:
  - Cluster name
  - GPU quota vs usage
  - CPU quota vs usage
  - Memory quota vs usage

##### Active Workloads
- Workloads by status (running, pending, queued)
- Workloads by project
- Recent submissions

##### Team Members
- Members list (quick view)
- Add member button

---

#### 8.3 User Dashboard

**Goal:** Quick access to user's workloads and teams.

**Sections:**

##### My Teams
- Teams list (cards or quick view)
- Click to enter team

##### My Recent Workloads
- Recent workloads across all teams
- Quick status view
- Quick actions (view, cancel)

##### Quick Actions
- Submit New Workload button
- View All Workloads link

---

### 9. Navigation & Sidebar

#### Admin Sidebar Navigation
- Dashboard
- Users
- Teams
- Clusters
- Policies
- Projects (admin view)
- Workloads (admin view)
- Settings (if applicable)
- Logout

#### Team Lead Sidebar Navigation
- Dashboard (team lead view)
- My Teams
- Workloads
- Settings
- Logout

#### User Sidebar Navigation
- Dashboard (user view)
- My Teams
- My Workloads
- Submit Workload
- Settings
- Logout

---

### 10. Common Components / Interactions

#### 10.1 Real-Time Updates (SSE)

**Endpoints with streaming:**
- Workload status: `GET /api/workloads/{id}/status/stream`
  - Event: `status`, data: `WorkloadDto`
  - Push on status change
  - Heartbeat every 25 seconds

- Workload pods: `GET /api/workloads/{id}/pods/stream`
  - Event: `pods`, data: `PodInfoDto[]`
  - Poll every 5 seconds

- Workload pod logs: `GET /api/workloads/{id}/pods/{podName}/logs/stream`
  - Event: `log`, data: full log string
  - Poll every 5 seconds

- Cluster details: `GET /api/clusters/{id}/details/stream`
  - Event: `details`, data: `ClusterDetailsDto`
  - Poll every 30 seconds

**UX Handling:**
- Show "live update" indicator when connected to SSE
- Show "last updated" timestamp
- Auto-reconnect with backoff if stream breaks
- Fallback to polling if SSE unavailable
- Show loading skeleton while waiting for first event

---

#### 10.2 Error Handling

**Error Types:**
- **Validation errors:** Show next to field, prevent submit
- **Network errors:** "Network error, please try again"
- **Unauthorized (401):** Auto-redirect to login
- **Forbidden (403):** "You don't have permission to access this"
- **Not found (404):** "Resource not found"
- **Server errors (5xx):** "Something went wrong, please try again"

**Display:**
- Toast / snackbar for transient messages
- Inline error message for form validation
- Error page for fatal navigation errors

---

#### 10.3 Confirmation Dialogs

**Pattern:**
- Clear title explaining action
- Concise description of consequence
- Two buttons: Cancel (neutral), Confirm (action color)
- Show resource name/identifier being affected

**Use cases:**
- Delete user/team/cluster/policy/project
- Cancel workload
- Remove team member
- Unassign cluster from team

---

#### 10.4 Search & Filter

**Pattern:**
- Search field (text input) + filter options (dropdown/checkboxes)
- Live search (debounced, 300ms)
- Filter chips showing active filters, click to remove
- Clear All button

**Apply to:**
- Users list (by username, email)
- Teams list (by name)
- Clusters list (by name)
- Projects list (by name, cluster, team)
- Workloads list (by name, status, type, project)

---

#### 10.5 Pagination

**Pattern:**
- Rows per page: 10, 25, 50 (dropdown)
- Current page indicator: "Page 1 of 5"
- Previous, Next, First, Last buttons
- Jump to page input (optional)

**Apply to:**
- Long lists (users, workloads)

---

#### 10.6 Loading States

**Pattern:**
- Skeleton loaders for data sections (matching layout)
- Spinner for form submission, actions
- "Loading..." text where appropriate

---

#### 10.7 Empty States

**Pattern:**
- Icon + message explaining why empty
- CTA button to create/add first item

Example:
- "No workloads yet. [Create Workload]"
- "No team members yet. [Add Member]"

---

#### 10.8 Status Badges

**Workload Status Colors (semantic):**
- Pending: neutral
- Queued: neutral
- Running: info/active
- Succeeded: success
- Failed: error/danger
- Preempted: warning
- Cancelled: neutral/disabled

**Cluster Status:**
- Online: success
- Offline: error/danger
- Unknown: warning

---

#### 10.9 User Profile / Account Menu

**Elements:**
- User avatar (initial letter or icon)
- Username display
- Dropdown menu:
  - My Profile (if detail page exists)
  - Settings
  - Logout

---

### 11. Edge Cases & Constraints

#### 11.1 Permission-Based UI

**Visibility Rules:**
- Admin: see all menus, all data, all actions
- Team Lead: see team menu, team data, can manage team only
- User: see own teams, own workloads, submit workloads in accessible projects

**Action Visibility:**
- Create User/Team/Cluster/Policy: Admin only
- Manage team members: Team lead + admin
- Submit workload: All authenticated users
- Cancel workload: Admin, workload submitter, team lead of owning team
- Delete workload: Admin, workload submitter (if terminal status)
- View pod logs: Admin, workload submitter, team lead of owning team

---

#### 11.2 Real-Time Constraints

**Cluster Offline:**
- Cannot submit workload to offline cluster
- Show warning if cluster offline in cluster detail
- Disable cluster selection in workload submit form

**Insufficient Resources:**
- Show quota remaining on project
- If user requests exceed quota, show error: "Insufficient GPU quota (requested X, available Y)"
- Prevent submission

---

#### 11.3 Terminal Statuses

**Workload Terminal Statuses:**
- SUCCEEDED
- FAILED
- CANCELLED
- PREEMPTED

**UX:**
- Once workload reaches terminal status, cannot change
- Cancel button hidden
- Show finish time, duration
- Can delete (permanent, with confirmation)

---

#### 11.4 Long-Running Operations

**Polling/Streaming:**
- Workload status: real-time updates critical, low latency needed
- Cluster metrics: 30-second interval acceptable
- Pod logs: 5-second interval acceptable
- Fallback gracefully if SSE not available

---

### 12. Accessibility & UX Principles

#### 12.1 Keyboard Navigation

- All interactive elements accessible via Tab
- Enter/Space to activate buttons
- Escape to close dialogs
- Arrow keys for select dropdowns

#### 12.2 Visual Feedback

- Hover states on clickable elements
- Focus states on form fields
- Disabled state for unavailable actions
- Loading states clearly indicated

#### 12.3 Error Prevention

- Confirmation dialogs for destructive actions
- Disable submit until all required fields filled
- Show validation errors before submission
- Confirm before leaving unsaved form (optional)

#### 12.4 Performance

- Lazy load images (avatar, icons)
- Paginate large lists
- Debounce search/filter input
- Show skeleton loaders while data loads

---

## Appendix: Data Models (UX Perspective)

### User

| Field | Meaning |
|-------|---------|
| id | Unique identifier |
| username | Display name for user |
| email | Email address |
| globalRole | ADMIN, USER |
| createdAt | Signup time |

### Team

| Field | Meaning |
|-------|---------|
| id | Unique identifier |
| name | Team display name |
| description | Short description |
| createdAt | Creation time |
| memberCount | Current member count |

### TeamMember

| Field | Meaning |
|-------|---------|
| userId | User ID |
| teamId | Team ID |
| role | MEMBER, TEAM_LEAD |

### Cluster

| Field | Meaning |
|-------|---------|
| id | Unique identifier |
| name | Display name |
| status | Online, Offline, Unknown |
| nodeCount | K8s node count |
| gpuCount | Total GPU slots |
| cpuCapacity | Total CPU cores |
| memoryCapacity | Total memory GB |
| lastHealthCheck | Last connection time |
| createdAt | Registration time |

### Policy

| Field | Meaning |
|-------|---------|
| id | Unique identifier |
| name | Policy name |
| clusterId | Cluster this policy belongs to |
| gpuQuota | Max GPUs allowed per project |
| cpuQuota | Max CPU cores allowed per project |
| memoryQuota | Max memory GB allowed per project |
| nodeAffinity | Optional K8s affinity rules |

### Project

| Field | Meaning |
|-------|---------|
| id | Unique identifier |
| name | Project name |
| teamId | Owning team |
| clusterId | Cluster this project runs on |
| policyId | Resource quota policy |
| createdAt | Creation time |

### Workload

| Field | Meaning |
|-------|---------|
| id | Unique identifier |
| name | User-provided workload name |
| type | NOTEBOOK, LLM_INFERENCE |
| projectId | Project this workload belongs to |
| status | PENDING, QUEUED, RUNNING, SUCCEEDED, FAILED, PREEMPTED, CANCELLED |
| gpuRequest | Number of GPUs requested |
| cpuRequest | CPU cores requested (optional) |
| memoryRequest | Memory requested in GB (optional) |
| image | Docker image (required) |
| submittedBy | User ID of submitter |
| submittedAt | Submission time |
| finishedAt | Completion time (if terminal) |
| extra | Type-specific JSON (PVC size, model source, vLLM params, replicas, env vars) |

### Pod

| Field | Meaning |
|-------|---------|
| name | Pod name in Kubernetes |
| status | Pending, Running, Succeeded, Failed |
| restartCount | Number of restarts |
| age | Time since creation |

---

**Last Updated:** 2026-04-28  
**Purpose:** AI Design Tool Prompt Template
