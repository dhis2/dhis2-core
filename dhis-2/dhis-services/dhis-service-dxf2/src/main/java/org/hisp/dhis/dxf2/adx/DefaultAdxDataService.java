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
package org.hisp.dhis.dxf2.adx;

import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;

import com.google.common.collect.Sets;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetQueryParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.staxwax.factory.XMLFactory;
import org.hisp.staxwax.writer.XMLWriter;
import org.springframework.stereotype.Service;

/**
 * @author bobj
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.dxf2.AdxDataService")
public class DefaultAdxDataService implements AdxDataService {

  private final DataValueSetService dataValueSetService;
  private final DataValueService dataValueService;
  private final PeriodService periodService;
  private final IdentifiableObjectManager identifiableObjectManager;

  @Override
  public DataExportParams getFromUrl(DataValueSetQueryParams urlParams) {
    IdSchemes outputIdSchemes = urlParams.getOutputIdSchemes();
    outputIdSchemes.setDefaultIdScheme(IdScheme.CODE);

    DataExportParams params = new DataExportParams();

    if (!isEmpty(urlParams.getDataSet())) {
      params.getDataSets().addAll(getByUidOrCode(DataSet.class, urlParams.getDataSet()));
    }
    if (!isEmpty(urlParams.getDataElement())) {
      params
          .getDataElements()
          .addAll(getByUidOrCode(DataElement.class, urlParams.getDataElement()));
    }
    if (!isEmpty(urlParams.getPeriod())) {
      params
          .getPeriods()
          .addAll(periodService.reloadIsoPeriods(new ArrayList<>(urlParams.getPeriod())));
    } else if (urlParams.getStartDate() != null && urlParams.getEndDate() != null) {
      params.setStartDate(urlParams.getStartDate());
      params.setEndDate(urlParams.getEndDate());
    }

    if (!isEmpty(urlParams.getOrgUnit())) {
      params
          .getOrganisationUnits()
          .addAll(getByUidOrCode(OrganisationUnit.class, urlParams.getOrgUnit()));
    }

    if (!isEmpty(urlParams.getOrgUnitGroup())) {
      params
          .getOrganisationUnitGroups()
          .addAll(getByUidOrCode(OrganisationUnitGroup.class, urlParams.getOrgUnitGroup()));
    }

    if (!isEmpty(urlParams.getAttributeOptionCombo())) {
      params
          .getAttributeOptionCombos()
          .addAll(getByUidOrCode(CategoryOptionCombo.class, urlParams.getAttributeOptionCombo()));
    }

    params.setIncludeDescendants(urlParams.isChildren());
    params.setIncludeDeleted(urlParams.isIncludeDeleted());
    params.setLastUpdated(urlParams.getLastUpdated());
    params.setLastUpdatedDuration(urlParams.getLastUpdatedDuration());
    params.setLimit(urlParams.getLimit());
    params.setOutputIdSchemes(outputIdSchemes);

    return params;
  }

  @Override
  public void writeDataValueSet(DataExportParams params, OutputStream out) throws AdxException {
    dataValueSetService.decideAccess(params);
    dataValueSetService.validate(params);

    XMLWriter adxWriter = XMLFactory.getXMLWriter(out);

    adxWriter.openElement("adx");
    adxWriter.writeAttribute("xmlns", "urn:ihe:qrph:adx:2015");

    IdSchemes idSchemes = params.getOutputIdSchemes();
    IdScheme ouScheme = idSchemes.getOrgUnitIdScheme();
    IdScheme dsScheme = idSchemes.getDataSetIdScheme();
    IdScheme deScheme = idSchemes.getDataElementIdScheme();

    for (DataSet dataSet : params.getDataSets()) {
      AdxDataSetMetadata metadata = new AdxDataSetMetadata(dataSet, idSchemes);

      for (CategoryOptionCombo aoc : getAttributeOptionCombos(dataSet, params)) {
        Map<String, String> attributeDimensions =
            metadata.getExplodedCategoryAttributes(aoc.getId());

        for (OrganisationUnit orgUnit : params.getAllOrganisationUnits()) {
          Period currentPeriod = null;
          OrganisationUnit currentOrgUnit = null;

          DataExportParams queryParams =
              new DataExportParams()
                  .setDataElements(dataSet.getDataElements())
                  .setOrganisationUnits(Sets.newHashSet(orgUnit))
                  .setIncludeDescendants(params.isIncludeDescendants())
                  .setIncludeDeleted(params.isIncludeDeleted())
                  .setLastUpdated(params.getLastUpdated())
                  .setLastUpdatedDuration(params.getLastUpdatedDuration())
                  .setPeriods(params.getPeriods())
                  .setStartDate(params.getStartDate())
                  .setEndDate(params.getEndDate())
                  .setAttributeOptionCombos(Sets.newHashSet(aoc))
                  .setOrderByOrgUnitPath(true)
                  .setOrderByPeriod(true);

          for (DataValue dv : dataValueService.getDataValues(queryParams)) {
            if (!dv.getPeriod().equals(currentPeriod) || !dv.getSource().equals(currentOrgUnit)) {
              if (currentPeriod != null) {
                adxWriter.closeElement(); // GROUP
              }

              currentPeriod = dv.getPeriod();
              currentOrgUnit = dv.getSource();

              adxWriter.openElement("group");
              adxWriter.writeAttribute("dataSet", dataSet.getPropertyValue(dsScheme));
              adxWriter.writeAttribute("period", AdxPeriod.serialize(currentPeriod));
              adxWriter.writeAttribute("orgUnit", currentOrgUnit.getPropertyValue(ouScheme));

              for (Map.Entry<String, String> e : attributeDimensions.entrySet()) {
                adxWriter.writeAttribute(e.getKey(), e.getValue());
              }
            }
            adxWriter.openElement("dataValue");

            adxWriter.writeAttribute("dataElement", dv.getDataElement().getPropertyValue(deScheme));

            CategoryOptionCombo coc = dv.getCategoryOptionCombo();

            Map<String, String> categoryDimensions =
                metadata.getExplodedCategoryAttributes(coc.getId());

            for (Map.Entry<String, String> e : categoryDimensions.entrySet()) {
              adxWriter.writeAttribute(e.getKey(), e.getValue());
            }

            if (dv.getDataElement().getValueType().isNumeric()) {
              adxWriter.writeAttribute("value", dv.getValue());
            } else {
              adxWriter.writeAttribute("value", "0");
              adxWriter.openElement("annotation");
              adxWriter.writeCharacters(dv.getValue());
              adxWriter.closeElement(); // ANNOTATION
            }
            adxWriter.closeElement(); // DATAVALUE
          }

          if (currentPeriod != null) {
            adxWriter.closeElement(); // GROUP
          }
        }
      }
    }

    adxWriter.closeElement(); // ADX

    adxWriter.closeWriter();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private <T extends IdentifiableObject> List<T> getByUidOrCode(Class<T> clazz, Set<String> ids) {
    return ids.stream()
        .map(id -> getByUidOrCode(clazz, id))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private <T extends IdentifiableObject> T getByUidOrCode(Class<T> clazz, String id) {
    if (isValidUid(id)) {
      T object = identifiableObjectManager.get(clazz, id);

      if (object != null) {
        return object;
      }
    }

    return identifiableObjectManager.getByCode(clazz, id);
  }

  private Set<CategoryOptionCombo> getAttributeOptionCombos(
      DataSet dataSet, DataExportParams params) {
    Set<CategoryOptionCombo> aocs = dataSet.getCategoryCombo().getOptionCombos();

    if (params.hasAttributeOptionCombos()) {
      aocs = new HashSet<>(aocs);

      aocs.retainAll(params.getAttributeOptionCombos());
    }

    return aocs;
  }
}
