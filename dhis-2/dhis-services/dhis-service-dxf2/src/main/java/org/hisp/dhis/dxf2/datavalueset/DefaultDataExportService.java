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
import static org.hisp.dhis.datavalue.DataExportStore.EncodeType.COC;
import static org.hisp.dhis.datavalue.DataExportStore.EncodeType.DE;
import static org.hisp.dhis.datavalue.DataExportStore.EncodeType.OU;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataExportGroup;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportService;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportStoreParams;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
  private final DataExportStore store;
  private final InputUtils inputUtils;
  private final AclService aclService;

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public DataExportValue exportValue(@Nonnull DataEntryKey key) throws ConflictException {
    // TODO access validation
    return store.getDataValue(key);
  }

  /**
   * @implNote since we return a stream whoever is calling must already be in a transaction because
   *     only then it is safe to process the stream in that transaction outside of this method. Once
   *     the transaction closes the stream becomes invalid.
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Stream<DataExportValue> exportValues(@Nonnull DataExportParams parameters)
      throws ConflictException {
    DataExportStoreParams params = decodeParams(parameters);
    validateFilters(params);
    validateAccess(params);
    return store.getDataValues(params);
  }

  @Override
  @Transactional(readOnly = true)
  public DataExportGroup.Output exportGroup(@Nonnull DataExportParams parameters, boolean sync)
      throws ConflictException {
    DataExportStoreParams params = decodeParams(parameters);
    if (sync) {
      params.setOrderForSync(true);
      params.setIncludeDeleted(true);
    } else {
      validateFilters(params);
      validateAccess(params);
    }

    IdSchemes schemes = parameters.getOutputIdSchemes();
    String groupDataSet = null;
    String groupPeriod = null;
    String groupOrgUnit = null;
    String groupAoc = null;
    if (params.isSingleDataValueSet()) {
      groupDataSet = params.getFirstDataSet().getPropertyValue(schemes.getDataSetIdScheme());
      groupPeriod = params.getFirstPeriod().getIsoDate();
      groupOrgUnit =
          params.getFirstOrganisationUnit().getPropertyValue(schemes.getOrgUnitIdScheme());
    }
    if (params.getAttributeOptionCombos().size() == 1) {
      groupAoc =
          params
              .getAttributeOptionCombos()
              .iterator()
              .next()
              .getPropertyValue(schemes.getAttributeOptionComboIdScheme());
    }
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            groupDataSet, groupPeriod, groupOrgUnit, groupAoc, null, Set.of(), Stream.empty());
    return group.withValues(encodeValues(group, store.getDataValues(params), schemes));
  }

  @Override
  @Transactional(readOnly = true)
  public Stream<DataExportGroup.Output> exportInGroups(@Nonnull DataExportParams parameters)
      throws ConflictException {
    DataExportStoreParams params = decodeParamsAdx(parameters);
    validateFilters(params);
    validateAccess(params);

    // TODO (JB) another PR: actual Stream processing of grouped values
    return listGroupsOfValues(params).stream();
  }

  private List<DataExportGroup.Output> listGroupsOfValues(DataExportStoreParams params)
      throws ConflictException {
    IdSchemes idSchemes = params.getOutputIdSchemes();
    IdScheme ouScheme = idSchemes.getOrgUnitIdScheme();
    IdScheme dsScheme = idSchemes.getDataSetIdScheme();
    IdScheme deScheme = idSchemes.getDataElementIdScheme();

    Set<OrganisationUnit> units = params.getAllOrganisationUnits();
    Map<UID, OrganisationUnit> unitsById = new HashMap<>();
    units.forEach(ou -> unitsById.put(UID.of(ou), ou));

    List<DataExportGroup.Output> groups = new ArrayList<>();
    for (DataSet dataSet : params.getDataSets()) {
      String groupDataSet = dataSet.getPropertyValue(dsScheme);
      // COC => C => CO
      Map<UID, Map<String, String>> categoryOptionMap =
          createCategoryOptionsMapping(dataSet, idSchemes);

      for (CategoryOptionCombo aoc : getAttributeOptionCombos(dataSet, params)) {
        Map<String, String> groupAttributeOptions = categoryOptionMap.get(UID.of(aoc));

        Set<DataElement> dsDataElements = dataSet.getDataElements();
        DataExportStoreParams groupParams =
            new DataExportStoreParams()
                .setDataElements(dsDataElements)
                .setOrganisationUnits(units)
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

        Set<UID> numericDataElements =
            dsDataElements.stream()
                .filter(de -> de.getValueType().isNumeric())
                .map(UID::of)
                .collect(toSet());
        Map<String, String> dataElementSchemaId =
            dsDataElements.stream()
                .collect(toMap(IdentifiableObject::getUid, de -> de.getPropertyValue(deScheme)));

        Set<String> numericUsedDataElements = new HashSet<>();
        List<DataExportValue.Output> groupValues = new ArrayList<>();
        String groupPeriod = null;
        UID currentOrgUnit = null;
        for (DataExportValue dv : store.getDataValues(groupParams).toList()) {
          if (!dv.period().equals(groupPeriod) || !dv.orgUnit().equals(currentOrgUnit)) {
            if (groupPeriod != null) {
              // add group (before starting next one)
              groups.add(
                  new DataExportGroup.Output(
                      groupDataSet,
                      groupPeriod,
                      unitsById.get(currentOrgUnit).getPropertyValue(ouScheme),
                      null,
                      groupAttributeOptions,
                      numericUsedDataElements,
                      groupValues.stream()));
              groupValues = new ArrayList<>();
              numericDataElements = new HashSet<>();
            }
            groupPeriod = dv.period();
            currentOrgUnit = dv.orgUnit();
          }
          String dataElement = dataElementSchemaId.get(dv.dataElement().getValue());
          if (numericDataElements.contains(dv.dataElement()))
            numericUsedDataElements.add(dataElement);
          groupValues.add(
              new DataExportValue.Output(
                  dataElement,
                  null,
                  null,
                  null,
                  categoryOptionMap.get(dv.categoryOptionCombo()),
                  null,
                  dv.value(),
                  dv.comment(),
                  dv.followUp(),
                  dv.storedBy(),
                  dv.created(),
                  dv.lastUpdated(),
                  dv.deleted()));
        }
        // add last group
        if (groupPeriod != null) {
          groups.add(
              new DataExportGroup.Output(
                  groupDataSet,
                  groupPeriod,
                  unitsById.get(currentOrgUnit).getPropertyValue(ouScheme),
                  null,
                  groupAttributeOptions,
                  numericUsedDataElements,
                  groupValues.stream()));
        }
      }
    }
    return groups;
  }

  private Stream<DataExportValue.Output> encodeValues(
      DataExportGroup.Output group, Stream<DataExportValue> values, IdSchemes schemes) {
    IdProperty deAs = IdProperty.of(schemes.getDataElementIdScheme());
    IdProperty ouAs = IdProperty.of(schemes.getOrgUnitIdScheme());
    IdProperty cocAs = IdProperty.of(schemes.getCategoryOptionComboIdScheme());
    IdProperty aocAs = IdProperty.of(schemes.getAttributeOptionComboIdScheme());
    UnaryOperator<String> deOf = UnaryOperator.identity();
    UnaryOperator<String> ouOf = UnaryOperator.identity();
    UnaryOperator<String> cocOf = UnaryOperator.identity();
    UnaryOperator<String> aocOf = UnaryOperator.identity();
    if (group.orgUnit() != null) ouOf = uid -> null;
    if (group.attributeOptionCombo() != null) aocOf = uid -> null;
    boolean deMap = deAs.isNotUID();
    boolean ouMap = ouAs.isNotUID() && group.orgUnit() == null;
    boolean cocMap = cocAs.isNotUID();
    boolean aocMap = aocAs.isNotUID() && group.attributeOptionCombo() == null;
    String groupPeriod = group.period();
    if (deMap || ouMap || cocMap || aocMap) {
      List<DataExportValue> list = values.toList();
      if (deMap)
        deOf = store.getIdMapping(DE, deAs, list.stream().map(DataExportValue::dataElement))::get;
      if (ouMap)
        ouOf = store.getIdMapping(OU, ouAs, list.stream().map(DataExportValue::orgUnit))::get;
      if (cocMap)
        cocOf =
            store.getIdMapping(COC, cocAs, list.stream().map(DataExportValue::categoryOptionCombo))
                ::get;
      if (aocMap)
        aocOf =
            store.getIdMapping(COC, aocAs, list.stream().map(DataExportValue::attributeOptionCombo))
                ::get;
      // by only doing this in case of other IDs being used UIDs can be truly stream processed
      values = list.stream(); // renew the already consumed stream
    }
    UnaryOperator<String> deOfFinal = deOf;
    UnaryOperator<String> ouOfFinal = ouOf;
    UnaryOperator<String> cocOfFinal = cocOf;
    UnaryOperator<String> aocOfFinal = aocOf;
    return values.map(
        dv ->
            new DataExportValue.Output(
                deOfFinal.apply(dv.dataElement().getValue()),
                groupPeriod != null ? null : dv.period(),
                ouOfFinal.apply(dv.orgUnit().getValue()),
                cocOfFinal.apply(dv.categoryOptionCombo().getValue()),
                null, // COs not yet supported
                aocOfFinal.apply(dv.attributeOptionCombo().getValue()),
                dv.value(),
                dv.comment(),
                dv.followUp(),
                dv.storedBy(),
                dv.created(),
                dv.lastUpdated(),
                dv.deleted()));
  }

  private DataExportStoreParams decodeParamsAdx(DataExportParams urlParams) {
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
          .addAll(urlParams.getPeriod().stream().map(PeriodType::getPeriodFromIsoString).toList());
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
    params.setOffset(urlParams.getOffset());
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
          .addAll(urlParams.getPeriod().stream().map(PeriodType::getPeriodFromIsoString).toList());
    }
    if (urlParams.getStartDate() != null && urlParams.getEndDate() != null) {
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

    if (!isEmpty(urlParams.getCategoryOptionCombo())) {
      params
          .getCategoryOptionCombos()
          .addAll(
              identifiableObjectManager.getObjects(
                  CategoryOptionCombo.class,
                  IdentifiableProperty.in(
                      inputIdSchemes, IdSchemes::getCategoryOptionComboIdScheme),
                  urlParams.getCategoryOptionCombo()));
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
        .setOffset(urlParams.getOffset())
        .setOrderByPeriod(urlParams.isOrderByPeriod())
        .setOutputIdSchemes(urlParams.getOutputIdSchemes());
  }

  public void validateFilters(DataExportStoreParams params) throws ConflictException {
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
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    if (currentUser.isSuper()) return;

    for (DataSet dataSet : params.getDataSets()) {
      if (!aclService.canDataRead(currentUser, dataSet)) {
        throw new ConflictException(ErrorCode.E2010, dataSet.getUid());
      }
    }

    // Verify attribute option combination data read sharing
    for (CategoryOptionCombo optionCombo : params.getAttributeOptionCombos()) {
      if (!aclService.canDataRead(currentUser, optionCombo)) {
        throw new ConflictException(ErrorCode.E2011, optionCombo.getUid());
      }
    }

    // Verify org unit being located within user data capture hierarchy
    for (OrganisationUnit unit : params.getOrganisationUnits()) {
      if (!currentUser.isInUserHierarchy(unit.getPath())) {
        throw new ConflictException(ErrorCode.E2012, unit.getUid());
      }
    }
  }

  private static Set<CategoryOptionCombo> getAttributeOptionCombos(
      DataSet dataSet, DataExportStoreParams params) {
    Set<CategoryOptionCombo> aocs = dataSet.getCategoryCombo().getOptionCombos();

    if (params.hasAttributeOptionCombos()) {
      aocs = new HashSet<>(aocs);

      aocs.retainAll(params.getAttributeOptionCombos());
    }

    return aocs;
  }

  private static Map<UID, Map<String, String>> createCategoryOptionsMapping(
      DataSet dataSet, IdSchemes idSchemes) throws ConflictException {
    Map<UID, Map<String, String>> res = new HashMap<>();

    Set<CategoryCombo> combos = new HashSet<>();
    combos.add(dataSet.getCategoryCombo());
    for (DataSetElement element : dataSet.getDataSetElements()) {
      combos.add(element.getResolvedCategoryCombo());
    }

    IdScheme cScheme = idSchemes.getCategoryIdScheme();
    IdScheme coScheme = idSchemes.getCategoryOptionIdScheme();

    for (CategoryCombo cc : combos) {
      for (CategoryOptionCombo coc : cc.getOptionCombos()) {
        Map<String, String> coByC = new HashMap<>();
        if (!coc.isDefault()) {
          for (Category category : coc.getCategoryCombo().getCategories()) {
            String cId = category.getPropertyValue(cScheme);
            checkNonEmptyIdentifier("Category ", cId, cScheme, category.getName());

            CategoryOption co = category.getCategoryOption(coc);
            String coId = co.getPropertyValue(coScheme);
            checkNonEmptyIdentifier("CategoryOption ", coId, coScheme, co.getName());
            coByC.put(cId, coId);
          }
        }
        res.put(UID.of(coc), coByC);
      }
    }
    return res;
  }

  private static void checkNonEmptyIdentifier(
      String objectType, String id, IdScheme scheme, String name) throws ConflictException {
    if (id == null || id.isEmpty()) {
      throw new ConflictException(
          objectType + scheme.name() + " for " + name + " is missing: " + id);
    }
  }
}
