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
package org.hisp.dhis.hibernate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Values computed at most once per transaction: the first call in a transaction computes the value,
 * later calls in the same transaction get the same value, and the values are dropped when the
 * transaction completes. Outside a transaction (no transaction synchronization) every call
 * computes.
 *
 * <p>Use it for lookups that several queries of one request need, such as the access of the current
 * user, when the value must not outlive the request: nothing is kept between transactions, so a
 * change by another transaction is seen by the next one.
 */
public final class TransactionMemo {

  /** The key of the transaction resource that holds the values. */
  private static final Object RESOURCE = new Object();

  private TransactionMemo() {}

  /**
   * @param key identifies the value within the transaction, compared with {@link Object#equals}
   * @param compute computes the value when the transaction has none for the key yet
   * @return the value of the key in the current transaction
   */
  @SuppressWarnings("unchecked")
  public static <T> T computeIfAbsent(@Nonnull Object key, @Nonnull Supplier<T> compute) {
    Map<Object, Object> values = getValues();
    if (values == null) return compute.get();
    if (values.containsKey(key)) return (T) values.get(key);
    T value = compute.get();
    values.put(key, value);
    return value;
  }

  @SuppressWarnings("unchecked")
  private static Map<Object, Object> getValues() {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return null;
    Map<Object, Object> values =
        (Map<Object, Object>) TransactionSynchronizationManager.getResource(RESOURCE);
    if (values != null) return values;
    Map<Object, Object> created = new HashMap<>();
    TransactionSynchronizationManager.bindResource(RESOURCE, created);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            TransactionSynchronizationManager.unbindResourceIfPossible(RESOURCE);
          }
        });
    return created;
  }
}
