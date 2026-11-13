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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;

/**
 * The stored CSP override document of one app (schema version 1), kept as JSON by {@link
 * CspAppOverrideStore}. Server-managed fields ({@code updatedAt}, {@code updatedBy}, the unsafe and
 * exempt flags) are computed on save.
 *
 * @author Morten Svanæs
 */
public record CspAppOverride(
    @JsonProperty int schemaVersion,
    @JsonProperty String appKey,
    @JsonProperty boolean enabled,
    @JsonProperty String updatedAt,
    @JsonProperty String updatedBy,
    @JsonProperty String approvedManifestCspHash,
    @JsonProperty String approvalNote,
    @JsonProperty boolean hasUnsafeDirectives,
    @JsonProperty boolean hasExemptEntrypoint,
    @JsonProperty List<String> unsafeTokens,
    @JsonProperty Map<String, EntrypointOverride> entrypoints) {

  public static final int SCHEMA_VERSION = 1;

  public Map<String, EntrypointOverride> entrypointsOrEmpty() {
    return entrypoints == null ? Map.of() : entrypoints;
  }

  @CheckForNull
  public EntrypointOverride entrypoint(String key) {
    return entrypointsOrEmpty().get(key);
  }

  /** The override of one entrypoint ({@code "*"}, {@code app}, {@code plugin}, {@code worker}). */
  public record EntrypointOverride(
      @JsonProperty CspEnforcementMode enforcementMode,
      @JsonProperty SourceChanges enforced,
      @JsonProperty Draft reportOnlyDraft) {

    public CspEnforcementMode modeOrDefault() {
      return enforcementMode == null ? CspEnforcementMode.ENFORCE : enforcementMode;
    }

    public SourceChanges enforcedOrEmpty() {
      return enforced == null ? SourceChanges.EMPTY : enforced;
    }

    public Draft draftOrEmpty() {
      return reportOnlyDraft == null ? Draft.EMPTY : reportOnlyDraft;
    }
  }

  /**
   * Sources added per directive, and base-override sources ({@code "<directive> <source>"}) removed
   * for this entrypoint.
   */
  public record SourceChanges(
      @JsonProperty Map<String, List<String>> add, @JsonProperty List<String> disabledBaseSources) {
    public static final SourceChanges EMPTY = new SourceChanges(Map.of(), List.of());

    public Map<String, List<String>> addOrEmpty() {
      return add == null ? Map.of() : add;
    }

    public List<String> disabledOrEmpty() {
      return disabledBaseSources == null ? List.of() : disabledBaseSources;
    }
  }

  /**
   * A report-only candidate. While {@code enabled}, the candidate is sent as {@code
   * Content-Security-Policy-Report-Only}; while {@code expiresAt} is in the future its additions
   * are also applied to the enforced policy ("test draft").
   */
  public record Draft(
      @JsonProperty boolean enabled,
      @JsonProperty String expiresAt,
      @JsonProperty Map<String, List<String>> add,
      @JsonProperty List<String> disabledBaseSources) {
    public static final Draft EMPTY = new Draft(false, null, Map.of(), List.of());

    public Map<String, List<String>> addOrEmpty() {
      return add == null ? Map.of() : add;
    }

    public List<String> disabledOrEmpty() {
      return disabledBaseSources == null ? List.of() : disabledBaseSources;
    }
  }

  /** Request body of {@code PUT} and {@code POST validate}. */
  public record Request(
      @JsonProperty Boolean enabled,
      @JsonProperty String approvalNote,
      @JsonProperty boolean confirmUnsafe,
      @JsonProperty boolean confirmExempt,
      @JsonProperty Map<String, EntrypointOverride> entrypoints) {

    public Map<String, EntrypointOverride> entrypointsOrEmpty() {
      return entrypoints == null ? new LinkedHashMap<>() : entrypoints;
    }
  }
}
