/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.datavalue;

import static java.lang.System.Logger.Level.INFO;
import static java.util.Comparator.comparingLong;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.feedback.DataEntrySummary.error;
import static org.hisp.dhis.security.Authorities.F_EDIT_EXPIRED;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.datavalue.DataEntryGroup.Options;
import org.hisp.dhis.datavalue.DataEntryStore.ObjectType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.feedback.DataEntrySummary.DataEntryError;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.log.TimeExecution;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements bulk import operation upsert and delete for data values.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@Service
@RequiredArgsConstructor
public class DefaultDataEntryService implements DataEntryService, DataDumpService {

  private final DataEntryStore store;

  @Override
  @Transactional(readOnly = true)
  public DataEntryValue decodeValue(@CheckForNull UID dataSet, @Nonnull DataEntryValue.Input value)
      throws BadRequestException {
    return decodeGroup(new DataEntryGroup.Input(List.of(value))).values().get(0);
  }

  @Override
  @Transactional(readOnly = true)
  public DataEntryGroup decodeGroupKeepUnspecified(@Nonnull DataEntryGroup.Input group)
      throws BadRequestException {
    return decodeGroup(group, true);
  }

  @Override
  @Transactional(readOnly = true)
  public DataEntryGroup decodeGroup(@Nonnull DataEntryGroup.Input group)
      throws BadRequestException {
    return decodeGroup(group, false);
  }

