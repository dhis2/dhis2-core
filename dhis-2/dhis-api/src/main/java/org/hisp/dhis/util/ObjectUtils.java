/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Lars Helge Overland
 */
public class ObjectUtils {
  /**
   * Returns the first non-null argument. Returns null if all arguments are null.
   *
   * @param objects the objects.
   * @return the first non-null argument.
   */
  @SafeVarargs
  public static <T> T firstNonNull(T... objects) {
    if (objects != null) {
      for (T object : objects) {
        if (object != null) {
          return object;
        }
      }
    }

    return null;
  }

  /**
   * Indicates whether all of the given argument object are not null.
   *
   * @param objects the objects.
   * @return true if all of the given argument object are not null.
   */
  public static boolean allNonNull(Object... objects) {
    if (objects == null) {
      return false;
    }

    for (Object object : objects) {
      if (object == null) {
        return false;
      }
    }

    return true;
  }

  /**
   * Indicates whether the given object is null.
   *
   * @param object the object.
   * @return true if null.
   */
  public static boolean isNull(Object object) {
    return object == null;
  }

  /**
   * Indicates whether the given object is not null.
   *
   * @param object the object.
   * @return true if not null.
   */
  public static boolean notNull(Object object) {
    return object != null;
  }

  /**
   * Indicates whether any of the given expressions is not null and true.
   *
   * @param conditions the conditions.
   * @return true if any the given conditions is not null and true, false otherwise.
   */
  public static boolean anyIsTrue(Boolean... expressions) {
    if (expressions != null) {
      for (Boolean condition : expressions) {
        if (condition != null && condition.booleanValue()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Indicates whether any of the given expressions is not null and false.
   *
   * @param expressions the expressions.
   * @return true if any of the given conditions is not null and false, false otherwise.
   */
  public static boolean anyIsFalse(Boolean... expressions) {
    if (expressions != null) {
      for (Boolean condition : expressions) {
        if (condition != null && !condition.booleanValue()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Indicates whether any of the given objects is null.
   *
   * @param objects the objects.
   * @return true if any of the given objects is null, false otherwise.
   */
  public static boolean anyIsNull(Object... objects) {
    if (objects != null) {
      for (Object object : objects) {
        if (object == null) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns a list of strings, where the strings are the result of calling String.valueOf( Object )
   * of each object in the given collection.
   *
   * @param objects the collection of objects.
   * @return a list of strings.
   */
  public static List<String> asStringList(Collection<?> objects) {
    List<String> list = new ArrayList<>();

    for (Object object : objects) {
      list.add(String.valueOf(object));
    }

    return list;
  }

  /**
   * Joins the elements of the provided collection into a string. The provided string mapping
   * function is used to produce the string for each object. Null is returned if the provided
   * collection is null.
   *
   * @param collection the collection of elements.
   * @param separator the separator of elements in the returned string.
   * @param stringMapper the function to produce the string for each object.
   * @return the joined string.
   */
  public static <T> String join(
      Collection<T> collection, String separator, Function<T, String> stringMapper) {
    if (collection == null) {
      return null;
    }

    List<String> list = collection.stream().map(stringMapper).collect(Collectors.toList());

    return StringUtils.join(list, separator);
  }

  /**
   * Throws the given runtime exception if the given object is null.
   *
   * @param <T> the object type.
   * @param <U> the runtime exception type.
   * @param object the object.
   * @param ex the supplier of {@link RuntimeException}.
   * @throws RuntimeException
   */
  public static <T, U extends RuntimeException> T throwIfNull(T object, Supplier<U> ex) throws U {
    if (object == null) {
      throw ex.get();
    }

    return object;
  }

  /**
   * If object is null return null, otherwise return function applied to object.
   *
   * @param object to check.
   * @param <U> the function return type.
   * @param <T> the object type.
   * @param function the function to be applied to non-null object.
   */
  public static <T, U> U applyIfNotNull(T object, Function<T, U> function) {
    if (object == null) {
      return null;
    }

    return function.apply(object);
  }

  /**
   * Util method that always returns a new Set, either instantiated from a non-null Set passed as an
   * argument, or if a null arg is passed then returning an empty Set. This helps reduce possible
   * NullPointerExceptions when trying to instantiate a Set with a null value.
   *
   * @param set
   * @return
   * @param <T>
   */
  @Nonnull
  public static <T> Set<T> copyOf(Set<T> set) {
    return set != null ? new HashSet<>(set) : new HashSet<>();
  }

  /**
   * Util method that always returns a new List, either instantiated from a non-null Set passed as
   * an argument, or if a null arg is passed then returning an empty Set. This helps reduce possible
   * NullPointerExceptions when trying to instantiate a Set with a null value.
   *
   * @param list
   * @return
   * @param <T>
   */
  @Nonnull
  public static <T> List<T> copyOf(List<T> list) {
    return list != null ? new ArrayList<>(list) : new ArrayList<>();
  }
}
