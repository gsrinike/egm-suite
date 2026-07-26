# RCC CSA Design

The RCC module family provides the current Regional Coordination Centre user
surface. It includes CGM import navigation, CSA orchestration, workflow
monitoring, and placeholders for future CC and OPC capabilities.

## Module Ownership

- `data.common`: DTOs for network case references, load-flow requests/results,
  security-analysis requests/results, RAO requests/results, CSA status, and
  workflow views.
- `srv.common.lfsa`: reusable load-flow and security-analysis REST boundary.
- `srv.common.rao`: reusable remedial-action optimization REST boundary.
- `srv.csa.services`: CSA orchestration service that coordinates common
  services and starts BPM through `com.infra`.
- `bpm.csa.service`: Camunda runtime that owns the `csa-end-to-end` process.
- `gui.lfsa.manager`: Vue LFSA manager UI for import search, run start, and
  security-analysis result browsing.
- `gui.rcc.manager`: Vue RCC manager UI that embeds CGM import and LFSA
  capability screens.
- `mock.srv.*`: in-memory service alternatives for frontend and orchestration
  development.

## CSA Flow

```mermaid
flowchart LR
  A["CSA case requested"] --> B["Start CSA process"]
  B --> C["Run load flow"]
  C --> D["Run security analysis"]
  D --> E{"Violations?"}
  E -->|Yes| F["Run RAO"]
  F --> G["Validate actions"]
  E -->|No| G
  G --> H["Publish workflow view"]
```

## Service Boundaries

Load flow, security analysis, and RAO are not embedded in CSA. They are common
services because CSA, capacity calculation, and operational planning can reuse
the same contracts.

CSA orchestration does not import `bpm.csa.service`. It starts and observes
workflow instances through the generic BPM contract in `com.infra` or through a
remote BPM endpoint.

## GUI

`gui.rcc.manager` has a sidebar with CGM, CSA, CC, OPC, and workflow monitoring.
CGM > Import Manager renders the CNM manager view while preserving configurable
CNM and IIDM backend URLs. CGM > Security Analysis renders the LFSA manager,
which searches successful imports, starts an asynchronous run, and lists stored
run results. CGM > Sensitivity Analysis, CC, and OPC are shown as inactive
placeholders until their services are introduced.

## CGM Security Analysis Flow

```mermaid
sequenceDiagram
  participant GUI as gui.rcc.manager / gui.lfsa.manager
  participant LFSA as srv.common.lfsa
  participant ES as Document Store
  participant MQ as RabbitMQ
  participant PS as PowSyBl Network APIs

  GUI->>LFSA: GET /api/common/lfsa/imports
  LFSA->>ES: Read successful cnm-imports
  LFSA-->>GUI: Import candidates
  GUI->>LFSA: POST /api/common/lfsa/security-analysis/runs
  LFSA->>ES: Save STARTED run
  LFSA->>MQ: Publish SecurityAnalysisRequested
  MQ->>LFSA: Consume requested event
  LFSA->>ES: Load iidm-networks for import
  LFSA->>PS: Reconstruct Network from XIIDM
  LFSA->>PS: Merge/bind networks in memory
  LFSA->>ES: Save DONE or FAILED run with diagnostics
  GUI->>LFSA: GET /api/common/lfsa/security-analysis/runs
  GUI->>LFSA: GET /api/common/lfsa/security-analysis/runs/{runId}
```
