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
import static org.hisp.dhis.feedback.ImportResult.error;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    UID ds = request.dataSet();
    List<AggDataValue> values = request.values();
    if (ds == null) {
      Map<String, Set<String>> dsByDe =
          store.getDataSetsByDataElements(values.stream().map(AggDataValue::dataElement));
      ds = commonDataSet(dsByDe);
      if (ds == null)
        throw new ConflictException(
            ErrorCode.E7606, dsByDe.values().stream().flatMap(Set::stream).distinct().toList());
    }

    validateAccess(ds, values);
    validateKeyConsistency(ds, values);

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

    return validateValues(values, errors);
  }

  /** Is the user allowed to write (capture) the data values? */
  private void validateAccess(UID ds, List<AggDataValue> values) throws ConflictException {
    // - OUs are in user hierarchy
    String userId = getCurrentUserDetails().getUid();
    List<String> noAccessOrgUnits =
        store.getOrgUnitsNotInUserHierarchy(
            UID.of(userId), values.stream().map(AggDataValue::orgUnit));
    if (!noAccessOrgUnits.isEmpty()) throw new ConflictException(ErrorCode.E7610, noAccessOrgUnits);

    // - DS ACL check canDataWrite
    boolean dsNoAccess = store.getDataSetAccessible(ds);
    if (!dsNoAccess) throw new ConflictException(ErrorCode.E7601, ds);

    // - CO of COCs + AOCs : ACL canDataWrite

  }

  /**
   * Are the data value components that make a unique data value key consistent with the metadata?
   */
  private void validateKeyConsistency(UID ds, List<AggDataValue> values)
      throws ConflictException, BadRequestException {
    // - DEs must belong to the specified DS
    List<String> deNotInDs =
        store.getDataElementsNotInDataSet(ds, values.stream().map(AggDataValue::dataElement));
    if (!deNotInDs.isEmpty()) throw new BadRequestException(ErrorCode.E7605, ds, deNotInDs);
    // - OU must be a source of the DS for the DE (could be in multiple :/)
    List<String> ouNotInDs =
        store.getOrgUnitsNotInDataSet(ds, values.stream().map(AggDataValue::orgUnit));
    if (!ouNotInDs.isEmpty()) throw new BadRequestException(ErrorCode.E7609, ds, ouNotInDs);

    // key consistency (Bad Request)
    // - COC must link (belong) to the CC of the DE (if CC override skip validation)
    // - AOC must link (belong) to the CC of the DS!

    // - PE ISO value must map to an existing PT
    // TODO change to filter those ISO values that are not of the target PT
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
    String ptTarget = store.getDataSetPeriodType(ds);
    List<String> isoWrongPt =
        values.stream()
            .map(AggDataValue::period)
            .distinct()
            .filter(iso -> isNotSamePeriodType(ptTarget, ptByIso.get(iso)))
            .toList();
    if (!isoWrongPt.isEmpty())
      throw new ConflictException(ErrorCode.E7608, ptTarget, ds, isoWrongPt);
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
    // TODO (JB): Should DEs have an optional options regex-pattern that is used instead for big
    // sets of uniform nature?
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
        errors.add(error(index, ErrorCode.E7618, e));
      } else {
        // - value valid for the DE value type?
        String error = ValidationUtils.valueIsValid(val, type);
        if (error != null) {
          errors.add(error(index, ErrorCode.E7619, type, e));
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
  private UID commonDataSet(Map<String, Set<String>> dsByDe) {
    if (dsByDe.isEmpty()) return null;
    if (dsByDe.values().stream().anyMatch(ds -> ds.size() != 1)) return null;
    Set<String> ds1 = dsByDe.values().iterator().next();
    return dsByDe.values().stream().allMatch(ds1::equals) ? UID.of(ds1.iterator().next()) : null;
  }
}
