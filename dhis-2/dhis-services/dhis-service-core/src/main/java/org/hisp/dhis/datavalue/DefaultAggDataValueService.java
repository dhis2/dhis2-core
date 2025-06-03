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
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.feedback.ImportResult.error;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ImportResult;
import org.hisp.dhis.feedback.ImportResult.ImportError;
import org.hisp.dhis.log.TimeExecution;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAggDataValueService implements AggDataValueService {

  private final AggDataValueImportStore store;

  @Override
  @Transactional
  public void importValue(AggDataValue value) throws ConflictException, BadRequestException {
    List<ImportError> errors = new ArrayList<>(1);
    List<AggDataValue> validValues =
        validate(new AggDataValueUpsertRequest(null, List.of(value)), errors);
    if (validValues.isEmpty()) throw new BadRequestException(errors.get(0).code(), value);
    store.upsertValues(List.of(value));
  }

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value import")
  public ImportResult importAll(AggDataValueUpsertRequest request)
      throws BadRequestException, ConflictException {
    List<ImportError> errors = new ArrayList<>();
    List<AggDataValue> validValues = validate(request, errors);
    int imported = store.upsertValues(validValues);
    return new ImportResult(validValues.size(), imported, errors);
  }

  @Override
  @Transactional
  public void deleteValue(AggDataValueKey key) {}

  @Override
  @Transactional
  @TimeExecution(level = INFO, name = "data value deletion")
  public int deleteAll(AggDataValueDeleteRequest request) throws BadRequestException {
    return 0;
  }

  // TODO job for data value FileResource cleanup
  // DE of files => data values (for the DEs)
  // 1. mark all as assigned that are used
  // 2. mark all as not assigned that are not used by any

  private List<AggDataValue> validate(AggDataValueUpsertRequest request, List<ImportError> errors)
      throws ConflictException, BadRequestException {
    Map<String, Set<String>> dsByDe =
        store.getDataSetsByDataElements(request.values().stream().map(AggDataValue::dataElement));
    validateAccess(request, dsByDe);
    validateKeyConsistency(request, dsByDe);

    // ----------------------------------------------
    // DS based gate-keepers (if DS specified only consider the single DS)
    // - DS not locked (lock exceptions)
    // - DS not already approved (data approval)
    // - DS input period is open (no entering in the past)
    // - DS any having the same DE is open (BS?)

    // AOC related
    // - PE must be within AOC "range" (range super complicated to find)
    // - OU must be linked to AOC (???)
    // ----------------------------------------------

    return validateValues(request.values(), errors);
  }

  /** Is the user allowed to write (capture) the data values? */
  private void validateAccess(AggDataValueUpsertRequest request, Map<String, Set<String>> dsByDe)
      throws ConflictException {
    // user access
    // - OUs are in user hierarchy
    String userId = getCurrentUserDetails().getUid();
    List<String> noAccessOrgUnits =
        store.getOrgUnitsNotInUserHierarchy(
            UID.of(userId), request.values().stream().map(AggDataValue::orgUnit));
    if (!noAccessOrgUnits.isEmpty()) throw new ConflictException(ErrorCode.E7610, noAccessOrgUnits);

    // - DS ACL check canDataWrite
    List<UID> allDs =
        dsByDe.values().stream().flatMap(Set::stream).distinct().map(UID::of).toList();
    // TODO run ACL check for user and all of the DS

    // - CO of COCs + AOCs : ACL canDataWrite

  }

  /**
   * Are the data value components that make a unique data value key consistent with the metadata?
   */
  private void validateKeyConsistency(
      AggDataValueUpsertRequest request, Map<String, Set<String>> dsByDe)
      throws ConflictException, BadRequestException {
    String ds = request.dataSet() == null ? commonDataSet(dsByDe) : request.dataSet().getValue();
    if (ds != null) {
      // - DE must belong to the specified DS (scope)
      List<String> deNotInDs =
          dsByDe.entrySet().stream()
              .filter(e -> !e.getValue().contains(ds))
              .map(Map.Entry::getKey)
              .toList();
      if (!deNotInDs.isEmpty()) throw new BadRequestException(ErrorCode.E7605, ds, deNotInDs);
    }
    // - do all DEs have a DS?
    Set<String> dataElements = dsByDe.keySet();
    List<AggDataValue> values = request.values();
    List<String> deNoDs =
        values.stream()
            .map(AggDataValue::dataElement)
            .map(UID::getValue)
            .filter(not(dataElements::contains))
            .distinct()
            .toList();
    if (!deNoDs.isEmpty()) throw new ConflictException(ErrorCode.E7606, deNoDs);

    // key consistency (Bad Request)
    // - COC must link (belong) to the CC of the DE (if CC override skip validation)
    // - AOC must link (belong) to the CC of the DS!

    // - OU must be a source of the DS for the DE (could be in multiple :/)
    if (ds != null) {
      Map<String, Set<String>> dsByOu =
          store.getDataSetsByOrgUnits(
              values.stream().map(AggDataValue::orgUnit), Stream.of(UID.of(ds)));
      List<String> ouNotInDs =
          values.stream()
              .map(AggDataValue::orgUnit)
              .map(UID::getValue)
              .distinct()
              .filter(ou -> !dsByOu.containsKey(ou) || !dsByOu.get(ou).contains(ds))
              .toList();
      if (!ouNotInDs.isEmpty()) throw new BadRequestException(ErrorCode.E7609, ds, ouNotInDs);
    } else {
      Map<String, Set<String>> dsByOu =
          store.getDataSetsByOrgUnits(
              values.stream().map(AggDataValue::orgUnit),
              dsByDe.values().stream().flatMap(Set::stream).map(UID::of));
      // TODO like above but for each DS
    }

    // - PE ISO value must map to an existing PT
    Map<String, String> ptByIso =
        store.getPeriodTypeByIsoPeriod(values.stream().map(AggDataValue::period));
    List<String> isoNoPT =
        values.stream()
            .map(AggDataValue::period)
            .filter(not(ptByIso::containsKey))
            .distinct()
            .toList();
    if (!isoNoPT.isEmpty()) throw new ConflictException(ErrorCode.E7607, isoNoPT);
    // - PE ISO must be of PT used by the DS for the DE (could be in multiple :/)
    if (ds != null) {
      String ptTarget = store.getPeriodTypeByDataSet(Stream.of(UID.of(ds))).get(ds);
      List<String> isoWrongPt =
          values.stream()
              .map(AggDataValue::period)
              .distinct()
              .filter(iso -> isNotSamePeriodType(ptTarget, ptByIso.get(iso)))
              .toList();
      if (!isoWrongPt.isEmpty())
        throw new ConflictException(ErrorCode.E7608, ptTarget, ds, isoWrongPt);
    } else {
      Map<String, String> ptByDs =
          store.getPeriodTypeByDataSet(dsByDe.values().stream().flatMap(Set::stream).map(UID::of));
      Map<String, Set<String>> ptByDe =
          values.stream()
              .map(AggDataValue::dataElement)
              .map(UID::getValue)
              .distinct()
              .collect(
                  toMap(de -> de, de -> dsByDe.get(de).stream().map(ptByDs::get).collect(toSet())));
      Map<String, Set<String>> isoByDe =
          values.stream()
              .collect(
                  groupingBy(
                      v -> v.dataElement().getValue(), mapping(AggDataValue::period, toSet())));
      Map.Entry<String, Set<String>> deIsoWrongPt =
          isoByDe.entrySet().stream()
              .flatMap(
                  e -> {
                    String de = e.getKey();
                    Set<String> ptTargets = ptByDe.get(de);
                    if (e.getValue().stream().anyMatch(iso -> ptTargets.contains(ptByIso.get(iso))))
                      return Stream.empty();
                    return Stream.of(
                        Map.entry(
                            de,
                            e.getValue().stream()
                                .filter(iso -> !ptTargets.contains(ptByIso.get(iso)))
                                .collect(toSet())));
                  })
              .findFirst()
              .orElse(null);
      if (deIsoWrongPt != null)
        throw new ConflictException(
            ErrorCode.E7608, ptByDe.get(deIsoWrongPt.getKey()), deIsoWrongPt.getValue());
    }
  }

  private static boolean isNotSamePeriodType(String expected, String actual) {
    return actual == null || !actual.equals(expected);
  }

  /**
   * Are the values valid only considering the DE value type and value and comment options as
   * context. Non-conforming values will be removed from the result, and an error is added to the
   * errors list.
   */
  private List<AggDataValue> validateValues(List<AggDataValue> values, List<ImportError> errors) {
    List<UID> dataElements = values.stream().map(AggDataValue::dataElement).distinct().toList();
    Map<String, Set<String>> optionsByDe = store.getOptionsByDataElements(dataElements.stream());
    Map<String, Set<String>> commentOptionsByDe =
        store.getCommentOptionsByDataElements(dataElements.stream());
    Map<String, ValueType> valueTypeByDe = store.getValueTypeByDataElements(dataElements.stream());

    int index = 0;
    List<AggDataValue> res = new ArrayList<>(values.size());
    for (AggDataValue e : values) {
      String de = e.dataElement().getValue();
      ValueType type = valueTypeByDe.get(de);
      String val = normalizeValue(e, type);
      // - value not null/empty (not for delete or deleted value)
      if ((val == null || val.isEmpty()) && e.deleted() != Boolean.TRUE) {
        errors.add(error(index, ErrorCode.E7619, e));
      } else {
        // - value valid for the DE value type?
        String error = ValidationUtils.valueIsValid(val, type);
        if (error != null) {
          errors.add(error(index, ErrorCode.E7619, e));
        } else {
          // - if DE uses OptionSet - is value a valid option?
          Set<String> options = optionsByDe.get(de);
          if (options != null && !options.contains(val)) {
            errors.add(error(index, ErrorCode.E7621, e));
          } else {
            // - if DE uses comment OptionSet - is comment a valid option?
            Set<String> cOptions = commentOptionsByDe.get(de);
            if (cOptions != null && (e.comment() == null || !cOptions.contains(e.comment()))) {
              errors.add(error(index, ErrorCode.E7620, e));
            } else {
              // - does the comment not exceed maximum length?
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

  private static String normalizeValue(AggDataValue e, ValueType type) {
    if (type == null || !type.isBoolean()) return e.value();
    String val = e.value();
    if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("f") || "0".equals(val))
      val = "false";
    if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("t") || "1".equals(val)) val = "true";
    return val;
  }

  @CheckForNull
  private String commonDataSet(Map<String, Set<String>> dsByDe) {
    if (dsByDe.isEmpty()) return null;
    if (dsByDe.values().stream().anyMatch(ds -> ds.size() != 1)) return null;
    Set<String> ds1 = dsByDe.values().iterator().next();
    return dsByDe.values().stream().allMatch(ds1::equals) ? ds1.iterator().next() : null;
  }
}
