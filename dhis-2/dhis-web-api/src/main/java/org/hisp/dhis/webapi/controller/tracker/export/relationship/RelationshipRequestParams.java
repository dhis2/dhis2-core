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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.tracker.export.PageRequestParams;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;

@OpenApi.Shared(name = "RelationshipRequestParams")
@OpenApi.Property
@Data
@NoArgsConstructor
public class RelationshipRequestParams implements PageRequestParams {
  static final String DEFAULT_FIELDS_PARAM =
      "relationship,relationshipType,createdAtClient,from[trackedEntity[trackedEntity],enrollment[enrollment],event[event]],to[trackedEntity[trackedEntity],enrollment[enrollment],event[event]]";

  @OpenApi.Property(defaultValue = "1")
  private Integer page;

  @OpenApi.Property(defaultValue = "50")
  private Integer pageSize;

  @OpenApi.Property(defaultValue = "false")
  private Boolean totalPages = false;

  private Boolean skipPaging = false;

  private List<OrderCriteria> order = new ArrayList<>();

  /**
   * @deprecated use {@link #trackedEntity} instead
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID.class, TrackedEntity.class})
  private UID tei;

  @OpenApi.Property({UID.class, TrackedEntity.class})
  private UID trackedEntity;

  @OpenApi.Property({UID.class, Enrollment.class})
  private UID enrollment;

  @OpenApi.Property({UID.class, Event.class})
  private UID event;

  @OpenApi.Property(value = String[].class)
  private List<FieldPath> fields = FieldFilterParser.parse(DEFAULT_FIELDS_PARAM);
}
