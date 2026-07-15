# RCC CSA Design

This document describes the first RCC capability increment: Coordinated Security Analysis (CSA). The module family is additive and does not move CNM import business logic.

## Module Boundaries

- `data.common`: transport DTOs shared by CSA, common LF/SA, common RAO, BPM, GUI, and mocks.
- `srv.common.lfsa`: common load-flow and security-analysis REST service.
- `mock.srv.common.lfsa`: mock LF/SA REST service for frontend and workflow development.
- `srv.common.rao`: common remedial-action optimization REST service.
- `mock.srv.common.rao`: mock RAO REST service for frontend and workflow development.
- `srv.csa.services`: CSA orchestration REST service. It starts CSA cases, calls LF/SA and RAO, and invokes BPM through `com.infra.bpm`.
- `mock.srv.csa.services`: mock CSA REST service aligned with the CSA OpenAPI contract.
- `bpm.csa.service`: Camunda-backed process host for the `csa-end-to-end` process.
- `gui.rcc.manager`: Vue RCC manager focused on CSA execution and workflow monitoring.

CSA uses network case references. CNM import remains responsible for RDF ingestion and network model metadata.

## CSA End-To-End

```mermaid
sequenceDiagram
    actor User
    participant GUI as gui.rcc.manager
    participant CSA as srv.csa.services
    participant BPM as bpm.csa.service
    participant LFSA as srv.common.lfsa
    participant RAO as srv.common.rao

    User->>GUI: Enter CSA case and network reference
    GUI->>CSA: POST /api/csa/cases
    CSA->>BPM: POST /api/bpm/processes/csa-end-to-end/start
    BPM-->>CSA: processInstanceId
    CSA->>LFSA: POST /api/common/lfsa/load-flow
    LFSA-->>CSA: LoadFlowResult
    CSA->>LFSA: POST /api/common/lfsa/security-analysis
    LFSA-->>CSA: SecurityAnalysisResult
    alt Violations detected and RAO enabled
        CSA->>RAO: POST /api/common/rao/optimize
        RAO-->>CSA: RaoResult
    else No RAO needed
        CSA->>CSA: Mark RAO task skipped
    end
    CSA-->>GUI: CsaCaseStatus
    GUI->>GUI: Render workflow tasks, violations, and RAO actions
```

## Common LF/SA Flow

```mermaid
sequenceDiagram
    participant CSA as srv.csa.services
    participant LFSA as srv.common.lfsa

    CSA->>LFSA: LoadFlowRequest(networkCase, options)
    LFSA-->>CSA: Line flows and convergence metadata
    CSA->>LFSA: SecurityAnalysisRequest(networkCase, contingencies)
    LFSA-->>CSA: Pre-contingency and post-contingency violations
```

`srv.common.lfsa` is common by design. CSA, capacity calculation, and operational planning modules should reuse it instead of embedding load-flow or security-analysis logic in their own services.

## RAO Flow

```mermaid
sequenceDiagram
    participant CSA as srv.csa.services
    participant RAO as srv.common.rao

    CSA->>CSA: Collect overloads from security analysis
    CSA->>RAO: RaoRequest(networkCase, violations, thresholds)
    RAO-->>CSA: Recommended remedial actions and before/after loading
    CSA->>CSA: Attach RAO result to CSA case status
```

`srv.common.rao` is shared because remedial-action optimization is needed by more than CSA.

## BPM Interaction

```mermaid
sequenceDiagram
    participant CSA as srv.csa.services
    participant Infra as com.infra.bpm remote adapter
    participant BPM as bpm.csa.service
    participant Camunda as Embedded Camunda Engine

    CSA->>Infra: start(ProcessStartRequest)
    Infra->>BPM: POST /api/bpm/processes/csa-end-to-end/start
    BPM->>Camunda: RuntimeService.startProcessInstanceByKey
    Camunda-->>BPM: process instance
    BPM-->>Infra: ProcessInstanceView
    Infra-->>CSA: ProcessInstanceView
```

`srv.csa.services` has no Maven dependency on `bpm.csa.service`. The process module can be deployed separately and replaced without changing CSA service code, as long as it preserves the process-neutral BPM REST shape.

## Workflow Monitor

```mermaid
sequenceDiagram
    actor User
    participant GUI as gui.rcc.manager
    participant CSA as srv.csa.services

    User->>GUI: Open Workflow Monitor
    GUI->>CSA: GET /api/csa/cases
    CSA-->>GUI: List<CsaCaseStatus>
    GUI->>GUI: Render process id, status, task states, and timestamps
    User->>GUI: Select a case
    GUI->>CSA: GET /api/csa/cases/{csaCaseId}
    CSA-->>GUI: CsaCaseStatus detail
```

## CGM Import Manager Entry

```mermaid
sequenceDiagram
    actor User
    participant RCC as gui.rcc.manager
    participant CNM as gui.cnm.manager component
    participant API as srv.cnm.services

    User->>RCC: Select CGM / Import Manager
    RCC->>CNM: Render CnmManagerView
    CNM->>API: GET /api/cnm/imports
    API-->>CNM: ImportStatus page
    User->>CNM: Upload RDF/XML or ZIP files
    CNM->>API: POST /api/cnm/imports/chunks
    CNM->>API: POST /api/cnm/imports/chunks/complete
    API-->>CNM: ImportStatus
```

The RCC GUI exposes CGM above CSA, CC, and OPC. The CGM / Import Manager entry embeds the CNM manager screens and preserves the existing CNM backend integration. Runtime frontend configuration keeps the CSA and CNM API base URLs separate so each environment can route `/api/csa/**` and `/api/cnm/**` independently.

CSA and Workflow Monitor remain active RCC items. CC and OPC remain disabled placeholders until their service modules are introduced.

## Local Ports

- `srv.common.lfsa`: `8091`
- `mock.srv.common.lfsa`: `8092`
- `srv.common.rao`: `8093`
- `mock.srv.common.rao`: `8094`
- `srv.csa.services`: `8095`
- `mock.srv.csa.services`: `8096`
- `bpm.csa.service`: `8097`
- `gui.rcc.manager`: `5174`
