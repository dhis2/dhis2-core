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

import static java.lang.System.Logger.Level.DEBUG;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.common.IdCoder.ObjectType.COC;
import static org.hisp.dhis.common.IdCoder.ObjectType.DE;
import static org.hisp.dhis.common.IdCoder.ObjectType.DS;
import static org.hisp.dhis.common.IdCoder.ObjectType.OU;
import static org.hisp.dhis.feedback.DataEntrySummary.error;
import static org.hisp.dhis.security.Authorities.F_EDIT_EXPIRED;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdCoder;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataset.DataSetCompletion;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.datavalue.DataEntryGroup.Options;
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
import org.hisp.dhis.util.DateUtils;
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
  private final IdCoder idCoder;

  @Override
  @Transactional(readOnly = true)
  public DataEntryValue decodeValue(@CheckForNull UID dataSet, @Nonnull DataEntryValue.Input value)
      throws BadRequestException {
    String ds = dataSet == null ? null : dataSet.getValue();
    return decodeGroup(new DataEntryGroup.Input(ds, List.of(value))).values().get(0);
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
    Function<String, Period> peOf = DefaultDataEntryService::decodeIso;
    UnaryOperator<String> dsOf = UnaryOperator.identity();
    UnaryOperator<String> deOf = UnaryOperator.identity();
    UnaryOperator<String> ouOf = UnaryOperator.identity();
    UnaryOperator<String> cocOf = UnaryOperator.identity();
    UnaryOperator<String> aocOf = UnaryOperator.identity();

    DataEntryGroup.Ids ids = group.ids();
    DataEntryGroup.Input.Scope deletion = group.deletion();
    Period peGroup = decodeIso(group.period());
    if (peGroup != null) peOf = iso -> iso != null ? decodeIso(iso) : peGroup;
    List<DataEntryValue.Input> values = group.values();
    String dataSet = group.dataSet();
    String deGroup = group.dataElement();
    String ouGroup = group.orgUnit();
    String aocGroup = group.attributeOptionCombo();
    if (ids != null) {
      if (dataSet != null && ids.dataSets().isNotUID())
        dsOf = idCoder.mapDecodedIds(DS, ids.dataSets(), Stream.of(dataSet))::get;
      if (ids.dataElements().isNotUID()) {
        Stream<String> deIds = values.stream().map(DataEntryValue.Input::dataElement);
        if (deGroup != null) deIds = Stream.concat(deIds, Stream.of(deGroup));
        if (deletion != null)
          deIds =
              Stream.concat(
                  deIds,
                  deletion.elements().stream()
                      .map(DataEntryGroup.Input.Scope.Element::dataElement));
        deOf = idCoder.mapDecodedIds(DE, ids.dataElements(), deIds)::get;
      }
      if (ids.orgUnits().isNotUID()) {
        Stream<String> ouIds = values.stream().map(DataEntryValue.Input::orgUnit);
        if (ouGroup != null) ouIds = Stream.concat(ouIds, Stream.of(ouGroup));
        if (deletion != null) ouIds = Stream.concat(ouIds, deletion.orgUnits().stream());
        ouOf = idCoder.mapDecodedIds(OU, ids.orgUnits(), ouIds)::get;
      }
      if (ids.categoryOptionCombos().isNotUID()) {
        Stream<String> cocIds = values.stream().map(DataEntryValue.Input::categoryOptionCombo);
        if (deletion != null)
          cocIds =
              Stream.concat(
                  cocIds,
                  deletion.elements().stream()
                      .map(DataEntryGroup.Input.Scope.Element::categoryOptionCombo));
        cocOf = idCoder.mapDecodedIds(COC, ids.categoryOptionCombos(), cocIds)::get;
      }
      if (ids.attributeOptionCombos().isNotUID()) {
        Stream<String> aocIds = values.stream().map(DataEntryValue.Input::attributeOptionCombo);
        if (aocGroup != null) aocIds = Stream.concat(aocIds, Stream.of(aocGroup));
        if (deletion != null)
          aocIds =
              Stream.concat(
                  aocIds,
                  deletion.elements().stream()
                      .map(DataEntryGroup.Input.Scope.Element::attributeOptionCombo));
        aocOf = idCoder.mapDecodedIds(COC, ids.attributeOptionCombos(), aocIds)::get;
      }
    }
    int i = 0;
    String dsStr = dsOf.apply(dataSet);
    if (dataSet != null && dsStr == null) throw new BadRequestException(ErrorCode.E8005, dataSet);
    UID ds = decodeUID(dsStr);
    if (dsStr != null && ds == null) throw new BadRequestException(ErrorCode.E8004, dsStr);
    String completionDateStr = group.completionDate();
    Date completionDate = decodeDate(completionDateStr);
    if (completionDateStr != null && !completionDateStr.isEmpty() && completionDate == null)
      throw new BadRequestException(ErrorCode.E8008, completionDateStr);
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
    if (completionDate != null && (dataSet == null || ouGroup == null || peGroup == null))
      // aoc may be null to indicate "default" so we cannot validate it
      throw new BadRequestException(ErrorCode.E8009);
    Map<String, List<String>> categoriesByDe = null;
    Map<String, Map<Set<String>, String>> cocByOptionsByDe = null;
    Map<String, Map<Set<String>, String>> aocOptionsByCc = null;
    for (DataEntryValue.Input dv : values) {
      // PE
      Period pe = peOf.apply(dv.period());
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
    DataSetCompletion completion;
    if (completionDate == null) {
      completion = null;
    } else {
      UID attributeOptionCombo = aocGroup == null ? null : UID.ofNullable(aocOf.apply(aocGroup));
      completion =
          new DataSetCompletion(
              ds, peGroup, UID.of(ouOf.apply(ouGroup)), attributeOptionCombo, completionDate);
    }
    // decode deletion scope
    DataEntryGroup.Scope del = null;
    if (deletion != null) {
      List<UID> ouScope = new ArrayList<>();
      for (String id : deletion.orgUnits()) ouScope.add(decodeID(id, ouOf));
      List<Period> peScope = deletion.periods().stream().map(Period::of).toList();
      List<DataEntryGroup.Scope.Element> elements = new ArrayList<>();
      for (DataEntryGroup.Input.Scope.Element e : deletion.elements())
        elements.add(
            new DataEntryGroup.Scope.Element(
                decodeID(e.dataElement(), deOf),
                decodeID(e.categoryOptionCombo(), cocOf),
                decodeID(e.attributeOptionCombo(), aocOf)));
      del = new DataEntryGroup.Scope(ouScope, peScope, elements);
    }
    return new DataEntryGroup(ds, completion, del, decoded);
  }

  private static UID decodeID(@CheckForNull String id, UnaryOperator<String> decoder)
      throws BadRequestException {
    if (id == null) return null;
    String uid = decoder.apply(id);
    if (uid == null) throw new BadRequestException(ErrorCode.E8034, id);
    UID res = decodeUID(uid);
    if (res == null) throw new BadRequestException(ErrorCode.E8035, id);
    return res;
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

  @CheckForNull
  private static Period decodeIso(@CheckForNull String period) {
    if (period == null || period.isEmpty()) return null;
    return Period.of(period);
  }

  @CheckForNull
  private static Date decodeDate(@CheckForNull String date) {
    if (date == null || date.isEmpty()) return null;
    try {
      return DateUtils.parseDate(date);
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
    Map<UID, List<DataEntryGroup.Scope.Element>> delScopeElemsByDs = new HashMap<>();
    values.forEach(
        v -> {
          Set<String> dataSets = datasetsByDe.get(v.dataElement().getValue());
          UID ds = UID.of(dataSets.iterator().next());
          valuesByDs.computeIfAbsent(ds, key -> new ArrayList<>()).add(v);
          DataEntryGroup.Scope.Element e = mixed.deletionScopeElement(v.dataElement());
          if (e != null) delScopeElemsByDs.computeIfAbsent(ds, key -> new ArrayList<>()).add(e);
        });
    return valuesByDs.keySet().stream()
        .map(
            ds -> {
              DataEntryGroup.Scope del = mixed.deletion();
              if (del != null) {
                List<DataEntryGroup.Scope.Element> elements = delScopeElemsByDs.get(ds);
                del =
                    elements == null
                        ? null
                        : new DataEntryGroup.Scope(del.orgUnits(), del.periods(), elements);
              }
              return new DataEntryGroup(
                  ds, mixed.completion(), del, List.copyOf(valuesByDs.get(ds)));
            })
        .toList();
  }

  @Override
  @IndirectTransactional
  public int upsertValuesForJdbcTest(DataValue... values) {
    if (values == null || values.length == 0) return 0;
    return store.upsertValuesForJdbcTest(DataValue.toDataEntryValues(List.of(values)));
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
    ValidationSource source = new ValuesValidationSource(List.of(value));
    DataEntryGroup valid = validate(force, dataSet, source, errors);
    if (valid.values().isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    store.upsertValues(List.of(value));
  }

  @Override
  @Transactional
  @TimeExecution(level = DEBUG, name = "data value upsert")
  public DataEntrySummary upsertGroup(
      @Nonnull Options options, @Nonnull DataEntryGroup group, @Nonnull JobProgress progress)
      throws ConflictException {

    List<DataEntryError> errors = new ArrayList<>();
    DataEntryGroup.Scope deletion = group.deletion();
    if (deletion != null) {
      ValidationSource source = new ScopeValidationSource(deletion);
      progress.startingStage("Validating deletion scope " + deletion);
      progress.runStageAndRethrow(
          ConflictException.class,
          () -> validate(options.force(), group.dataSet(), source, errors));
    }

    DataEntryGroup valid = null;
    List<DataEntryValue> values = group.values();
    int entered = values.size();
    int attempted = entered;
    if (!values.isEmpty()) {
      ValidationSource source = new ValuesValidationSource(values);
      progress.startingStage("Validating group " + group.describe());
      valid =
          progress.runStageAndRethrow(
              ConflictException.class,
              () -> validate(options.force(), group.dataSet(), source, errors));
      attempted = valid.values().size();
      if (options.atomic() && entered > attempted) {
        // keep original single error if possible
        if (entered == 1 && errors.size() == 1) {
          DataEntryError error = errors.get(0);
          throw new ConflictException(error.code(), error.args());
        }
        String error = errors.isEmpty() ? "" : errors.get(0).message();
        throw new ConflictException(ErrorCode.E8000, attempted, entered, error);
      }
    }

    int deleted = 0;
    if (deletion != null) {
      progress.startingStage("Deleting scope " + deletion);
      deleted =
          progress.runStage(
              0, () -> options.dryRun() ? store.countScope(deletion) : store.deleteScope(deletion));
    }

    int succeeded = 0;
    if (valid != null) {
      String verb = "Upserting";
      if (group.values().stream().allMatch(dv -> dv.deleted() == Boolean.TRUE)) verb = "Deleting";
      int drySucceeded = attempted;
      List<DataEntryValue> validValues = valid.values();
      progress.startingStage("%s group %s".formatted(verb, valid.describe()));
      succeeded =
          progress.runStage(
              0, () -> options.dryRun() ? drySucceeded : store.upsertValues(validValues));
    }

    return new DataEntrySummary(entered, attempted, succeeded, deleted, errors);
  }

  @Override
  @Transactional
  @TimeExecution(level = DEBUG, name = "data value deletion")
  public DataEntrySummary deleteGroup(
      @Nonnull Options options, @Nonnull DataEntryGroup group, @Nonnull JobProgress progress)
      throws ConflictException {
    List<DataEntryValue> values = group.values().stream().map(DataEntryValue::toDeleted).toList();
    DataEntryGroup deleted =
        new DataEntryGroup(group.dataSet(), group.completion(), group.deletion(), values);
    return upsertGroup(options, deleted, progress);
  }

  @Override
  @Transactional
  public boolean deleteValue(boolean force, @CheckForNull UID dataSet, @Nonnull DataValueKey key)
      throws ConflictException, BadRequestException {
    DataEntryValue value = key.toDeletedValue();
    List<DataEntryError> errors = new ArrayList<>(1);
    ValidationSource source = new ValuesValidationSource(List.of(value));
    DataEntryGroup valid = validate(force, dataSet, source, errors);
    if (valid.values().isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    return store.deleteByKeys(List.of(key)) > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public LockStatus getEntryStatus(UID dataSet, @Nonnull DataValueKey key)
      throws ConflictException {
    DataEntryValue e = key.toDeletedValue();
    ValidationSource source = new ValuesValidationSource(List.of(e));
    UID ds = dataSet != null ? dataSet : autoTargetDataSet(source);
    try {
      validateEntryTimeliness(ds, source);
      return LockStatus.OPEN;
    } catch (ConflictException ex) {
      if (ex.getCode() == ErrorCode.E8033) return LockStatus.APPROVED;
      return LockStatus.LOCKED;
    }
  }

  @Override
  public Set<UID> getNotReadableCategoryOptions(Collection<UID> optionCombos) {
    return store.getCategoryOptionsCanNotDataRead(optionCombos.stream()).stream()
        .map(UID::of)
        .collect(toSet());
  }

  /**
   * If the DS was not specified but all DEs only map to the same single DS we can infer that DS
   * without risk of misinterpretation of the request.
   */
  @Nonnull
  private UID autoTargetDataSet(ValidationSource source) throws ConflictException {
    List<UID> deUnique = source.dataElements().distinct().toList();
    List<String> dsForDe = store.getDataSets(deUnique.stream());
    if (dsForDe.isEmpty()) throw new ConflictException(ErrorCode.E8003, deUnique);
    if (dsForDe.size() != 1) throw new ConflictException(ErrorCode.E8002, dsForDe);
    return UID.of(dsForDe.get(0));
  }

  private DataEntryGroup validate(
      boolean force, UID ds, ValidationSource source, List<DataEntryError> errors)
      throws ConflictException {
    if (ds == null) ds = autoTargetDataSet(source);

    validateUserAccess(ds, source);
    validateKeyConsistency(ds, source);
    boolean skipTimeliness = force && getCurrentUserDetails().isSuper();
    if (!skipTimeliness) validateEntryTimeliness(ds, source);

    return new DataEntryGroup(ds, null, null, validateValues(ds, source.values(), errors));
  }

  /** Is the user allowed to write (capture) the data values? */
  private void validateUserAccess(UID ds, ValidationSource source) throws ConflictException {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return; // super can always write

    // - require: DS ACL check canDataWrite
    boolean dsNoAccess = store.getDataSetCanDataWrite(ds);
    if (!dsNoAccess) throw new ConflictException(ErrorCode.E8010, ds);

    // - require: OUs are in user hierarchy
    UID userId = UID.of(user.getUid());
    List<String> noAccessOrgUnits = store.getOrgUnitsNotInUserHierarchy(userId, source.orgUnits());
    if (!noAccessOrgUnits.isEmpty()) throw new ConflictException(ErrorCode.E8011, noAccessOrgUnits);

    // - require: AOCs + COCs => COs : ACL canDataWrite
    List<String> coNoAccess = store.getCategoryOptionsCanNotDataWrite(source.optionCombos());
    if (!coNoAccess.isEmpty()) throw new ConflictException(ErrorCode.E8012, coNoAccess);
  }

  /**
   * Are the data value components that make a unique data value key consistent with the metadata?
   * These also implicitly make sure the used key dimensions do actually exist
   */
  private void validateKeyConsistency(UID ds, ValidationSource source) throws ConflictException {
    // - require: DEs must belong to the specified DS
    List<String> deNotInDs = store.getDataElementsNotInDataSet(ds, source.dataElements());
    if (!deNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8020, ds, deNotInDs);

    // - require: OU must be a source of the DS for the DE
    List<String> ouNotInDs = store.getOrgUnitsNotInDataSet(ds, source.orgUnits());
    if (!ouNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8022, ds, ouNotInDs);

    // - require: PE ISO must be of PT used by the DS
    List<String> isoNotUsableInDs =
        store.getIsoPeriodsNotUsableInDataSet(ds, source.periods().map(Period::getIsoDate));
    if (!isoNotUsableInDs.isEmpty())
      throw new ConflictException(ErrorCode.E8021, ds, isoNotUsableInDs);

    // - require: AOC must link (belong) to the CC of the DS
    List<String> aocNotInDs = store.getAocNotInDataSet(ds, source.attributeOptionCombos());
    if (!aocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8023, ds, aocNotInDs);

    // - require: COC must link (belong) to the CC of the DE
    Iterator<UID> deIter = source.dataElements().iterator();
    while (deIter.hasNext()) {
      UID de = deIter.next();
      List<String> cocNotInDs =
          store.getCocNotInDataSet(ds, de, source.categoryOptionCombosForDataElement(de));
      if (!cocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E8024, ds, de, cocNotInDs);
    }

    // - require: OU must be within the hierarchy of each CO for AOC => COs => OUs
    Set<String> aocOuRestricted =
        Set.copyOf(store.getAocWithOrgUnitHierarchy(source.attributeOptionCombos()));
    if (!aocOuRestricted.isEmpty()) {
      Iterator<UID> aocIter = source.attributeOptionCombos().iterator();
      while (aocIter.hasNext()) {
        UID aoc = aocIter.next();
        if (!aocOuRestricted.contains(aoc.getValue())) continue;
        List<String> ouNotInAoc =
            store.getOrgUnitsNotInAocHierarchy(aoc, source.orgUnitsForAttributeOptionCombo(aoc));
        if (!ouNotInAoc.isEmpty()) throw new ConflictException(ErrorCode.E8025, aoc, ouNotInAoc);
      }
    }

    // - require: PEs must be within the OU's operational span
    List<Period> isoPeriods = source.periods().toList();
    Map<String, DateRange> ouOpSpan =
        store.getEntrySpanByOrgUnit(source.orgUnits(), timeframeOf(isoPeriods));
    if (!ouOpSpan.isEmpty()) {
      List<Map.Entry<UID, Period>> peNotInOuSpan =
          source
              .orgUnitPeriodPairs()
              .filter(
                  e -> {
                    DateRange operational = ouOpSpan.get(e.getKey().getValue());
                    if (operational == null) return false; // null => no issue with the timeframe
                    Period pe = e.getValue();
                    return !operational.includes(pe.getStartDate());
                  })
              .distinct()
              .toList();
      if (!peNotInOuSpan.isEmpty()) {
        // this error only indicates issues for the first OU in conflict
        // as there is no good way to describe more than one combination
        UID ou = peNotInOuSpan.get(0).getKey();
        List<Period> ouPeriods =
            peNotInOuSpan.stream()
                .filter(e -> ou.equals(e.getKey()))
                .map(Map.Entry::getValue)
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
      UID ds, List<DataEntryValue> values, List<DataEntryError> errors) throws ConflictException {
    if (values.isEmpty()) return values;

    // - require: no two values may affect the same data value (=key =row)
    Map<DataValueKey, List<DataEntryValue>> valuesByKey =
        values.stream().collect(groupingBy(DataEntryValue::toKey));
    if (valuesByKey.size() != values.size()) {
      // only report first to keep error message manageable
      List<DataEntryValue> duplicates =
          valuesByKey.values().stream()
              .filter(l -> l.size() > 1)
              // to make it deterministic take the lowest index offender
              .min(comparingInt(e -> e.get(0).index()))
              .orElse(List.of());
      if (!duplicates.isEmpty())
        throw new ConflictException(
            ErrorCode.E8128,
            duplicates.stream().map(DataEntryValue::index).toList(),
            duplicates.get(0).toKey());
    }

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
      boolean isMultiText = type == ValueType.MULTI_TEXT;
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
          if (options != null
              && !(options.contains(val)
                  || (isMultiText && options.containsAll(List.of(val.split(",")))))) {
            String noOption = val;
            if (isMultiText)
              noOption =
                  Stream.of(val.split(",")).filter(not(options::contains)).findFirst().orElse(val);
            errors.add(error(e, ErrorCode.E8123, e.index(), noOption, de));
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
                res.add(e.withValue(val));
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

  private void validateEntryTimeliness(UID ds, ValidationSource source) throws ConflictException {
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
          List<Period> peNoLongerOpen =
              source
                  .orgUnitPeriodPairs()
                  // only check values not exempt
                  .filter(
                      e -> {
                        Set<String> ouPeriods = exemptedIsoByOu.get(e.getKey().getValue());
                        return ouPeriods == null || !ouPeriods.contains(e.getValue().getIsoDate());
                      })
                  // find the values entered outside their input period
                  .filter(
                      e -> {
                        Period p = e.getValue();
                        // +1 because the period end date is start of day but should include that
                        // day
                        Date endOfEntryPeriod =
                            new Date(
                                p.getEndDate().getTime() + TimeUnit.DAYS.toMillis(expiryDays + 1L));
                        return now.after(endOfEntryPeriod);
                      })
                  .map(Map.Entry::getValue)
                  .distinct()
                  .toList();
          if (!peNoLongerOpen.isEmpty())
            throw new ConflictException(ErrorCode.E8030, ds, peNoLongerOpen);
        }

        if (expiryDays >= 0) {
          // - require: DS entry for period already allowed?
          // (how much earlier can data be entered relative to the current period)
          int openPeriodsOffset = store.getDataSetOpenPeriodsOffset(ds);
          List<Period> isoPeriods = source.periods().toList();
          PeriodType type = isoPeriods.get(0).getPeriodType();
          Period latestOpen = type.getFuturePeriod(openPeriodsOffset);
          List<Period> isoNotYetOpen =
              isoPeriods.stream().filter(p -> p.isAfter(latestOpen)).toList();
          if (!isoNotYetOpen.isEmpty())
            throw new ConflictException(ErrorCode.E8030, ds, isoNotYetOpen);
        }
      }
    } else {
      // - require: DS input period is open explicitly
      // entry time-frame(s) are explicitly defined...
      List<Period> peNotOpen =
          source
              .periods()
              .filter(
                  pe -> {
                    List<DateRange> openSpan = entrySpansByIso.get(pe.getIsoDate());
                    if (openSpan == null) return true;
                    return openSpan.stream().anyMatch(range -> range.includes(now));
                  })
              .toList();
      if (!peNotOpen.isEmpty()) throw new ConflictException(ErrorCode.E8030, ds, peNotOpen);
    }

    // - require: DS not already approved (data approval)
    Set<String> aocInApproval = Set.copyOf(store.getDataSetAocInApproval(ds));
    if (!aocInApproval.isEmpty()) {
      Iterator<UID> iterAoc = source.attributeOptionCombos().iterator();
      while (iterAoc.hasNext()) {
        UID aoc = iterAoc.next();
        if (!aocInApproval.contains(aoc.getValue())) continue;
        Map<String, Set<String>> peByOu =
            store.getApprovedIsoPeriodsByOrgUnit(
                ds, aoc, source.orgUnitsForAttributeOptionCombo(aoc));
        if (!peByOu.isEmpty()) {
          Predicate<Map.Entry<UID, Period>> dvApproved =
              ouPe -> {
                Set<String> ouIsoPeriods = peByOu.get(ouPe.getKey().getValue());
                return ouIsoPeriods != null && ouIsoPeriods.contains(ouPe.getValue().getIsoDate());
              };
          List<Map.Entry<UID, Period>> ouPeInApproval =
              source.orgUnitPeriodPairs().filter(dvApproved).distinct().toList();
          if (!ouPeInApproval.isEmpty()) {
            // this error only indicates issues for the first AOC+OU in conflict
            // as there is no good way to describe more than one combination
            UID ou = ouPeInApproval.get(0).getKey();
            List<Period> peApprovedForOu =
                ouPeInApproval.stream()
                    .filter(ouPe -> ou.equals(ouPe.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
            throw new ConflictException(ErrorCode.E8033, aoc, ou, peApprovedForOu);
          }
        }
      }
    }

    // - require: PE must be within AOC "date range" (timeframe from AOC => COs)
    Map<String, DateRange> entrySpanByAoc = store.getEntrySpanByAoc(source.attributeOptionCombos());
    if (!entrySpanByAoc.isEmpty()) {
      int openPeriodsAfterCoEndDate = store.getDataSetOpenPeriodsAfterCoEndDate(ds);
      List<Map.Entry<UID, Period>> peNotInAocRange =
          source
              .attributeOptionComboPeriodPairs()
              .filter(
                  e -> {
                    DateRange span = entrySpanByAoc.get(e.getKey().getValue());
                    if (span == null) return false;
                    Period pe = e.getValue();
                    Date start = pe.getStartDate();
                    Date end = pe.getEndDate();
                    if (span.includes(start) && span.includes(end)) return false;
                    if (openPeriodsAfterCoEndDate == 0) return true;
                    // instead of moving range forward we move period backwards
                    // and check against the unchanged span
                    PeriodType type = pe.getPeriodType();
                    end = type.getRewindedDate(end, openPeriodsAfterCoEndDate);
                    start = type.getRewindedDate(start, openPeriodsAfterCoEndDate);
                    return !span.includes(start) || !span.includes(end);
                  })
              .toList();
      if (!peNotInAocRange.isEmpty()) {
        // this error only indicates issues for the first AOC in conflict
        // as there is no good way to describe more than one combination
        UID aoc = peNotInAocRange.get(0).getKey();
        List<Period> aocPeriods =
            peNotInAocRange.stream()
                .filter(e -> Objects.equals(aoc, e.getKey()))
                .map(Map.Entry::getValue)
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

  private sealed interface ValidationSource {

    /**
     * @return all data elements in the source (no nulls; not necessarily distinct yet)
     */
    Stream<UID> dataElements();

    /**
     * @return all org units in the source (no nulls; not necessarily distinct yet)
     */
    Stream<UID> orgUnits();

    /**
     * @return all attribute- and category option combos in the source (nulls allowed; not
     *     necessarily distinct yet)
     */
    Stream<UID> optionCombos();

    /**
     * @return all attribute option combos in the source (nulls allowed; not necessarily distinct
     *     yet)
     */
    Stream<UID> attributeOptionCombos();

    /**
     * @return all distinct periods in the source (no nulls; no duplicates)
     */
    Stream<Period> periods();

    /**
     * @return all distinct combinations of org units and periods included in the source (no
     *     duplicates)
     */
    Stream<Map.Entry<UID, Period>> orgUnitPeriodPairs();

    /**
     * @return all distinct combinations of AOC and period included in the source (no null AOCs, no
     *     duplicates)
     */
    Stream<Map.Entry<UID, Period>> attributeOptionComboPeriodPairs();

    /**
     * @param de filter
     * @return all COCs used in combination with the given DE (must maintain nulls, no duplicates)
     */
    Stream<UID> categoryOptionCombosForDataElement(UID de);

    /**
     * @param aoc filter
     * @return all org units used in combination with the given AOC (no nulls, no duplicates)
     */
    Stream<UID> orgUnitsForAttributeOptionCombo(UID aoc);

    /**
     * @return all values for value level validation (value, comment)
     */
    List<DataEntryValue> values();
  }

  private record ValuesValidationSource(List<DataEntryValue> values) implements ValidationSource {

    @Override
    public Stream<UID> dataElements() {
      return values.stream().map(DataEntryValue::dataElement);
    }

    @Override
    public Stream<UID> orgUnits() {
      return values.stream().map(DataEntryValue::orgUnit);
    }

    @Override
    public Stream<UID> optionCombos() {
      return Stream.concat(
          attributeOptionCombos(), values.stream().map(DataEntryValue::categoryOptionCombo));
    }

    @Override
    public Stream<UID> attributeOptionCombos() {
      return values.stream().map(DataEntryValue::attributeOptionCombo);
    }

    @Override
    public Stream<Period> periods() {
      return values.stream().map(DataEntryValue::period).distinct();
    }

    @Override
    public Stream<Map.Entry<UID, Period>> orgUnitPeriodPairs() {
      return values.stream().map(dv -> Map.entry(dv.orgUnit(), dv.period())).distinct();
    }

    @Override
    public Stream<Map.Entry<UID, Period>> attributeOptionComboPeriodPairs() {
      return values.stream()
          .filter(dv -> dv.attributeOptionCombo() != null)
          .map(dv -> Map.entry(dv.attributeOptionCombo(), dv.period()))
          .distinct();
    }

    @Override
    public Stream<UID> categoryOptionCombosForDataElement(UID de) {
      return values.stream()
          .filter(dv -> dv.dataElement().equals(de))
          .map(DataEntryValue::categoryOptionCombo)
          .filter(Objects::nonNull)
          .distinct();
    }

    @Override
    public Stream<UID> orgUnitsForAttributeOptionCombo(UID aoc) {
      return values.stream()
          .filter(dv -> Objects.equals(dv.attributeOptionCombo(), aoc))
          .map(DataEntryValue::orgUnit)
          .distinct();
    }
  }

  private record ScopeValidationSource(DataEntryGroup.Scope scope) implements ValidationSource {
    @Override
    public Stream<UID> dataElements() {
      return scope.elements().stream().map(DataEntryGroup.Scope.Element::dataElement);
    }

    @Override
    public Stream<UID> orgUnits() {
      return scope.orgUnits().stream();
    }

    @Override
    public Stream<UID> optionCombos() {
      return Stream.concat(
          attributeOptionCombos(),
          scope.elements().stream().map(DataEntryGroup.Scope.Element::categoryOptionCombo));
    }

    @Override
    public Stream<UID> attributeOptionCombos() {
      return scope.elements().stream().map(DataEntryGroup.Scope.Element::attributeOptionCombo);
    }

    @Override
    public Stream<Period> periods() {
      return scope.periods().stream();
    }

    @Override
    public Stream<Map.Entry<UID, Period>> orgUnitPeriodPairs() {
      return scope.orgUnits().stream()
          .flatMap(ou -> scope.periods().stream().map(pe -> Map.entry(ou, pe)));
    }

    @Override
    public Stream<Map.Entry<UID, Period>> attributeOptionComboPeriodPairs() {
      return scope.elements().stream()
          .map(DataEntryGroup.Scope.Element::attributeOptionCombo)
          .filter(Objects::nonNull)
          .flatMap(aoc -> scope.periods().stream().map(pe -> Map.entry(aoc, pe)))
          .distinct();
    }

    @Override
    public Stream<UID> categoryOptionCombosForDataElement(UID de) {
      return scope.elements().stream()
          .filter(e -> e.dataElement().equals(de))
          .map(DataEntryGroup.Scope.Element::categoryOptionCombo)
          .distinct();
    }

    @Override
    public Stream<UID> orgUnitsForAttributeOptionCombo(UID aoc) {
      return scope.orgUnits().stream();
    }

    @Override
    public List<DataEntryValue> values() {
      return List.of(); // no value level validation
    }
  }
}
