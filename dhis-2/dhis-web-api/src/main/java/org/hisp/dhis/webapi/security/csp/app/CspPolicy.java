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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * A mutable, ordered CSP policy body (directive name to source list) used to compose per-app
 * policies. Values are never parsed from untrusted input here: callers add only validated sources.
 *
 * @author Morten Svanæs
 */
public final class CspPolicy {
  /** Fallback chains per CSP Level 3, used to seed a directive that is not declared yet. */
  private static final Map<String, List<String>> FALLBACKS =
      Map.of(
          "frame-src", List.of("child-src", "default-src"),
          "worker-src", List.of("child-src", "script-src", "default-src"),
          "form-action", List.of());

  private final Map<String, Set<String>> directives = new LinkedHashMap<>();

  private CspPolicy() {}

  public static CspPolicy parse(String policy) {
    CspPolicy result = new CspPolicy();
    for (String part : policy.split(";")) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String[] tokens = trimmed.split("\\s+");
      Set<String> values = result.directives.computeIfAbsent(tokens[0], k -> new LinkedHashSet<>());
      for (int i = 1; i < tokens.length; i++) {
        values.add(tokens[i]);
      }
    }
    return result;
  }

  public CspPolicy copy() {
    CspPolicy copy = new CspPolicy();
    directives.forEach((k, v) -> copy.directives.put(k, new LinkedHashSet<>(v)));
    return copy;
  }

  @CheckForNull
  public Set<String> get(String directive) {
    return directives.get(directive);
  }

  public boolean allows(String directive, String source) {
    Set<String> values = resolve(directive);
    return values != null && values.contains(source);
  }

  /**
   * Adds sources to a directive. A directive that is not declared yet is first seeded with the
   * sources of its fallback directive so adding a source never takes away what the fallback
   * allowed.
   */
  public void add(String directive, Collection<String> sources) {
    if (sources.isEmpty()) {
      return;
    }
    Set<String> values = directives.get(directive);
    if (values == null) {
      values = new LinkedHashSet<>();
      Set<String> fallback = resolveFallback(directive);
      if (fallback != null) {
        values.addAll(fallback);
      }
      directives.put(directive, values);
    }
    values.addAll(sources);
    if (values.size() > 1) {
      values.remove("'none'");
    }
  }

  public void remove(String directive, String source) {
    Set<String> values = directives.get(directive);
    if (values != null) {
      values.remove(source);
    }
  }

  @CheckForNull
  private Set<String> resolve(String directive) {
    Set<String> values = directives.get(directive);
    return values != null ? values : resolveFallback(directive);
  }

  @CheckForNull
  private Set<String> resolveFallback(String directive) {
    for (String fallback : FALLBACKS.getOrDefault(directive, List.of("default-src"))) {
      Set<String> values = directives.get(fallback);
      if (values != null) {
        return values;
      }
    }
    return null;
  }

  /** Serializes the policy body, each directive terminated by {@code ;}. */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    directives.forEach(
        (name, values) -> {
          if (!sb.isEmpty()) {
            sb.append(' ');
          }
          sb.append(name);
          values.forEach(v -> sb.append(' ').append(v));
          sb.append(';');
        });
    return sb.toString();
  }
}
