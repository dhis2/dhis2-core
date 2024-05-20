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

import static java.util.Arrays.stream;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OpenApi;

/**
 * Utility with helpers to evaluate properties based on {@link org.hisp.dhis.common.OpenApi}
 * annotations.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class OpenApiAnnotations {

  @CheckForNull
  static Class<?> getEntityType(@Nonnull Class<?> controller) {
    Class<?> entityClass =
        controller.isAnnotationPresent(OpenApi.EntityType.class)
            ? controller.getAnnotation(OpenApi.EntityType.class).value()
            : null;
    if (entityClass != null && entityClass != OpenApi.EntityType.class) return entityClass;
    Type abstractController = controller.getGenericSuperclass();
    if (abstractController instanceof ParameterizedType pt)
      return (Class<?>) pt.getActualTypeArguments()[0];
    return null;
  }

  @Nonnull
  static Class<?> getEntityType(@Nonnull Method endpoint, @Nonnull Class<?> controller) {
    return getAnnotated(
        endpoint,
        OpenApi.EntityType.class,
        OpenApi.EntityType::value,
        c -> c != OpenApi.EntityType.class,
        () -> getEntityType(controller));
  }

  /*
  Generic Helpers
   */

  static <A extends Annotation> Stream<A> getAnnotations(Method on, Class<A> type) {
    return stream(ConsistentAnnotatedElement.of(on).getAnnotationsByType(type));
  }

  static <A extends Annotation, T extends AnnotatedElement> void whenAnnotated(
      T on, Class<A> type, Consumer<A> whenPresent) {
    AnnotatedElement target = ConsistentAnnotatedElement.of(on);
    if (target.isAnnotationPresent(type)) {
      whenPresent.accept(target.getAnnotation(type));
    }
  }

  static <A extends Annotation, B, T extends AnnotatedElement> B getAnnotated(
      T on, Class<A> type, Function<A, B> whenPresent, Predicate<B> test, Supplier<B> otherwise) {
    AnnotatedElement target = ConsistentAnnotatedElement.of(on);
    if (!target.isAnnotationPresent(type)) {
      return otherwise.get();
    }
    B value = whenPresent.apply(target.getAnnotation(type));
    return test.test(value) ? value : otherwise.get();
  }
}
