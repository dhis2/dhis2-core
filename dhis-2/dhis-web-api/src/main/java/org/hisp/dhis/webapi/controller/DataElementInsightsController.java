/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementInsightsService;
import org.hisp.dhis.dataelement.DataElementInsightsSummary;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints exposing aggregated insights about data elements.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
@RestController
@RequestMapping("/api/dataElementInsights")
@RequiredArgsConstructor
public class DataElementInsightsController {

  private final DataElementInsightsService insightsService;

  private final DataElementStore dataElementStore;

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public DataElementInsightsSummary getInsights() {
    return insightsService.getSummary();
  }

  @GetMapping(value = "/summary", produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> getLegacySummary() {
    List<DataElement> elements = dataElementStore.getAll();
    Map<String, Object> response = new HashMap<>();
    response.put("total", elements.size());
    Map<String, Integer> byDomain = new HashMap<>();
    for (DataElement element : elements) {
      String key = element.getDomainType() == null ? "unknown" : element.getDomainType().name();
      byDomain.merge(key, 1, Integer::sum);
    }
    response.put("byDomain", byDomain);
    response.put(
        "aggregatable",
        elements.stream().filter(element -> element.getValueType().isAggregatable()).count());
    return response;
  }

  @GetMapping(value = "/details", produces = APPLICATION_JSON_VALUE)
  public List<Map<String, Object>> getDetails(@RequestParam String de) {
    List<DataElement> elements = insightsService.resolveDataElements(Arrays.asList(de.split(",")));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (DataElement element : elements) {
      Map<String, Object> row = new HashMap<>();
      row.put("id", element.getUid());
      row.put("name", element.getName());
      row.put("valueType", element.getValueType().name());
      rows.add(row);
    }
    return rows;
  }

  @GetMapping(value = "/count", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public Map<String, Long> countByName(@RequestParam String nameFilter) {
    return Map.of("count", insightsService.countByNameFilter(nameFilter));
  }

  @PostMapping("/snapshot")
  public WebMessage createSnapshot() {
    try {
      insightsService.saveSnapshot();
      return ok("Insights snapshot created");
    } catch (Exception ex) {
      return conflict(ex.getMessage());
    }
  }

  @PutMapping("/comment")
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public WebMessage updateComment(@RequestParam UID de, @RequestParam String comment) {
    insightsService.updateInsightsComment(de, comment);
    return ok("Comment updated");
  }
}
