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
package org.hisp.dhis.tracker.export.timeout;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.function.Supplier;
import org.hibernate.annotations.QueryHints;

/**
 * Bounds tracker export queries that go through Hibernate by the remaining budget of the request on
 * the current thread.
 *
 * <p>The JDBC export stores get this for free from {@link DeadlineAwareJdbcTemplate}. Hibernate has
 * no such interception point, so its export stores have to ask per query. A static helper rather
 * than a base class or an interceptor on {@code HibernateGenericStore}, because that store is
 * shared by every Hibernate store in DHIS2 and a deadline check there would not be tracker scoped.
 */
public final class DeadlineQueries {

  private DeadlineQueries() {
    throw new UnsupportedOperationException("utility class");
  }

  /**
   * Runs {@code query} bounded by the budget left for this request, turning the timeout Hibernate
   * reports into {@link DeadlineExceededException} so it reaches the client as 504. Without that
   * translation the JPA {@code QueryTimeoutException} is a {@code PersistenceException}, which the
   * advice maps to 409.
   *
   * @throws DeadlineExceededException if the budget is already used up, so we fail fast rather than
   *     issue a query that cannot finish in time
   */
  public static <T> List<T> resultList(Query query) {
    return bounded(withDeadline(query)::getResultList);
  }

  /** {@link #resultList} for a query returning a single row, such as a count. */
  public static <T> T singleResult(TypedQuery<T> query) {
    return bounded(withDeadline(query)::getSingleResult);
  }

  /**
   * Arms {@code query} with the budget left for this request without running it. Prefer {@link
   * #resultList} or {@link #singleResult}, which arm and run in one call. This is only for a query
   * that needs more configuration after it is created, such as paging.
   */
  public static <Q extends Query> Q withDeadline(Q query) {
    Deadline deadline = DeadlineHolder.get();
    if (deadline == null) {
      return query;
    }
    DeadlineHolder.checkNotExpired();
    // this hint is in milliseconds
    query.setHint(QueryHints.TIMEOUT_JAKARTA_JPA, (int) deadline.remaining().toMillis());
    return query;
  }

  private static <T> T bounded(Supplier<T> execute) {
    try {
      return execute.get();
    } catch (jakarta.persistence.QueryTimeoutException | org.hibernate.QueryTimeoutException e) {
      Deadline deadline = DeadlineHolder.get();
      if (deadline == null) {
        // a timeout from some other source, not ours to translate
        throw e;
      }
      throw new DeadlineExceededException(deadline.budget(), e);
    }
  }
}
