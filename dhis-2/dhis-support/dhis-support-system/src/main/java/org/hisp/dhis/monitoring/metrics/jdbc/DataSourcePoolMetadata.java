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
package org.hisp.dhis.monitoring.metrics.jdbc;

import javax.sql.DataSource;

/**
 * Provides access meta-data that is commonly available from most pooled {@link DataSource}
 * implementations.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public interface DataSourcePoolMetadata {

  /**
   * Return the usage of the pool as value between 0 and 1 (or -1 if the pool is not limited).
   *
   * <ul>
   *   <li>1 means that the maximum number of connections have been allocated
   *   <li>0 means that no connection is currently active
   *   <li>-1 means there is not limit to the number of connections that can be allocated
   * </ul>
   *
   * This may also return {@code null} if the data source does not provide the necessary information
   * to compute the poll usage.
   *
   * @return the usage value or {@code null}
   */
  Float getUsage();

  /**
   * Return the current number of active connections that have been allocated from the data source
   * or {@code null} if that information is not available.
   *
   * @return the number of active connections or {@code null}
   */
  Integer getActive();

  /**
   * Return the number of established but idle connections. Can also return {@code null} if that
   * information is not available.
   *
   * @return the number of established but idle connections or {@code null}
   * @since 2.2.0
   * @see #getActive()
   */
  default Integer getIdle() {
    return null;
  }

  /**
   * Return the maximum number of active connections that can be allocated at the same time or
   * {@code -1} if there is no limit. Can also return {@code null} if that information is not
   * available.
   *
   * @return the maximum number of active connections or {@code null}
   */
  Integer getMax();

  /**
   * Return the minimum number of idle connections in the pool or {@code null} if that information
   * is not available.
   *
   * @return the minimum number of active connections or {@code null}
   */
  Integer getMin();

  /**
   * Return the query to use to validate that a connection is valid or {@code null} if that
   * information is not available.
   *
   * @return the validation query or {@code null}
   */
  String getValidationQuery();

  /**
   * The default auto-commit state of connections created by this pool. If not set ({@code null}),
   * default is JDBC driver default (If set to null then the
   * java.sql.Connection.setAutoCommit(boolean) method will not be called.)
   *
   * @return the default auto-commit state or {@code null}
   */
  Boolean getDefaultAutoCommit();
}
