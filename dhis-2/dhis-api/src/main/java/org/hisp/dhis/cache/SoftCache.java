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
package org.hisp.dhis.cache;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A soft cache uses a concurrent map with {@link java.lang.ref.SoftReference} boxed values.
 *
 * <p>This means the values might get GC-ed if the JVM needs memory. Therefore, when accessing a
 * value by key, a {@link java.util.function.Supplier} needs to be passed in case the value needs
 * re-computing.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public final class SoftCache<T> {

  private final ConcurrentMap<String, SoftReference<T>> cache = new ConcurrentHashMap<>();

  /**
   * Access a cached value by key.
   *
   * @param key the key to get from the cache, when null the value is computed and not cached
   * @param onMiss a function to compute the value on a cache miss; the function must not return
   *     null
   * @return the cached or computed value
   */
  @Nonnull
  public T get(@CheckForNull String key, @Nonnull Supplier<T> onMiss) {
    if (key == null) return onMiss.get();
    Supplier<T> memOnMiss = memorize(onMiss);
    T res =
        cache
            .compute(
                key,
                (k, v) -> {
                  if (v == null) return new SoftReference<>(memOnMiss.get());
                  if (v.get() != null) return v;
                  return new SoftReference<>(memOnMiss.get());
                })
            .get();
    // in theory the get() of the SoftReference can become null
    // just when it is returned from the map
    // in which case the res is taken directly from the supplier
    // since this should mean the JVM is low on memory
    return res == null ? memOnMiss.get() : res;
  }

  /**
   * @param onMiss assumed expensive to compute
   * @return a supplier that will memorize the result once it has been computed
   */
  private Supplier<T> memorize(Supplier<T> onMiss) {
    Object[] memorize = new Object[1];
    return () -> {
      @SuppressWarnings("unchecked")
      T v = (T) memorize[0];
      if (v != null) return v;
      v = onMiss.get();
      memorize[0] = v;
      return v;
    };
  }
}
