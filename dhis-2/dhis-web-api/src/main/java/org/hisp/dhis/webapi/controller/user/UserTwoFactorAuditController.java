/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller.user;

import static org.hisp.dhis.security.Authorities.ALL;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.security.twofa.audit.TwoFactorAuditQueryService;
import org.hisp.dhis.security.twofa.audit.TwoFactorAuditQueryService.PrivilegedCounts;
import org.hisp.dhis.security.twofa.audit.TwoFactorAuditQueryService.Status;
import org.hisp.dhis.security.twofa.audit.TwoFactorAuditQueryService.UserAuditRow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only 2FA enrolment audit for the user base. Restricted to callers holding {@link
 * org.hisp.dhis.security.Authorities#ALL}. All aggregation and filtering is delegated to {@link
 * TwoFactorAuditQueryService}, which runs native SQL against the user tables — no User entities or
 * lazy role collections are hydrated here.
 *
 * @author Morten Svanaes
 */
@OpenApi.Document(
    group = OpenApi.Document.GROUP_QUERY,
    classifiers = {"team:platform", "purpose:security"})
@RestController
@RequestMapping("/api/users/twoFactor")
@RequiredArgsConstructor
@RequiresAuthority(anyOf = ALL)
public class UserTwoFactorAuditController {

  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int MAX_PAGE_SIZE = 1000;
  private static final int UNPAGED_HARD_CEILING = 10_000;

  private final TwoFactorAuditQueryService auditService;

  @GetMapping("/summary")
  public TwoFactorAuditSummary getSummary() {
    Map<TwoFactorType, Long> byType = auditService.countByType();
    long total = byType.values().stream().mapToLong(Long::longValue).sum();
    long enabled =
        byType.getOrDefault(TwoFactorType.TOTP_ENABLED, 0L)
            + byType.getOrDefault(TwoFactorType.EMAIL_ENABLED, 0L);
    long disabled = total - enabled;
    double coverage = total == 0 ? 0d : Math.round((double) enabled / total * 1000d) / 10d;
    PrivilegedCounts privileged = auditService.countPrivileged();
    return new TwoFactorAuditSummary(
        total,
        enabled,
        disabled,
        coverage,
        byType,
        new PrivilegedUserStats(
            privileged.withAllAuthority(), privileged.withAllAuthorityMissing2FA()));
  }

  @GetMapping
  public TwoFactorAuditList getList(
      @RequestParam(required = false, defaultValue = "ALL") Status status,
      @CheckForNull @RequestParam(required = false) List<TwoFactorType> type,
      @RequestParam(required = false, defaultValue = "true") boolean paging,
      @RequestParam(required = false, defaultValue = "1") int page,
      @RequestParam(required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize) {
    int total = auditService.count(status, type);
    int requestedPageSize =
        paging
            ? Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE)
            : Math.max(1, Math.min(total, UNPAGED_HARD_CEILING));
    Pager pager = new Pager(page, total, requestedPageSize);
    int offset = paging ? pager.getOffset() : 0;
    int limit = paging ? pager.getPageSize() : UNPAGED_HARD_CEILING;
    List<TwoFactorAuditEntry> entries =
        auditService.list(status, type, offset, limit).stream()
            .map(UserTwoFactorAuditController::toEntry)
            .toList();
    return new TwoFactorAuditList(pager, entries);
  }

  private static TwoFactorAuditEntry toEntry(UserAuditRow row) {
    return new TwoFactorAuditEntry(
        row.uid(),
        row.username(),
        row.name(),
        row.twoFactorType(),
        row.lastLogin(),
        row.email(),
        row.disabled(),
        row.invitation());
  }

  public record TwoFactorAuditSummary(
      @JsonProperty long totalUsers,
      @JsonProperty long enabled,
      @JsonProperty long disabled,
      @JsonProperty double coveragePercent,
      @JsonProperty Map<TwoFactorType, Long> byType,
      @JsonProperty PrivilegedUserStats privileged) {}

  public record PrivilegedUserStats(
      @JsonProperty long withAllAuthority, @JsonProperty long withAllAuthorityMissing2FA) {}

  public record TwoFactorAuditList(
      @JsonProperty Pager pager, @JsonProperty List<TwoFactorAuditEntry> users) {}

  public record TwoFactorAuditEntry(
      @JsonProperty String id,
      @JsonProperty String username,
      @JsonProperty String name,
      @JsonProperty TwoFactorType twoFactorType,
      @JsonProperty Date lastLogin,
      @JsonProperty String email,
      @JsonProperty boolean disabled,
      @JsonProperty boolean invitation) {}
}
