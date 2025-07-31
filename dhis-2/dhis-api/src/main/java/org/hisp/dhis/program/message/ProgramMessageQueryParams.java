/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.program.message;

import java.util.Date;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Data
@Builder
public class ProgramMessageQueryParams {
  private Set<String> organisationUnit;

  private ProgramMessageStatus messageStatus;

  private Enrollment enrollment;

  private TrackerEvent event;

  private Date afterDate;

  private Date beforeDate;

  private Integer page;

  private Integer pageSize;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasOrganisationUnit() {
    return organisationUnit != null;
  }

  public boolean hasEnrollment() {
    return enrollment != null;
  }

  public boolean hasEvent() {
    return event != null;
  }

  public boolean hasPaging() {
    return page != null && pageSize != null;
  }
}
