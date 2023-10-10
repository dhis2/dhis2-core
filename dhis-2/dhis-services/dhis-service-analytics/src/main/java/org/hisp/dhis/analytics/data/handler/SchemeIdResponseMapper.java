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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElementOperandIdSchemeMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemIdSchemeMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing methods that encapsulate the id schema mapping for
 * response elements based on given URL ID schemes.
 *
 * @author maikel arabori
 */
@Component
public class SchemeIdResponseMapper {
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
    Map<String, String> map =
        getDimensionItemIdSchemeMap(params.getAllDimensionItems(), params.getOutputIdScheme());

    // Apply general output ID scheme
    if (params.isGeneralOutputIdSchemeSet()) {
      applyIdSchemeMapping(params, map);
    }

    // Handle output format {@link OutputFormat#DATA_VALUE_SET}
    // Apply data element operand output ID scheme
    if (params.isOutputFormat(DATA_VALUE_SET)
        && params.isOutputDataElementIdSchemeSet()
        && !params.getDataElementOperands().isEmpty()) {
      applyDataElementOperandIdSchemeMapping(params, map);
    }

    // Apply data item output ID scheme
    if (params.isOutputDataItemIdSchemeSet()) {
      applyIdSchemeMapping(params.getDataElements(), map, params.getOutputDataItemIdScheme());
      applyIdSchemeMapping(params.getIndicators(), map, params.getOutputDataItemIdScheme());
      applyIdSchemeMapping(params.getProgramIndicators(), map, params.getOutputDataItemIdScheme());
    }

    // Apply data element output ID scheme
    if (params.isOutputDataElementIdSchemeSet()) {
      applyIdSchemeMapping(params.getDataElements(), map, params.getOutputDataElementIdScheme());
    }

    // Apply organisation unit output ID scheme
    if (params.isOutputOrgUnitIdSchemeSet()) {
      applyIdSchemeMapping(params.getOrganisationUnits(), map, params.getOutputOrgUnitIdScheme());
    }

