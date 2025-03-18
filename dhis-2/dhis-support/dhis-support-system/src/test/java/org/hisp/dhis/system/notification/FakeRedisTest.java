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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisOperations;

/**
 * Tests basic correctness of the {@link FakeRedis}.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
class FakeRedisTest {

  private final RedisOperations<String, String> redis = new FakeRedis().api();

  @Test
  void testKeys_readingDoesNotAddKeys() {
    BoundZSetOperations<String, String> set = redis.boundZSetOps("test");
    assertEquals(0L, set.zCard());
    assertEquals(Set.of(), redis.keys("*"));
    assertEquals(Set.of(), redis.keys("test"));
  }

  @Test
  void testKeys_zSet() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("test:x");
    set1.add("1", 1);
    assertEquals(Set.of("test:x"), redis.keys("*"));
    assertEquals(Set.of("test:x"), redis.keys("test:*"));
    assertEquals(Set.of("test:x"), redis.keys("test:x"));

    BoundZSetOperations<String, String> set2 = redis.boundZSetOps("foo:bar:7");
    set2.add("2", 2);
    assertEquals(Set.of("test:x", "foo:bar:7"), redis.keys("*"));
    assertEquals(Set.of("foo:bar:7"), redis.keys("foo:*"));
    assertEquals(Set.of("foo:bar:7"), redis.keys("foo:bar:*"));
    assertEquals(Set.of("foo:bar:7"), redis.keys("foo:bar:7"));
    assertEquals(Set.of(), redis.keys("foo:bar:9"));
  }

  @Test
  void testKeys_hashTable() {
    BoundHashOperations<String, String, String> table1 = redis.boundHashOps("table");
    table1.put("a", "b");
    assertEquals(Set.of("table"), redis.keys("*"));
    assertEquals(Set.of("table"), redis.keys("table"));

    BoundHashOperations<String, String, String> table2 = redis.boundHashOps("summaries:id");
    table2.put("c", "d");
    assertEquals(Set.of("table", "summaries:id"), redis.keys("*"));
    assertEquals(Set.of("table"), redis.keys("table"));
    assertEquals(Set.of("summaries:id"), redis.keys("summaries:*"));
    assertEquals(Set.of("summaries:id"), redis.keys("summaries:id"));
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void testDelete_NonExistingHasNoEffect() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("empty:s");
    assertEquals(0L, set1.zCard());
    assertFalse(redis.delete("empty:s"));
    assertFalse(redis.delete("x"));

    BoundHashOperations<String, String, String> table1 = redis.boundHashOps("empty:t");
    assertNull(table1.get("x"));
    assertFalse(redis.delete("empty:t"));
    assertFalse(redis.delete("x"));
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void testDelete_byKey() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("my:set");
    set1.add("val", 42);
    set1.add("val2", 43);
    assertEquals(Set.of("my:set"), redis.keys("*"));
    assertTrue(redis.delete("my:set"));
    assertEquals(Set.of(), redis.keys("*"));
  }

  @Test
  void testDelete_byKeys() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("my:set");
    set1.add("val", 42);
    set1.add("val2", 43);
    BoundHashOperations<String, String, String> table1 = redis.boundHashOps("my:table");
    table1.put("a", "b");

    assertEquals(Set.of("my:set", "my:table"), redis.keys("my:*"));
    assertEquals(2, redis.delete(Set.of("my:set", "my:table")));
    assertEquals(Set.of(), redis.keys("*"));
  }

  @Test
  void testZCard() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("my:set");
    assertEquals(0L, set1.zCard());

    set1.add("1", 1);
    assertEquals(1L, set1.zCard());

    set1.add("2", 2);
    assertEquals(2L, set1.zCard());

    set1.add("1", 3);
    assertEquals(3L, set1.zCard());

    set1.removeRange(0, 0);
    assertEquals(2L, set1.zCard());
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void testRange() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("my:set");
    set1.add("1", 1);
    set1.add("2", 2);
    set1.add("3", 3);
    set1.add("4", 4);

    assertEquals(List.of("1", "2", "3", "4"), List.copyOf(set1.range(0, -1)));
    assertEquals(List.of("4"), List.copyOf(set1.range(-1, -1)));
    assertEquals(List.of("1"), List.copyOf(set1.range(0, 0)));
    assertEquals(List.of("2", "3"), List.copyOf(set1.range(1, 2)));
    assertEquals(List.of(), List.copyOf(set1.range(10, 20)));
    assertEquals(List.of("3", "4"), List.copyOf(set1.range(2, 20)));
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void testReverseRange() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("my:set");
    set1.add("1", 1);
    set1.add("2", 2);
    set1.add("3", 3);
    set1.add("4", 4);

    assertEquals(List.of("4", "3", "2", "1"), List.copyOf(set1.reverseRange(0, -1)));
    assertEquals(List.of("1"), List.copyOf(set1.reverseRange(-1, -1)));
    assertEquals(List.of("4"), List.copyOf(set1.reverseRange(0, 0)));
    assertEquals(List.of("3", "2"), List.copyOf(set1.reverseRange(1, 2)));
  }

  @Test
  @SuppressWarnings("DataFlowIssue")
  void testRemoveRange() {
    BoundZSetOperations<String, String> set1 = redis.boundZSetOps("my:set");
    set1.add("1", 1);
    set1.add("2", 2);
    set1.add("3", 3);
    set1.add("4", 4);

    assertEquals(1L, set1.removeRange(1, 1));
    assertEquals(List.of("1", "3", "4"), List.copyOf(set1.range(0, -1)));
    assertEquals(1L, set1.removeRange(-1, -1));
    assertEquals(List.of("1", "3"), List.copyOf(set1.range(0, -1)));
    assertEquals(1L, set1.removeRange(0, 0));
    assertEquals(List.of("3"), List.copyOf(set1.range(0, -1)));
  }

  @Test
  void testPut() {
    BoundHashOperations<String, String, String> table1 = redis.boundHashOps("my:table");

    table1.put("a", "b");
    assertEquals("b", table1.get("a"));

    table1.put("b", "x");
    assertEquals("x", table1.get("b"));
  }
}
