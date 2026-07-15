# srv.common.lfsa

Spring Boot service that owns reusable Load Flow and Security Analysis APIs for
CSA, CC, and OPC workflows.

The first increment returns deterministic in-memory results. A later increment
can replace the service implementation with a PowSyBl-backed engine without
changing CSA orchestration contracts.
