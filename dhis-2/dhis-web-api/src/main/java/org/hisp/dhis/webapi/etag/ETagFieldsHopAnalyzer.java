/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.etag;

import java.util.List;
import java.util.StringJoiner;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/**
 * Decides whether a {@code fields=} expression stays within the freshness guarantees of the
 * conditional ETag cache.
 *
 * <p>ETag dependency sets cover exactly one reference hop (the schema class plus its direct {@code
 * REFERENCE} properties, see {@code ETagConditionalInterceptor#buildDependencyTypes}). A fields
 * expression that traverses two or more reference hops (for example {@code
 * categoryCombo[categories[name]]} on {@code /api/dataElements}) embeds data whose changes do not
 * rotate the endpoint's ETag; without intervention such responses are served bounded-stale until
 * the TTL window flips.
 *
 * <p>This analyzer walks the parsed field paths against the schema graph and returns {@link
 * Verdict#DEEP} when any path crosses more than one reference hop, or descends into a reference
 * held by an embedded object (that reference is not a direct reference of the root, so its type is
 * outside the dependency set). Filter paths count like field paths: a filter on {@code
 * categoryCombo.categories.name} selects the listed objects by data two hops away. Callers skip
 * ETag caching for DEEP requests, trading a cache hit for guaranteed freshness. Every ambiguity
 * resolves to DEEP (fail-safe): unknown segments, nested presets, unresolvable schemas and parser
 * failures all opt out of caching rather than risking staleness.
 *
 * <p>Verdicts are cached in a bounded cache keyed by root type and raw fields string. The bound
 * matters: the fields parameter is client-controlled, so an unbounded memoization would be a memory
 * DoS vector.
 *
 * @author Morten Svanaes
 */
@Component
public class ETagFieldsHopAnalyzer {

  /** Outcome of analyzing one {@code fields=} expression against a root schema. */
  public enum Verdict {
    /** All paths stay within one reference hop; the ETag dependency set covers them. */
    SHALLOW,
    /** At least one path escapes the dependency set; skip ETag caching for this request. */
    DEEP
  }

  private static final int VERDICT_CACHE_MAX_SIZE = 1024;

  private static final char PRESET_MARKER = ':';
  private static final char EXCLUDE_MARKER_BANG = '!';
  private static final char EXCLUDE_MARKER_DASH = '-';

  private final SchemaService schemaService;

  private final Cache<String, Verdict> verdictCache =
      new Cache2kBuilder<String, Verdict>() {}.entryCapacity(VERDICT_CACHE_MAX_SIZE)
          .eternal(true)
          .build();

  public ETagFieldsHopAnalyzer(SchemaService schemaService) {
    this.schemaService = schemaService;
  }

  /**
   * Analyzes a {@code fields=} expression against the schema of the given root type.
   *
   * @param rootType the entity class the endpoint lists (the resource schema class)
   * @param fields the raw {@code fields=} parameter value, may be {@code null} or blank
   * @return {@link Verdict#SHALLOW} when caching is safe, {@link Verdict#DEEP} when the request
   *     must bypass the ETag cache
   */
  @Nonnull
  public Verdict analyze(@Nonnull Class<?> rootType, @CheckForNull String fields) {
    if (fields == null || fields.isBlank()) {
      return Verdict.SHALLOW;
    }
    String key = rootType.getName() + '|' + fields;
    Verdict cached = verdictCache.peek(key);
    if (cached != null) {
      return cached;
    }
    Verdict verdict = analyzeUncached(rootType, fields);
    verdictCache.put(key, verdict);
    return verdict;
  }

  /**
   * Analyzes the {@code fields=} and {@code filter=} parameters of a request against the schema of
   * the given root type. Every {@code fields} value counts (Spring joins repeated values into one
   * expression), and so does the property path of every {@code filter} value.
   *
   * @param rootType the entity class the endpoint lists (the resource schema class)
   * @param fields the {@code fields=} parameter values, may be {@code null}
   * @param filters the {@code filter=} parameter values, may be {@code null}
   * @return {@link Verdict#SHALLOW} when caching is safe, {@link Verdict#DEEP} when the request
   *     must bypass the ETag cache
   */
  @Nonnull
  public Verdict analyze(
      @Nonnull Class<?> rootType, @CheckForNull String[] fields, @CheckForNull String[] filters) {
    StringJoiner expression = new StringJoiner(",");
    if (fields != null) {
      for (String value : fields) {
        if (value != null && !value.isBlank()) {
          expression.add(value);
        }
      }
    }
    if (filters != null) {
      for (String filter : filters) {
        if (filter != null && !filter.isBlank()) {
          expression.add(filterPathAsFieldsPath(filter));
        }
      }
    }
    return analyze(rootType, expression.toString());
  }

