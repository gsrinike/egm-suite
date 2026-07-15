# srv.csa.services

CSA orchestration service.

It starts CSA cases, invokes reusable LF/SA and RAO services over REST, and
attempts to start the CSA BPM process via the `com.infra` BPM contract. It does
not depend on `bpm.csa.service` directly.
