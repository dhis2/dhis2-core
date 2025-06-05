/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * A {@link Pointcut} implementation that filters on marker annotations. One for the {@link
 * #classLevel} and one for the {@link #methodLevel}.
 *
 * <p>The reason we cannot use existing implementation that basically do this is mainly that this
 * seems to always end up requiring to switch to AspectJ AOP but also that the existing
 * implementations do not consider interface-implementation situations where the annotation is on
 * the implementation class. This is surprising since that is the standard way of doing it.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
public class StaticAnnotationPointcut implements Pointcut, ClassFilter, MethodMatcher {

  private final Class<? extends Annotation> classLevel;
  private final Class<? extends Annotation> methodLevel;

  @Nonnull
  @Override
  public ClassFilter getClassFilter() {
    return this;
  }

  @Nonnull
  @Override
  public MethodMatcher getMethodMatcher() {
    return this;
  }

  @Override
  public boolean matches(Class<?> cls) {
    if (!cls.isAnnotationPresent(classLevel)) return false;
    return Stream.of(cls.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(methodLevel));
  }

  @Override
  public boolean matches(Method method, @Nonnull Class<?> targetClass) {
    if (method.getDeclaringClass() == targetClass) return method.isAnnotationPresent(methodLevel);
    try {
      method = targetClass.getMethod(method.getName(), method.getParameterTypes());
      return method.isAnnotationPresent(methodLevel);
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @Override
  public boolean isRuntime() {
    return false;
  }

  @Override
  public boolean matches(
      @Nonnull Method method, @Nonnull Class<?> targetClass, @Nonnull Object... args) {
    return matches(method, targetClass);
  }
}
