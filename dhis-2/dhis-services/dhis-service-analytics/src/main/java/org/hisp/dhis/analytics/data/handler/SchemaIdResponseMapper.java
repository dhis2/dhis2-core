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
package org.hisp.dhis.analytics.data.handler;

import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElementOperandIdSchemeMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemIdSchemeMap;

import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.option.Option;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for encapsulating the id schema mapping for the response elements
 * based on the given URL ID schemes.
 *
 * @author maikel arabori
 */
@Component
public class SchemaIdResponseMapper {
  /**
   * This method will map the respective element UID's with their respective ID scheme set. The
   * 'outputIdScheme' is considered the most general ID scheme parameter. If set, it will map the
   * scheme id set to all dimension items.
   *
   * <p>The other two ID scheme parameters supported ('outputDataElementIdScheme' and
   * 'outputOrgUnitIdScheme') will allow fine-grained id scheme definitions on top of the general
   * 'outputIdScheme'. If they are set, they will override the 'outputIdScheme' definition.
   *
   * @param params the {@link DataQueryParams} where the identifier scheme options are defined. The
   *     supported URL parameters are outputIdScheme, outputDataElementIdScheme and
   *     outputOrgUnitIdScheme.
   * @return a map of UID and mapping value.
   */
  public Map<String, String> getSchemeIdResponseMap(DataQueryParams params) {
    Map<String, String> responseMap =
        getDimensionItemIdSchemeMap(params.getAllDimensionItems(), params.getOutputIdScheme());

    if (params.isGeneralOutputIdSchemeSet()) {
      // Apply an ID scheme to all data element operands using the general
      // output ID scheme defined
      applyIdSchemeMapping(params, responseMap);
    }

    // This section overrides the general ID scheme, so it can be
    // fine-grained
    if (params.isOutputFormat(DATA_VALUE_SET)) {
      if (params.isOutputDataElementIdSchemeSet()) {
        if (!params.getDataElementOperands().isEmpty()) {
          // Replace all data elements operands respecting their ID
          // scheme definition
          applyDataElementOperandIdSchemeMapping(params, responseMap);
        } else if (!params.getDataElements().isEmpty()) {
          // Replace all data elements respecting their ID scheme
          // definition
          applyDataElementsIdSchemeMapping(params, responseMap);
        }
      }
    }

    // If "outputOrgUnitIdScheme" is set, we replace all org units
    // values respecting it's definition
    if (params.isOutputOrgUnitIdSchemeSet()) {
      applyOrgUnitIdSchemeMapping(params, responseMap);
    }

    return responseMap;
  }

  private void applyIdSchemeMapping(DataQueryParams params, Map<String, String> map) {
    map.putAll(
        getDataElementOperandIdSchemeMap(
            asTypedList(params.getDataElementOperands()), params.getOutputIdScheme()));

    if (params.hasProgramStage()) {
      map.put(
          params.getProgramStage().getUid(),
          params.getProgramStage().getPropertyValue(params.getOutputIdScheme()));
    }

    if (params.hasProgram()) {
      map.put(
          params.getProgram().getUid(),
          params.getProgram().getPropertyValue(params.getOutputIdScheme()));
    }

    if (params instanceof EventQueryParams
        && CollectionUtils.isNotEmpty(((EventQueryParams) params).getItemOptions())) {
      Set<Option> options = ((EventQueryParams) params).getItemOptions();

      for (Option option : options) {
        map.put(option.getCode(), option.getPropertyValue(params.getOutputIdScheme()));
      }
    }
  }

  private void applyDataElementOperandIdSchemeMapping(
      DataQueryParams params, Map<String, String> map) {
    map.putAll(
        getDataElementOperandIdSchemeMap(
            asTypedList(params.getDataElementOperands()), params.getOutputDataElementIdScheme()));
  }

  private void applyDataElementsIdSchemeMapping(DataQueryParams params, Map<String, String> map) {
    map.putAll(
        getDimensionItemIdSchemeMap(
            asTypedList(params.getDataElements()), params.getOutputDataElementIdScheme()));
  }

  private void applyOrgUnitIdSchemeMapping(DataQueryParams params, Map<String, String> map) {
    map.putAll(
        getDimensionItemIdSchemeMap(
            asTypedList(params.getOrganisationUnits()), params.getOutputOrgUnitIdScheme()));
  }
}
