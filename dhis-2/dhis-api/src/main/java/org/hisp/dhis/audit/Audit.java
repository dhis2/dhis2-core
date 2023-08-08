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
package org.hisp.dhis.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Class for Audit message persistence, meant to be a complete replacement for our existing audit
 * hibernate classes.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
public class Audit implements Serializable {
  private Long id;

  /** Type of audit. */
  @JsonProperty private final AuditType auditType;

  /** Scope of audit. */
  @JsonProperty private final AuditScope auditScope;

  /**
   * When audit was done. Necessary since there might be delayed from when the Audit is put on the
   * queue, to when its actually persisted.
   */
  @JsonProperty private final LocalDateTime createdAt;

  /** Who initiated the audit action. */
  @JsonProperty private final String createdBy;

  /** Name of klass being audited. Only required if linked directly to an object. */
  @JsonProperty private String klass;

  /** UID of object being audited, implies required for klass. */
  @JsonProperty private String uid;

  /** Code of object being audited, implies required for klass. */
  @JsonProperty private String code;

  /** Additional attributes attached to Audit. Stored as JSONB in the database. */
  @JsonProperty @Builder.Default private AuditAttributes attributes = new AuditAttributes();

  /** GZipped payload. Exposed as raw json, make sure that the payload is decompressed first. */
  @JsonRawValue private final String data;
}
