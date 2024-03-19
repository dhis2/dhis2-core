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
package org.hisp.dhis.analytics.tei;

import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.params.IdentifiableKey;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * This class is a wrapper for all possible parameters related to a tei. All attributes present here
 * should be correctly typed and ready to be used by the service layers.
 *
 * @author maikel arabori
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class TeiQueryParams implements IdentifiableKey {
  private final TrackedEntityType trackedEntityType;

  private final CommonParams commonParams;

  /** Returns a unique key representing this query. This key is suitable for caching. */
  public String getKey() {
    QueryKey key = new QueryKey();

    key.addIgnoreNull("teiType", trackedEntityType);
    key.addIgnoreNull("paging", commonParams.getPagingParams().wrappedKey());
    key.addIgnoreNull("includeMetadataDetails", commonParams.isIncludeMetadataDetails());
    key.addIgnoreNull("displayProperty", commonParams.getDisplayProperty());
    key.addIgnoreNull(
        "userOrgUnit",
        commonParams.getUserOrgUnit().stream()
            .map(IdentifiableObject::getUid)
            .sorted()
            .collect(Collectors.joining(";")));
    key.addIgnoreNull("ouMode", commonParams.getOuMode());
    key.addIgnoreNull("dataIdScheme", commonParams.getDataIdScheme());
    key.addIgnoreNull("relativePeriodDate", commonParams.getRelativePeriodDate());
    key.addIgnoreNull("skipMeta", commonParams.isSkipMeta());
    key.addIgnoreNull("skipData", commonParams.isSkipData());
    key.addIgnoreNull("skipHeaders", commonParams.isSkipHeaders());
    key.addIgnoreNull("skipRounding", commonParams.isSkipRounding());
    key.addIgnoreNull("hierarchyMeta", commonParams.isHierarchyMeta());
    key.addIgnoreNull("showHierarchy", commonParams.isShowHierarchy());

    commonParams.getOrderParams().forEach(param -> key.add("ordering", param.wrappedKey()));
    commonParams
        .getDimensionIdentifiers()
        .forEach(dim -> key.add("dimensionIdentifiers", dim.wrappedKey()));
    commonParams.getPrograms().forEach(program -> key.add("programs", program.getUid()));
    commonParams.getHeaders().forEach(header -> key.addIgnoreNull("headers", header));

    return key.build();
  }
}
