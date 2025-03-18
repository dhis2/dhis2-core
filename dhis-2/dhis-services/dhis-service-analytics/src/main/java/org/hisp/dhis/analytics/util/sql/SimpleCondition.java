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
package org.hisp.dhis.analytics.util.sql;

/**
 * Represents a basic SQL condition string without any transformation. Unlike {@link Condition.Raw},
 * this condition does not remove leading WHERE/AND keywords.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * // Basic conditions
 * new SimpleCondition("active = true")
 *     -> "active = true"
 *
 * // Comparison operations
 * new SimpleCondition("age >= 18")
 *     -> "age >= 18"
 *
 * // IN clauses
 * new SimpleCondition("status IN ('ACTIVE', 'PENDING')")
 *     -> "status IN ('ACTIVE', 'PENDING')"
 *
 * // LIKE patterns
 * new SimpleCondition("name LIKE 'John%'")
 *     -> "name LIKE 'John%'"
 *
 * // Complex conditions
 * new SimpleCondition("(age >= 18 AND status = 'ACTIVE')")
 *     -> "(age >= 18 AND status = 'ACTIVE')"
 * }</pre>
 */
public record SimpleCondition(String condition) implements Condition {
  @Override
  public String toSql() {
    return condition;
  }
}
