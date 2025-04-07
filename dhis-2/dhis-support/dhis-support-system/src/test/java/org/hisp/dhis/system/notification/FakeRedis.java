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
package org.hisp.dhis.system.notification;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisOperations;

/**
 * Implementation of {@link RedisOperations} that can be used to simulate redis during tests.
 *
 * <p>It only supports the operations actually used by the {@link RedisNotifierStore}.
 *
 * <p>Since the redis API interface are huge {@link Mockito} is used not as a mocking to but as a
 * way of wiring API calls to handwritten implementation methods.
 *
 * @author Jan Bernitt
 */
record FakeRedis(
    Map<String, FakeZSet> zSets,
    Map<String, FakeHashTable> hTables,
    RedisOperations<String, String> api) {

  static Notifier notifier(SystemSettings settings, LongSupplier clock) {
    SystemSettingsService settingsService = mock(SystemSettingsService.class);
    when(settingsService.getCurrentSettings()).thenReturn(settings);
    return new DefaultNotifier(
        new RedisNotifierStore(new FakeRedis().api()), new ObjectMapper(), settingsService, clock);
  }

  FakeRedis() {
    this(new HashMap<>(), new HashMap<>(), redisOps());
  }

  FakeRedis {
    when(api.keys(anyString())).thenAnswer(this::keys);
    when(api.boundZSetOps(anyString())).thenAnswer(this::boundZSetOps);
    when(api.boundHashOps(anyString())).thenAnswer(this::boundHashOps);
    when(api.delete(anyString())).thenAnswer(this::delete);
    when(api.delete(anyCollection())).thenAnswer(this::deleteAll);
  }

  private Set<String> keys(InvocationOnMock i) {
    String pattern = i.getArgument(0, String.class);
    return Stream.concat(
            zSets.entrySet().stream()
                .filter(e -> !e.getValue().entries.isEmpty() && patternMatches(pattern, e.getKey()))
                .map(Map.Entry::getKey),
            hTables.entrySet().stream()
                .filter(e -> !e.getValue().entries.isEmpty() && patternMatches(pattern, e.getKey()))
                .map(Map.Entry::getKey))
        .collect(toUnmodifiableSet());
  }

  private static boolean patternMatches(String pattern, String key) {
    return key.matches(pattern.replace("*", ".*"));
  }

  private boolean delete(InvocationOnMock i) {
    String key = i.getArgument(0, String.class);
    boolean zSetDeleted = zSets.containsKey(key) && !zSets.remove(key).entries.isEmpty();
    boolean hTableDeleted = hTables.containsKey(key) && !hTables.remove(key).entries.isEmpty();
    return zSetDeleted || hTableDeleted;
  }

  private long deleteAll(InvocationOnMock i) {
    @SuppressWarnings("unchecked")
    Collection<String> keys = i.getArgument(0, Collection.class);
    int size = zSets.size() + hTables.size();
    keys.forEach(zSets::remove);
    keys.forEach(hTables::remove);
    // Note: the size is off if the set/table were empty
    // but handling that seems unnecessary at this point
    return size - (zSets.size() + hTables.size());
  }

  private BoundZSetOperations<String, String> boundZSetOps(InvocationOnMock i) {
    String key = i.getArgument(0, String.class);
    return zSets.computeIfAbsent(key, k -> new FakeZSet(new LinkedList<>(), boundZSetOps())).api();
  }

  private BoundHashOperations<String, String, String> boundHashOps(InvocationOnMock i) {
    String key = i.getArgument(0, String.class);
    return hTables
        .computeIfAbsent(key, k -> new FakeHashTable(new HashMap<>(), boundHashOps()))
        .api();
  }

  record FakeZSet(LinkedList<String> entries, BoundZSetOperations<String, String> api) {

    FakeZSet {
      when(api.zCard()).thenAnswer(i -> (long) entries.size());
      when(api.removeRange(anyLong(), anyLong())).thenAnswer(this::removeRange);
      when(api.add(anyString(), anyDouble())).thenAnswer(this::add);
      when(api.range(anyLong(), anyLong())).then(this::range);
      when(api.reverseRange(anyLong(), anyLong())).then(this::reverseRange);
    }

    private long removeRange(InvocationOnMock i) {
      int start = i.getArgument(0, Long.class).intValue();
      int end = i.getArgument(1, Long.class).intValue();
      if (start < 0) start = entries.size() + start;
      if (end < 0) end = entries.size() + end;
      int len = end - start + 1;
      for (int j = 0; j < len; j++) entries.remove(start);
      return len;
    }

    private boolean add(InvocationOnMock i) {
      String value = i.getArgument(0, String.class);
      // Note: the score is not supported since we only append with increased score
      // always adding last is the same as sorting by score
      entries.addLast(value);
      return true;
    }

    private Set<String> range(InvocationOnMock i) {
      int start = i.getArgument(0, Long.class).intValue();
      int end = i.getArgument(1, Long.class).intValue();
      return range(start, end);
    }

    private Set<String> range(int start, int end) {
      int size = entries.size();
      if (start < 0) start = size + start;
      if (end < 0) end = size + end;
      if (start >= size || start < 0) return Set.of();
      if (end >= size) end = size - 1;
      return new LinkedHashSet<>(entries.subList(start, end + 1));
    }

    private Set<String> reverseRange(InvocationOnMock i) {
      int start = i.getArgument(0, Long.class).intValue();
      int end = i.getArgument(1, Long.class).intValue();
      LinkedList<String> tmp = new LinkedList<>();
      range(-end - 1, -start - 1).forEach(tmp::addFirst);
      return new LinkedHashSet<>(tmp);
    }
  }

  record FakeHashTable(
      Map<String, String> entries, BoundHashOperations<String, String, String> api) {

    FakeHashTable {
      doAnswer(
              i -> {
                put(i);
                return null;
              })
          .when(api)
          .put(anyString(), anyString());
      when(api.get(anyString())).thenAnswer(this::get);
    }

    private void put(InvocationOnMock i) {
      String key = i.getArgument(0, String.class);
      String value = i.getArgument(1, String.class);
      entries.put(key, value);
    }

    private String get(InvocationOnMock i) {
      String key = i.getArgument(0, String.class);
      return entries.get(key);
    }
  }

  private static final MockSettings SETTINGS = Mockito.withSettings().strictness(LENIENT);

  @SuppressWarnings("unchecked")
  private static RedisOperations<String, String> redisOps() {
    return mock(RedisOperations.class, SETTINGS);
  }

  @SuppressWarnings("unchecked")
  private static BoundHashOperations<String, String, String> boundHashOps() {
    return mock(BoundHashOperations.class, SETTINGS);
  }

  @SuppressWarnings("unchecked")
  private static BoundZSetOperations<String, String> boundZSetOps() {
    return mock(BoundZSetOperations.class, SETTINGS);
  }
}
