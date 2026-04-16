# KB Changelog

Append-only. Newest on top. One entry per session, one line per file touched where practical.

Format: `YYYY-MM-DD · agent · commit-sha · file(s) · what-and-why`

---

- 2026-04-16 · claude-sonnet-4-6 · 0fcfa39f1c · authn/pat.md populated (status: verified). Key corrections vs prior understanding: both V1 and V2 PATs use SHA-256 (not SHA-512); authorities are cached for 1h (not refreshed per request); `api_token.sharing` column exists and semantics are an open question; `hashToken` SHA-512 branch is currently dead code.
- 2026-04-16 · claude-sonnet-4-6 · 0fcfa39f1c · authz/authorities catalogued: 192 distinct `F_*` tokens enumerated, grouped into user/metadata/data/tracker/analytics/system/other. Definition sources traced to `Authorities.java` enum (52) and per-schema `SchemaDescriptor` registrations (~140). `_index.md` master + 7 group index files populated (status: verified).
- 2026-04-16 · claude-sonnet-4-6 · 0fcfa39f1c · scaffolded KB structure, templates, authn/authz stubs, chapter placeholders, staleness script
