# AGENTS.md — KB operating protocol

Read this before editing anything under `docs/kb/`. This is the protocol every agent follows.

## 1. Purpose

Descriptive, code-grounded knowledge about DHIS2. Read by future agents doing any task. Not a narrative, not a tutorial.

## 2. Non-negotiables

1. **Every non-obvious factual claim ends with a citation**: `([`ClassName.method`](relative/path/File.java#L42))` or `(file.java:L42-L80)`. No citation → move the claim to `## Open questions` or delete it.
2. **One concept per file.** Split when a file exceeds ~300 lines.
3. **No duplication.** Check [`INDEX.md`](INDEX.md) before creating a new entry. Update the existing one if it exists.
4. **Frontmatter is mandatory** and schema-typed. See §4.
5. **Agent-only tone.** Dense, structured, imperative. No "Welcome to…", no explanations of obvious terms, no headers just to restate the file name.
6. **Descriptive, not prescriptive.** Opinions, findings, or "this should change" belong in `docs/security-review/`, not here.

## 3. Iteration loop

When asked to improve or add KB on topic X, execute this decision tree:

```
1. Does an entry exist for X in INDEX.md?
   ├── No  → pick chapter → pick template → create entry → update INDEX → append CHANGELOG
   └── Yes →
       2. Run staleness check on the entry's sources.
          ├── sources changed since last_verified.commit → set status: stale → re-verify → refresh
          └── unchanged → continue
       3. Does the entry have open_questions?
          ├── Yes → pick one, investigate, answer it or restate it more precisely
          └── No  → find claims without citations → add them, or downgrade to open_question
       4. Do see_also links resolve?
          ├── Broken → fix, or create stub with status: draft
          └── OK    → pick a neighbouring entry's open question
```

Every session ends with an append to [`CHANGELOG.md`](CHANGELOG.md).

## 4. Frontmatter schema

```yaml
---
title: string                           # human title, matches H1
type: concept | flow | component | authority | decision
status: draft | verified | stale | deprecated
last_verified:
  commit: string                        # git SHA the entry was verified against
  date: YYYY-MM-DD
  by: string                            # agent model id or human name
sources:
  - path: string                        # repo-relative path
    lines: string                       # "42" or "42-80"
    role: primary | supporting | test
see_also:
  - string                              # repo-relative path to another KB entry
tags: [string, ...]
confidence: high | medium | low
open_questions:
  - string                              # free-form, each a single question
redirect_to: string                     # only when status: deprecated
---
```

- `last_verified.commit` **must** be a real SHA in this repo; do not invent one.
- `sources[].path` **must** exist at that SHA; verify with `git cat-file -e <sha>:<path>` if unsure.
- `confidence: low` is fine; fabrication is not.

## 5. Body structure by `type`

Use the matching template under [`templates/`](templates/). Section order is load-bearing — agents read by section.

- `concept.md`: Summary · How it works · Entry points · Invariants · Edge cases · Gotchas · Open questions
- `flow.md`: Summary · Actors · Steps · Failure modes · Logs & audit · Open questions
- `component.md`: Summary · Responsibility · Collaborators · Public surface · Threading & state · Open questions
- `authority.md`: Summary · Granted to by default · Checked at · Unlocks · Adjacent authorities · Open questions
- `decision.md`: Context · Decision · Consequences · Alternatives considered · Status · Open questions

## 6. File naming

- Lowercase, kebab-case, `.md`. `F_USER_ADD` → `f-user-add.md`.
- Chapter index files are named `_index.md`. The underscore keeps them at the top in directory listings.
- Never rename a file without leaving a deprecated stub at the old path with `redirect_to:` set.

## 7. Staleness

Before trusting an entry, run:

```bash
./docs/kb/scripts/check-staleness.sh docs/kb/path/to/entry.md
```

If it reports changes, set `status: stale` **before** editing, so the in-progress state is visible to concurrent agents. Re-verify against HEAD, refresh, then flip `status: verified`.

## 8. What does NOT belong here

- Findings, CVEs, severity ratings, or "should be fixed" language → [`docs/security-review/`](../security-review/)
- TODOs as action items → open a GitHub issue or add to the review plan
- Ephemeral task state → conversation / memory, not KB
- Narrative history ("in 2019 we moved from…") unless it's a `decision` entry with code-cited consequences today
- Copy-pasted code blocks longer than ~20 lines — cite, don't duplicate

## 9. Renaming / deprecating

1. Create the new entry at the new path.
2. Replace the old file's body with a stub:
   ```yaml
   ---
   status: deprecated
   redirect_to: authz/authorities/user/f-user-add.md
   ---
   ```
3. Update every `see_also` that pointed at the old path.
4. Leave the stub for one full review cycle before deletion.

## 10. Chapter ownership

Each chapter's `_index.md` is the landing page for that topic. It lists every entry in the chapter with a one-line hook. Update the chapter `_index.md` whenever you add, move, or deprecate an entry inside it.

## 11. Working with authorities

Authorities (`F_*`) are grouped by subfolder under [`authz/authorities/`](authz/authorities/). Groups: `user/`, `metadata/`, `data/`, `tracker/`, `system/`, `analytics/`, `other/`. Place a new authority entry by functional area, not by first letter. Each group has its own `_index.md` listing its authorities in a table.

## 12. Commit discipline

- KB edits can ship in a dedicated commit separate from code changes, or bundled when the code change motivated the KB update.
- Commit message prefix: `docs(kb): …`
- Never include KB edits in a commit that the user has not explicitly authorised (see the master CLAUDE.md on committing).