  /**
   * The property path of a filter as a fields path: {@code categoryCombo.categories.name:eq:x}
   * becomes {@code categoryCombo[categories[name]]}.
   */
  static String filterPathAsFieldsPath(String filter) {
    int operatorStart = filter.indexOf(':');
    String path = (operatorStart < 0 ? filter : filter.substring(0, operatorStart)).strip();
    String[] segments = path.split("\\.", -1);
    StringBuilder fieldsPath = new StringBuilder(segments[0]);
    for (int i = 1; i < segments.length; i++) {
      fieldsPath.append('[').append(segments[i]);
    }
    return fieldsPath.append("]".repeat(segments.length - 1)).toString();
  }

  private Verdict analyzeUncached(Class<?> rootType, String fields) {
    Schema root = schemaService.getSchema(rootType);
    if (root == null) {
      return Verdict.DEEP;
    }
    List<FieldPath> paths;
    try {
      paths = FieldFilterParser.parse(fields);
    } catch (RuntimeException ex) {
      return Verdict.DEEP;
    }
    for (FieldPath path : paths) {
      if (isDeep(root, path)) {
        return Verdict.DEEP;
      }
    }
    return Verdict.SHALLOW;
  }

  /**
   * Walks one path root-to-leaf, counting reference hops. Returns {@code true} when the path
   * escapes the one-hop dependency set or cannot be fully resolved.
   */
  private boolean isDeep(Schema root, FieldPath fieldPath) {
    if (fieldPath.isExclude()) {
      return false; // exclusions remove fields, they never add data
    }

    List<String> segments = fieldPath.getPath().segments().map(CharSequence::toString).toList();

    Schema current = root;
    int hops = 0;
    boolean belowEmbedded = false;
    for (int i = 0; i < segments.size(); i++) {
      String segment = stripExcludeMarker(segments.get(i));
      if (isPreset(segment)) {
        if (i > 0) {
          return true; // nested preset (userGroups[*]): expansion depth unknown, fail safe
        }
        continue; // root-level preset renders references as id-stubs, 1-hop data
      }
      Property property = resolveProperty(current, segment);
      if (property == null) {
        return true; // empty or unknown segment (includes attribute UIDs), fail safe
      }
      boolean descends = i < segments.size() - 1;
      if (isReference(property)) {
        hops++;
        if (hops >= 2) {
          return true;
        }
        if (descends && belowEmbedded && !root.getReferences().contains(targetType(property))) {
          // a reference held by an embedded object is not a direct reference of the root: its
          // fields are outside the dependency set (a leaf id-stub is stored with the root)
          return true;
        }
        current = schemaFor(property);
      } else if (descends) {
        // complex/embedded property with nested segments: same-row data, no hop
        belowEmbedded = true;
        current = schemaFor(property);
      }
    }
    return false;
  }

  private static boolean isReference(Property property) {
    return property.isCollection()
        ? property.getItemPropertyType() == PropertyType.REFERENCE
        : property.getPropertyType() == PropertyType.REFERENCE;
  }

  private static boolean isPreset(String segment) {
    return !segment.isEmpty() && segment.charAt(0) == PRESET_MARKER;
  }

  /**
   * Returns the property named by the segment, or null when the segment is empty, the previous
   * segment's type has no schema, or the schema has no such property.
   */
  @CheckForNull
  private static Property resolveProperty(@CheckForNull Schema current, String segment) {
    if (segment.isEmpty() || current == null) {
      return null;
    }
    return current.getProperty(segment);
  }

  @CheckForNull
  private Schema schemaFor(Property property) {
    Class<?> target = targetType(property);
    return target == null ? null : schemaService.getSchema(target);
  }

  @CheckForNull
  private static Class<?> targetType(Property property) {
    return property.isCollection() ? property.getItemKlass() : property.getKlass();
  }

  private static String stripExcludeMarker(String segment) {
    if (!segment.isEmpty()
        && (segment.charAt(0) == EXCLUDE_MARKER_BANG || segment.charAt(0) == EXCLUDE_MARKER_DASH)) {
      return segment.substring(1);
    }
    return segment;
  }
}
