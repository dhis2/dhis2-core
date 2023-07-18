/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.cache;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to efficiently as possible estimate the memory usage of an object similar to the {@code
 * sizeof} operator in C.
 *
 * @author Jan Bernitt
 */
@Slf4j
public final class GenericSizeof implements Sizeof {

  /** Size of any primitive except long and double. */
  private static final Sizeof WORD = Sizeof.constant(4);

  /** Size of long and double */
  private static final Sizeof DOUBLE_WORD = Sizeof.constant(8);

  /** Result that means size in not fixed */
  private static final long NOT_FIXED = -1L;

  /** Marker value for fixed size computation */
  private static final long FIXED_SIZE_RECURSIVE_MARKER = -2L;

  private static final Sizeof SIZE_RECURSIVE_MARKER = obj -> 0L;

  /**
   * The number of bytes we assume each {@link Object} requires for JVM object headers which are not
   * visible to reflection.
   */
  private final long objectHeaderSize;

  private final UnaryOperator<Object> unwrap;

  /**
   * A cache to remember the SizeofOperator that can compute the size of an object of the key type.
   */
  private final Map<Type, Sizeof> sizeofByType = new ConcurrentHashMap<>();

  /**
   * A cache to remember the constant size values of the key type have. If a type does not have a
   * constant size its value is {@code <= 0}.
   */
  private final Map<Class<?>, Long> fixedSizeOfType = new ConcurrentHashMap<>();

  public GenericSizeof(long objectHeaderSize, UnaryOperator<Object> unwrap) {
    this.objectHeaderSize = objectHeaderSize;
    this.unwrap = unwrap;
    fixedSizeOfType.put(Object.class, -1L);
    fixedSizeOfType.put(Class.class, 0L);
    fixedSizeOfType.put(Constructor.class, 0L);
    fixedSizeOfType.put(Method.class, 0L);
    fixedSizeOfType.put(Field.class, 0L);
    fixedSizeOfType.put(Pattern.class, 0L);
    sizeofByType.put(String.class, str -> 52L + ((String) str).length());
  }

  @Override
  public long sizeof(Object obj) {
    if (obj == null) {
      return 0L;
    }
    Class<?> type = unwrap.apply(obj).getClass();
    Sizeof sizeof = getSizeof(type);
    if (sizeof == this) {
      log.warn("sizeof: resolution of " + type + " has cycles.");
      return objectHeaderSize; // give ip;
    }
    return sizeof.sizeof(obj);
  }

  public boolean isFixedSize(Class<?> type) {
    Long fixedSize = fixedSizeOfType.get(type);
    return fixedSize != null && fixedSize >= 0L;
  }

  private Sizeof getSizeof(Class<?> type) {
    return getSizeof(type, type);
  }

  private Sizeof getSizeof(Class<?> type, Type genericType) {
    Sizeof sizeof = sizeofByType.get(type);
    if (sizeof != null) {
      if (sizeof == SIZE_RECURSIVE_MARKER) {
        return Sizeof.constant(objectHeaderSize);
      }
      return sizeof;
    }
    try {
      // OBS! we cannot use computeIfAbsent because this could cause
      // recursive
      // updates where computation of the value includes putting the key
      sizeofByType.putIfAbsent(genericType, SIZE_RECURSIVE_MARKER);
      Sizeof newSizeof = computeSizeof(type, genericType);
      return sizeofByType.compute(
          genericType,
          (key, value) -> value == null || value == SIZE_RECURSIVE_MARKER ? newSizeof : value);
    } catch (Exception | StackOverflowError t) {
      log.error("sizeof: Failed to compute function for " + type + ": ", t);
      return Sizeof.constant(objectHeaderSize); // give up
    }
  }

  private Sizeof getSizeof(Type keyType) {
    return keyType instanceof Class
        ? getSizeof((Class<?>) keyType, keyType)
        : getSizeof((Class<?>) ((ParameterizedType) keyType).getRawType(), keyType);
  }

  private Sizeof computeSizeof(Class<?> type, Type genericType) {
    if (type.isPrimitive()) {
      return isDoubleWord(type) ? DOUBLE_WORD : WORD;
    }
    if (type.isArray()) {
      return computeArraySizeof(type);
    }
    long fixedSize = getFixedSize(type);
    if (fixedSize >= 0) {
      return Sizeof.constant(fixedSize);
    }
    if (Collection.class.isAssignableFrom(type)) {
      return computeCollectionSizeof(genericType);
    }
    if (Map.class.isAssignableFrom(type)) {
      return computeMapSizeof(genericType);
    }
    if (type == Object.class || isNotStaticallyDeterminedInSize(type)) {
      // we have to find out dynamically when we have the actual object
      return this;
    }
    return computeFieldBasedSize(type);
  }

