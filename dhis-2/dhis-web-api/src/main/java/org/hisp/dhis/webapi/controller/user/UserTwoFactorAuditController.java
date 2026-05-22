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
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only 2FA enrolment audit for the user base. Restricted to callers holding {@link
 * org.hisp.dhis.security.Authorities#ALL}.
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

  private final UserService userService;

  @GetMapping("/summary")
  public TwoFactorAuditSummary getSummary() {
    Map<TwoFactorType, Long> byType = new EnumMap<>(TwoFactorType.class);
    for (TwoFactorType type : TwoFactorType.values()) {
      byType.put(type, 0L);
    }
    long total = 0;
    long enabled = 0;
    long withAllAuthority = 0;
    long withAllAuthorityMissing2FA = 0;
    for (User user : userService.getAllUsers()) {
      total++;
      TwoFactorType type = effectiveType(user);
      byType.merge(type, 1L, Long::sum);
      if (type.isEnabled()) {
        enabled++;
      }
      if (user.isSuper()) {
        withAllAuthority++;
        if (!type.isEnabled()) {
          withAllAuthorityMissing2FA++;
        }
      }
    }
    long disabled = total - enabled;
    double coverage = total == 0 ? 0d : Math.round((double) enabled / total * 1000d) / 10d;
    return new TwoFactorAuditSummary(
        total,
        enabled,
        disabled,
        coverage,
        byType,
        new PrivilegedUserStats(withAllAuthority, withAllAuthorityMissing2FA));
  }

  @GetMapping
  public TwoFactorAuditList getList(
      @RequestParam(required = false, defaultValue = "ALL") AuditStatus status,
      @CheckForNull @RequestParam(required = false) List<TwoFactorType> type) {
    List<TwoFactorAuditEntry> entries =
        userService.getAllUsers().stream()
            .filter(u -> matchesStatus(u, status))
            .filter(u -> matchesType(u, type))
            .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
            .map(UserTwoFactorAuditController::toEntry)
            .toList();
    return new TwoFactorAuditList(entries.size(), entries);
  }

  private static boolean matchesStatus(User user, AuditStatus status) {
    TwoFactorType type = effectiveType(user);
    return switch (status) {
      case ALL -> true;
      case ENABLED -> type.isEnabled();
      case DISABLED -> !type.isEnabled();
    };
  }

  private static boolean matchesType(User user, @CheckForNull List<TwoFactorType> types) {
    return types == null || types.isEmpty() || types.contains(effectiveType(user));
  }

  private static TwoFactorType effectiveType(User user) {
    TwoFactorType type = user.getTwoFactorType();
    return type == null ? TwoFactorType.NOT_ENABLED : type;
  }

  private static TwoFactorAuditEntry toEntry(User user) {
    return new TwoFactorAuditEntry(
        user.getUid(),
        user.getUsername(),
        user.getName(),
        effectiveType(user),
        user.getLastLogin());
  }

  public enum AuditStatus {
    ALL,
    ENABLED,
    DISABLED
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
      @JsonProperty long total, @JsonProperty List<TwoFactorAuditEntry> users) {}

  public record TwoFactorAuditEntry(
      @JsonProperty String id,
      @JsonProperty String username,
      @JsonProperty String name,
      @JsonProperty TwoFactorType twoFactorType,
      @JsonProperty Date lastLogin) {}
}
