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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
public class AuditQuery {
  /** This narrows the search scope for audits by types. */
  @Builder.Default private Set<AuditType> auditType = new HashSet<>();

  /** This narrows the search scope for audits by scopes. */
  @Builder.Default private Set<AuditScope> auditScope = new HashSet<>();

  /**
   * This narrows the search scope for audits, the class name should be fully qualified.
   *
   * <p>TODO should it be fully qualified? what about refactors? what about duplicate class names if
   * we don't do it?
   */
  @Builder.Default private Set<String> klass = new HashSet<>();

  /**
   * This narrows the search scope by search by a list of UIDs. This binds an AND relationship with
   * klass, and a OR relationship with code.
   */
  @Builder.Default private Set<String> uid = new HashSet<>();

  /**
   * This narrows the search scope by search by a list of codes. This binds an AND relationship with
   * klass, and a OR relationship with uid.
   */
  @Builder.Default private Set<String> code = new HashSet<>();

  /** This narrows the search by filtering records base on the values of {@link AuditAttributes} */
  @Builder.Default private AuditAttributes auditAttributes = new AuditAttributes();

  /** From/To dates to query from. */
  private Range range;

  static Range range(LocalDateTime from) {
    return Range.builder().from(from).build();
  }

  static Range range(LocalDateTime from, LocalDateTime to) {
    return Range.builder().from(from).to(to).build();
  }

  @Value
  @Builder(access = AccessLevel.PRIVATE)
  public static class Range {
    /** From date to fetch audits from. */
    private @Nonnull LocalDateTime from;

    /** To date to fetch audits from. */
    private LocalDateTime to;
  }
}
