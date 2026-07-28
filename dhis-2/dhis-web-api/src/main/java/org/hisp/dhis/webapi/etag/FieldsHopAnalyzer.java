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
 * REFERENCE} properties, see {@code ConditionalETagInterceptor#buildDependencyTypes}). A fields
 * expression that traverses two or more reference hops (for example {@code
 * categoryCombo[categories[name]]} on {@code /api/dataElements}) embeds data whose changes do not
 * rotate the endpoint's ETag; without intervention such responses are served bounded-stale until
 * the TTL window flips.
 *
 * <p>This analyzer walks the parsed field paths against the schema graph and returns {@link
 * Verdict#DEEP} when any path crosses more than one reference hop. Callers skip ETag caching for
 * DEEP requests, trading a cache hit for guaranteed freshness. Every ambiguity resolves to DEEP
 * (fail-safe): unknown segments, nested presets, unresolvable schemas and parser failures all opt
 * out of caching rather than risking staleness.
 *
 * <p>Verdicts are cached in a bounded cache keyed by root type and raw fields string. The bound
 * matters: the fields parameter is client-controlled, so an unbounded memoization would be a memory
 * DoS vector.
 *
 * @author Morten Svanaes
 */
@Component
public class FieldsHopAnalyzer {

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

  public FieldsHopAnalyzer(SchemaService schemaService) {
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
    for (int i = 0; i < segments.size(); i++) {
      String segment = stripExcludeMarker(segments.get(i));
      if (segment.isEmpty()) {
        return true;
      }
      if (segment.charAt(0) == PRESET_MARKER) {
        if (i > 0) {
          return true; // nested preset (userGroups[*]): expansion depth unknown, fail safe
        }
        continue; // root-level preset renders references as id-stubs, 1-hop data
      }
      if (current == null) {
        return true; // previous segment's type has no schema, cannot reason further
      }
      Property property = current.getProperty(segment);
      if (property == null) {
        return true; // unknown segment (includes attribute UIDs), fail safe
      }
      if (isReference(property)) {
        hops++;
        if (hops >= 2) {
          return true;
        }
        current = schemaFor(property);
      } else if (i < segments.size() - 1) {
        // complex/embedded property with nested segments: same-row data, no hop
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

  @CheckForNull
  private Schema schemaFor(Property property) {
    Class<?> target = property.isCollection() ? property.getItemKlass() : property.getKlass();
    return target == null ? null : schemaService.getSchema(target);
  }

  private static String stripExcludeMarker(String segment) {
    if (!segment.isEmpty()
        && (segment.charAt(0) == EXCLUDE_MARKER_BANG || segment.charAt(0) == EXCLUDE_MARKER_DASH)) {
      return segment.substring(1);
    }
    return segment;
  }
}
