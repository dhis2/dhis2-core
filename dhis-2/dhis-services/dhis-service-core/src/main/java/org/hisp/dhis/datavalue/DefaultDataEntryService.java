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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DataEntryGroup.Options;
import org.hisp.dhis.datavalue.DataEntryStore.KeyTable;
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
public class DefaultDataEntryService implements DataEntryService {

  private final DataEntryStore store;

  @Override
  @Transactional(readOnly = true)
  public DataEntryGroup decode(DataEntryGroup.Input group) throws BadRequestException {
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
    if (ids != null) {
      if (dataSet != null && ids.dataSets().isNotUid())
        dsOf = store.mapToUid(KeyTable.DS, ids.dataSets(), Stream.of(dataSet))::get;
      if (ids.dataElements().isNotUid()) {
        Stream<String> deIds = values.stream().map(DataEntryValue.Input::dataElement);
        deOf = store.mapToUid(KeyTable.DE, ids.dataElements(), deIds)::get;
      }
      if (ids.orgUnits().isNotUid()) {
        Stream<String> ouIds = values.stream().map(DataEntryValue.Input::orgUnit);
        ouOf = store.mapToUid(KeyTable.OU, ids.orgUnits(), ouIds)::get;
      }
      if (ids.categoryOptionCombos().isNotUid()) {
        Stream<String> cocIds = values.stream().map(DataEntryValue.Input::categoryOptionCombo);
        cocOf = store.mapToUid(KeyTable.COC, ids.categoryOptionCombos(), cocIds)::get;
      }
      if (ids.attributeOptionCombos().isNotUid()) {
        Stream<String> aocIds = values.stream().map(DataEntryValue.Input::attributeOptionCombo);
        aocOf = store.mapToUid(KeyTable.COC, ids.attributeOptionCombos(), aocIds)::get;
      }
    }
    int i = 0;
    String dsStr = dsOf.apply(dataSet);
    if (dataSet != null && dsStr == null) throw new BadRequestException(ErrorCode.E7816, dataSet);
    UID ds = dsStr == null ? null : decodeUID(dsStr);
    if (dsStr != null && ds == null) throw new BadRequestException(ErrorCode.E7817, dsStr);
    List<DataEntryValue> decoded = new ArrayList<>(values.size());
    String deGroup = group.dataElement();
    String ouGroup = group.orgUnit();
    String aocGroup = group.attrOptionCombo();
    for (DataEntryValue.Input e : values) {
      String pe = isoOf.apply(e.period());
      if (pe == null) throw new BadRequestException(ErrorCode.E7818, i, e);
      String deVal = e.dataElement();
      if (deVal == null && deGroup == null) throw new BadRequestException(ErrorCode.E7819, i, e);
      String deStr = deOf.apply(deVal == null ? deGroup : deVal);
      if (deStr == null)
        throw new BadRequestException(ErrorCode.E7828, i, deVal == null ? deGroup : deVal);
      UID de = decodeUID(deStr);
      if (de == null) throw new BadRequestException(ErrorCode.E7821, i, deStr);
      String ouVal = e.orgUnit();
      if (ouVal == null && ouGroup == null) throw new BadRequestException(ErrorCode.E7820, i, e);
      String ouStr = ouOf.apply(ouVal == null ? ouGroup : ouVal);
      if (ouStr == null)
        throw new BadRequestException(ErrorCode.E7827, i, ouVal == null ? ouGroup : ouVal);
      UID ou = decodeUID(ouStr);
      if (ou == null) throw new BadRequestException(ErrorCode.E7822, i, ouStr);
      String cocVal = e.categoryOptionCombo();
      String cocStr = cocOf.apply(cocVal);
      if (cocStr == null && cocVal != null)
        throw new BadRequestException(ErrorCode.E7823, i, cocVal);
      UID coc = decodeUID(cocStr);
      if (coc == null && cocStr != null) throw new BadRequestException(ErrorCode.E7824, i, cocStr);
      String aocVal = e.attributeOptionCombo();
      if (aocVal == null) aocVal = aocGroup;
      String aocStr = aocOf.apply(aocVal);
      if (aocStr == null && aocVal != null)
        throw new BadRequestException(ErrorCode.E7825, i, aocVal);
      UID aoc = decodeUID(aocStr);
      if (aoc == null && aocStr != null) throw new BadRequestException(ErrorCode.E7826, i, aocStr);

      decoded.add(
          new DataEntryValue(
              i++, de, ou, coc, aoc, pe, e.value(), e.comment(), e.followUp(), null));
    }
    return new DataEntryGroup(ds, decoded);
  }