  private DataEntryGroup decodeGroup(DataEntryGroup.Input group, boolean partial)
      throws BadRequestException {
    UnaryOperator<String> isoOf = UnaryOperator.identity();
    UnaryOperator<String> dsOf = UnaryOperator.identity();
    UnaryOperator<String> deOf = UnaryOperator.identity();
    UnaryOperator<String> ouOf = UnaryOperator.identity();
    UnaryOperator<String> cocOf = UnaryOperator.identity();
    UnaryOperator<String> aocOf = UnaryOperator.identity();

    DataEntryGroup.Ids ids = group.ids();
    String isoGroup = group.period();
    if (isoGroup != null) isoOf = iso -> iso != null ? iso : isoGroup;
    List<DataEntryValue.Input> values = group.values();
    String dataSet = group.dataSet();
    String deGroup = group.dataElement();
    String ouGroup = group.orgUnit();
    String aocGroup = group.attributeOptionCombo();
    if (ids != null) {
      if (dataSet != null && ids.dataSets().isNotUID())
        dsOf = store.getIdMapping(ObjectType.DS, ids.dataSets(), Stream.of(dataSet))::get;
      if (ids.dataElements().isNotUID()) {
        Stream<String> deIds = values.stream().map(DataEntryValue.Input::dataElement);
        if (deGroup != null) deIds = Stream.concat(deIds, Stream.of(deGroup));
        deOf = store.getIdMapping(ObjectType.DE, ids.dataElements(), deIds)::get;
      }
      if (ids.orgUnits().isNotUID()) {
        Stream<String> ouIds = values.stream().map(DataEntryValue.Input::orgUnit);
        if (ouGroup != null) ouIds = Stream.concat(ouIds, Stream.of(ouGroup));
        ouOf = store.getIdMapping(ObjectType.OU, ids.orgUnits(), ouIds)::get;
      }
      if (ids.categoryOptionCombos().isNotUID()) {
        Stream<String> cocIds = values.stream().map(DataEntryValue.Input::categoryOptionCombo);
        cocOf = store.getIdMapping(ObjectType.COC, ids.categoryOptionCombos(), cocIds)::get;
      }
      if (ids.attributeOptionCombos().isNotUID()) {
        Stream<String> aocIds = values.stream().map(DataEntryValue.Input::attributeOptionCombo);
        if (aocGroup != null) aocIds = Stream.concat(aocIds, Stream.of(aocGroup));
        aocOf = store.getIdMapping(ObjectType.COC, ids.attributeOptionCombos(), aocIds)::get;
      }
    }
    int i = 0;
    String dsStr = dsOf.apply(dataSet);
    if (dataSet != null && dsStr == null) throw new BadRequestException(ErrorCode.E8005, dataSet);
    UID ds = decodeUID(dsStr);
    if (dsStr != null && ds == null) throw new BadRequestException(ErrorCode.E8004, dsStr);
    List<DataEntryValue> decoded = new ArrayList<>(values.size());
    Map<String, String> aoGroup = group.attributeOptions();
    IdProperty categories = ids == null ? IdProperty.UID : ids.categories();
    IdProperty categoryOptions = ids == null ? IdProperty.UID : ids.categoryOptions();
    if (aocGroup == null && aoGroup != null && !aoGroup.isEmpty()) {
      if (ds == null) throw new BadRequestException(ErrorCode.E8101, "*", aoGroup);
      List<String> aocKeyOrder = store.getDataSetAocCategories(ds, categories);
      Set<String> aocKey = aocKeyOrder.stream().map(aoGroup::get).collect(toSet());
      Map<Set<String>, String> aocByKey = store.getDataSetAocIdMapping(ds, categoryOptions);
      aocGroup = aocByKey.get(aocKey);
    }
    Map<String, List<String>> categoriesByDe = null;
    Map<String, Map<Set<String>, String>> cocByOptionsByDe = null;
    Map<String, Map<Set<String>, String>> aocOptionsByCc = null;
    for (DataEntryValue.Input dv : values) {
      // PE
      String pe = isoOf.apply(dv.period());
      if (pe == null) throw new BadRequestException(ErrorCode.E8100, i, dv);
      // DE
      String deVal = dv.dataElement();
      if (deVal == null) deVal = deGroup;
      if (deVal == null) throw new BadRequestException(ErrorCode.E8102, i, dv);
      String deUID = deOf.apply(deVal);
      if (deUID == null) throw new BadRequestException(ErrorCode.E8103, i, deVal);
      UID de = decodeUID(deUID);
      if (de == null) throw new BadRequestException(ErrorCode.E8104, i, deUID);
      // OU
      String ouVal = dv.orgUnit();
      if (ouVal == null) ouVal = ouGroup;
      if (ouVal == null) throw new BadRequestException(ErrorCode.E8105, i, dv);
      String ouUID = ouOf.apply(ouVal);
      if (ouUID == null) throw new BadRequestException(ErrorCode.E8107, i, ouVal);
      UID ou = decodeUID(ouUID);
      if (ou == null) throw new BadRequestException(ErrorCode.E8106, i, ouUID);
      // COC
      String cocVal = dv.categoryOptionCombo();
      if (cocVal != null && cocVal.isEmpty()) cocVal = null;
      Map<String, String> co = dv.categoryOptions();
      String cocUID = null;
      if (cocVal == null && co != null) {
        if (categoriesByDe == null) {
          if (ds == null) throw new BadRequestException(ErrorCode.E8101, i, co);
          IdProperty dataElements = ids == null ? IdProperty.UID : ids.dataElements();
          List<String> dataElementIds =
              values.stream().map(DataEntryValue.Input::dataElement).toList();
          categoriesByDe =
              store.getDataElementCocCategories(
                  ds, categories, dataElements, dataElementIds.stream());
          cocByOptionsByDe =
              store.getDataElementCocIdMapping(
                  ds, categoryOptions, dataElements, dataElementIds.stream());
        }
        Set<String> key = categoriesByDe.get(deVal).stream().map(co::get).collect(toSet());
        cocUID = cocByOptionsByDe.get(deVal).get(key);
        if (cocUID == null) throw new BadRequestException(ErrorCode.E8108, i, co);
      } else {
        cocUID = cocVal == null ? null : cocOf.apply(cocVal);
        if (cocUID == null && cocVal != null)
          throw new BadRequestException(ErrorCode.E8108, i, cocVal);
      }
      UID coc = decodeUID(cocUID);
      if (coc == null && cocUID != null) throw new BadRequestException(ErrorCode.E8109, i, cocUID);
      // AOC
      String aocVal = dv.attributeOptionCombo();
      if (aocVal != null && aocVal.isEmpty()) aocVal = null;
      if (aocVal == null) aocVal = aocGroup;
      String aCc = dv.attributeCombo();
      if (aocVal == null && aCc != null) {
        if (aocOptionsByCc == null)
          aocOptionsByCc =
              store.getCategoryComboAocIdMapping(
                  values.stream().map(DataEntryValue.Input::attributeCombo));
        Map<Set<String>, String> aocByOptions = aocOptionsByCc.get(aCc);
        if (aocByOptions == null) throw new BadRequestException(ErrorCode.E8126, i, aCc);
        aocVal = aocByOptions.get(dv.attributeOptions());
        if (aocVal == null)
          throw new BadRequestException(ErrorCode.E8127, i, aCc, dv.attributeOptions());
      }
      String aocUID = aocVal == null ? null : aocOf.apply(aocVal);
      if (aocUID == null && aocVal != null)
        throw new BadRequestException(ErrorCode.E8110, i, aocVal);
      UID aoc = decodeUID(aocUID);
      if (aoc == null && aocUID != null) throw new BadRequestException(ErrorCode.E8111, i, aocUID);

      String value = dv.value();
      String comment = dv.comment();
      Boolean followUp = dv.followUp();
      Boolean deleted = dv.deleted();
      // auto-fill current value on partial update
      if (partial && (value == null || comment == null || followUp == null || deleted == null)) {
        // note: this is done 1 by 1 assuming this never sees much use in true bulk
        DataEntryValue dvOld = store.getPartialDataValue(de, ou, coc, aoc, pe);
        if (dvOld != null) {
          if (value == null) value = dvOld.value();
          if (comment == null) comment = dvOld.comment();
          if (followUp == null) followUp = dvOld.followUp();
          // the special logic on deleted is for backwards compatability
          if (deleted == null && value == null) deleted = dvOld.deleted();
        }
      }
      // add the value
      decoded.add(new DataEntryValue(i++, de, ou, coc, aoc, pe, value, comment, followUp, deleted));
    }
    return new DataEntryGroup(ds, decoded);
  }

