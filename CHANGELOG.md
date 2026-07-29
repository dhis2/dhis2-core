# Changelog

- 2026-07-30 - **Ignore /api/loginConfig in Spring Security chain (DHIS2-21909)** Public login bootstrap no longer runs Basic/session auth filters, so authenticated cold hits cannot N+1-load `userroleauthorities` / `userrolerestrictions` while building `UserDetails`. See `dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java`.
