## Branch

`feature/<name>`

## Task checklist

<!-- Copy the relevant feature/<name> section from README.md's task checklist here, checked off. -->

- [ ]

## Commit summary

<!-- One line per meaningful commit, in the order they were made. -->

## Test checklist

- [ ] `mvn test` passes for every module touched by this branch
- [ ] Unit tests added/updated for new logic
- [ ] Integration tests added where applicable (Testcontainers, WireMock, embedded Kafka)
- [ ] Manually verified locally where the change is user/HTTP-facing

## Code review checklist

- [ ] Contract/implementation pattern respected (interface at the root of `service/`, implementation in `service/impl/`)
- [ ] Every endpoint returns `ApiResponse<T>` from `common-lib`, never reimplemented
- [ ] No business logic, JPA entity, repository, or domain-specific DTO added to `common-lib`
- [ ] No cross-service foreign key or direct database access across a service boundary; IDs referencing another service's entity are plain columns
- [ ] Kafka events (if any) named in the past tense and published only after the local transaction that produced them has committed
- [ ] Every class carries the mandatory class-level Javadoc block (see `.claude/CLAUDE.md`)
- [ ] No em dash character introduced anywhere
