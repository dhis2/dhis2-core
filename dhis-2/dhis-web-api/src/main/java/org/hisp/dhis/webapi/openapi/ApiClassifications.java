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
package org.hisp.dhis.webapi.openapi;

import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.OpenApi;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * The aggregated classifiers map for a set of given controller {@link Class}es.
 *
 * @author Jan Bernitt
 * @since 2.42
 * @param controllers All controllers considered in the classification. This is the basis of the
 *     analysis and therefore also the key or identity of a classification.
 * @param classifiers All possible values for each type of classifier
 */
public record ApiClassifications(
    Set<Class<?>> controllers, Map<String, List<Classifier>> classifiers) {

  public record Classifier(String value, int percentage) {}

  private static final Map<Set<Class<?>>, ApiClassifications> CACHE = new ConcurrentHashMap<>();

  static ApiClassifications of(Set<Class<?>> controllers) {
    return CACHE.computeIfAbsent(controllers, ApiClassifications::ofInternal);
  }

  private static ApiClassifications ofInternal(Set<Class<?>> controllers) {
    Map<String, Set<String>> classifiers = collectAllClassifiers(controllers);
    return new ApiClassifications(controllers, sortedByValueCount(controllers, classifiers));
  }

  @Nonnull
  private static Map<String, Set<String>> collectAllClassifiers(Set<Class<?>> controllers) {
    Map<String, Set<String>> classifiers = new TreeMap<>();
    controllers.forEach(
        c ->
            OpenApiAnnotations.getClassifiers(c)
                .forEach(
                    (classifier, value) ->
                        classifiers.computeIfAbsent(classifier, k -> new TreeSet<>()).add(value)));
    return classifiers;
  }

  @Nonnull
  private static Map<String, List<Classifier>> sortedByValueCount(
      @Nonnull Set<Class<?>> controllers, Map<String, Set<String>> classifiers) {
    return unmodifiableMap(
        classifiers.entrySet().stream()
            .sorted(comparingInt(e -> e.getValue().size()))
            .collect(
                toMap(
                    Map.Entry::getKey,
                    e ->
                        e.getValue().stream()
                            .map(v -> toClassifier(e.getKey(), v, controllers))
                            .toList(),
                    (x, y) -> y,
                    LinkedHashMap::new)));
  }

  private static Classifier toClassifier(
      String key, String value, @Nonnull Set<Class<?>> controllers) {
    int c = matchesCount(controllers, Map.of(key, Set.of(value)));
    return new Classifier(value, 100 * c / controllers.size());
  }

  static int matchesCount(
      @Nonnull Set<Class<?>> controllers, @Nonnull Map<String, Set<String>> filters) {
    return (int) controllers.stream().filter(c -> matches(filters, c)).count();
  }

  static Set<Class<?>> matches(
      @Nonnull Set<Class<?>> controllers, @Nonnull Map<String, Set<String>> filters) {
    return controllers.stream()
        .filter(c -> matches(filters, c))
        .collect(toCollection((() -> new TreeSet<>(comparing(Class::getName)))));
  }

  private static boolean matches(@Nonnull Map<String, Set<String>> filters, Class<?> controller) {
    if (!isControllerType(controller)) return false;
    if (filters.isEmpty()) return true;
    Map<String, String> present = OpenApiAnnotations.getClassifiers(controller);
    for (Map.Entry<String, Set<String>> filter : filters.entrySet()) {
      String value = present.get(filter.getKey());
      if (value != null && filter.getValue().contains(value)) return true;
    }
    return false;
  }

  private static boolean isControllerType(Class<?> source) {
    return (source.isAnnotationPresent(RestController.class)
            || source.isAnnotationPresent(Controller.class))
        && !source.isAnnotationPresent(OpenApi.Ignore.class);
  }
}
