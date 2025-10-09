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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.webapi.controller.tracker.FieldsRequestParam;
import org.hisp.dhis.webapi.controller.tracker.PageRequestParams;

@OpenApi.Shared(name = "RelationshipRequestParams")
@OpenApi.Property
@Data
@NoArgsConstructor
public class RelationshipRequestParams implements PageRequestParams, FieldsRequestParam {
  static final String DEFAULT_FIELDS_PARAM =
      "relationship,relationshipType,createdAtClient,from[trackedEntity[trackedEntity],enrollment[enrollment],event[event]],to[trackedEntity[trackedEntity],enrollment[enrollment],event[event]]";
  static final Fields DEFAULT_FIELDS_PARAM_PARSED = FieldsParser.parse(DEFAULT_FIELDS_PARAM);

  @OpenApi.Description(
"""
Get the given page.
""")
  @OpenApi.Property(defaultValue = "1")
  private Integer page;

  @OpenApi.Description(
"""
Get given number of items per page.
""")
  @OpenApi.Property(defaultValue = "50")
  private Integer pageSize;

  @OpenApi.Description(
"""
Get the total number of items and pages in the pager.

**Only enable this if absolutely necessary as this is resource intensive.** Use the pagers
`prev/nextPage` to determine if there is a previous or a next page instead.
""")
  private boolean totalPages = false;

  @OpenApi.Description(
"""
Get all items by specifying `paging=false`. Requests are paginated by default.

**Be aware that the performance is directly related to the amount of data requested. Larger pages
will take more time to return.**
""")
  private boolean paging = true;

  private List<OrderCriteria> order = new ArrayList<>();

  @OpenApi.Property({UID.class, TrackedEntity.class})
  private UID trackedEntity;

  @OpenApi.Property({UID.class, Enrollment.class})
  private UID enrollment;

  @OpenApi.Property({UID.class, TrackerEvent.class})
  private UID event;

  @OpenApi.Property(value = String[].class)
  private Fields fields = DEFAULT_FIELDS_PARAM_PARSED;

  private boolean includeDeleted;
}
