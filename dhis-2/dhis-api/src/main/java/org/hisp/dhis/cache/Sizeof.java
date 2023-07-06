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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

/**
 * {@link Sizeof} is a operator that given an object returns the estimated size of this object in
 * bytes. Can estimate the size of a particular type.
 *
 * <p>To be efficient in the estimation of objects a {@link Sizeof} is composed based on the {@link
 * Class} metadata available for given object type. This composed operator will have a part that is
 * determined once when statically analysing a certain {@link Class} and might have a dynamic part
 * for those parts of an object that do not allow static analysis. An example would be a field of
 * type {@link Object} which could hold any actual object. In such a case the analysis would resolve
 * the {@link Sizeof} operator dynamically for the actual {@link Class} of the present value.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
interface Sizeof {

  /**
   * Estimates the total size of the given java object in the heap memory in number of bytes. This
   * includes all objects referenced by fields of this objects recursively.
   *
   * <p>It cannot be emphasised enough that this size is an estimation based on assumptions such as
   * the size of the java object header or the size of a java reference pointer. Also this
   * estimation uses a simplified perspective where details such additional size for array object
   * headers are neglected.
   *
   * <p>At times the estimation is deliberately pessimistic. For example for fields of a type of
   * fixed know size (like an {@link Integer}) the estimation will assume the field does hold a
   * value independent of the actual passed object pointing to {@code null} for that field or not.
   * This is simply to allow efficient estimation of complex and deeply hierarchical object
   * structures.
   *
   * @param obj an arbitrary java object
   * @return number of bytes of memory used by the object
   */
  long sizeof(Object obj);

  /**
   * Creates a {@link Sizeof} operator that simply returns a constant size. This size has been a
   * result of static analysis. Like the size of an a primitive or the sum of multiple of those and
   * or the java object header.
   *
   * @param size number of bytes
   * @return a {@link Sizeof} operator that simply returns the given fixed size
   */
  static Sizeof constant(long size) {
    return obj -> size;
  }

  /**
   * Creates a {@link Sizeof} operator that sums up different parts of the analysis. Usually this is
   * a {@link #constant(long)} part and one or more {@link #fieldSizeof(Field, UnaryOperator,
   * Sizeof)} parts.
   *
   * @param parts parts of object sizeof analysis to sum
   * @return a {@link Sizeof} operator that sums up all given parts
   */
  static Sizeof sum(Sizeof... parts) {
    return obj -> {
      long sum = 0L;
      for (Sizeof part : parts) {
        sum += part.sizeof(obj);
      }
      return sum;
    };
  }

  /**
   * Creates a {@link Sizeof} for fields which size value needs to be evaluated based on the actual
   * value given the provided {@link Sizeof} operator.
   *
   * @param f field to extract.
   * @param unwrap operator to unwrap the field value
   * @param operator {@link Sizeof} operator to apply to the field value
   * @return A {@link Sizeof} that extracts the given field from the object and estimates it size
   *     given the provided {@link Sizeof} operator
   */
  static Sizeof fieldSizeof(Field f, UnaryOperator<Object> unwrap, Sizeof operator) {
    f.setAccessible(true);
    return obj -> {
      if (obj == null) {
        return 0L; // this field
      }
      try {
        // this would not be used for primitives
        Object value = unwrap.apply(f.get(obj));
        if (value == null) {
          return 0L;
        }
        return operator.sizeof(value);
      } catch (IllegalAccessException e) {
        return 8L; // very, very optimistic guess
      }
    };
  }

  /**
   * Creates a {@link Sizeof} operator for primitive arrays. It will assume it is only applied to
   * objects that are arrays of primitives.
   */
  static Sizeof arrayOfPrimitive(long objectHeaderSize) {
    return obj -> {
      if (obj == null) {
        return 0L;
      }
      int length = Array.getLength(obj);
      if (obj instanceof byte[]) {
        return objectHeaderSize + length;
      }
      if (obj instanceof char[]) {
        return objectHeaderSize + length * 2L;
      }
      if (obj instanceof long[] || obj instanceof double[]) {
        return objectHeaderSize + length * 8L;
      }
      // all others we assume the worst implementation
      return objectHeaderSize + length * 4L;
    };
  }

  /**
   * Creates a {@link Sizeof} operator for reference arrays of element types with a fixed size. Like
   * an array if {@link Integer}
   */
  static Sizeof arrayOfFixed(long objectHeaderSize, long elementSize) {
    return obj -> {
      if (obj == null) {
        return 0L;
      }
      Object[] array = (Object[]) obj;
      return objectHeaderSize + array.length * (4L + elementSize);
    };
  }

  /**
   * Creates a {@link Sizeof} operator for reference arrays of element types with a size that is not
   * statically known. Like an array of {@link String} (as each value might have unpredictable
   * length) or other more complex types with array or complex fields.
   */
  static Sizeof arrayOfDynamic(long objectHeaderSize, Sizeof elementSize) {
    return obj -> {
      if (obj == null) {
        return 0L;
      }
      Object[] array = (Object[]) obj;
      long sum = objectHeaderSize + array.length * 4L; // the array itself
      for (Object e : array) {
        sum += elementSize.sizeof(e);
      }
      return sum;
    };
  }

  /**
   * Creates a {@link Sizeof} operator for {@link Collection}s with an element type that has a fixed
   * statically known size. Like a {@link java.util.List} of {@link Integer}.
   */
  static Sizeof collectionOfFixed(long objectHeaderSize, long elementSize) {
    return obj -> {
      if (obj == null) {
        return 0L;
      }
      return objectHeaderSize + ((Collection<?>) obj).size() * (8L + elementSize);
    };
  }

  /**
   * Creates a {@link Sizeof} operator for {@link Collection}s with an element type with a size that
   * is not statically known and that needs to be estimated by the provided {@link Sizeof} operator
   * for elements.
   */
  static Sizeof collectionOfDynamic(long objectHeaderSize, Sizeof elementSize) {
    return obj -> {
      if (obj == null) {
        return 0L;
      }
      // for the root ref and state in the collection
      long sum = objectHeaderSize;
      for (Object e : (Collection<?>) obj) {
        // assume at least 2 reference per element need to organise
        sum += 8L + elementSize.sizeof(e);
      }
      return sum;
    };
  }

  /**
   * Creates a {@link Sizeof} operator for {@link Map}s. This is used for both maps with keys and/or
   * values of fixed size and statically unknown size.
   *
   * <p>In case a key or value has a fixed size the provided operator will simply return a {@link
   * #constant(long)} size.
   */
  static Sizeof mapOfDynamic(long objectHeaderSize, Sizeof keySize, Sizeof valueSize) {
    return obj -> {
      if (obj == null) {
        return 0L;
      }
      long sum = objectHeaderSize;
      for (Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
        sum += keySize.sizeof(e.getKey());
        sum += valueSize.sizeof(e.getValue());
        sum += objectHeaderSize + 12L; // for the entry
      }
      return sum;
    };
  }
}