  private Sizeof computeArraySizeof(Class<?> type) {
    Class<?> elementType = type.getComponentType();
    if (elementType.isPrimitive()) {
      return Sizeof.arrayOfPrimitive(objectHeaderSize);
    }
    long elementSize = getFixedSize(elementType);
    if (elementSize >= 0) {
      return Sizeof.arrayOfFixed(objectHeaderSize, elementSize);
    }
    return Sizeof.arrayOfDynamic(objectHeaderSize, getSizeof(elementType));
  }

  private Sizeof computeCollectionSizeof(Type collectionType) {
    Type elementType = getTypeParameter(0, 1, collectionType);
    if (elementType instanceof Class) {
      long elementSize = getFixedSize((Class<?>) elementType);
      if (elementSize >= 0L) {
        return Sizeof.collectionOfFixed(objectHeaderSize, elementSize);
      }
    }
    return Sizeof.collectionOfDynamic(objectHeaderSize, getSizeof(elementType));
  }

  private Sizeof computeMapSizeof(Type mapType) {
    return Sizeof.mapOfDynamic(
        objectHeaderSize,
        getSizeof(getTypeParameter(0, 2, mapType)),
        getSizeof(getTypeParameter(1, 2, mapType)));
  }

  private Type getTypeParameter(int index, int expectedTypeArguments, Type genericType) {
    if (genericType instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) genericType;
      Type[] typeArguments = type.getActualTypeArguments();
      return typeArguments.length != expectedTypeArguments ? Object.class : typeArguments[index];
    }
    return Object.class; // we give up => dynamic analysis based on object
  }

  private boolean isDoubleWord(Class<?> type) {
    return type == long.class || type == double.class;
  }

  private Sizeof computeFieldBasedSize(Class<?> type) {
    long fixedSizeSum = objectHeaderSize;
    List<Sizeof> parts = new ArrayList<>();
    Class<?> t = type;
    while (t != null) {
      for (Field field : t.getDeclaredFields()) {
        if (isInstanceField(field)) {
          long fixedSize = getFixedSize(field.getType());
          if (fixedSize >= 0) {
            fixedSizeSum += fixedSize;
          } else {
            parts.add(
                Sizeof.fieldSizeof(
                    field, unwrap, getSizeof(field.getType(), field.getGenericType())));
          }
          if (!field.getType().isPrimitive()) {
            fixedSizeSum += 4L;
          }
        }
      }
      t = t.getSuperclass();
    }
    if (fixedSizeSum > 0) {
      if (parts.isEmpty()) {
        return Sizeof.constant(fixedSizeSum);
      }
      parts.add(Sizeof.constant(fixedSizeSum));
    }
    return Sizeof.sum(parts.toArray(new Sizeof[0]));
  }

  private boolean isInstanceField(Field f) {
    return !Modifier.isStatic(f.getModifiers());
  }

  private long getFixedSize(Class<?> type) {
    Long size = fixedSizeOfType.get(type);
    if (size != null) {
      if (size == FIXED_SIZE_RECURSIVE_MARKER) {
        return NOT_FIXED;
      }
      return size;
    }
    fixedSizeOfType.putIfAbsent(type, FIXED_SIZE_RECURSIVE_MARKER);
    long fixedSize = computeFixedSizeof(type);
    // OBS! we cannot use computeIfAbsent because this could cause recursive
    // updates where computation of the value includes putting the key
    return fixedSizeOfType.compute(
        type,
        (key, value) -> value == null || value == FIXED_SIZE_RECURSIVE_MARKER ? fixedSize : value);
  }

  private long computeFixedSizeof(Class<?> type) {
    if (type.isPrimitive()) {
      return isDoubleWord(type) ? 8L : 4L;
    }
    if (isRuntimeType(type)) {
      return 0L; // no extra costs
    }
    if (Proxy.isProxyClass(type)) {
      log.info("sizeof: Ignoring proxy: " + type);
      return 0L; // some bad design referencing services in data objects
    }
    if (isNotStaticallyDeterminedInSize(type)) {
      return -1L; // certainly not constant in size
    }
    return computedFixedSizeofFields(type);
  }

  private long computedFixedSizeofFields(Class<?> type) {
    long sum = objectHeaderSize;
    Class<?> currentType = type;
    while (currentType != null && currentType != Object.class) {
      for (Field field : currentType.getDeclaredFields()) {
        if (isInstanceField(field)) {
          long fieldSize = getFixedSize(field.getType());
          if (fieldSize < 0L) {
            return -1L; // meh, not a constant type
          }
          sum += fieldSize;
          if (!field.getType().isPrimitive()) {
            sum += 4L;
          }
        }
      }
      // now also add supertypes
      currentType = currentType.getSuperclass();
    }
    return sum;
  }

  private boolean isNotStaticallyDeterminedInSize(Class<?> type) {
    return type.isInterface() || type.isArray() || Modifier.isAbstract(type.getModifiers());
  }

  private boolean isRuntimeType(Class<?> type) {
    return type == Class.class
        || type == Constructor.class
        || type == Method.class
        || type == Field.class
        || ClassLoader.class.isAssignableFrom(type)
        || type.getName().contains("lambda");
  }
}
