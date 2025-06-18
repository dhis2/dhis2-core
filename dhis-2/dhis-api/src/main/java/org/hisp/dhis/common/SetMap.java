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
package org.hisp.dhis.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Set implementation with keys mapped to a set of values.
 *
 * @author Lars Helge Overland
 */
public class SetMap<T, V> extends HashMap<T, Set<V>> {
  public SetMap() {
    super();
  }

  public SetMap(SetMap<T, V> setMap) {
    super(setMap);
  }

  /**
   * Returns the set of values for the given key, or an empty set if no values are present.
   *
   * @param key the key to retrieve values for.
   * @return a set of values.
   */
  public void putValue(T key, V value) {
    computeIfAbsent(key, k -> new HashSet<>()).add(value);
  }

  /**
   * Adds all values from the given collection to the set of values for the specified key. If the
   * key does not exist, it will be created with an empty set.
   *
   * @param key the key to which the values will be added.
   * @param values the collection of values to add.
   */
  public void putValues(T key, Collection<? extends V> values) {
    computeIfAbsent(key, k -> new HashSet<>()).addAll(values);
  }

  /**
   * Adds all values from the given SetMap to this {@link SetMap}. If a key already exists, the
   * values will be added to the existing set of values for that key.
   *
   * @param setMap the SetMap containing keys and values to add.
   */
  public void putValues(SetMap<T, V> setMap) {
    setMap.forEach(this::putValues);
  }

  /**
   * Produces a {@link SetMap} based on the given set of values. The key for each entry is produced
   * by applying the given keyMapper function.
   *
   * @param values the values of the map.
   * @param keyMapper the function producing the key for each entry.
   * @return a SetMap.
   */
  public static <T, V> SetMap<T, V> getSetMap(Set<V> values, Function<V, T> keyMapper) {
    SetMap<T, V> map = new SetMap<>();

    for (V value : values) {
      T key = keyMapper.apply(value);

      map.putValue(key, value);
    }

    return map;
  }
}
