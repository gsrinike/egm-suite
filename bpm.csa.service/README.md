# bpm.csa.service

Camunda-backed CSA process module.

It owns the `csa-end-to-end` BPMN definition and exposes the remote BPM REST API
shape consumed by `com.infra.bpm.remote.RemoteBusinessProcessService`.

```mermaid
flowchart LR
  A[CSA case accepted] --> B[Initialize CSA case]
  B --> C[Run Load Flow and Security Analysis]
  C --> D{Violations detected?}
  D -->|yes| E[Optimize remedial actions]
  E --> F[Validate CSA outcome]
  D -->|no| F
  F --> G[CSA completed]
```
