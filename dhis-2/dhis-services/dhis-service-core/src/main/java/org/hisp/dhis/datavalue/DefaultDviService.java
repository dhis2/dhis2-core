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
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.feedback.ImportResult.error;
import static org.hisp.dhis.security.Authorities.F_EDIT_EXPIRED;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DviUpsertRequest.Options;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ImportResult;
import org.hisp.dhis.feedback.ImportResult.ImportError;
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
public class DefaultDviService implements DviService {

  private final DviStore store;

  @Override
  @Transactional
  public void importValue(@CheckForNull UID dataSet, @Nonnull DviValue value)
      throws ConflictException, BadRequestException {
    List<ImportError> errors = new ArrayList<>(1);
    List<DviValue> validValues = validate(false, dataSet, List.of(value), errors);
    if (validValues.isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    store.upsertValues(List.of(value));
  }

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value import")
  public ImportResult importAll(Options options, DviUpsertRequest request, JobProgress progress)
      throws ConflictException {

    progress.startingStage("Unifying %d values...".formatted(request.values().size()));
    List<DviValue> values = progress.runStage(List.of(), () -> completedValues(request));

    progress.startingStage("Validating %d values...".formatted(values.size()));
    List<ImportError> errors = new ArrayList<>();
    List<DviValue> validValues =
        progress.runStage(
            List.of(), () -> validate(options.force(), request.dataSet(), values, errors));
    if (options.atomic() && values.size() > validValues.size())
      throw new ConflictException(ErrorCode.E7625, validValues.size(), values.size());

    progress.startingStage("Importing %d values...".formatted(validValues.size()));
    int imported =
        progress.runStage(
            0, () -> options.dryRun() ? validValues.size() : store.upsertValues(validValues));

    return new ImportResult(validValues.size(), imported, errors);
  }

  @Override
  @Transactional
  public void deleteValue(DviKey key) {
    // TODO
  }

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value deletion")
  public int deleteAll(DviDeleteRequest request) throws BadRequestException {
    // TODO
    return 0;
  }

  private List<DviValue> validate(
      boolean force, UID ds, List<DviValue> values, List<ImportError> errors)
      throws ConflictException {
    if (ds == null) {
      /*
       * If the DS was not specified but all DEs only map to the same single DS we can infer that DS
       * without risk of misinterpretation of the request.
       */
      List<String> dsForDe = store.getDataSets(values.stream().map(DviValue::dataElement));
      if (dsForDe.isEmpty())
        throw new ConflictException(
            ErrorCode.E7605, "", values.stream().map(DviValue::dataElement).distinct().toList());
      if (dsForDe.size() != 1) throw new ConflictException(ErrorCode.E7606, dsForDe);
      ds = UID.of(dsForDe.get(0));
    }

    validateUserAccess(ds, values);
    validateKeyConsistency(ds, values);
    boolean skipTimeliness = force && getCurrentUserDetails().isSuper();
    if (!skipTimeliness) validateEntryTimeliness(ds, values);

    return validateValues(ds, values, errors);
  }

  /** Is the user allowed to write (capture) the data values? */
  private void validateUserAccess(UID ds, List<DviValue> values) throws ConflictException {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return; // super always can

    // - require: OUs are in user hierarchy
    String userId = user.getUid();
    List<String> noAccessOrgUnits =
        store.getOrgUnitsNotInUserHierarchy(UID.of(userId), values.stream().map(DviValue::orgUnit));
    if (!noAccessOrgUnits.isEmpty()) throw new ConflictException(ErrorCode.E7610, noAccessOrgUnits);

    // - require: DS ACL check canDataWrite
    boolean dsNoAccess = store.getDataSetCanDataWrite(ds);
    if (!dsNoAccess) throw new ConflictException(ErrorCode.E7601, ds);

    // - require: AOCs + COCs => COs : ACL canDataWrite
    List<String> coNoAccess =
        store.getCategoryOptionsCanNotDataWrite(
            Stream.concat(
                values.stream().map(DviValue::attributeOptionCombo),
                values.stream().map(DviValue::categoryOptionCombo)));
    if (!coNoAccess.isEmpty()) throw new ConflictException(ErrorCode.E7627, coNoAccess);
  }

  /**
   * Are the data value components that make a unique data value key consistent with the metadata?
   */
  private void validateKeyConsistency(UID ds, List<DviValue> values) throws ConflictException {
    // - require: DEs must belong to the specified DS
    List<String> deNotInDs =
        store.getDataElementsNotInDataSet(ds, values.stream().map(DviValue::dataElement));
    if (!deNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7605, ds, deNotInDs);

    // - require: OU must be a source of the DS for the DE
    List<String> ouNotInDs =
        store.getOrgUnitsNotInDataSet(ds, values.stream().map(DviValue::orgUnit));
    if (!ouNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7609, ds, ouNotInDs);

    // - require: PE ISO must be of PT used by the DS
    List<String> isoNotUsableInDs =
        store.getIsoPeriodsNotUsableInDataSet(ds, values.stream().map(DviValue::period));
    if (!isoNotUsableInDs.isEmpty())
      throw new ConflictException(ErrorCode.E7608, ds, isoNotUsableInDs);

    // - require: AOC must link (belong) to the CC of the DS
    List<String> aocNotInDs =
        store.getAocNotInDataSet(ds, values.stream().map(DviValue::attributeOptionCombo));
    if (!aocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7623, ds, aocNotInDs);

    // - require: COC must link (belong) to the CC of the DE
    Map<UID, List<DviValue>> valuesByDe =
        values.stream().collect(groupingBy(DviValue::dataElement));
    for (Map.Entry<UID, List<DviValue>> e : valuesByDe.entrySet()) {
      UID de = e.getKey();
      List<String> cocNotInDs =
          store.getCocNotInDataSet(
              ds, de, e.getValue().stream().map(DviValue::categoryOptionCombo));
      if (!cocNotInDs.isEmpty()) throw new ConflictException(ErrorCode.E7624, ds, de, cocNotInDs);
    }

    // - require: OU must be within the hierarchy of each CO for AOC => COs => OUs
    List<String> aocOuRestricted =
        store.getAocWithOrgUnitHierarchy(values.stream().map(DviValue::attributeOptionCombo));
    if (!aocOuRestricted.isEmpty()) {
      Map<UID, List<DviValue>> ouByAoc =
          values.stream().collect(groupingBy(DviValue::attributeOptionCombo));
      for (Map.Entry<UID, List<DviValue>> e : ouByAoc.entrySet()) {
        UID aoc = e.getKey();
        if (!aocOuRestricted.contains(aoc.getValue())) continue;
        List<String> ouNotInAoc =
            store.getOrgUnitsNotInAocHierarchy(aoc, e.getValue().stream().map(DviValue::orgUnit));
        if (!ouNotInAoc.isEmpty()) throw new ConflictException(ErrorCode.E7628, aoc, ouNotInAoc);
      }
    }
  }

  /**
   * Are the values valid only considering the DE value type and value and comment options as
   * context. Non-conforming values will be removed from the result, and an error is added to the
   * errors list.
   */
  private List<DviValue> validateValues(UID ds, List<DviValue> values, List<ImportError> errors) {
    boolean commentAllowsEmptyValue = store.getDataSetCommentAllowsEmptyValue(ds);
    List<UID> dataElements = values.stream().map(DviValue::dataElement).distinct().toList();
    Map<String, Set<String>> optionsByDe = store.getOptionsByDataElements(dataElements.stream());
    Map<String, Set<String>> commentOptionsByDe =
        store.getCommentOptionsByDataElements(dataElements.stream());
    Map<String, ValueType> valueTypeByDe = store.getValueTypeByDataElements(dataElements.stream());

    int index = 0;
    List<DviValue> res = new ArrayList<>(values.size());
    for (DviValue e : values) {
      String de = e.dataElement().getValue();
      ValueType type = valueTypeByDe.get(de);
      String val = normalizeValue(e, type);
      boolean allowEmptyValue =
          e.deleted() == Boolean.TRUE || commentAllowsEmptyValue && !isNotEmpty(e.comment());
      // - require: value not null/empty (not for delete or deleted value)
      boolean emptyValue = isEmpty(val);
      if (emptyValue && !allowEmptyValue) {
        errors.add(error(index, ErrorCode.E7618, e));
      } else {
        // - require: value valid for the DE value type?
        String error = emptyValue ? null : ValidationUtils.valueIsValid(val, type);
        if (error != null) {
          errors.add(error(index, ErrorCode.E7619, type, e));
        } else {
          // - require: if DE uses OptionSet - is value a valid option?
          Set<String> options = emptyValue ? null : optionsByDe.get(de);
          if (options != null && !options.contains(val)) {
            errors.add(error(index, ErrorCode.E7621, e));
          } else {
            // - require: if DE uses comment OptionSet - is comment a valid option?
            Set<String> cOptions = commentOptionsByDe.get(de);
            if (cOptions != null && (e.comment() == null || !cOptions.contains(e.comment()))) {
              errors.add(error(index, ErrorCode.E7620, e));
            } else {
              // - require: does the comment not exceed maximum length?
              if (e.comment() != null && e.comment().length() > 5000) {
                errors.add(error(index, ErrorCode.E7620, e));
              } else {
                // finally: all is good, we try to upsert this value
                res.add(e);
              }
            }
          }
        }
      }
      index++;
    }
    return res;
  }

  private static String normalizeValue(DviValue e, ValueType type) {
    if (type == null || !type.isBoolean()) return e.value();
    String val = e.value();
    if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("f") || "0".equals(val))
      val = "false";
    if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("t") || "1".equals(val)) val = "true";
    return val;
  }

  private static List<DviValue> completedValues(DviUpsertRequest request) {
    List<DviValue> values = request.values();
    UID de = request.dataElement();
    UID ou = request.orgUnit();
    String pe = request.period();
    UID aoc = request.attrOptionCombo();
    return (de == null && ou == null && pe == null && aoc == null)
        ? values
        : values.stream().map(e -> completeValue(e, de, ou, pe, aoc)).toList();
  }

  private static DviValue completeValue(
      DviValue e, UID dataElement, UID orgUnit, String period, UID aoc) {
    if (e.orgUnit() != null
        && e.dataElement() != null
        && e.period() != null
        && e.attributeOptionCombo() != null) return e;
    return new DviValue(
        e.dataElement() == null ? dataElement : e.dataElement(),
        e.orgUnit() == null ? orgUnit : e.orgUnit(),
        e.categoryOptionCombo(),
        e.attributeOptionCombo() == null ? aoc : e.attributeOptionCombo(),
        e.period() == null ? period : e.period(),
        e.value(),
        e.comment(),
        e.followUp(),
        e.deleted());
  }

  /*
  Everything below belongs to the additional DS based data entry validation mechanisms
  like input periods, locking, approval...
  */

  private void validateEntryTimeliness(UID ds, List<DviValue> values) throws ConflictException {
    Map<String, List<DateRange>> entryPeriodsByIso = store.getEntryPeriodsByIsoPeriod(ds);
    // only if no explicit ranges are defined use expiry and future periods
    if (entryPeriodsByIso.isEmpty()) {
      // - require: DS entry for period still allowed?
      // (how much later can data be entered relative to current period)
      int expiryDays = store.getDataSetExpiryDays(ds);
      if (!getCurrentUserDetails().isAuthorized(F_EDIT_EXPIRED)) {
        if (expiryDays > 0) { // 0 = no expiry, always open
          Date now = new Date();
          Map<String, Set<String>> exemptedIsoByOu =
              store.getExpiryDaysExemptedIsoPeriodsByOrgUnit(ds);
          List<String> isoNoLongerOpen =
              values.stream()
                  // only check values not exempt
                  .filter(
                      dv -> {
                        Set<String> isoPeriods = exemptedIsoByOu.get(dv.orgUnit().getValue());
                        return isoPeriods == null || !isoPeriods.contains(dv.period());
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
                  .map(DviValue::period)
                  .distinct()
                  .toList();
          if (!isoNoLongerOpen.isEmpty())
            throw new ConflictException(ErrorCode.E7629, ds, isoNoLongerOpen);
        }

        if (expiryDays >= 0) {
          // - require: DS entry for period already allowed?
          // (how much earlier can data be entered relative to the current period)
          int openPeriodsOffset = store.getDataSetOpenPeriodsOffset(ds);
          List<String> isoPeriods = values.stream().map(DviValue::period).distinct().toList();
          PeriodType type = PeriodType.getPeriodTypeFromIsoString(isoPeriods.get(0));
          Period latestOpen = type.getFuturePeriod(openPeriodsOffset);
          List<String> isoNotYetOpen =
              values.stream()
                  .map(DviValue::period)
                  .distinct()
                  .filter(iso -> PeriodType.getPeriodFromIsoString(iso).isAfter(latestOpen))
                  .toList();
          if (!isoNotYetOpen.isEmpty())
            throw new ConflictException(ErrorCode.E7629, ds, isoNotYetOpen);
        }
      }
    } else {
      // - require: DS input period is open explicitly
      // entry time-frame(s) are explicitly defined...
      Date now = new Date();
      List<String> isoNotOpen =
          values.stream()
              .filter(
                  dv -> {
                    List<DateRange> open = entryPeriodsByIso.get(dv.period());
                    if (open == null) return true;
                    return open.stream().anyMatch(range -> range.includes(now));
                  })
              .map(DviValue::period)
              .distinct()
              .toList();
      if (!isoNotOpen.isEmpty()) throw new ConflictException(ErrorCode.E7629, ds, isoNotOpen);
    }

    // - require: DS not already approved (data approval)
    List<String> aocInApproval = store.getDataSetAocInApproval(ds);
    if (!aocInApproval.isEmpty()) {
      Map<UID, List<DviValue>> byAoc =
          values.stream().collect(groupingBy(DviValue::attributeOptionCombo));
      for (Map.Entry<UID, List<DviValue>> e : byAoc.entrySet()) {
        UID aoc = e.getKey();
        if (!aocInApproval.contains(aoc.getValue())) continue;
        Map<String, Set<String>> isoByOu =
            store.getApprovedIsoPeriodsByOrgUnit(
                ds, aoc, e.getValue().stream().map(DviValue::orgUnit));
        if (!isoByOu.isEmpty()) {
          Predicate<DviValue> dvApproved =
              dv -> {
                Set<String> isoPeriods = isoByOu.get(dv.orgUnit().getValue());
                return isoPeriods != null && isoPeriods.contains(dv.period());
              };
          List<String> ouPeInApproval =
              e.getValue().stream()
                  .filter(dvApproved)
                  .map(dv -> dv.orgUnit() + "-" + dv.period())
                  .distinct()
                  .toList();
          if (!ouPeInApproval.isEmpty())
            throw new ConflictException(ErrorCode.E7626, aoc, ouPeInApproval);
        }
      }
    }

    // - require: PE must be within AOC "date range" (range super complicated to find)
  }
}
