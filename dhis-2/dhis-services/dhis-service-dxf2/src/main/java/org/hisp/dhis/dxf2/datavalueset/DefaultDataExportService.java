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

import static org.hisp.dhis.common.IdCoder.ObjectType.COC;
import static org.hisp.dhis.common.IdCoder.ObjectType.DE;
import static org.hisp.dhis.common.IdCoder.ObjectType.DEG;
import static org.hisp.dhis.common.IdCoder.ObjectType.DS;
import static org.hisp.dhis.common.IdCoder.ObjectType.OU;
import static org.hisp.dhis.common.IdCoder.ObjectType.OUG;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdCoder;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataExportGroup;
import org.hisp.dhis.datavalue.DataExportInputParams;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportParams.Order;
import org.hisp.dhis.datavalue.DataExportService;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
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
    return store.exportValue(key);
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
    return store.exportValues(params);
  }

  @Override
  @Transactional(readOnly = true)
  public DataExportGroup.Output exportGroup(@Nonnull DataExportInputParams parameters, boolean sync)
      throws ConflictException {
    DataExportParams params = decodeParams(parameters);
    if (sync) {
      // TODO something to skip ACL params.setOrderForSync(true);
      params =
          params.toBuilder()
              .includeDeleted(true)
              .orders(List.of(Order.PE, Order.CREATED, Order.DE))
              .build();
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
    if (!params.isExactOrgUnitsFilter()) ou = null; // result may contain other units
    UID aoc = getUnique(params.getAttributeOptionCombos());
    return encodeGroup(
        new DataExportGroup(ds, pe, ou, aoc, store.exportValues(params)), encodeTo, false);
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

    // for each DS (and each DE or DEG given directly? ADX requires a DS in the header thou)
    List<DataExportGroup> groups = new ArrayList<>();
    for (UID ds : params.getDataSets()) {
      DataExportParams dsParams =
          params.toBuilder()
              .dataSets(List.of(ds))
              .dataElements(null)
              .dataElementGroups(null)
              // we must enforce this order to allow splitting the stream into groups
              // by simply recognising when one of these changes compared to the previous value
              // the order PE, OU, AOC is used because that is the order in the PK index
              .orders(List.of(Order.PE, Order.OU, Order.AOC))
              .build();

      Iterator<DataExportValue> iter = store.exportValues(dsParams).iterator();
      String peG = null;
      UID ouG = null;
      UID aocG = null;
      List<DataExportValue> valuesG = null;
      // split into groups of same PE, OU, AOC
      while (iter.hasNext()) {
        DataExportValue dv = iter.next();
        String pe = dv.period();
        UID ou = dv.orgUnit();
        UID aoc = dv.attributeOptionCombo();
        if (!pe.equals(peG) || !ou.equals(ouG) || !aoc.equals(aocG)) {
          if (valuesG != null)
            groups.add(
                new DataExportGroup(ds, getPeriodFromIsoString(peG), ouG, aocG, valuesG.stream()));
          valuesG = new ArrayList<>();
        } else {
          valuesG.add(dv);
        }
        peG = pe;
        ouG = ou;
        aocG = aoc;
      }
      // add last group
      if (valuesG != null && !valuesG.isEmpty())
        groups.add(
            new DataExportGroup(ds, getPeriodFromIsoString(peG), ouG, aocG, valuesG.stream()));
    }

    IdSchemes encodeTo = parameters.getOutputIdSchemes();
    return groups.stream().map(g -> encodeGroup(g, encodeTo, true));
  }

  private DataExportGroup.Output encodeGroup(
      DataExportGroup group, IdSchemes to, boolean cocAsMap) {
    IdProperty deTo = IdProperty.of(to.getDataElementIdScheme());
    IdProperty ouTo = IdProperty.of(to.getOrgUnitIdScheme());
    IdProperty cocTo = IdProperty.of(to.getCategoryOptionComboIdScheme());
    IdProperty aocTo = IdProperty.of(to.getAttributeOptionComboIdScheme());
    Function<UID, String> dvDe = UID::getValue;
    Function<UID, String> dvOu = UID::getValue;
    Function<UID, String> dvCoc = UID::getValue;
    Map<UID, Map<String, String>> dvCocMap = Map.of();
    Function<UID, String> dvAoc = UID::getValue;
    Map<UID, Map<String, String>> gAocMap = Map.of();
    Period peG = group.period();
    UID ouG = group.orgUnit();
    UID aocG = group.attributeOptionCombo();
    if (ouG != null) dvOu = uid -> null;
    if (aocG != null) dvAoc = uid -> null;
    boolean encodeDe = deTo.isNotUID();
    boolean encodeOu = ouTo.isNotUID() && ouG == null;
    boolean encodeCoc = cocTo.isNotUID() && !cocAsMap;
    boolean encodeAoc = aocTo.isNotUID() && aocG == null && !cocAsMap;

    Stream<DataExportValue> values = group.values();
    if (encodeDe || encodeOu || encodeCoc || encodeAoc || cocAsMap) {
      // this is guarded, so we only consume the original stream
      // if we have to, in order to fetch id mappings
      List<DataExportValue> list = values.toList();
      if (encodeDe)
        dvDe =
            idCoder.mapEncodedIds(DE, deTo, list.stream().map(DataExportValue::dataElement))::get;
      if (encodeOu)
        dvOu = idCoder.mapEncodedIds(OU, ouTo, list.stream().map(DataExportValue::orgUnit))::get;
      if (encodeCoc)
        dvCoc =
            idCoder.mapEncodedIds(
                    COC, cocTo, list.stream().map(DataExportValue::categoryOptionCombo))
                ::get;
      if (encodeAoc)
        dvAoc =
            idCoder.mapEncodedIds(
                    COC, aocTo, list.stream().map(DataExportValue::attributeOptionCombo))
                ::get;
      if (cocAsMap) {
        IdProperty cTo = IdProperty.of(to.getCategoryIdScheme());
        IdProperty coTo = IdProperty.of(to.getCategoryOptionIdScheme());
        dvCocMap =
            idCoder.mapEncodedOptionCombosAsCategoryAndOption(
                cTo, coTo, list.stream().map(DataExportValue::categoryOptionCombo));
        gAocMap = idCoder.mapEncodedOptionCombosAsCategoryAndOption(cTo, coTo, Stream.of(aocG));
      }
      // by only doing this in case of other IDs being used UIDs can be truly stream processed
      values = list.stream(); // renew the already consumed stream
    }
    Function<UID, String> deOfFinal = dvDe;
    Function<UID, String> ouOfFinal = dvOu;
    Function<UID, String> cocOfFinal = dvCoc;
    Function<UID, String> aocOfFinal = dvAoc;
    Map<UID, Map<String, String>> dvCocMapFinal = dvCocMap;
    Map<String, String> aocGMap = cocAsMap ? gAocMap.get(aocG) : null;
    return new DataExportGroup.Output(
        idCoder.getEncodedId(DS, IdProperty.of(to.getDataSetIdScheme()), group.dataSet()),
        peG == null ? null : peG.getIsoDate(),
        idCoder.getEncodedId(OU, ouTo, ouG),
        aocGMap != null ? null : idCoder.getEncodedId(COC, aocTo, aocG),
        aocGMap,
        values.map(
            dv -> {
              Map<String, String> cocMap =
                  cocAsMap ? dvCocMapFinal.get(dv.categoryOptionCombo()) : null;
              return new DataExportValue.Output(
                  deOfFinal.apply(dv.dataElement()),
                  peG != null ? null : dv.period(),
                  ouOfFinal.apply(dv.orgUnit()),
                  cocMap != null ? null : cocOfFinal.apply(dv.categoryOptionCombo()),
                  cocMap,
                  aocOfFinal.apply(dv.attributeOptionCombo()),
                  dv.type(),
                  dv.value(),
                  dv.comment(),
                  dv.followUp(),
                  dv.storedBy(),
                  dv.created(),
                  dv.lastUpdated(),
                  dv.deleted());
            }));
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

    List<UID> attributeOptionCombos = decodeIds(COC, anyIn, params.getAttributeOptionCombo());
    if (attributeOptionCombos == null && params.getAttributeOptions() != null) {
      UID aoc =
          store.getAttributeOptionCombo(
              params.getAttributeCombo(), params.getAttributeOptions().stream());
      if (aoc != null) attributeOptionCombos = List.of(aoc);
    }

    List<Order> orders = params.getOrder();
    if (params.isOrderByPeriod()) {
      if (orders == null) {
        orders = List.of(Order.PE);
      } else if (!orders.contains(Order.PE)) {
        orders = new ArrayList<>(orders);
        orders.add(0, Order.PE);
      }
    }

    return DataExportParams.builder()
        .dataSets(decodeIds(DS, dsIn, params.getDataSet()))
        .dataElementGroups(decodeIds(DEG, degIn, params.getDataElementGroup()))
        .dataElements(decodeIds(DE, deIn, params.getDataElement()))
        .organisationUnits(decodeIds(OU, ouIn, params.getOrgUnit()))
        .organisationUnitGroups(decodeIds(OUG, anyIn, params.getOrgUnitGroup()))
        .orgUnitLevel(params.getLevel())
        .categoryOptionCombos(decodeIds(COC, anyIn, params.getCategoryOptionCombo()))
        .attributeOptionCombos(attributeOptionCombos)
        .periods(decodePeriods(params.getPeriod()))
        .startDate(params.getStartDate())
        .endDate(params.getEndDate())
        .includeDescendants(params.isChildren())
        .includeDeleted(params.isIncludeDeleted())
        .lastUpdated(params.getLastUpdated())
        .lastUpdatedDuration(DateUtils.getDuration(params.getLastUpdatedDuration()))
        .limit(params.getLimit())
        .offset(params.getOffset())
        .orders(orders)
        .build();
  }

  private List<UID> decodeIds(
      IdCoder.ObjectType type, @CheckForNull IdentifiableProperty from, Collection<String> ids) {
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
    if (!params.hasDataElementFilters()) throw new ConflictException(ErrorCode.E2001);
    if (!params.hasPeriodFilters()) throw new ConflictException(ErrorCode.E2002);
    if (!params.hasOrgUnitFilters()) throw new ConflictException(ErrorCode.E2006);
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
  private void validateAccess(List<UID> dataSets, List<UID> attributeCombos, List<UID> orgUnits)
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
}
