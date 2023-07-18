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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

@NoArgsConstructor
@EqualsAndHashCode(exclude = {"identifier", "identifierName", "identifierClass"})
class LegacyRequestParams extends PagingAndSortingCriteriaAdapter {

  @Setter
  private String trackedEntity;

  @Setter
  private String enrollment;

  @Setter
  private String event;

  private String identifier;

  private String identifierName;

  private Class<?> identifierClass;

  public String getIdentifierParam() {
    if (this.identifier != null) {
      return this.identifier;
    }

    if (this.trackedEntity != null) {
      this.identifier = this.trackedEntity;
      this.identifierName = "trackedEntity";
      this.identifierClass = org.hisp.dhis.trackedentity.TrackedEntity.class;
    }
    if (this.enrollment != null) {
      this.identifier = this.enrollment;
      this.identifierName = "enrollment";
      this.identifierClass = org.hisp.dhis.program.Enrollment.class;
    }
    if (this.event != null) {
      this.identifier = this.event;
      this.identifierName = "event";
      this.identifierClass = org.hisp.dhis.program.Event.class;
    }

    return this.identifier;
  }

  public String getIdentifierName() {
    if (this.identifierName == null) {
      this.getIdentifierParam();
    }
    return this.identifierName;
  }

  public Class<?> getIdentifierClass() {
    if (this.identifierClass == null) {
      this.getIdentifierParam();
    }
    return this.identifierClass;
  }
}
