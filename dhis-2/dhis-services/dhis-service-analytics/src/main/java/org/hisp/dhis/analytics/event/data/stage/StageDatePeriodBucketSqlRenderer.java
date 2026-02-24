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
package org.hisp.dhis.analytics.event.data.stage;

import java.util.Optional;
import org.hisp.dhis.common.QueryItem;

/**
 * Resolves and renders period bucket SQL expressions for stage date dimensions.
 *
 * <p>Implementations can provide database-specific SQL generation while keeping manager code
 * database-agnostic.
 */
public interface StageDatePeriodBucketSqlRenderer {
  /**
   * Resolves the common period bucket column for all requested dimension values of a stage date
   * item.
   *
   * @param item the stage date query item
   * @return the resolved period bucket column, empty if values cannot be represented as one bucket
   */
  Optional<String> resolvePeriodBucketColumn(QueryItem item);

  /**
   * Renders the SQL expression used to derive the period bucket from the stage date column.
   *
   * @param item the stage date query item
   * @param periodBucketColumn the target bucket column (for example {@code yearly}, {@code
   *     monthly})
   * @return SQL expression for SELECT/GROUP BY
   */
  String renderPeriodBucketExpression(QueryItem item, String periodBucketColumn);
}
