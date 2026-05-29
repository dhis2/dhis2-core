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
package org.hisp.dhis.test.junit;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * Installs a fixed {@link Clock} for tests annotated with {@link TestClock}, so a test never reads
 * the wall clock and therefore never fails at midnight, on year-end, on leap days or across
 * timezones.
 *
 * <p>The clock is built from the {@link TestClock} annotation on the test class as {@code
 * Clock.fixed(Instant.parse(instant), ZoneId.of(zone))}. It is made available to a test via either
 * of:
 *
 * <ul>
 *   <li>a {@link Clock} field on the test instance &ndash; assigned before each test (see {@link
 *       #beforeEach(ExtensionContext)});
 *   <li>a {@link Clock} parameter on a test or lifecycle method &ndash; resolved (see {@link
 *       #resolveParameter(ParameterContext, ExtensionContext)}).
 * </ul>
 *
 * <p>The resolved clock is intended to be passed into the code under test. This extension covers
 * the "time is an input to the unit under test" cases (buckets A/B). Components that stamp the time
 * themselves server-side (bucket C) additionally need a {@link Clock} injected into the production
 * bean; wiring the fixed clock into the Spring {@code ApplicationContext} is tracked as a separate
 * follow-up and is intentionally not done here.
 *
 * @see TestClock
 */
public class FixedClockExtension implements BeforeEachCallback, ParameterResolver {

  private static final Namespace NAMESPACE = Namespace.create(FixedClockExtension.class);
  private static final String CLOCK_KEY = "clock";

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Clock clock = getClock(context);

    Object testInstance = context.getRequiredTestInstance();
    List<Field> fields =
        ReflectionSupport.findFields(
            testInstance.getClass(),
            field -> field.getType() == Clock.class,
            HierarchyTraversalMode.TOP_DOWN);
    for (Field field : fields) {
      field.setAccessible(true);
      field.set(testInstance, clock);
    }
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == Clock.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getClock(extensionContext);
  }

  /**
   * Returns the fixed clock for this test, building it once per test class from the {@link
   * TestClock} annotation and caching it in the extension store.
   */
  private static Clock getClock(ExtensionContext context) {
    return context
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent(CLOCK_KEY, key -> buildClock(context), Clock.class);
  }

  private static Clock buildClock(ExtensionContext context) {
    TestClock testClock =
        findAnnotation(context.getRequiredTestClass(), TestClock.class)
            .orElseThrow(
                () ->
                    new ParameterResolutionException(
                        "FixedClockExtension is active but @TestClock is missing on "
                            + context.getRequiredTestClass().getName()));
    return Clock.fixed(Instant.parse(testClock.instant()), ZoneId.of(testClock.zone()));
  }
}
