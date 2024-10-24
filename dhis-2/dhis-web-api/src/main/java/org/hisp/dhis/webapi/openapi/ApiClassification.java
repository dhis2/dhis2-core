/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.openapi;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * The aggregated classifiers map for a set of given controller {@link Class}es.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ApiClassification {

  /**
   * All controllers considered in the classification. This is the basis of the analysis and
   * therefore also the key or identity of a classification.
   */
  @EqualsAndHashCode.Include private final Set<Class<?>> controllers;

  @Getter private final Map<String, Set<String>> classifiers;

  private static final Map<Set<Class<?>>, ApiClassification> CACHE = new ConcurrentHashMap<>();

  static ApiClassification of(Set<Class<?>> controllers) {
    return CACHE.computeIfAbsent(controllers, ApiClassification::create);
  }

  private static ApiClassification create(Set<Class<?>> controllers) {
    TreeMap<String, Set<String>> classifiers = new TreeMap<>();
    controllers.forEach(
        c ->
            OpenApiAnnotations.getClassifiers(c)
                .forEach(
                    (classifier, value) ->
                        classifiers.computeIfAbsent(classifier, k -> new TreeSet<>()).add(value)));
    LinkedHashMap<String, Set<String>> sortedBySize =
        classifiers.entrySet().stream()
            .sorted(comparingInt(e -> e.getValue().size()))
            .collect(
                toMap(
                    Map.Entry::getKey,
                    e -> unmodifiableSet(e.getValue()),
                    (x, y) -> y,
                    LinkedHashMap::new));
    return new ApiClassification(controllers, unmodifiableMap(sortedBySize));
  }
}