  @CheckForNull
  private static UID decodeUID(@CheckForNull String uid) {
    if (uid == null) return null;
    try {
      return UID.of(uid);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataEntryGroup> splitGroup(@Nonnull DataEntryGroup mixed) throws ConflictException {
    List<DataEntryValue> values = mixed.values();

    Map<String, Set<String>> datasetsByDe =
        store.getDataSetsByDataElement(values.stream().map(DataEntryValue::dataElement));

    List<String> deNoDs =
        datasetsByDe.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .toList();
    if (!deNoDs.isEmpty()) throw new ConflictException(ErrorCode.E8003, deNoDs);

    Map<UID, List<DataEntryValue>> valuesByDs = new HashMap<>();
    values.forEach(
        v -> {
          UID ds = UID.of(datasetsByDe.get(v.dataElement().getValue()).iterator().next());
          valuesByDs.computeIfAbsent(ds, key -> new ArrayList<>()).add(v);
        });
    return valuesByDs.entrySet().stream()
        .map(e -> new DataEntryGroup(e.getKey(), List.copyOf(e.getValue())))
        .toList();
  }

  @Override
  @Transactional
  public int upsertValues(DataValue... values) {
    if (values == null || values.length == 0) return 0;
    return store.upsertValues(DataValue.toDataEntryValues(List.of(values)));
  }

  @Override
  @Transactional
  public int upsertValues(DataEntryValue... values) {
    if (values == null || values.length == 0) return 0;
    return store.upsertValues(List.of(values));
  }

  @Override
  @Transactional
  public int upsertValues(DataEntryValue.Input... values) throws BadRequestException {
    if (values == null || values.length == 0) return 0;
    return store.upsertValues(decodeGroup(new DataEntryGroup.Input(List.of(values))).values());
  }

  @Override
  @Transactional
  public void upsertValue(boolean force, @CheckForNull UID dataSet, @Nonnull DataEntryValue value)
      throws ConflictException, BadRequestException {
    List<DataEntryError> errors = new ArrayList<>(1);
    DataEntryGroup valid = validate(force, dataSet, List.of(value), errors);
    if (valid.values().isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    store.upsertValues(List.of(value));
  }

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value upsert")
  public DataEntrySummary upsertGroup(
      @Nonnull Options options, @Nonnull DataEntryGroup group, @Nonnull JobProgress progress)
      throws ConflictException {
    List<DataEntryValue> values = group.values();
    if (values.isEmpty()) return new DataEntrySummary(0, 0, 0, List.of());

    List<DataEntryError> errors = new ArrayList<>();
    progress.startingStage("Validating group " + group.describe());
    DataEntryGroup valid =
        progress.runStageAndRethrow(
            ConflictException.class,
            () -> validate(options.force(), group.dataSet(), values, errors));
    int entered = values.size();
    int attempted = valid.values().size();
    if (options.atomic() && entered > attempted)
      throw new ConflictException(ErrorCode.E8000, attempted, entered);

    String verb = "Upserting";
    if (group.values().stream().allMatch(dv -> dv.deleted() == Boolean.TRUE)) verb = "Deleting";
    progress.startingStage("%s group %s".formatted(verb, valid.describe()));
    int succeeded =
        progress.runStage(
            0, () -> options.dryRun() ? attempted : store.upsertValues(valid.values()));

    return new DataEntrySummary(entered, attempted, succeeded, errors);
  }

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value deletion")
  public DataEntrySummary deleteGroup(
      @Nonnull Options options, @Nonnull DataEntryGroup group, @Nonnull JobProgress progress)
      throws ConflictException {
    DataEntryGroup deleted =
        new DataEntryGroup(
            group.dataSet(), group.values().stream().map(DataEntryValue::toDeleted).toList());
    return upsertGroup(options, deleted, progress);
  }

  @Override
  @Transactional
  public boolean deleteValue(boolean force, @CheckForNull UID dataSet, @Nonnull DataEntryKey key)
      throws ConflictException, BadRequestException {
    DataEntryValue value = key.toDeletedValue();
    List<DataEntryError> errors = new ArrayList<>(1);
    DataEntryGroup valid = validate(force, dataSet, List.of(value), errors);
    if (valid.values().isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    return store.deleteByKeys(List.of(key)) > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public LockStatus getEntryStatus(UID dataSet, @Nonnull DataEntryKey key)
      throws ConflictException {
    DataEntryValue e = key.toDeletedValue();
    UID ds = dataSet != null ? dataSet : autoTargetDataSet(List.of(e));
    try {
      validateEntryTimeliness(ds, List.of(e));
      return LockStatus.OPEN;
    } catch (ConflictException ex) {
      if (ex.getCode() == ErrorCode.E8033) return LockStatus.APPROVED;
      return LockStatus.LOCKED;
    }
  }

  @Override
  public Set<UID> getNotReadableOptionCombos(Collection<UID> optionCombos) {
    return store.getCategoryOptionsCanNotDataRead(optionCombos.stream()).stream()
        .map(UID::of)
        .collect(toSet());
  }

  /**
   * If the DS was not specified but all DEs only map to the same single DS we can infer that DS
   * without risk of misinterpretation of the request.
   */
  @Nonnull
  private UID autoTargetDataSet(List<DataEntryValue> values) throws ConflictException {
    List<String> dsForDe = store.getDataSets(values.stream().map(DataEntryValue::dataElement));
    if (dsForDe.isEmpty())
      throw new ConflictException(
          ErrorCode.E8003, values.stream().map(DataEntryValue::dataElement).distinct().toList());
    if (dsForDe.size() != 1) throw new ConflictException(ErrorCode.E8002, dsForDe);
    return UID.of(dsForDe.get(0));
  }

  private DataEntryGroup validate(
      boolean force, UID ds, List<DataEntryValue> values, List<DataEntryError> errors)
      throws ConflictException {
    if (ds == null) ds = autoTargetDataSet(values);

    validateUserAccess(ds, values);
    validateKeyConsistency(ds, values);
    boolean skipTimeliness = force && getCurrentUserDetails().isSuper();
    if (!skipTimeliness) validateEntryTimeliness(ds, values);

    return new DataEntryGroup(ds, validateValues(ds, values, errors));
  }

  /** Is the user allowed to write (capture) the data values? */
  private void validateUserAccess(UID ds, List<DataEntryValue> values) throws ConflictException {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return; // super can always write

    // - require: DS ACL check canDataWrite
    boolean dsNoAccess = store.getDataSetCanDataWrite(ds);
    if (!dsNoAccess) throw new ConflictException(ErrorCode.E8010, ds);

    // - require: OUs are in user hierarchy
    String userId = user.getUid();
    List<String> noAccessOrgUnits =
        store.getOrgUnitsNotInUserHierarchy(
            UID.of(userId), values.stream().map(DataEntryValue::orgUnit));
    if (!noAccessOrgUnits.isEmpty()) throw new ConflictException(ErrorCode.E8011, noAccessOrgUnits);

    // - require: AOCs + COCs => COs : ACL canDataWrite
    List<String> coNoAccess =
        store.getCategoryOptionsCanNotDataWrite(
            Stream.concat(
                values.stream().map(DataEntryValue::attributeOptionCombo),
                values.stream().map(DataEntryValue::categoryOptionCombo)));
    if (!coNoAccess.isEmpty()) throw new ConflictException(ErrorCode.E8012, coNoAccess);
  }

  /**
   * Are the data value components that make a unique data value key consistent with the metadata?
   */
  private void validateKeyConsistency(UID ds, List<DataEntryValue> values)
      throws ConflictException {
    // - require: DEs must belong to the specified DS
    List<String> deNotInDs =
        store.getDataElementsNotInDataSet(ds, values.stream().map(DataEntryValue::dataElement));
    if (!deNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8020, ds, deNotInDs);

    // - require: OU must be a source of the DS for the DE
    List<String> ouNotInDs =
        store.getOrgUnitsNotInDataSet(ds, values.stream().map(DataEntryValue::orgUnit));
    if (!ouNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8022, ds, ouNotInDs);

    // - require: PE ISO must be of PT used by the DS
    List<String> isoNotUsableInDs =
        store.getIsoPeriodsNotUsableInDataSet(ds, values.stream().map(DataEntryValue::period));
    if (!isoNotUsableInDs.isEmpty())
      throw new ConflictException(ErrorCode.E8021, ds, isoNotUsableInDs);

    // - require: AOC must link (belong) to the CC of the DS
    List<String> aocNotInDs =
        store.getAocNotInDataSet(ds, values.stream().map(DataEntryValue::attributeOptionCombo));
    if (!aocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8023, ds, aocNotInDs);

    // - require: COC must link (belong) to the CC of the DE
    Map<UID, List<DataEntryValue>> valuesByDe =
        values.stream().collect(groupingBy(DataEntryValue::dataElement));
    for (Map.Entry<UID, List<DataEntryValue>> e : valuesByDe.entrySet()) {
      UID de = e.getKey();
      List<String> cocNotInDs =
          store.getCocNotInDataSet(
              ds, de, e.getValue().stream().map(DataEntryValue::categoryOptionCombo));
      if (!cocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8024, ds, de, cocNotInDs);
    }

    // - require: OU must be within the hierarchy of each CO for AOC => COs => OUs
    Set<String> aocOuRestricted =
        Set.copyOf(
            store.getAocWithOrgUnitHierarchy(
                values.stream().map(DataEntryValue::attributeOptionCombo)));
    if (!aocOuRestricted.isEmpty()) {
      Map<UID, List<DataEntryValue>> ouByAoc =
          values.stream()
              .filter(dv -> dv.attributeOptionCombo() != null)
              .filter(dv -> aocOuRestricted.contains(dv.attributeOptionCombo().getValue()))
              .collect(groupingBy(DataEntryValue::attributeOptionCombo));
      for (Map.Entry<UID, List<DataEntryValue>> e : ouByAoc.entrySet()) {
        UID aoc = e.getKey();
        if (!aocOuRestricted.contains(aoc.getValue())) continue;
        List<String> ouNotInAoc =
            store.getOrgUnitsNotInAocHierarchy(
                aoc, e.getValue().stream().map(DataEntryValue::orgUnit));
        if (!ouNotInAoc.isEmpty()) throw new ConflictException(ErrorCode.E8025, aoc, ouNotInAoc);
      }
    }

    // - require: PEs must be within the OU's operational span
    List<String> isoPeriods = values.stream().map(DataEntryValue::period).distinct().toList();
    PeriodType type = PeriodType.getPeriodTypeFromIsoString(isoPeriods.get(0));
    Map<String, Period> peByIso =
        isoPeriods.stream().collect(toMap(identity(), type::createPeriod));
    Map<String, DateRange> ouOpSpan =
        store.getEntrySpanByOrgUnit(
            values.stream().map(DataEntryValue::orgUnit), timeframeOf(peByIso.values()));
    if (!ouOpSpan.isEmpty()) {
      List<DataEntryValue> peNotInOuSpan =
          values.stream()
              .filter(
                  dv -> {
                    DateRange operational = ouOpSpan.get(dv.orgUnit().getValue());
                    if (operational == null) return false; // null => no issue with the timeframe
                    Period pe = peByIso.get(dv.period());
                    return !operational.includes(pe.getStartDate());
                  })
              .distinct()
              .toList();
      if (!peNotInOuSpan.isEmpty()) {
        // this error only indicates issues for the first OU in conflict
        // as there is no good way to describe more than one combination
        UID ou = peNotInOuSpan.get(0).orgUnit();
        List<String> ouPeriods =
            peNotInOuSpan.stream()
                .filter(dv -> ou.equals(dv.orgUnit()))
                .map(DataEntryValue::period)
                .distinct()
                .toList();
        throw new ConflictException(ErrorCode.E8031, ou, ouPeriods);
      }
    }
  }

  /**
   * Are the values valid only considering the DE value type and value and comment options as
   * context. Non-conforming values will be removed from the result, and an error is added to the
   * errors list.
   */
  private List<DataEntryValue> validateValues(
      UID ds, List<DataEntryValue> values, List<DataEntryError> errors) {
    if (values.stream().allMatch(v -> v.deleted() == Boolean.TRUE)) return values;
    boolean commentAllowsEmptyValue = store.getDataSetCommentAllowsEmptyValue(ds);
    List<UID> dataElements = values.stream().map(DataEntryValue::dataElement).distinct().toList();
    Map<String, Set<String>> optionsByDe = store.getOptionsByDataElements(dataElements.stream());
    Map<String, Set<String>> commentOptionsByDe =
        store.getCommentOptionsByDataElements(dataElements.stream());
    Map<String, ValueType> valueTypeByDe = store.getValueTypeByDataElements(dataElements.stream());

    List<DataEntryValue> res = new ArrayList<>(values.size());
    for (DataEntryValue e : values) {
      String de = e.dataElement().getValue();
      ValueType type = valueTypeByDe.get(de);
      String val = normalizeValue(e, type);
      String comment = e.comment();
      boolean allowEmptyValue =
          e.deleted() == Boolean.TRUE || commentAllowsEmptyValue && !isNotEmpty(comment);
      // - require: value not null/empty (not for delete or deleted value)
      boolean emptyValue = isEmpty(val);
      if (emptyValue && !allowEmptyValue) {
        ErrorCode code = commentAllowsEmptyValue ? ErrorCode.E8121 : ErrorCode.E8120;
        errors.add(error(e, code, e.index()));
      } else {
        // - require: value valid for the DE value type?
        String error = emptyValue ? null : ValidationUtils.valueIsValid(val, type);
        if (error != null) {
          errors.add(error(e, ErrorCode.E8122, e.index(), val, type, error));
        } else {
          // - require: if DE uses OptionSet - is value a valid option?
          Set<String> options = emptyValue ? null : optionsByDe.get(de);
          if (options != null && !options.contains(val)) {
            errors.add(error(e, ErrorCode.E8123, e.index(), val, de));
          } else {
            // - require: if DE uses comment OptionSet - is comment a valid option?
            Set<String> cOptions = commentOptionsByDe.get(de);
            if (cOptions != null && (comment == null || !cOptions.contains(comment))) {
              errors.add(error(e, ErrorCode.E8124, e.index(), comment));
            } else {
              // - require: does the comment not exceed maximum length?
              if (comment != null && comment.length() > 5000) {
                errors.add(error(e, ErrorCode.E8125, e.index(), 5000));
              } else {
                // finally: all is good, we try to upsert this value
                res.add(e);
              }
            }
          }
        }
      }
    }
    return res;
  }

  private static String normalizeValue(DataEntryValue e, ValueType type) {
    String val = e.value();
    if (val == null || type == null || !type.isBoolean()) return val;
    int len = val.length();
    if (len > 5) return val;
    String lower = val.toLowerCase();
    if (len == 1) {
      char c = lower.charAt(0);
      if (c == 'f' || c == 'n' || c == '0') return "false";
      if (c == 't' || c == 'y' || c == '1') return "true";
      return val;
    }
    if ("no".equals(lower) || "false".equals(lower)) return "false";
    if ("yes".equals(lower) || "true".equals(lower)) return "true";
    return val;
  }

  /*
  Everything below belongs to the additional DS based data entry validation mechanisms
  like input periods, locking, approval...
  that are concerned with the question of "can the data be entered now".
  So the current moment is the key parameter to these checks.
  */

  private void validateEntryTimeliness(UID ds, List<DataEntryValue> values)
      throws ConflictException {
    Date now = new Date();
    Map<String, List<DateRange>> entrySpansByIso = store.getEntrySpansByIsoPeriod(ds);
    // only if no explicit ranges are defined use expiry and future periods
    if (entrySpansByIso.isEmpty()) {
      // - require: DS entry for period still allowed?
      // (how much later can data be entered relative to current period)
      int expiryDays = store.getDataSetExpiryDays(ds);
      if (!getCurrentUserDetails().isAuthorized(F_EDIT_EXPIRED)) {
        if (expiryDays > 0) { // 0 = no expiry, always open

          Map<String, Set<String>> exemptedIsoByOu =
              store.getExpiryDaysExemptedIsoPeriodsByOrgUnit(ds);
          List<String> isoNoLongerOpen =
              values.stream()
                  // only check values not exempt
                  .filter(
                      dv -> {
                        Set<String> ouIsoPeriods = exemptedIsoByOu.get(dv.orgUnit().getValue());
                        return ouIsoPeriods == null || !ouIsoPeriods.contains(dv.period());
                      })
                  // find the values entered outside their input period
                  .filter(
                      dv -> {
                        Period p = PeriodType.getPeriodFromIsoString(dv.period());
                        // +1 because the period end date is start of day but should include that
                        // day
                        Date endOfEntryPeriod =
                            new Date(
                                p.getEndDate().getTime() + TimeUnit.DAYS.toMillis(expiryDays + 1L));
                        return now.after(endOfEntryPeriod);
                      })
                  .map(DataEntryValue::period)
                  .distinct()
                  .toList();
          if (!isoNoLongerOpen.isEmpty())
            throw new ConflictException(ErrorCode.E8030, ds, isoNoLongerOpen);
        }

        if (expiryDays >= 0) {
          // - require: DS entry for period already allowed?
          // (how much earlier can data be entered relative to the current period)
          int openPeriodsOffset = store.getDataSetOpenPeriodsOffset(ds);
          List<String> isoPeriods = values.stream().map(DataEntryValue::period).distinct().toList();
          PeriodType type = PeriodType.getPeriodTypeFromIsoString(isoPeriods.get(0));
          Period latestOpen = type.getFuturePeriod(openPeriodsOffset);
          List<String> isoNotYetOpen =
              isoPeriods.stream()
                  .filter(iso -> PeriodType.getPeriodFromIsoString(iso).isAfter(latestOpen))
                  .toList();
          if (!isoNotYetOpen.isEmpty())
            throw new ConflictException(ErrorCode.E8030, ds, isoNotYetOpen);
        }
      }
    } else {
      // - require: DS input period is open explicitly
      // entry time-frame(s) are explicitly defined...
      List<String> isoNotOpen =
          values.stream()
              .filter(
                  dv -> {
                    List<DateRange> openSpan = entrySpansByIso.get(dv.period());
                    if (openSpan == null) return true;
                    return openSpan.stream().anyMatch(range -> range.includes(now));
                  })
              .map(DataEntryValue::period)
              .distinct()
              .toList();
      if (!isoNotOpen.isEmpty()) throw new ConflictException(ErrorCode.E8030, ds, isoNotOpen);
    }

    // - require: DS not already approved (data approval)
    Set<String> aocInApproval = Set.copyOf(store.getDataSetAocInApproval(ds));
    if (!aocInApproval.isEmpty()) {
      Map<UID, List<DataEntryValue>> byAoc =
          values.stream()
              .filter(dv -> dv.attributeOptionCombo() != null)
              .filter(dv -> aocInApproval.contains(dv.attributeOptionCombo().getValue()))
              .collect(groupingBy(DataEntryValue::attributeOptionCombo));
      for (Map.Entry<UID, List<DataEntryValue>> e : byAoc.entrySet()) {
        UID aoc = e.getKey();
        if (!aocInApproval.contains(aoc.getValue())) continue;
        Map<String, Set<String>> isoByOu =
            store.getApprovedIsoPeriodsByOrgUnit(
                ds, aoc, e.getValue().stream().map(DataEntryValue::orgUnit));
        if (!isoByOu.isEmpty()) {
          Predicate<DataEntryValue> dvApproved =
              dv -> {
                Set<String> ouIsoPeriods = isoByOu.get(dv.orgUnit().getValue());
                return ouIsoPeriods != null && ouIsoPeriods.contains(dv.period());
              };
          List<DataEntryValue> ouPeInApproval =
              e.getValue().stream().filter(dvApproved).distinct().toList();
          if (!ouPeInApproval.isEmpty()) {
            // this error only indicates issues for the first AOC+OU in conflict
            // as there is no good way to describe more than one combination
            UID ou = ouPeInApproval.get(0).orgUnit();
            List<String> ouPeriods =
                ouPeInApproval.stream()
                    .filter(dv -> ou.equals(dv.orgUnit()))
                    .map(DataEntryValue::period)
                    .toList();
            throw new ConflictException(ErrorCode.E8033, aoc, ou, ouPeriods);
          }
        }
      }
    }

    // - require: PE must be within AOC "date range" (timeframe from AOC => COs)
    Map<String, DateRange> entrySpanByAoc =
        store.getEntrySpanByAoc(values.stream().map(DataEntryValue::attributeOptionCombo));
    if (!entrySpanByAoc.isEmpty()) {
      int openPeriodsAfterCoEndDate = store.getDataSetOpenPeriodsAfterCoEndDate(ds);
      List<DataEntryValue> isoNotInAocRange =
          values.stream()
              .filter(dv -> dv.attributeOptionCombo() != null)
              .map(
                  dv -> {
                    DateRange span = entrySpanByAoc.get(dv.attributeOptionCombo().getValue());
                    if (span == null) return null;
                    Period p = PeriodType.getPeriodFromIsoString(dv.period());
                    Date start = p.getStartDate();
                    Date end = p.getEndDate();
                    if (span.includes(start) && span.includes(end)) return null;
                    if (openPeriodsAfterCoEndDate == 0) return dv;
                    // instead of moving range forward we move period backwards
                    // and check against the unchanged span
                    PeriodType type = p.getPeriodType();
                    end = type.getRewindedDate(end, openPeriodsAfterCoEndDate);
                    start = type.getRewindedDate(start, openPeriodsAfterCoEndDate);
                    return span.includes(start) && span.includes(end) ? null : dv;
                  })
              .filter(Objects::nonNull)
              .toList();
      if (!isoNotInAocRange.isEmpty()) {
        // this error only indicates issues for the first AOC in conflict
        // as there is no good way to describe more than one combination
        UID aoc = isoNotInAocRange.get(0).attributeOptionCombo();
        List<String> aocPeriods =
            isoNotInAocRange.stream()
                .filter(dv -> Objects.equals(aoc, dv.attributeOptionCombo()))
                .map(DataEntryValue::period)
                .distinct()
                .toList();
        throw new ConflictException(ErrorCode.E8032, aoc, aocPeriods);
      }
    }
  }

  private DateRange timeframeOf(Collection<Period> isoPeriods) {
    Date start =
        isoPeriods.stream()
            .map(Period::getStartDate)
            .min(comparingLong(Date::getTime))
            .orElse(null);
    Date end =
        isoPeriods.stream().map(Period::getEndDate).max(comparingLong(Date::getTime)).orElse(null);
    return new DateRange(start, end);
  }
}
