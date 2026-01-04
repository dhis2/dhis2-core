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
package org.hisp.dhis.analytics.data.handler;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElementOperandIdSchemeMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemIdSchemeMap;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.ValueType.BOOLEAN;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Data;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * This component iws responsible for providing methods that encapsulate the id schema mapping for
 * response elements based on given URL ID schemes.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class SchemeIdResponseMapper {
  @Nonnull private final I18nManager i18nManager;

  /**
   * This method will map the respective element UID's with their respective ID scheme set. The
   * 'outputIdScheme' is considered the most general ID scheme parameter. If set, it will map the
   * scheme id set to all dimension items.
   *
   * <p>The other two ID scheme parameters supported ('outputDataElementIdScheme' and
   * 'outputOrgUnitIdScheme') will allow fine-grained id scheme definitions on top of the general
   * 'outputIdScheme'. If they are set, they will override the 'outputIdScheme' definition.
   *
   * @param schemeInfo the {@link SchemeInfo} where the scheme settings and objects are defined.
   * @return a map of UIDs and their respective scheme value.
   */
  public Map<String, String> getSchemeIdResponseMap(SchemeInfo schemeInfo) {
    Data schemeData = schemeInfo.data();
    Settings schemeSettings = schemeInfo.settings();
    IdScheme idScheme =
        firstNonNull(schemeSettings.getOutputIdScheme(), schemeSettings.getDataIdScheme());

    Map<String, String> map =
        getDimensionItemIdSchemeMap(schemeData.getDimensionalItemObjects(), idScheme);

    // Apply general output ID scheme.
    if (schemeSettings.isGeneralOutputIdSchemeSet() || schemeSettings.isDataIdSchemeSet()) {
      applyIdSchemeMapping(schemeInfo, map);
    }

    // Handle output format {@link OutputFormat#DATA_VALUE_SET}.
    // Apply data element operand output ID scheme.
    if (schemeSettings.isOutputFormat(DATA_VALUE_SET)
        && schemeSettings.isOutputDataElementIdSchemeSet()
        && isNotEmpty(schemeData.getDataElementOperands())) {
      applyDataElementOperandIdSchemeMapping(schemeInfo, map);
    }

    // Apply data item output ID scheme.
    if (schemeSettings.isOutputDataItemIdSchemeSet()) {
      applyIdSchemeMapping(
          schemeSettings.getOutputDataItemIdScheme(), schemeData.getDataElements(), map);
      applyIdSchemeMapping(
          schemeSettings.getOutputDataItemIdScheme(), schemeData.getIndicators(), map);
      applyIdSchemeMapping(
          schemeSettings.getOutputDataItemIdScheme(), schemeData.getProgramIndicators(), map);
    }

    // Apply data element output ID scheme.
    if (schemeSettings.isOutputDataElementIdSchemeSet()) {
      applyIdSchemeMapping(
          schemeSettings.getOutputDataElementIdScheme(), schemeData.getDataElements(), map);
    }

    // Apply organisation unit output ID scheme.
    if (schemeSettings.isOutputOrgUnitIdSchemeSet()) {
      applyIdSchemeMapping(
          schemeSettings.getOutputOrgUnitIdScheme(), schemeData.getOrganizationUnits(), map);
    }

    return map;
  }

  /**
   * Substitutes the metadata of the grid with the identifier scheme metadata property indicated in
   * the query. This happens only when a custom identifier scheme is specified.
   *
   * @param schemeInfo the {@link SchemeInfo}.
   * @param grid the {@link Grid}.
   */
  public void applyCustomIdScheme(SchemeInfo schemeInfo, Grid grid) {
    Settings schemeSettings = schemeInfo.settings();

    if (schemeSettings.hasCustomIdSchemeSet()) {
      grid.substituteMetaData(getSchemeIdResponseMap(schemeInfo));
    }
  }

  /**
   * Substitutes the metadata in the given grid. The replacement will only be done if the grid
   * header has option set or legend set.
   *
   * @param idScheme the {@link IdScheme}.
   * @param grid the {@link Grid}.
   */
  public void applyOptionAndLegendSetMapping(IdScheme idScheme, Grid grid) {
    if (idScheme != null) {
      for (int i = 0; i < grid.getHeaders().size(); i++) {
        GridHeader header = grid.getHeaders().get(i);

        if (header.hasOptionSet()) {
          Map<String, String> optionMap =
              getOptionCodePropertyMap(header.getOptionSetObject(), idScheme);
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
   * Substitutes the metadata in the given grid for boolean value types.
   *
   * @param idScheme the {@link IdScheme}.
   * @param grid the {@link Grid}.
   */
  public void applyBooleanMapping(IdScheme idScheme, Grid grid) {
    if (idScheme != null && idScheme == NAME) {
      for (int i = 0; i < grid.getHeaders().size(); i++) {
        GridHeader header = grid.getHeaders().get(i);

        if (header.hasValueType(BOOLEAN)) {
          Map<String, String> booleanPropertyMap = getBooleanPropertyMap();
          grid.substituteMetaData(i, i, booleanPropertyMap);
        }
      }
    }
  }

  private Map<String, String> getOptionCodePropertyMap(OptionSet set, IdScheme idScheme) {
    return set.getOptions().stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Option::getCode, o -> o.getDisplayPropertyValue(idScheme)));
  }

  private Map<String, String> getBooleanPropertyMap() {
    I18n i18n = i18nManager.getI18n();
    return Map.of("0", i18n.getString("no", "No"), "1", i18n.getString("yes", "Yes"));
  }

  /**
   * Adds mapping entries from the UID to the property specified by the output identifier scheme to
   * the given map. The included entities are programs, program stages and options.
   *
   * @param schemeInfo the {@link SchemeInfo}.
   * @param map the map to add entries.
   */
  private void applyIdSchemeMapping(SchemeInfo schemeInfo, Map<String, String> map) {
    Data schemeData = schemeInfo.data();
    Settings schemeSettings = schemeInfo.settings();
    IdScheme idScheme =
        firstNonNull(schemeSettings.getOutputIdScheme(), schemeSettings.getDataIdScheme());

    map.putAll(
        getDataElementOperandIdSchemeMap(
            asTypedList(schemeData.getDataElementOperands()), idScheme));

    if (schemeData.getProgram() != null) {
      map.put(
          schemeData.getProgram().getUid(),
          schemeData.getProgram().getDisplayPropertyValue(idScheme));
    }

    if (schemeData.getProgramStage() != null) {
      map.put(
          schemeData.getProgramStage().getUid(),
          schemeData.getProgramStage().getDisplayPropertyValue(idScheme));
    }

    if (isNotEmpty(schemeData.getOptions())) {
      Set<Option> options = schemeData.getOptions();

      for (Option option : options) {
        map.put(option.getCode(), option.getDisplayPropertyValue(idScheme));
      }
    }

    // Multi-programs.
    handleIdSchemeForMultiPrograms(map, schemeData, schemeSettings);
  }

  /**
   * This method handles specific attributes that are present only in multi programs requests.
   *
   * @param map the map of entries ids - scheme values.
   * @param schemeData the {@SchemeInfo.Data}.
   * @param schemeSettings the {@SchemeInfo.Settings}.
   */
  private static void handleIdSchemeForMultiPrograms(
      Map<String, String> map, Data schemeData, Settings schemeSettings) {
    if (isNotEmpty(schemeData.getPrograms())) {
      for (Program program : schemeData.getPrograms()) {
        map.put(
            program.getUid(), program.getDisplayPropertyValue(schemeSettings.getOutputIdScheme()));
      }
    }

    if (isNotEmpty(schemeData.getProgramStages())) {
      for (ProgramStage stage : schemeData.getProgramStages()) {
        map.put(stage.getUid(), stage.getDisplayPropertyValue(schemeSettings.getOutputIdScheme()));
      }
    }
  }

  private void applyDataElementOperandIdSchemeMapping(
      SchemeInfo schemeInfo, Map<String, String> map) {
    Data schemeData = schemeInfo.data();
    Settings schemeSettings = schemeInfo.settings();

    map.putAll(
        getDataElementOperandIdSchemeMap(
            asTypedList(schemeData.getDataElementOperands()),
            schemeSettings.getOutputDataElementIdScheme()));
  }

  /**
   * Adds the entries to the given map.
   *
   * @param outputIdScheme the output {@link IdScheme}.
   * @param dimensionalItemObjects the list of {@link DimensionalItemObject}.
   * @param map the map.
   */
  private void applyIdSchemeMapping(
      IdScheme outputIdScheme,
      List<DimensionalItemObject> dimensionalItemObjects,
      Map<String, String> map) {
    if (isNotEmpty(dimensionalItemObjects)) {
      map.putAll(getDimensionItemIdSchemeMap(asTypedList(dimensionalItemObjects), outputIdScheme));
    }
  }
}
