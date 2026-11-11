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
package org.hisp.dhis.webapi.security.csp.app;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_REPORT_ENABLED;
import static org.hisp.dhis.security.utils.CspConstants.OVERRIDABLE_DIRECTIVES;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.security.csp.CspPolicyService;
import org.springframework.stereotype.Component;

/**
 * Deduplicated CSP violations, kept in a bounded in-memory cache region only (no database writes):
 * per app at most {@value #MAX_PER_APP} fingerprints, least recently seen evicted first, each
 * dropped 24 hours after it was last seen. A fingerprint is (app, entrypoint, disposition,
 * effective directive with {@code -elem}/{@code -attr} folded, blocked origin or keyword). Counts
 * are per node in a cluster.
 *
 * @author Morten Svanæs
 */
@Component
public class CspViolationService {
  static final int MAX_PER_APP = 100;

  static final Duration TTL = Duration.ofHours(24);

  /** Pseudo app key for violations of pages that are not served from an app. */
  public static final String PLATFORM_APP_KEY = "_platform";

  private final Cache<Bucket> buckets;
  private final CspAppPolicyService appPolicyService;
  private final CspPolicyService cspPolicyService;
  private final DhisConfigurationProvider config;

  public CspViolationService(
      CacheProvider cacheProvider,
      CspAppPolicyService appPolicyService,
      CspPolicyService cspPolicyService,
      DhisConfigurationProvider config) {
    this.buckets = cacheProvider.createCspViolationCache();
    this.appPolicyService = appPolicyService;
    this.cspPolicyService = cspPolicyService;
    this.config = config;
  }

  /** Violation counts of one app over the last 24 hours. */
  public record Counts(
      @JsonProperty long total,
      @JsonProperty long enforce,
      @JsonProperty long report,
      @JsonProperty int distinct) {
    public static final Counts ZERO = new Counts(0, 0, 0, 0);
  }

  /** The API view of one deduplicated violation. */
  public record Violation(
      @JsonProperty String id,
      @JsonProperty String appKey,
      @JsonProperty String entrypoint,
      @JsonProperty String disposition,
      @JsonProperty String effectiveDirective,
      @JsonProperty String blocked,
      @JsonProperty String violationKind,
      @JsonProperty boolean suggestable,
      @JsonProperty String suggestedDirective,
      @JsonProperty String suggestedSource,
      @JsonProperty String remediation,
      @JsonProperty long count,
      @JsonProperty String firstSeen,
      @JsonProperty String lastSeen,
      @JsonProperty String sampleDocumentUri,
      @JsonProperty String sampleSourceFile,
      @JsonProperty Integer sampleLine,
      @JsonProperty Integer sampleColumn,
      @JsonProperty String status) {}

  /**
   * A parsed, redacted violation report.
   *
   * @param appKey validated app key or {@value #PLATFORM_APP_KEY}
   * @param entrypoint validated entrypoint
   * @param disposition {@code enforce} or {@code report}
   * @param effectiveDirective the directive, {@code -elem}/{@code -attr} already folded
   * @param blocked the blocked origin ({@code scheme://host[:port]}) or keyword ({@code inline},
   *     {@code eval}, {@code wasm-eval}, {@code data}, {@code blob}, {@code self})
   * @param documentUri the document URI without query and fragment
   * @param sourceFile the source file without query and fragment, or {@code null}
   * @param line the line number, or {@code null}
   * @param column the column number, or {@code null}
   */
  public record Report(
      String appKey,
      String entrypoint,
      String disposition,
      String effectiveDirective,
      String blocked,
      String documentUri,
      String sourceFile,
      Integer line,
      Integer column) {}

  static final class Entry implements Serializable {
    String id;
    String appKey;
    String entrypoint;
    String disposition;
    String directive;
    String blocked;
    long count;
    long firstSeen;
    long lastSeen;
    String documentUri;
    String sourceFile;
    Integer line;
    Integer column;
  }

  /** The violations of one app; all access goes through its synchronized methods. */
  static final class Bucket implements Serializable {
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

    synchronized void add(String fingerprint, Report report, Instant now) {
      prune(now);
      Entry entry = entries.remove(fingerprint);
      if (entry == null) {
        entry = new Entry();
        entry.id = id(fingerprint);
        entry.appKey = report.appKey();
        entry.entrypoint = report.entrypoint();
        entry.disposition = report.disposition();
        entry.directive = report.effectiveDirective();
        entry.blocked = report.blocked();
        entry.firstSeen = now.toEpochMilli();
      }
      entry.count++;
      entry.lastSeen = now.toEpochMilli();
      entry.documentUri = report.documentUri();
      entry.sourceFile = report.sourceFile();
      entry.line = report.line();
      entry.column = report.column();
      entries.put(fingerprint, entry);
      while (entries.size() > MAX_PER_APP) {
        entries.remove(entries.keySet().iterator().next());
      }
    }

    synchronized List<Entry> snapshot(Instant now) {
      prune(now);
      return new ArrayList<>(entries.values());
    }

    private void prune(Instant now) {
      long cutoff = now.minus(TTL).toEpochMilli();
      entries.values().removeIf(e -> e.lastSeen < cutoff);
    }
  }

  public boolean isReportingEnabled() {
    return config.isEnabled(CSP_ENABLED) && config.isEnabled(CSP_REPORT_ENABLED);
  }

  public void recordReport(Report report) {
    recordReport(report, Instant.now());
  }

