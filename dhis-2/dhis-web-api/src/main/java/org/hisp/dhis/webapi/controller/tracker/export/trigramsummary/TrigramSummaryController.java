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
package org.hisp.dhis.webapi.controller.tracker.export.trigramsummary;

import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Trigram Summary endpoint to get a summary of all the trigram indexes and indexable attributes
 *
 * @author Ameen Mohamed
 */
@OpenApi.Document(
    entity = TrackedEntityAttribute.class,
    classifiers = {"team:tracker", "purpose:metadata"})
@Controller
@RequestMapping("/api/trigramSummary")
@AllArgsConstructor
public class TrigramSummaryController {
  public static final String DEFAULT_FIELDS_PARAM = "id,displayName";

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

  protected final ContextService contextService;

  protected final AclService aclService;

  private final FieldFilterService fieldFilterService;

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody TrigramSummary getTrigramSummary(
      @RequestParam Map<String, String> rpParameters,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields) {
    TrigramSummary trigramSummary = new TrigramSummary();

    Set<TrackedEntityAttribute> allIndexableAttributes =
        trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes();

    Set<String> allIndexableAttributeUids =
        allIndexableAttributes.stream()
            .map(TrackedEntityAttribute::getUid)
            .collect(Collectors.toSet());

    List<Long> indexedAttributeIds =
        trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndex();

    List<TrackedEntityAttribute> allIndexedAttributes;

    List<TrackedEntityAttribute> indexedAttributes = new ArrayList<>();

    Set<TrackedEntityAttribute> indexableAttributes = new HashSet<>(allIndexableAttributes);
    List<TrackedEntityAttribute> obsoleteIndexedAttributes = new ArrayList<>();

    if (!indexedAttributeIds.isEmpty()) {
      allIndexedAttributes =
          trackedEntityAttributeService.getTrackedEntityAttributesById(indexedAttributeIds);

      for (TrackedEntityAttribute indexedAttribute : allIndexedAttributes) {
        if (!allIndexableAttributeUids.contains(indexedAttribute.getUid())) {
          obsoleteIndexedAttributes.add(indexedAttribute);
        } else {
          indexedAttributes.add(indexedAttribute);
        }
      }

      indexableAttributes.removeAll(allIndexedAttributes);
    }

    trigramSummary.setIndexedAttributes(
        fieldFilterService.toObjectNodes(indexedAttributes, fields));
    trigramSummary.setObsoleteIndexedAttributes(
        fieldFilterService.toObjectNodes(obsoleteIndexedAttributes, fields));
    trigramSummary.setIndexableAttributes(
        fieldFilterService.toObjectNodes(new ArrayList<>(indexableAttributes), fields));

    return trigramSummary;
  }
}
