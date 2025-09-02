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
package org.hisp.dhis.dxf2.datavalueset;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportStoreParams;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.dxf2.adx.AdxPeriod;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.hisp.staxwax.factory.XMLFactory;
import org.hisp.staxwax.writer.XMLWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that a mock BatchHandler factory is being injected.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDataExportService implements DataExportService {

  private final IdentifiableObjectManager identifiableObjectManager;

  private final CategoryService categoryService;

  private final PeriodService periodService;

  private final CompleteDataSetRegistrationService registrationService;

  private final DataValueStore
      dataValueStore; // TODO remove later when load methods have been moved to DataExportStore
  private final DataExportStore dataExportStore;

  private final InputUtils inputUtils;

  private final AclService aclService;

  public DataExportStoreParams decodeParamsAdx(DataExportParams urlParams) {
    IdSchemes outputIdSchemes = urlParams.getOutputIdSchemes();
    outputIdSchemes.setDefaultIdScheme(IdScheme.CODE);

    DataExportStoreParams params = new DataExportStoreParams();

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

  private DataExportStoreParams decodeParams(DataExportParams urlParams) {
    DataExportStoreParams params = new DataExportStoreParams();
    IdSchemes inputIdSchemes = urlParams.getInputIdSchemes();

    if (!isEmpty(urlParams.getDataSet())) {
      params
          .getDataSets()
          .addAll(
              identifiableObjectManager.getObjects(
                  DataSet.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getDataSetIdScheme),
                  urlParams.getDataSet()));
    }

    if (!isEmpty(urlParams.getDataElementGroup())) {
      params
          .getDataElementGroups()
          .addAll(
              identifiableObjectManager.getObjects(
                  DataElementGroup.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getDataElementGroupIdScheme),
                  urlParams.getDataElementGroup()));
    }

    if (!isEmpty(urlParams.getDataElement())) {
      params
          .getDataElements()
          .addAll(
              identifiableObjectManager.getObjects(
                  DataElement.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getDataElementIdScheme),
                  urlParams.getDataElement()));
    }

    if (!isEmpty(urlParams.getPeriod())) {
      params
          .getPeriods()
          .addAll(periodService.reloadIsoPeriods(new ArrayList<>(urlParams.getPeriod())));
    } else if (urlParams.getStartDate() != null && urlParams.getEndDate() != null) {
      params.setStartDate(urlParams.getStartDate()).setEndDate(urlParams.getEndDate());
    }

    if (!isEmpty(urlParams.getOrgUnit())) {
      params
          .getOrganisationUnits()
          .addAll(
              identifiableObjectManager.getObjects(
                  OrganisationUnit.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getOrgUnitIdScheme),
                  urlParams.getOrgUnit()));
    }

    if (!isEmpty(urlParams.getOrgUnitGroup())) {
      params
          .getOrganisationUnitGroups()
          .addAll(
              identifiableObjectManager.getObjects(
                  OrganisationUnitGroup.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getOrgUnitGroupIdScheme),
                  urlParams.getOrgUnitGroup()));
    }

    if (!isEmpty(urlParams.getAttributeOptionCombo())) {
      params
          .getAttributeOptionCombos()
          .addAll(
              identifiableObjectManager.getObjects(
                  CategoryOptionCombo.class,
                  IdentifiableProperty.in(
                      inputIdSchemes, IdSchemes::getAttributeOptionComboIdScheme),
                  urlParams.getAttributeOptionCombo()));
    } else if (urlParams.getAttributeCombo() != null && !isEmpty(urlParams.getAttributeOptions())) {
      params
          .getAttributeOptionCombos()
          .addAll(
              Lists.newArrayList(
                  inputUtils.getAttributeOptionCombo(
                      urlParams.getAttributeCombo(), urlParams.getAttributeOptions())));
    }

    return params
        .setIncludeDescendants(urlParams.isChildren())
        .setIncludeDeleted(urlParams.isIncludeDeleted())
        .setLastUpdated(urlParams.getLastUpdated())
        .setLastUpdatedDuration(urlParams.getLastUpdatedDuration())
        .setLimit(urlParams.getLimit())
        .setOutputIdSchemes(urlParams.getOutputIdSchemes());
  }

  public void validateParams(DataExportStoreParams params) throws ConflictException {
    if (params == null) throw new ConflictException(ErrorCode.E2000);

    if (!params.hasDataElements() && !params.hasDataSets() && !params.hasDataElementGroups())
      throw new ConflictException(ErrorCode.E2001);

    if (!params.hasPeriods()
        && !params.hasStartEndDate()
        && !params.hasLastUpdated()
        && !params.hasLastUpdatedDuration()) throw new ConflictException(ErrorCode.E2002);

    if (params.hasPeriods() && params.hasStartEndDate())
      throw new ConflictException(ErrorCode.E2003);

    if (params.hasStartEndDate() && params.getStartDate().after(params.getEndDate()))
      throw new ConflictException(ErrorCode.E2004);

    if (params.hasLastUpdatedDuration()
        && DateUtils.getDuration(params.getLastUpdatedDuration()) == null)
      throw new ConflictException(ErrorCode.E2005);

    if (!params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups())
      throw new ConflictException(ErrorCode.E2006);

    if (params.isIncludeDescendants() && params.hasOrganisationUnitGroups())
      throw new ConflictException(ErrorCode.E2007);

    if (params.isIncludeDescendants() && !params.hasOrganisationUnits())
      throw new ConflictException(ErrorCode.E2008);

    if (params.hasLimit() && params.getLimit() < 0)
      throw new ConflictException(ErrorCode.E2009, params.getLimit());
  }

  private void validateAccess(DataExportStoreParams params) throws ConflictException {
    // Verify data set read sharing
    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    for (DataSet dataSet : params.getDataSets()) {
      if (!aclService.canDataRead(currentUserDetails, dataSet)) {
        throw new ConflictException(ErrorCode.E2010, dataSet.getUid());
      }
    }

    // Verify attribute option combination data read sharing
    for (CategoryOptionCombo optionCombo : params.getAttributeOptionCombos()) {
      if (!aclService.canDataRead(currentUserDetails, optionCombo)) {
        throw new ConflictException(ErrorCode.E2011, optionCombo.getUid());
      }
    }

    // Verify org unit being located within user data capture hierarchy
    for (OrganisationUnit unit : params.getOrganisationUnits()) {
      if (!currentUserDetails.isInUserHierarchy(unit.getPath())) {
        throw new ConflictException(ErrorCode.E2012, unit.getUid());
      }
    }
  }

  // -------------------------------------------------------------------------
  // Write
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void exportDataValueSetXml(DataExportParams params, OutputStream out)
      throws ConflictException {
    DataExportStoreParams storeParams = decodeParams(params);
    validateParams(storeParams);
    validateAccess(storeParams);

    dataExportStore.exportDataValueSetXml(storeParams, getCompleteDate(storeParams), out);
  }

  @Override
  @Transactional
  public void exportDataValueSetJson(DataExportParams params, OutputStream out)
      throws ConflictException {
    DataExportStoreParams storeParams = decodeParams(params);
    validateParams(storeParams);
    validateAccess(storeParams);

    List<DataExportValue> values = dataValueStore.getDataValues(storeParams);

    dataExportStore.exportDataValueSetJson(storeParams, getCompleteDate(storeParams), out);
  }

  @Override
  @Transactional
  public void exportDataValueSetJson(
      Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes) {
    // Note: Used in sync so no validation!
    DataExportStoreParams params =
        new DataExportStoreParams().setLastUpdated(lastUpdated).setOrderForSync(true);

    List<DataExportValue> values = dataValueStore.getDataValues(params);
    // TODO write as JSON
  }

  @Override
  @Transactional
  public void exportDataValueSetJson(
      Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes, int pageSize, int page) {
    // Note: Used in sync so no validation!
    DataExportStoreParams params =
        new DataExportStoreParams()
            .setLastUpdated(lastUpdated)
            .setLimit(pageSize)
            .setOffset((page - 1) * pageSize)
            .setOrderForSync(true);

    List<DataExportValue> values = dataValueStore.getDataValues(params);
    // TODO write as JSON
  }

  @Override
  @Transactional
  public void exportDataValueSetCsv(DataExportParams params, OutputStream out)
      throws ConflictException {
    DataExportStoreParams storeParams = decodeParams(params);
    validateParams(storeParams);
    validateAccess(storeParams);

    dataExportStore.exportDataValueSetCsv(
        storeParams, getCompleteDate(storeParams), new PrintWriter(out));
  }

  @Override
  @Transactional
  public void exportDataValueSetXmlAdx(DataExportParams params, OutputStream out)
      throws ConflictException {
    DataExportStoreParams storeParams = decodeParamsAdx(params);
    validateParams(storeParams);
    validateAccess(storeParams);

    // ADX fetches values in ADX groups...
    writeDataValuesAdx(out, storeParams);
  }

  private void writeDataValuesAdx(OutputStream out, DataExportStoreParams params)
      throws ConflictException {
    XMLWriter adxWriter = XMLFactory.getXMLWriter(out);

    adxWriter.openElement("adx");
    adxWriter.writeAttribute("xmlns", "urn:ihe:qrph:adx:2015");

    IdSchemes idSchemes = params.getOutputIdSchemes();
    IdScheme ouScheme = idSchemes.getOrgUnitIdScheme();
    IdScheme dsScheme = idSchemes.getDataSetIdScheme();
    IdScheme deScheme = idSchemes.getDataElementIdScheme();

    Set<OrganisationUnit> units = params.getAllOrganisationUnits();

    for (DataSet dataSet : params.getDataSets()) {
      AdxDataSetMetadata metadata = new AdxDataSetMetadata(dataSet, idSchemes);

      for (CategoryOptionCombo aoc : getAttributeOptionCombos(dataSet, params)) {
        Map<String, String> attributeDimensions =
            metadata.getExplodedCategoryAttributes(aoc.getUid());

        for (OrganisationUnit orgUnit : units) {
          String currentPeriod = null;
          UID currentOrgUnit = null;

          Set<DataElement> dsDataElements = dataSet.getDataElements();
          DataExportStoreParams groupParams =
              new DataExportStoreParams()
                  .setDataElements(dsDataElements)
                  .setOrganisationUnits(Set.of(orgUnit))
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

          List<DataExportValue> dataValues = dataValueStore.getDataValues(groupParams);
          Set<UID> numericDataElements =
              dsDataElements.stream()
                  .filter(de -> de.getValueType().isNumeric())
                  .map(UID::of)
                  .collect(toSet());
          Map<String, String> dataElementSchemaId =
              dsDataElements.stream()
                  .collect(toMap(IdentifiableObject::getUid, de -> de.getPropertyValue(deScheme)));
          for (DataExportValue dv : dataValues) {
            if (!dv.period().equals(currentPeriod) || !dv.orgUnit().equals(currentOrgUnit)) {
              if (currentPeriod != null) {
                adxWriter.closeElement(); // GROUP
              }

              currentPeriod = dv.period();
              currentOrgUnit = dv.orgUnit();

              adxWriter.openElement("group");
              adxWriter.writeAttribute("dataSet", dataSet.getPropertyValue(dsScheme));
              adxWriter.writeAttribute(
                  "period", AdxPeriod.serialize(PeriodType.getPeriodFromIsoString(currentPeriod)));
              adxWriter.writeAttribute("orgUnit", orgUnit.getPropertyValue(ouScheme));

              for (Map.Entry<String, String> e : attributeDimensions.entrySet()) {
                adxWriter.writeAttribute(e.getKey(), e.getValue());
              }
            }
            adxWriter.openElement("dataValue");

            adxWriter.writeAttribute(
                "dataElement", dataElementSchemaId.get(dv.dataElement().getValue()));

            Map<String, String> categoryDimensions =
                metadata.getExplodedCategoryAttributes(dv.categoryOptionCombo().getValue());

            for (Map.Entry<String, String> e : categoryDimensions.entrySet()) {
              adxWriter.writeAttribute(e.getKey(), e.getValue());
            }

            if (numericDataElements.contains(dv.dataElement())) {
              adxWriter.writeAttribute("value", dv.value());
            } else {
              adxWriter.writeAttribute("value", "0");
              adxWriter.openElement("annotation");
              adxWriter.writeCharacters(dv.value());
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

  private Date getCompleteDate(DataExportStoreParams params) {
    if (params.isSingleDataValueSet()) {
      // FIXME only "works" for default COC
      CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo(); // TODO

      CompleteDataSetRegistration registration =
          registrationService.getCompleteDataSetRegistration(
              params.getFirstDataSet(),
              params.getFirstPeriod(),
              params.getFirstOrganisationUnit(),
              optionCombo);

      return registration != null ? registration.getDate() : null;
    }

    return null;
  }

  private Set<CategoryOptionCombo> getAttributeOptionCombos(
      DataSet dataSet, DataExportStoreParams params) {
    Set<CategoryOptionCombo> aocs = dataSet.getCategoryCombo().getOptionCombos();

    if (params.hasAttributeOptionCombos()) {
      aocs = new HashSet<>(aocs);

      aocs.retainAll(params.getAttributeOptionCombos());
    }

    return aocs;
  }
}
