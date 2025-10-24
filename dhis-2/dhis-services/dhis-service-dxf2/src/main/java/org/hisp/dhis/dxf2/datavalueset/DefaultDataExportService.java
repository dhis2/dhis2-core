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
import static org.hisp.dhis.common.IdCoder.ObjectType.COC;
import static org.hisp.dhis.common.IdCoder.ObjectType.DE;
import static org.hisp.dhis.common.IdCoder.ObjectType.DEG;
import static org.hisp.dhis.common.IdCoder.ObjectType.DS;
import static org.hisp.dhis.common.IdCoder.ObjectType.OU;
import static org.hisp.dhis.common.IdCoder.ObjectType.OUG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdCoder;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataExportGroup;
import org.hisp.dhis.datavalue.DataExportInputParams;
import org.hisp.dhis.datavalue.DataExportService;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the core functions to export (access) aggregate data.
 *
 * @author Jan Bernitt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDataExportService implements DataExportService {

  private final DataExportStore store;
  private final IdCoder idCoder;

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public DataExportValue exportValue(@Nonnull DataEntryKey key) throws ConflictException {
    validateAccess(key);
    return store.getDataValue(key);
  }

  /**
   * @implNote since we return a stream whoever is calling must already be in a transaction because
   *     only then it is safe to process the stream in that transaction outside of this method. Once
   *     the transaction closes the stream becomes invalid.
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Stream<DataExportValue> exportValues(@Nonnull DataExportInputParams parameters)
      throws ConflictException {
    DataExportParams params = decodeParams(parameters);
    validateFilters(params);
    validateAccess(params);
    return store.getDataValues(params);
  }

  @Override
  @Transactional(readOnly = true)
  public DataExportGroup.Output exportGroup(@Nonnull DataExportInputParams parameters, boolean sync)
      throws ConflictException {
    DataExportParams params = decodeParams(parameters);
    if (sync) {
      params.setOrderForSync(true);
      params.setIncludeDeleted(true);
    } else {
      validateFilters(params);
      validateAccess(params);
    }
    return exportGroupInternal(params, parameters.getOutputIdSchemes());
  }

  @Nonnull
  private DataExportGroup.Output exportGroupInternal(DataExportParams params, IdSchemes encodeTo) {
    UID ds = getUnique(params.getDataSets());
    Period pe = getUnique(params.getPeriods());
    UID ou = getUnique(params.getOrganisationUnits());
    if (params.isIncludeDescendants() || params.hasOrganisationUnitGroups())
      ou = null; // result may contain other units
    UID aoc = getUnique(params.getAttributeOptionCombos());
    return encodeGroup(new DataExportGroup(ds, pe, ou, aoc, null, store.getDataValues(params)), encodeTo);
  }

  private <T> T getUnique(List<T> elements) {
    if (elements == null || elements.size() != 1) return null;
    return elements.get(0);
  }

  @Override
  @Transactional(readOnly = true)
  public Stream<DataExportGroup.Output> exportInGroups(@Nonnull DataExportInputParams parameters)
      throws ConflictException {
    DataExportParams params = decodeParams(parameters);
    validateFilters(params);
    validateAccess(params);

    // for each DS
    // export to values (DB level)
    //  group per AOC + PE + OU
    //  per group:
    //    encode
    //    + resolve all COC+AOC => (C => CO) [as new option opt-in]
    //    + valueType to each DV [as new option opt-in]

    return listGroupsOfValues(params, parameters.getOutputIdSchemes()).stream();
  }

  /**
   * @implNote For now we don't do full Stream processing where this returns a Stream (not List) as
   *     that makes this a fair bit more complicated. Might be a future PR
   */
  private List<DataExportGroup.Output> listGroupsOfValues(DataExportParams params, IdSchemes encodeTo)
      throws ConflictException {
    IdScheme ouScheme = encodeTo.getOrgUnitIdScheme();
    IdScheme dsScheme = encodeTo.getDataSetIdScheme();
    IdScheme deScheme = encodeTo.getDataElementIdScheme();

    Set<OrganisationUnit> units = params.getAllOrganisationUnits();
    Map<UID, OrganisationUnit> unitsById = new HashMap<>();
    units.forEach(ou -> unitsById.put(UID.of(ou), ou));

    List<DataExportGroup.Output> groups = new ArrayList<>();
    for (DataSet dataSet : params.getDataSets()) {
      String groupDataSet = dataSet.getPropertyValue(dsScheme);
      // COC => C => CO
      Map<UID, Map<String, String>> categoryOptionMap =
          createCategoryOptionsMapping(dataSet, encodeTo);

      for (CategoryOptionCombo aoc : getAttributeOptionCombos(dataSet, params)) {
        Map<String, String> groupAttributeOptions = categoryOptionMap.get(UID.of(aoc));

        Set<DataElement> dsDataElements = dataSet.getDataElements();
        DataExportParams groupParams =
            new DataExportParams()
                .setDataElements(dsDataElements)
                .setOrganisationUnits(units)
                .setIncludeDescendants(params.isIncludeDescendants())
                .setIncludeDeleted(params.isIncludeDeleted())
                .setLastUpdated(params.getLastUpdated())
                .setLastUpdatedDuration(params.getLastUpdatedDuration())
                .setPeriods(params.getPeriods())
                .setStartDate(params.getStartDate())
                .setEndDate(params.getEndDate())
                .setAttributeOptionCombos(List.of(aoc))
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

        //TODO replace with adding ValueType to the Output record?
        Set<String> numericUsedDataElements = new HashSet<>();
        List<DataExportValue.Output> groupValues = new ArrayList<>();
        IdProperty ouAs = IdProperty.of(ouScheme);
        Function<UID, String> encodeOu =
            uid -> {
              if (ouAs == IdProperty.UID) return uid.getValue();
              OrganisationUnit ou = unitsById.get(uid);
              if (ou != null) return ou.getPropertyValue(ouScheme);
              // ou can be null when children are included!
              // Note: This single ID lookup is not ideal and only a temporary fix
              // until use of the object model is replaced in the entire processing
              return idCoder.mapEncodedIds(OU, ouAs, Stream.of(uid)).get(uid.getValue());
            };
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
                      encodeOu.apply(currentOrgUnit),
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
                  encodeOu.apply(currentOrgUnit),
                  null,
                  groupAttributeOptions,
                  numericUsedDataElements,
                  groupValues.stream()));
        }
      }
    }
    return groups;
  }

  private DataExportGroup.Output encodeGroup(
      DataExportGroup group, IdSchemes to) {
    IdProperty deAs = IdProperty.of(to.getDataElementIdScheme());
    IdProperty ouAs = IdProperty.of(to.getOrgUnitIdScheme());
    IdProperty cocAs = IdProperty.of(to.getCategoryOptionComboIdScheme());
    IdProperty aocAs = IdProperty.of(to.getAttributeOptionComboIdScheme());
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
    Period pe = group.period();
    String groupPeriod = pe == null ? null : pe.getIsoDate();
    Stream<DataExportValue> values = group.values();
    if (deMap || ouMap || cocMap || aocMap) {
      List<DataExportValue> list = values.toList();
      if (deMap)
        deOf =
            idCoder.mapEncodedIds(DE, deAs, list.stream().map(DataExportValue::dataElement))::get;
      if (ouMap)
        ouOf = idCoder.mapEncodedIds(OU, ouAs, list.stream().map(DataExportValue::orgUnit))::get;
      if (cocMap)
        cocOf =
            idCoder.mapEncodedIds(
                    COC, cocAs, list.stream().map(DataExportValue::categoryOptionCombo))
                ::get;
      if (aocMap)
        aocOf =
            idCoder.mapEncodedIds(
                    COC, aocAs, list.stream().map(DataExportValue::attributeOptionCombo))
                ::get;
      // by only doing this in case of other IDs being used UIDs can be truly stream processed
      values = list.stream(); // renew the already consumed stream
    }
    UnaryOperator<String> deOfFinal = deOf;
    UnaryOperator<String> ouOfFinal = ouOf;
    UnaryOperator<String> cocOfFinal = cocOf;
    UnaryOperator<String> aocOfFinal = aocOf;
    return new DataExportGroup.Output(
      idCoder.getEncodedId(DS, IdProperty.of(to.getDataSetIdScheme()), group.dataSet()),
    groupPeriod,
    idCoder.getEncodedId(OU, ouAs, group.orgUnit()),
    idCoder.getEncodedId(COC, aocAs, group.attributeOptionCombo()),
    null,
    null,
    values.map(
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
                dv.deleted())));
  }

  private DataExportParams decodeParams(DataExportInputParams params) {
    boolean codeFallback = Boolean.TRUE.equals(params.getInputUseCodeFallback());
    IdentifiableProperty anyIn = params.getInputIdScheme();
    IdentifiableProperty dsIn = params.getInputDataSetIdScheme();
    IdentifiableProperty deIn = params.getInputDataElementIdScheme();
    IdentifiableProperty ouIn = params.getInputOrgUnitIdScheme();
    IdentifiableProperty degIn = params.getInputDataElementGroupIdScheme();
    if (anyIn == null && !codeFallback) anyIn = IdentifiableProperty.UID;
    if (dsIn == null) dsIn = anyIn;
    if (deIn == null) deIn = anyIn;
    if (ouIn == null) ouIn = anyIn;
    if (degIn == null) degIn = anyIn;

    List<UID> attributeOptionCombos =
        decodeIds(COC, anyIn, params.getAttributeOptionCombo());
    if (attributeOptionCombos == null && params.getAttributeOptions() != null) {
      UID aoc =
          store.getAttributeOptionCombo(
              params.getAttributeCombo(), params.getAttributeOptions().stream());
      if (aoc != null) attributeOptionCombos = List.of(aoc);
    }

    return new DataExportParams()
        .setDataSets(decodeIds(DS, dsIn, params.getDataSet()))
        .setDataElementGroups(decodeIds(DEG, degIn, params.getDataElementGroup()))
        .setDataElements(decodeIds(DE, deIn, params.getDataElement()))
        .setOrganisationUnits(decodeIds(OU, ouIn, params.getOrgUnit()))
        .setOrganisationUnitGroups(decodeIds(OUG, anyIn, params.getOrgUnitGroup()))
        .setCategoryOptionCombos(decodeIds(COC, anyIn, params.getCategoryOptionCombo()))
        .setAttributeOptionCombos(attributeOptionCombos)
        .setPeriods(decodePeriods(params.getPeriod()))
        .setStartDate(params.getStartDate())
        .setEndDate(params.getEndDate())
        .setIncludeDescendants(params.isChildren())
        .setIncludeDeleted(params.isIncludeDeleted())
        .setLastUpdated(params.getLastUpdated())
        .setLastUpdatedDuration(params.getLastUpdatedDuration())
        .setLimit(params.getLimit())
        .setOffset(params.getOffset())
        .setOrderByPeriod(params.isOrderByPeriod());
  }

  private List<UID> decodeIds(IdCoder.ObjectType type, @CheckForNull IdentifiableProperty from, Collection<String> ids) {
    if (ids == null || ids.isEmpty()) return null;
    IdProperty p = IdProperty.of(from);
    if (p == IdProperty.UID) {
      if (from != null) return ids.stream().map(UID::of).toList();
      // from being null means UID was just assumed by default
      // and code should be attempted as fall-back
      List<UID> res = ids.stream().filter(UID::isValid).map(UID::of).toList();
      if (res.size() == ids.size()) return res;
      return Stream.concat(
              res.stream(),
              idCoder.listDecodedIds(type, IdProperty.CODE, ids.stream()).map(UID::of))
          .toList();
    }
    return idCoder.listDecodedIds(type, p, ids.stream()).map(UID::of).toList();
  }

  private List<Period> decodePeriods(Collection<String> isoPeriods) {
    if (isoPeriods == null || isoPeriods.isEmpty()) return null;
    return isoPeriods.stream().map(PeriodType::getPeriodFromIsoString).toList();
  }

  public void validateFilters(DataExportParams params) throws ConflictException {
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

  private void validateAccess(DataEntryKey key) throws ConflictException {
    UID aoc = key.attributeOptionCombo();
    validateAccess(List.of(), aoc == null ? List.of() : List.of(aoc), List.of(key.orgUnit()));
  }

  private void validateAccess(DataExportParams params) throws ConflictException {
    validateAccess(
        params.getDataSets(), params.getAttributeOptionCombos(), params.getOrganisationUnits());
  }

  /**
   * @implNote This should be done in SQL based on UIDs (not require having the objects). It is also
   *     suspicious that this does not validate COC access similar to AOC access (as we do on
   *     writes) - likely this is missing and should be added
   */
  private void validateAccess(
      List<UID> dataSets,
      List<UID> attributeCombos,
      List<UID> orgUnits)
      throws ConflictException {
    // Verify data set read sharing
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    if (currentUser.isSuper()) return;

    // TODO
    // - user can data read all datasets
    // => throw new ConflictException(ErrorCode.E2010, dataSet.getUid());

    // - user can data read all AOCs (used as filter? seems wrong - should be all AOCs returned)
    // => throw new ConflictException(ErrorCode.E2011, optionCombo.getUid());

    // - all OUs targeted are in user capture hierarchy
    // => throw new ConflictException(ErrorCode.E2012, unit.getUid());
  }

  private static Set<CategoryOptionCombo> getAttributeOptionCombos(
      DataSet dataSet, DataExportParams params) {
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
