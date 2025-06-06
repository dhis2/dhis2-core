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
package org.hisp.dhis.log;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hisp.dhis.common.IdentifiableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interceptor that allows to annotate (service) bean methods with {@link TimeExecution} to have
 * their execution time logged to the annotated classes logger.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
public class TimeExecutionInterceptor implements MethodInterceptor {

  /**
   * Remembers how many components of the record should be included in the log so that it is fast to
   * decide if further reflection is needed to build the log line information.
   */
  private static final Map<Class<? extends Record>, Integer> INCLUDES_COMPONENTS_CACHE =
      new ConcurrentHashMap<>();

  /**
   * The issue is that the invocation will use the interface method but the annotation is on the
   * implementation. This cache is to minimize the overhead of the method and annotation lookup.
   */
  private static final Map<Method, TimeExecution> INFO_CACHE = new ConcurrentHashMap<>();

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object target = invocation.getThis();
    if (target == null) return invocation.proceed();
    Class<?> targetClass = target.getClass();
    Method method = invocation.getMethod();
    TimeExecution timeExecution =
        INFO_CACHE.computeIfAbsent(method, m -> getAnnotation(m, targetClass));
    if (timeExecution == null) return invocation.proceed();
    Class<?> loggerClass =
        timeExecution.logger() == Class.class ? targetClass : timeExecution.logger();
    Logger logger = LoggerFactory.getLogger(loggerClass);
    System.Logger.Level level = timeExecution.level();
    // fast-forward if log level is disabled
    if (!switch (level) {
      case TRACE -> logger.isTraceEnabled();
      case DEBUG -> logger.isDebugEnabled();
      case INFO -> logger.isInfoEnabled();
      case WARNING -> logger.isWarnEnabled();
      case ERROR -> logger.isErrorEnabled();
      default -> false;
    }) return invocation.proceed();

    String name = timeExecution.name();
    if (name.isEmpty()) name = method.getName();

    Object[] args = invocation.getArguments();
    List<String> values = List.of();
    if (args.length > 0 && args[0] instanceof Record r) values = extractParameterValues(r);
    String withArgs = values.isEmpty() ? "" : " args: " + String.join(", ", values);

    long maxTime = timeExecution.threshold();

    long startTime = System.nanoTime();
    if (maxTime <= 0) {
      String template = "Starting {}{}";
      switch (level) {
        case TRACE -> logger.trace(template, name, withArgs);
        case DEBUG -> logger.debug(template, name, withArgs);
        case INFO -> logger.info(template, name, withArgs);
        case WARNING -> logger.warn(template, name, withArgs);
        case ERROR -> logger.error(template, name, withArgs);
        default -> {} // nothing for ALL or OFF
      }
      withArgs = "";
    }

    Object result = invocation.proceed();

    long duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
    if (maxTime <= 0 || duration > maxTime) {
      String template = "Completed {} in {} ms{}";
      switch (level) {
        case TRACE -> logger.trace(template, name, duration, withArgs);
        case DEBUG -> logger.debug(template, name, duration, withArgs);
        case INFO -> logger.info(template, name, duration, withArgs);
        case WARNING -> logger.warn(template, name, duration, withArgs);
        case ERROR -> logger.error(template, name, duration, withArgs);
        default -> {} // nothing for ALL or OFF
      }
    }
    return result;
  }

  private static TimeExecution getAnnotation(Method method, Class<?> targetClass) {
    TimeExecution res = method.getAnnotation(TimeExecution.class);
    if (res != null) return res;
    Method m = null;
    try {
      m = targetClass.getMethod(method.getName(), method.getParameterTypes());
    } catch (NoSuchMethodException e) {
      return null;
    }
    return m.getAnnotation(TimeExecution.class);
  }

  /** Extracts values of components annotated with @Loggable from a record. */
  private static List<String> extractParameterValues(Record arg) {
    int c = numberOfIncludedComponents(arg.getClass());
    if (c == 0) return List.of();
    List<String> values = new ArrayList<>(c);
    for (RecordComponent component : arg.getClass().getRecordComponents()) {
      if (component.isAnnotationPresent(TimeExecution.Include.class)) {
        try {
          Method accessor = component.getAccessor();
          Object value = toArgumentInfo(accessor.invoke(arg));
          values.add(component.getName() + "=" + value);
        } catch (Exception ex) {
          values.add(component.getName() + "=" + "<%s>".formatted(ex.getClass().getSimpleName()));
        }
      }
    }
    return values;
  }

  private static Object toArgumentInfo(Object val) {
    if (val == null) return null;
    if (val instanceof String) return val;
    if (val instanceof Number) return val;
    if (val instanceof Boolean) return val;
    if (val instanceof Character) return val;
    if (val instanceof Enum<?>) return val;
    if (val instanceof Date) return val;
    if (val instanceof Collection<?> c) return c.size();
    if (val instanceof Map<?, ?> m) return m.size();
    if (val instanceof IdentifiableObject obj) return obj.getUid();
    return "<%s>"
        .formatted(
            val.getClass()
                .getSimpleName()); // value is shortened (omitted) as we don't know if it is too
    // verbose
  }

  private static int numberOfIncludedComponents(Class<? extends Record> type) {
    return INCLUDES_COMPONENTS_CACHE.computeIfAbsent(
        type,
        t ->
            (int)
                Stream.of(t.getRecordComponents())
                    .filter(c -> c.isAnnotationPresent(TimeExecution.Include.class))
                    .count());
  }
}
