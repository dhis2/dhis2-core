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

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;

@OpenApi.Shared(name = "RelationshipRequestParams")
@OpenApi.Property
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"identifier", "identifierName", "identifierClass"})
class RequestParams extends PagingAndSortingCriteriaAdapter {
  static final String DEFAULT_FIELDS_PARAM =
      "relationship,relationshipType,from[trackedEntity[trackedEntity],enrollment[enrollment],event[event]],to[trackedEntity[trackedEntity],enrollment[enrollment],event[event]]";

  /**
   * @deprecated use {@link #trackedEntity} instead
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID.class, TrackedEntity.class})
  @Setter
  private UID tei;

  @OpenApi.Property({UID.class, TrackedEntity.class})
  @Setter
  private UID trackedEntity;

  @OpenApi.Property({UID.class, Enrollment.class})
  @Setter
  private UID enrollment;

  @OpenApi.Property({UID.class, Event.class})
  @Setter
  private UID event;

  private String identifier;

  private String identifierName;

  private Class<?> identifierClass;

  @OpenApi.Property(value = String[].class)
  @Getter
  @Setter
  private List<FieldPath> fields = FieldFilterParser.parse(DEFAULT_FIELDS_PARAM);

  @OpenApi.Ignore
  public String getIdentifierParam() throws BadRequestException {
    if (this.identifier != null) {
      return this.identifier;
    }

    if (this.trackedEntity != null && this.tei != null) {
      throw new IllegalArgumentException(
          "Only one parameter of 'tei' and 'trackedEntity' must be specified. Prefer 'trackedEntity' as 'tei' will be removed.");
    }

    int count = 0;
    if (this.trackedEntity != null) {
      this.identifier = this.trackedEntity.getValue();
      this.identifierName = "trackedEntity";
      this.identifierClass = org.hisp.dhis.trackedentity.TrackedEntity.class;
      count++;
    }
    if (this.tei != null) {
      this.identifier = this.tei.getValue();
      this.identifierName = "trackedEntity";
      this.identifierClass = org.hisp.dhis.trackedentity.TrackedEntity.class;
      count++;
    }
    if (this.enrollment != null) {
      this.identifier = this.enrollment.getValue();
      this.identifierName = "enrollment";
      this.identifierClass = org.hisp.dhis.program.Enrollment.class;
      count++;
    }
    if (this.event != null) {
      this.identifier = this.event.getValue();
      this.identifierName = "event";
      this.identifierClass = org.hisp.dhis.program.Event.class;
      count++;
    }

    if (count == 0) {
      throw new BadRequestException(
          "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.");
    } else if (count > 1) {
      throw new BadRequestException(
          "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.");
    }
    return this.identifier;
  }

  @OpenApi.Ignore
  public String getIdentifierName() throws BadRequestException {
    if (this.identifierName == null) {
      this.getIdentifierParam();
    }
    return this.identifierName;
  }

  @OpenApi.Ignore
  public Class<?> getIdentifierClass() throws BadRequestException {
    if (this.identifierClass == null) {
      this.getIdentifierParam();
    }
    return this.identifierClass;
  }
}
