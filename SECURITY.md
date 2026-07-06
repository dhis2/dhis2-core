# DHIS2 Security Policy

The DHIS2 security team participates in responsible disclosure and welcomes
collaboration with the wider community on security issues. This document
explains how to report a vulnerability in `dhis2-core`, what you can expect from
us, and what we ask of you.

This policy follows the official
[DHIS2 Vulnerability Reporting & Disclosure Policy](https://dhis2.org/trust/vulnerability-policy/).
For `dhis2-core`, we prefer GitHub private vulnerability reporting as the intake
channel (see below). Where the substance of this file and the published policy
differ (scope, timelines, or disclosure terms), the published policy takes
precedence.

## Reporting a Vulnerability

**Do not report security issues through public GitHub issues, pull requests, the
Jira issue tracker, or the public mailing lists.** These channels are visible to
people outside the security team and may expose other users to risk before a fix
is available.

For issues in `dhis2-core`, the **preferred** channel is GitHub's private
vulnerability reporting. It keeps the report, triage discussion, fix, and
published advisory together in one tracked place, which lets us respond faster
and avoid losing reports:

- **GitHub Security Advisories (preferred):** Open a private report using the
  [**"Report a vulnerability"**](https://github.com/dhis2/dhis2-core/security/advisories/new)
  button on the repository's _Security_ tab. This requires a GitHub account and
  routes directly to the maintainers without exposing any details publicly.
- **Email (alternative):** Use **security@dhis2.org** if you do not have a GitHub
  account, or if the issue is not specific to `dhis2-core` (for example,
  `dhis2.org`, `apps.dhis2.org`, the Android app, or an issue spanning multiple
  DHIS2 products). Include a brief summary and your contact details so we can
  continue on a secure channel.

To help us triage and reproduce the issue quickly, your report should include:

- DHIS2 version
- DHIS2 build number
- A description of the issue
- Why you consider it a security vulnerability
- Clear, written steps to reproduce (a proof of concept is welcome)
- Whether you would like to be credited (YES / NO)

Reports written in English are preferred.

## What to Expect

- **Acknowledgement:** We aim to respond within **10 working days**.
- **Triage:** We will work with you to validate the issue, determine its
  severity and impact, and identify affected versions.
- **Remediation:** Investigating, fixing, and releasing a correction takes time,
  as does giving administrators a window to upgrade. We will keep you informed of
  progress.
- **Recognition:** We do not currently offer monetary rewards. With your
  permission, we credit reporters in the
  [Security Hall of Fame](https://dhis2.org/trust/hall-of-fame/) and in change
  logs and advisories.

## Supported Versions

We provide security fixes for the **three most recent releases** of DHIS2. Older
releases are out of scope and do not receive security patches.

For the current list of supported releases, see the
[DHIS2 software releases page](https://dhis2.org/releases/) and the
[upgrade guide](https://docs.dhis2.org/en/manage/concepts/upgrade-guide.html). If
you are running an unsupported version, upgrade to a supported release.

## Scope

The following targets are in scope:

- Your own private instances of the DHIS2 software
- The open source DHIS2 software, including this repository (`dhis2-core`)
- `dhis2.org`
- `apps.dhis2.org`
- The current Android Capture App and the Android SDK

### Out of Scope

- **Unsupported versions of DHIS2.** Only the last three releases are supported.
- **Third-party apps built on the DHIS2 platform.** Apps not published or
  developed by the University of Oslo. Report these to their maintainers.
- **Lack of rate limiting and volumetric denial-of-service.** Reports concerning
  volumetric DoS attacks are out of scope.
- **Vulnerabilities requiring outdated browsers.** Issues affecting outdated
  browser versions, or those caused by browser extensions.
- **Man-in-the-middle attacks.** We cannot protect users on untrusted networks.
- **Email policy (DMARC, SPF, DKIM).** Our current settings intentionally
  balance security against email deliverability.

## Threat Model

Security reports are evaluated against the
[DHIS2 Threat Model](https://github.com/dhis2/threat-model). It defines the
principals (DHIS2 users and administrators), adversaries, trust assumptions, and
security invariants we design against. These invariants are relevant when
assessing whether a finding is in scope:

- Account sign-up requires email verification.
- An unauthenticated party cannot confirm whether a specific DHIS2 account
  exists.
- Any sensitive operation requires re-authentication.
- Any sensitive operation generates a security log event.

A DHIS2 administrator is highly privileged by design: they can manage users,
change system-wide settings, install apps, and access the database and its
secrets. Findings that rely solely on legitimate administrator capabilities are
treated as expected behaviour rather than vulnerabilities.

## Coordinated Disclosure

The security team is committed to disclosing security issues responsibly. An
issue may be embargoed while a fix or workaround is developed and while users are
given a reasonable window to upgrade.

We ask participating researchers to:

- Give DHIS2 reasonable time to fix a reported issue before disclosing it to
  third parties.
- Not publicly disclose vulnerabilities or related details without explicit
  written authorization from DHIS2.
- Not include sensitive or personally identifying data in any disclosure.
- Make a good-faith effort to avoid privacy violations, data destruction, and
  service interruption while researching. Test only against your own instances,
  not against production deployments operated by others.

## CVE Assignment

CVE identifiers for DHIS2 are assigned by the DHIS2 security team as part of the
coordinated disclosure process, via
[GitHub Security Advisories](https://github.com/dhis2/dhis2-core/security/advisories).
Please **do not request or self-assign a CVE** for a DHIS2 issue independently;
doing so before a coordinated fix and accurate advisory are ready can mislead
downstream scanners and DHIS2 implementers. We will coordinate identifier
assignment, affected-version ranges, and severity with you.

## Learn More

- Security overview and features: <https://dhis2.org/security/>
- Trust centre: <https://dhis2.org/trust/>
- Hall of Fame: <https://dhis2.org/trust/hall-of-fame/>
- Threat model: <https://github.com/dhis2/threat-model>
- GHSA: <https://github.com/dhis2/dhis2-core/security/advisories/new>

> Contact: security@dhis2.org \
> Preferred-Languages: en \
> Policy: https://dhis2.org/trust/vulnerability-policy