  void recordReport(Report report, Instant now) {
    String fingerprint =
        String.join(
            "|",
            report.appKey(),
            report.entrypoint(),
            report.disposition(),
            report.effectiveDirective(),
            report.blocked());
    Bucket bucket = buckets.get(report.appKey(), k -> new Bucket());
    bucket.add(fingerprint, report, now);
    buckets.put(report.appKey(), bucket);
  }

  /** All violations, newest first, optionally of one app only. */
  public List<Violation> list(@CheckForNull String appKey) {
    return list(appKey, Instant.now());
  }

  List<Violation> list(@CheckForNull String appKey, Instant now) {
    List<Entry> entries = new ArrayList<>();
    if (appKey != null) {
      buckets.getIfPresent(appKey).ifPresent(b -> entries.addAll(b.snapshot(now)));
    } else {
      buckets.getAll().forEach(b -> entries.addAll(b.snapshot(now)));
    }
    entries.sort(Comparator.comparingLong((Entry e) -> e.lastSeen).reversed());
    Map<String, CspPolicy> policies = new LinkedHashMap<>();
    List<Violation> result = new ArrayList<>(entries.size());
    for (Entry e : entries) {
      result.add(toView(e, policies));
    }
    return result;
  }

  public List<Violation> recent(String appKey, int limit) {
    List<Violation> all = list(appKey);
    return all.size() <= limit ? all : all.subList(0, limit);
  }

  public Map<String, Counts> counts24h() {
    Instant now = Instant.now();
    Map<String, Counts> result = new LinkedHashMap<>();
    for (String key : buckets.keys()) {
      Optional<Bucket> bucket = buckets.getIfPresent(key);
      if (bucket.isEmpty()) {
        continue;
      }
      long enforce = 0;
      long report = 0;
      List<Entry> entries = bucket.get().snapshot(now);
      for (Entry e : entries) {
        if ("report".equals(e.disposition)) {
          report += e.count;
        } else {
          enforce += e.count;
        }
      }
      result.put(key, new Counts(enforce + report, enforce, report, entries.size()));
    }
    return result;
  }

  public void clear(String appKey) {
    buckets.invalidate(appKey);
  }

  private Violation toView(Entry e, Map<String, CspPolicy> policies) {
    Classification c = classify(e.directive, e.blocked);
    if (PLATFORM_APP_KEY.equals(e.appKey) && c.suggestable()) {
      // platform pages are not apps: nothing to add an override to
      c = new Classification(c.kind(), false, c.directive(), c.source());
    }
    String status = "NEW";
    if (c.suggestable()) {
      CspPolicy policy =
          policies.computeIfAbsent(
              e.appKey + "|" + e.entrypoint,
              k ->
                  appPolicyService.enforcedPolicy(
                      e.appKey, e.entrypoint, appPolicyService.getOverride(e.appKey)));
      if (policy.allows(c.directive(), c.source())) {
        status = "ALLOWED_NOW";
      }
    }
    return new Violation(
        e.id,
        e.appKey,
        e.entrypoint,
        e.disposition,
        e.directive,
        e.blocked,
        c.kind(),
        c.suggestable(),
        c.suggestable() ? c.directive() : null,
        c.suggestable() ? c.source() : null,
        c.remediation(e.appKey),
        e.count,
        Instant.ofEpochMilli(e.firstSeen).toString(),
        Instant.ofEpochMilli(e.lastSeen).toString(),
        e.documentUri,
        e.sourceFile,
        e.line,
        e.column,
        status);
  }

  record Classification(String kind, boolean suggestable, String directive, String source) {
    String remediation(String appKey) {
      return switch (kind) {
        case "EXTERNAL" ->
            suggestable
                ? "Add " + source + " to " + directive + " for " + appKey
                : "The " + directive + " directive cannot be extended for " + source;
        case "INLINE" ->
            "Inline "
                + (directive.startsWith("style") ? "style" : "script or event handler")
                + " blocked: move it into a file served by the app, or allow its hash ('sha256-…')"
                + " in "
                + directive
                + "; 'unsafe-inline' only as a last resort (needs confirmUnsafe)";
        case "EVAL" ->
            "eval()/new Function() blocked: fix the app, or add 'unsafe-eval' to script-src as a"
                + " last resort (needs confirmUnsafe)";
        case "WASM_EVAL" ->
            "WebAssembly compilation blocked: add 'wasm-unsafe-eval' to script-src (needs"
                + " confirmUnsafe)";
        case "DATA", "BLOB" ->
            kind.toLowerCase()
                + ": URL blocked in "
                + directive
                + "; only script-src accepts it as an unsafe source";
        default -> "Not suggestable";
      };
    }
  }

  Classification classify(String directive, String blocked) {
    switch (blocked) {
      case "inline":
        return new Classification("INLINE", false, directive, null);
      case "eval":
        return new Classification("EVAL", false, directive, null);
      case "wasm-eval":
        return new Classification("WASM_EVAL", false, directive, null);
      case "data":
        return new Classification("DATA", false, directive, null);
      case "blob":
        return new Classification("BLOB", false, directive, null);
      case "self":
        return new Classification("SELF", false, directive, null);
      default:
        break;
    }
    if (!blocked.contains("://")) {
      return new Classification("OTHER", false, directive, null);
    }
    String suggestionDirective = "frame-src".equals(directive) ? "frame-src" : directive;
    boolean suggestable =
        OVERRIDABLE_DIRECTIVES.contains(suggestionDirective)
            && CspOverrideValidator.isAcceptableOnRead(
                suggestionDirective, blocked, cspPolicyService.isHttpsEnabled());
    return new Classification("EXTERNAL", suggestable, suggestionDirective, blocked);
  }

  private static String id(String fingerprint) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 8);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
