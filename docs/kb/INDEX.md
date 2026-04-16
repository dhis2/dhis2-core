# Index

Every KB entry, one line each. Keep alphabetical within each chapter.

Status legend: `D` draft · `V` verified · `S` stale · `X` deprecated.

## authn/

| Status | Entry | Hook |
|---|---|---|
| D | [authn/overview.md](authn/overview.md) | Entry point to all authN mechanisms |
| D | [authn/login-flow.md](authn/login-flow.md) | Username/password login and session establishment |
| D | [authn/session.md](authn/session.md) | Server-side session lifecycle |
| D | [authn/jwt.md](authn/jwt.md) | JWT issuance and validation |
| D | [authn/oauth2-oidc.md](authn/oauth2-oidc.md) | External identity providers |
| V | [authn/pat.md](authn/pat.md) | Personal access tokens |
| D | [authn/2fa.md](authn/2fa.md) | Second-factor flows |
| D | [authn/remember-me.md](authn/remember-me.md) | Persistent-login tokens |
| D | [authn/impersonation.md](authn/impersonation.md) | `switchUser` / user override |

## authz/

| Status | Entry | Hook |
|---|---|---|
| D | [authz/overview.md](authz/overview.md) | Three-layer authorization model |
| D | [authz/acl-sharing.md](authz/acl-sharing.md) | Object-level sharing and AclService |
| D | [authz/org-unit-scoping.md](authz/org-unit-scoping.md) | Capture vs view scope, hierarchy traversal |
| D | [authz/tracker-access.md](authz/tracker-access.md) | Program access levels and ownership |
| V | [authz/authorities/_index.md](authz/authorities/_index.md) | Authority catalogue — 192 distinct `F_*` authorities |
| V | [authz/authorities/user/_index.md](authz/authorities/user/_index.md) | User / role / group / impersonation authorities |
| V | [authz/authorities/metadata/_index.md](authz/authorities/metadata/_index.md) | Metadata-object CRUD and merge authorities |
| V | [authz/authorities/data/_index.md](authz/authorities/data/_index.md) | Data-value, data-set, approval authorities |
| V | [authz/authorities/tracker/_index.md](authz/authorities/tracker/_index.md) | Program / tracker authorities |
| V | [authz/authorities/analytics/_index.md](authz/authorities/analytics/_index.md) | Analytics / visualisation / report authorities |
| V | [authz/authorities/system/_index.md](authz/authorities/system/_index.md) | System / scheduling / routes / SQL-view authorities |
| V | [authz/authorities/other/_index.md](authz/authorities/other/_index.md) | Catch-all (empty) |

## Other chapters

| Chapter | Status |
|---|---|
| [api/](api/_index.md) | placeholder |
| [services/](services/_index.md) | placeholder |
| [domain/](domain/_index.md) | placeholder |
| [persistence/](persistence/_index.md) | placeholder |
| [tracker/](tracker/_index.md) | placeholder |
| [analytics/](analytics/_index.md) | placeholder |
| [import-export/](import-export/_index.md) | placeholder |
| [apps/](apps/_index.md) | placeholder |
| [expression/](expression/_index.md) | placeholder |
| [caching/](caching/_index.md) | placeholder |
| [messaging/](messaging/_index.md) | placeholder |
| [scheduling/](scheduling/_index.md) | placeholder |
| [infrastructure/](infrastructure/_index.md) | placeholder |
| [decisions/](decisions/_index.md) | placeholder |
