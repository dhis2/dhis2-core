# Changelog

- 2026-07-30 - **Batch-load role authorities/restrictions in createUserDetails (DHIS2-21909)** `DefaultUserService.createUserDetails` resolves role authorities and restrictions via `UserRoleStore` batch SQL (`userroleauthorities` / `userrolerestrictions`) when collections are lazy, avoiding N+1 during `UserDetails` construction. Transient/initialized roles stay in-memory; roles are not mutated. See `DefaultUserService`, `UserRoleStore`, `HibernateUserRoleStore`, `UserDetails`.