  private static UID decodeUID(String uid) {
    if (uid == null) return null;
    try {
      return UID.of(uid);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataEntryGroup> groupByDataSet(DataEntryGroup mixed) {
    List<DataEntryValue> values = mixed.values();

    Map<String, Set<String>> dsxByDe =
        store.getDataSetsByDataElement(values.stream().map(DataEntryValue::dataElement));
    if (dsxByDe.size() == 1) return List.of(mixed);

    Map<UID, List<DataEntryValue>> valuesByDs = new HashMap<>();
    values.forEach(
        v -> {
          UID ds = UID.of(dsxByDe.get(v.dataElement().getValue()).iterator().next());
          valuesByDs.computeIfAbsent(ds, key -> new ArrayList<>()).add(v);
        });
    return valuesByDs.entrySet().stream()
        .map(e -> new DataEntryGroup(e.getKey(), List.copyOf(e.getValue())))
        .toList();
  }

  @Override
  @Transactional
  public void upsertDataValue(
      boolean force, @CheckForNull UID dataSet, @Nonnull DataEntryValue value)
      throws ConflictException, BadRequestException {
    List<DataEntryError> errors = new ArrayList<>(1);
    List<DataEntryValue> validValues = validate(force, dataSet, List.of(value), errors);
    if (validValues.isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    store.upsertValues(List.of(value));
  }

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value import")
  public DataEntrySummary upsertDataValueGroup(
      Options options, DataEntryGroup group, JobProgress progress) throws ConflictException {
    List<DataEntryValue> values = group.values();
    if (values.isEmpty()) return new DataEntrySummary(0, 0, List.of());

    progress.startingStage("Validating %d values".formatted(values.size()));
    List<DataEntryError> errors = new ArrayList<>();
    List<DataEntryValue> validValues =
        progress.runStageAndRethrow(
            ConflictException.class,
            () -> validate(options.force(), group.dataSet(), values, errors));
    if (options.atomic() && values.size() > validValues.size())
      throw new ConflictException(ErrorCode.E7808, validValues.size(), values.size());

    progress.startingStage("Writing %d values".formatted(validValues.size()));
    int imported =
        progress.runStage(
            0, () -> options.dryRun() ? validValues.size() : store.upsertValues(validValues));

    return new DataEntrySummary(validValues.size(), imported, errors);
  }

  @Override
  @Transactional
  public boolean deleteDataValue(boolean force, @CheckForNull UID dataSet, DataEntryKey key)
      throws ConflictException, BadRequestException {
    DataEntryValue value = key.toDeletedValue();
    List<DataEntryError> errors = new ArrayList<>(1);
    List<DataEntryValue> validValues = validate(force, dataSet, List.of(value), errors);
    if (validValues.isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    return store.deleteByKeys(List.of(key)) > 0;
  }

  private List<DataEntryValue> validate(
      boolean force, UID ds, List<DataEntryValue> values, List<DataEntryError> errors)
      throws ConflictException {
    if (ds == null) {
      /*
       * If the DS was not specified but all DEs only map to the same single DS we can infer that DS
       * without risk of misinterpretation of the request.
       */
      List<String> dsForDe = store.getDataSets(values.stream().map(DataEntryValue::dataElement));
      if (dsForDe.isEmpty())
        throw new ConflictException(
            ErrorCode.E7801,
            "",
            values.stream().map(DataEntryValue::dataElement).distinct().toList());
      if (dsForDe.size() != 1) throw new ConflictException(ErrorCode.E7802, dsForDe);
      ds = UID.of(dsForDe.get(0));
    }

    validateUserAccess(ds, values);
    validateKeyConsistency(ds, values);
    boolean skipTimeliness = force && getCurrentUserDetails().isSuper();
    if (!skipTimeliness) validateEntryTimeliness(ds, values);

    return validateValues(ds, values, errors);
  }

  /** Is the user allowed to write (capture) the data values? */
  private void validateUserAccess(UID ds, List<DataEntryValue> values) throws ConflictException {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return; // super always can

    // - require: DS ACL check canDataWrite
    boolean dsNoAccess = store.getDataSetCanDataWrite(ds);
    if (!dsNoAccess) throw new ConflictException(ErrorCode.E7815, ds);

    // - require: OUs are in user hierarchy
    String userId = user.getUid();
    List<String> noAccessOrgUnits =
        store.getOrgUnitsNotInUserHierarchy(
            UID.of(userId), values.stream().map(DataEntryValue::orgUnit));
    if (!noAccessOrgUnits.isEmpty()) throw new ConflictException(ErrorCode.E7814, noAccessOrgUnits);

    // - require: AOCs + COCs => COs : ACL canDataWrite
    List<String> coNoAccess =
        store.getCategoryOptionsCanNotDataWrite(
            Stream.concat(
                values.stream().map(DataEntryValue::attributeOptionCombo),
                values.stream().map(DataEntryValue::categoryOptionCombo)));
    if (!coNoAccess.isEmpty()) throw new ConflictException(ErrorCode.E7810, coNoAccess);
  }

  /**
   * Are the data value components that make a unique data value key consistent with the metadata?
   */
  private void validateKeyConsistency(UID ds, List<DataEntryValue> values)
      throws ConflictException {
    // - require: DEs must belong to the specified DS
    List<String> deNotInDs =
        store.getDataElementsNotInDataSet(ds, values.stream().map(DataEntryValue::dataElement));
    if (!deNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7801, ds, deNotInDs);

    // - require: OU must be a source of the DS for the DE
    List<String> ouNotInDs =
        store.getOrgUnitsNotInDataSet(ds, values.stream().map(DataEntryValue::orgUnit));
    if (!ouNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7805, ds, ouNotInDs);

    // - require: PE ISO must be of PT used by the DS
    List<String> isoNotUsableInDs =
        store.getIsoPeriodsNotUsableInDataSet(ds, values.stream().map(DataEntryValue::period));
    if (!isoNotUsableInDs.isEmpty())
      throw new ConflictException(ErrorCode.E7804, ds, isoNotUsableInDs);

    // - require: AOC must link (belong) to the CC of the DS
    List<String> aocNotInDs =
        store.getAocNotInDataSet(ds, values.stream().map(DataEntryValue::attributeOptionCombo));
    if (!aocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7806, ds, aocNotInDs);

    // - require: COC must link (belong) to the CC of the DE
    Map<UID, List<DataEntryValue>> valuesByDe =
        values.stream().collect(groupingBy(DataEntryValue::dataElement));
    for (Map.Entry<UID, List<DataEntryValue>> e : valuesByDe.entrySet()) {
      UID de = e.getKey();
      List<String> cocNotInDs =
          store.getCocNotInDataSet(
              ds, de, e.getValue().stream().map(DataEntryValue::categoryOptionCombo));
      if (!cocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7807, ds, de, cocNotInDs);
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
        if (!ouNotInAoc.isEmpty()) throw new ConflictException(ErrorCode.E7811, aoc, ouNotInAoc);
      }
    }

    // - require: PEs must be within the OU's operational span
    List<String> isoPeriods = values.stream().map(DataEntryValue::period).distinct().toList();
    PeriodType type = PeriodType.getPeriodTypeFromIsoString(isoPeriods.get(0));
    Map<String, Period> peByIso =
        isoPeriods.stream().collect(toMap(identity(), type::createPeriod));
    Map<String, DateRange> ouOpSpan =
        store.getOrgUnitOperationalSpan(
            values.stream().map(DataEntryValue::orgUnit), timeframeOf(peByIso.values()));
    if (!ouOpSpan.isEmpty()) {
      List<String> peNotInOuSpan =
          values.stream()
              .filter(
                  dv -> {
                    DateRange operational = ouOpSpan.get(dv.orgUnit().getValue());
                    if (operational == null) return false; // null => no issue with the timeframe
                    Period pe = peByIso.get(dv.period());
                    if (operational.getStartDate().after(pe.getStartDate())) return true;
                    Date endDate = operational.getEndDate();
                    return endDate != null && pe.getEndDate().after(endDate);
                  })
              .map(dv -> dv.period() + "-" + dv.orgUnit())
              .distinct()
              .toList();
      if (!peNotInOuSpan.isEmpty()) throw new ConflictException(ErrorCode.E7813, peNotInOuSpan);
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
        errors.add(error(e, ErrorCode.E7618, e.dataElement()));
      } else {
        // - require: value valid for the DE value type?
        String error = emptyValue ? null : ValidationUtils.valueIsValid(val, type);
        if (error != null) {
          errors.add(error(e, ErrorCode.E7619, type, val));
        } else {
          // - require: if DE uses OptionSet - is value a valid option?
          Set<String> options = emptyValue ? null : optionsByDe.get(de);
          if (options != null && !options.contains(val)) {
            errors.add(error(e, ErrorCode.E7621, de));
          } else {
            // - require: if DE uses comment OptionSet - is comment a valid option?
            Set<String> cOptions = commentOptionsByDe.get(de);
            if (cOptions != null && (comment == null || !cOptions.contains(comment))) {
              errors.add(error(e, ErrorCode.E7620, comment));
            } else {
              // - require: does the comment not exceed maximum length?
              if (comment != null && comment.length() > 5000) {
                errors.add(error(e, ErrorCode.E7620, comment));
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
    if (type == null || !type.isBoolean()) return e.value();
    String val = e.value();
    if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("f") || "0".equals(val))
      val = "false";
    if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("t") || "1".equals(val)) val = "true";
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
            throw new ConflictException(ErrorCode.E7812, ds, isoNoLongerOpen);
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
            throw new ConflictException(ErrorCode.E7812, ds, isoNotYetOpen);
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
      if (!isoNotOpen.isEmpty()) throw new ConflictException(ErrorCode.E7812, ds, isoNotOpen);
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
          List<String> ouPeInApproval =
              e.getValue().stream()
                  .filter(dvApproved)
                  .map(dv -> dv.orgUnit() + "-" + dv.period())
                  .distinct()
                  .toList();
          if (!ouPeInApproval.isEmpty())
            throw new ConflictException(ErrorCode.E7809, aoc, ouPeInApproval);
        }
      }
    }

    // - require: PE must be within AOC "date range" (range super complicated to find)
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
