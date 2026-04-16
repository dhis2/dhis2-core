# DHIS2 Knowledge Base

Descriptive, code-grounded knowledge about DHIS2 architecture. Agent-only audience.

**Agents**: read [`AGENTS.md`](AGENTS.md) before editing anything here.

## Layout

- [`AGENTS.md`](AGENTS.md) — operating protocol (mandatory)
- [`GLOSSARY.md`](GLOSSARY.md) — canonical terms
- [`INDEX.md`](INDEX.md) — every entry, one line each
- [`CHANGELOG.md`](CHANGELOG.md) — append-only log of KB edits
- [`templates/`](templates/) — entry templates (concept / flow / component / authority / decision)
- [`scripts/check-staleness.sh`](scripts/check-staleness.sh) — detect entries whose sources changed since `last_verified.commit`

## Chapters

- [`authn/`](authn/) — authentication
- [`authz/`](authz/) — authorization (authorities, sharing/ACL, scoping, tracker access)
- [`api/`](api/) — REST layer
- [`services/`](services/) — business-logic services
- [`domain/`](domain/) — core domain entities
- [`persistence/`](persistence/) — Hibernate, stores, Flyway
- [`tracker/`](tracker/) — tracker subsystem
- [`analytics/`](analytics/) — analytics engine
- [`import-export/`](import-export/) — DXF2, metadata/data import
- [`apps/`](apps/) — app manager
- [`expression/`](expression/) — expression parser, rule engine
- [`caching/`](caching/) — EhCache, Redis
- [`messaging/`](messaging/) — Artemis, event hooks, notifications
- [`scheduling/`](scheduling/) — job scheduler
- [`infrastructure/`](infrastructure/) — Docker, deployment, config
- [`decisions/`](decisions/) — cross-cutting design decisions

## Relationship to the security review

This KB is descriptive ("how it works today"). Findings and evaluations live under [`../security-review/`](../security-review/) (frozen artefacts per review cycle). The review feeds the KB; the KB does not cite findings.
