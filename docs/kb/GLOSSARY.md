# Glossary

Canonical spelling and meaning for DHIS2 terms. Use exactly these forms in KB entries. Alphabetical.

| Term | Canonical form | Meaning |
|---|---|---|
| ACL | `ACL` | Access Control List — DHIS2's object-level permission check layer, implemented by `AclService`. |
| Aggregate data | `aggregate data` | Data values summarised by period/org-unit/data-element. Distinct from tracker data. |
| Authority | `authority` (noun), `F_*` (token) | A named privilege. Strings start with `F_`. Granted via `UserRole`. |
| Break-the-glass | `break-the-glass` | Temporary ownership override on a protected program, requiring audit. |
| Category option combo | `CategoryOptionCombo` (COC) | Fine-grained disaggregation key on aggregate data. |
| Data capture OU scope | `data-capture scope` | The org-unit subtree a user can write data into. |
| Data view OU scope | `data-view scope` | The org-unit subtree a user can read data from. |
| DXF2 | `DXF2` | Data Exchange Format v2. DHIS2's metadata + data interchange format (XML/JSON). |
| Enrollment | `enrollment` | A tracked entity's participation in a program. |
| Event | `event` | A single data-collection occurrence in a tracker or event program. |
| Identifiable object | `IdentifiableObject` | Base type with a UID and sharing metadata. |
| Metadata | `metadata` | Configuration objects (data elements, indicators, programs, etc.) as opposed to data values. |
| Organisation unit | `organisation unit` (OU) | A node in the reporting hierarchy. Abbreviation `OU` is acceptable. |
| PAT | `PAT` | Personal Access Token. A scoped, revocable API credential bound to a user. |
| Program | `program` | A tracker or event collection definition. Has access level OPEN / AUDITED / PROTECTED / CLOSED. |
| Program stage | `program stage` | A step inside a program defining what data is collected. |
| Sharing | `sharing` | The per-object access settings: `publicAccess`, `userAccesses`, `userGroupAccesses`. |
| Superuser | `superuser` | A user with authority `ALL` (a.k.a. `F_SYSTEM_ALL`), bypassing most checks. |
| TEI | `tracked entity` (was `tracked entity instance` / `TEI`) | A person or subject tracked over time. Prefer `tracked entity` in new text. |
| Tracker | `tracker` | The subsystem for individual-level longitudinal data. Lives in the `dhis-tracker` module. |
| UID | `UID` | 11-character stable identifier on every `IdentifiableObject`. |
| User group | `UserGroup` | Named collection of users, used as a sharing principal. |
| User role | `UserRole` | Named collection of authorities, assigned to users. |
