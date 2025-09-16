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
package org.hisp.dhis.webapi.controller.tracker.deduplication;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.deduplication.DeduplicationStatus;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.tracker.FieldsRequestParam;
import org.hisp.dhis.webapi.controller.tracker.PageRequestParams;

@OpenApi.Shared(name = "PotentialDuplicateRequestParams")
@OpenApi.Property
@Data
@NoArgsConstructor
public class PotentialDuplicateRequestParams implements PageRequestParams, FieldsRequestParam {
  private static final String DEFAULT_FIELDS_PARAM =
      "id,created,lastUpdated,original,duplicate,status";

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

  /** Parameter {@code totalPages} is not supported. */
  @OpenApi.Ignore
  @Override
  public boolean isTotalPages() {
    return false;
  }

  @OpenApi.Description(
"""
Get all items by specifying `paging=false`. Requests are paginated by default.

**Be aware that the performance is directly related to the amount of data requested. Larger pages
will take more time to return.**
""")
  private boolean paging = true;

  @OpenApi.Description(
"""
Get potential duplicates that are in given status.
""")
  private DeduplicationStatus status = DeduplicationStatus.OPEN;

  @OpenApi.Description(
"""
`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get potential duplicates in given order.

Valid `sortDirection`s are `asc` and `desc`. `sortDirection` is case-insensitive.
""")
  private List<OrderCriteria> order = new ArrayList<>();

  @OpenApi.Description(
"""
`<uid1>[,<uid2>...]`

Get potential duplicates for given tracked entities.
""")
  private List<UID> trackedEntities = new ArrayList<>();

  @OpenApi.Description(
"""
Get only the given fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the JSON response and in some cases decrease the response time. Refer to
[metadata field filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter)
on how to use it.
""")
  @OpenApi.Property(value = String[].class)
  private Fields fields = FieldsParser.parse(DEFAULT_FIELDS_PARAM);
}
