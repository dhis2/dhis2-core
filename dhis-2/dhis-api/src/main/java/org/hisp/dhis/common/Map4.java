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
package org.hisp.dhis.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jim Grace
 */
public class Map4<R, S, T, U, V> extends HashMap<R, MapMapMap<S, T, U, V>> {
  private static final long serialVersionUID = -2806972962597162013L;

  public MapMapMap<S, T, U, V> putEntry(R key1, S key2, T key3, U key4, V value) {
    MapMapMap<S, T, U, V> map = this.get(key1);
    map = map == null ? new MapMapMap<>() : map;
    map.putEntry(key2, key3, key4, value);
    return this.put(key1, map);
  }

  public void putEntries(R key1, MapMapMap<S, T, U, V> m) {
    MapMapMap<S, T, U, V> map = this.get(key1);
    map = map == null ? new MapMapMap<>() : map;
    map.putMap(m);
    this.put(key1, map);
  }

  public void putMap(Map4<R, S, T, U, V> map) {
    for (Entry<R, MapMapMap<S, T, U, V>> entry : map.entrySet()) {
      this.putEntries(entry.getKey(), entry.getValue());
    }
  }

  public V getValue(R key1, S key2, T key3, U key4) {
    return this.get(key1) == null ? null : this.get(key1).getValue(key2, key3, key4);
  }

  @SafeVarargs
  public static <R, S, T, U, V> Map4<R, S, T, U, V> ofEntries(
      Map.Entry<R, MapMapMap<S, T, U, V>>... entries) {
    Map4<R, S, T, U, V> map = new Map4<>();

    for (Map.Entry<R, MapMapMap<S, T, U, V>> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }

    return map;
  }
}