    return map;
  }

  /**
   * This method will map the respective element UID's with their respective ID scheme set. The
   * 'outputIdScheme' is considered the most general ID scheme parameter. If set, it will map the
   * scheme id set to all dimension items.
   *
   * <p>The other two ID scheme parameters supported ('outputDataElementIdScheme' and
   * 'outputOrgUnitIdScheme') will allow fine-grained id scheme definitions on top of the general
   * 'outputIdScheme'. If they are set, they will override the 'outputIdScheme' definition.
   *
   * @param params the {@link CommonParams} where the identifier scheme options are defined. The
   *     supported URL parameters are outputIdScheme, outputDataElementIdScheme and
   *     outputOrgUnitIdScheme.
   * @return a map of UID and mapping value.
   */
  public Map<String, String> getSchemeIdResponseMap(CommonParams params) {
    Map<String, String> map =
        getDimensionItemIdSchemeMap(
            params.delegate().getAllDimensionalItemObjects(), params.getOutputIdScheme());

    // Apply general output ID scheme
    if (params.isGeneralOutputIdSchemeSet()) {
      applyIdSchemeMapping(params, map);
    }

    List<DimensionalItemObject> dataElements = params.delegate().getAllDataElements();

    if (isNotEmpty(dataElements)) {
      // Apply data element output ID scheme
      applyIdSchemeMapping(dataElements, map, params.getOutputDataElementIdScheme());
    }

    List<DimensionalItemObject> orgUnits = params.delegate().getOrgUnitDimensionOrFilterItems();

    // Apply organisation unit output ID scheme
    if (params.isOutputOrgUnitIdSchemeSet() && isNotEmpty(orgUnits)) {
      applyIdSchemeMapping(orgUnits, map, params.getOutputOrgUnitIdScheme());
    }

    return map;
  }

  /**
   * Substitutes the metadata of the grid with the identifier scheme metadata property indicated in
   * the query. This happens only when a custom identifier scheme is specified.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid}.
   */
  public void applyCustomIdScheme(EventQueryParams params, Grid grid) {
    if (!params.isSkipMeta() && params.hasCustomIdSchemeSet()) {
      grid.substituteMetaData(getSchemeIdResponseMap(params));
    }
  }

  /**
   * Substitutes the metadata of the grid with the identifier scheme metadata property indicated in
   * the query. This happens only when a custom identifier scheme is specified.
   *
   * @param params the {@link CommonParams}.
   * @param grid the {@link Grid}.
   */
  public void applyCustomIdScheme(CommonParams params, Grid grid) {
    if (!params.isSkipMeta() && params.hasCustomIdSchemaSet()) {
      grid.substituteMetaData(getSchemeIdResponseMap(params));
    }
  }

  /**
   * Substitutes the metadata in the given grid. The replacement will only be done if the grid
   * header has option set or legend set.
   *
   * @param grid the {@link Grid}.
   * @param idScheme the {@link IdScheme}.
   */
  public void applyOptionAndLegendSetMapping(Grid grid, IdScheme idScheme) {
    if (idScheme != null) {
      for (int i = 0; i < grid.getHeaders().size(); i++) {
        GridHeader header = grid.getHeaders().get(i);

        if (header.hasOptionSet()) {
          Map<String, String> optionMap =
              header.getOptionSetObject().getOptionCodePropertyMap(idScheme);
          grid.substituteMetaData(i, i, optionMap);
        } else if (header.hasLegendSet()) {
          Map<String, String> legendMap =
              header.getLegendSetObject().getLegendUidPropertyMap(idScheme);
          grid.substituteMetaData(i, i, legendMap);
        }
      }
    }
  }

  /**
   * Adds mapping entries from the UID to the property specified by the output identifier scheme to
   * the given map. The included entities are programs, program stages and options.
   *
   * @param params the {@link CommonParams}.
   * @param map the map to add entries.
   */
  private void applyIdSchemeMapping(CommonParams params, Map<String, String> map) {
    if (isNotEmpty(params.getPrograms())) {
      for (Program program : params.getPrograms()) {
        map.put(program.getUid(), program.getDisplayPropertyValue(params.getOutputIdScheme()));
      }
    }

    if (isNotEmpty(params.delegate().getProgramStages())) {
      for (ProgramStage stage : params.delegate().getProgramStages()) {
        map.put(stage.getUid(), stage.getDisplayPropertyValue(params.getOutputIdScheme()));
      }
    }

    if (isNotEmpty(params.delegate().getItemsOptions())) {
      Set<Option> options = params.delegate().getItemsOptions();

      for (Option option : options) {
        map.put(option.getCode(), option.getDisplayPropertyValue(params.getOutputIdScheme()));
      }
    }
  }

  /**
   * Adds mapping entries from the UID to the property specified by the output identifier scheme to
   * the given map. The included entities are data element operands, programs, program stages and
   * options.
   *
   * @param params the {@link DataQueryParams}.
   * @param map the map to add entries.
   */
  private void applyIdSchemeMapping(DataQueryParams params, Map<String, String> map) {
    map.putAll(
        getDataElementOperandIdSchemeMap(
            asTypedList(params.getDataElementOperands()), params.getOutputIdScheme()));

    if (params.hasProgram()) {
      map.put(
          params.getProgram().getUid(),
          params.getProgram().getDisplayPropertyValue(params.getOutputIdScheme()));
    }

    if (params.hasProgramStage()) {
      map.put(
          params.getProgramStage().getUid(),
          params.getProgramStage().getDisplayPropertyValue(params.getOutputIdScheme()));
    }

    if (params instanceof EventQueryParams
        && isNotEmpty(((EventQueryParams) params).getItemOptions())) {
      Set<Option> options = ((EventQueryParams) params).getItemOptions();

      for (Option option : options) {
        map.put(option.getCode(), option.getDisplayPropertyValue(params.getOutputIdScheme()));
      }
    }
  }

  private void applyDataElementOperandIdSchemeMapping(
      DataQueryParams params, Map<String, String> map) {
    map.putAll(
        getDataElementOperandIdSchemeMap(
            asTypedList(params.getDataElementOperands()), params.getOutputDataElementIdScheme()));
  }

  /**
   * Adds the entries to the given map.
   *
   * @param dimensionalItemObjects the list of {@link DimensionalItemObject}.
   * @param map the map.
   * @param outputIdScheme the output {@link IdScheme}.
   */
  private void applyIdSchemeMapping(
      List<DimensionalItemObject> dimensionalItemObjects,
      Map<String, String> map,
      IdScheme outputIdScheme) {
    if (!dimensionalItemObjects.isEmpty()) {
      map.putAll(getDimensionItemIdSchemeMap(asTypedList(dimensionalItemObjects), outputIdScheme));
    }
  }
}
