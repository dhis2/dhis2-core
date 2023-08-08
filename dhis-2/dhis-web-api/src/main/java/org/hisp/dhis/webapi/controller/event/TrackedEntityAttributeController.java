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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.reservedvalue.ReserveValueException;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.textpattern.TextPatternGenerationException;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags("tracker")
@Controller
@RequestMapping(value = TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT)
public class TrackedEntityAttributeController
    extends AbstractCrudController<TrackedEntityAttribute> {

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private TextPatternService textPatternService;

  @Autowired private ReservedValueService reservedValueService;

  @Autowired private ContextService context;

  @GetMapping(
      value = "/{id}/generateAndReserve",
      produces = {ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_JAVASCRIPT})
  @ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
  public @ResponseBody List<ReservedValue> generateAndReserveValues(
      @RequestParam(required = false, defaultValue = "1") Integer numberToReserve,
      @RequestParam(required = false, defaultValue = "60") Integer expiration,
      @PathVariable String id)
      throws WebMessageException {
    return reserve(id, numberToReserve, expiration);
  }

  /**
   * This method is legacy and will do the same as generateAndReserveValues, but with only 3 days
   * expiration. The use-case for this endpoint is to get a single id when filling in the form, so
   * we assume the form is submitted within 3 days. generateAndReserveValues is designed to account
   * for offline devices that need to reserve ids in batches for a longer period of time.
   *
   * @param id
   * @return The id generated
   * @throws WebMessageException
   */
  @GetMapping("/{id}/generate")
  @ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
  public @ResponseBody ReservedValue legacyQueryTrackedEntityInstancesJson(
      @PathVariable String id,
      @RequestParam(required = false, defaultValue = "3") Integer expiration)
      throws WebMessageException {
    return reserve(id, 1, expiration).get(0);
  }

  @GetMapping("/{id}/requiredValues")
  @ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
  public @ResponseBody Map<String, List<String>> getRequiredValues(@PathVariable String id)
      throws WebMessageException {
    TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttribute(id);

    return textPatternService.getRequiredValues(trackedEntityAttribute.getTextPattern());
  }

  // Helpers

  private List<ReservedValue> reserve(String id, int numberToReserve, int daysToLive)
      throws WebMessageException {
    if (numberToReserve > 1000 || numberToReserve < 1) {
      throw new WebMessageException(
          badRequest("You can only reserve between 1 and 1000 values in a single request."));
    }

    TrackedEntityAttribute attribute = getTrackedEntityAttribute(id);

    Map<String, List<String>> params = context.getParameterValuesMap();

    Map<String, String> values = getRequiredValues(attribute, params);

    Date expiration = DateUtils.addDays(new Date(), daysToLive);

    try {
      List<ReservedValue> result =
          reservedValueService.reserve(attribute, numberToReserve, values, expiration);

      if (result.isEmpty()) {
        throw new WebMessageException(
            conflict(
                "Unable to reserve id. This may indicate that there are too few available ids left."));
      }

      return result;
    } catch (ReserveValueException ex) {
      throw new WebMessageException(conflict(ex.getMessage()));
    } catch (TextPatternGenerationException ex) {
      throw new WebMessageException(error(ex.getMessage()));
    }
  }

  private Map<String, String> getRequiredValues(
      TrackedEntityAttribute attr, Map<String, List<String>> values) throws WebMessageException {
    List<String> requiredValues =
        textPatternService.getRequiredValues(attr.getTextPattern()).get("REQUIRED");

    Map<String, String> result =
        values.entrySet().stream()
            .filter((entry) -> requiredValues.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> entry.getValue().get(0)));

    requiredValues.removeAll(result.keySet());

    if (requiredValues.size() > 0) {
      throw new WebMessageException(
          conflict(
              "Missing required values: "
                  + StringUtils.collectionToCommaDelimitedString(requiredValues)));
    }

    return result;
  }

  @Override
  protected void forceFiltering(final WebOptions webOptions, final List<String> filters) {
    if (webOptions == null || !webOptions.isTrue("indexableOnly")) {
      return;
    }

    if (filters.stream().anyMatch(f -> f.startsWith("id:"))) {
      throw new IllegalArgumentException(
          "indexableOnly parameter cannot be set if a separate filter for id is specified");
    }

    Set<TrackedEntityAttribute> indexableTeas =
        trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes();

    StringBuilder sb = new StringBuilder("id:in:");
    sb.append(
        indexableTeas.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.joining(",", "[", "]")));

    filters.add(sb.toString());
  }

  private TrackedEntityAttribute getTrackedEntityAttribute(String id) throws WebMessageException {
    TrackedEntityAttribute trackedEntityAttribute =
        trackedEntityAttributeService.getTrackedEntityAttribute(id);

    if (trackedEntityAttribute == null) {
      throw new WebMessageException(notFound(TrackedEntityAttribute.class, id));
    }

    if (trackedEntityAttribute.getTextPattern() == null) {
      throw new WebMessageException(badRequest("Attribute does not contain pattern."));
    }

    return trackedEntityAttribute;
  }
}
