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
package org.hisp.dhis.audit;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a single DML (INSERT/UPDATE/DELETE) operation intercepted at the JDBC level via
 * datasource-proxy. Captures the SQL table name, optional mapped entity class, and primary key
 * value.
 */
@Value
@Builder
public class DmlEvent {

  public enum DmlOperation {
    INSERT,
    UPDATE,
    DELETE
  }

  DmlOperation operation;

  /** Raw SQL table name as it appears in the DML statement. */
  String tableName;

  /**
   * Fully-qualified entity class name, or null if the table is not mapped to a Hibernate entity.
   */
  String entityClassName;

  /** Extracted primary key value, or null if not extractable from the SQL parameters. */
  Serializable entityId;

  /** Column names from the SET clause for UPDATE operations. Empty for INSERT/DELETE. */
  @Builder.Default Set<String> updatedColumns = Set.of();

  Instant timestamp;

  String connectionId;
}
