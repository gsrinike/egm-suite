# srv.csa.services

`srv.csa.services` is the CSA orchestration REST service.

## Responsibilities

- Accept CSA case start requests.
- Call the common LF/SA service for load-flow and security-analysis work.
- Call the common RAO service when remedial actions are needed.
- Start and observe the CSA workflow through the generic BPM contract in
  `com.infra`.
- Return CSA case and workflow views using `data.common` DTOs.

The service does not depend directly on `bpm.csa.service`.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.csa.services -am test
```
