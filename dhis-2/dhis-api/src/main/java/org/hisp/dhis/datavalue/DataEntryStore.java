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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;

/**
 * DB support for the bulk Data Value Import.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
public interface DataEntryStore {

  /*
  Decode support
   */

  /** Tables for which ID property values may have to be resolved to UID */
  enum ObjectType {
    DS,
    DE,
    OU,
    COC
  }

  /**
   * Fetches a mapping between the provided IDs (that used the provided {@code idsProperty}) and the
   * UID of the same object.
   *
   * <p>For example, when {@code idsProperty} is CODE, the result looks like:
   *
   * <pre>
   * {code1} => {uid1}
   * {code2} => {uid2}
   * </pre>
   *
   * @param type the target object type or table
   * @param idsProperty the ID property used (provided)
   * @param ids the IDs to map from property to UID
   * @return a map from given ID to the corresponding UID (does not include entries for input IDs
   *     that do not exist and thus do not have a corresponding UID)
   */
  Map<String, String> getIdMapping(
      @Nonnull ObjectType type, @Nonnull IdProperty idsProperty, @Nonnull Stream<String> ids);

  /**
   * Fetches the IDs of the categories of the DS's CC (as used for AOCs).
   *
   * @param dataSet the target DS (for scope)
   * @param categories the ID property of categories to fetch
   * @return the IDs for the categories used by the AOC of the provided DS in alphabetical order
   */
  List<String> getDataSetAocCategories(@Nonnull UID dataSet, @Nonnull IdProperty categories);

  /**
   * Fetches a mapping that allows to map input given in form of category and option to the AOC UID
   * that belongs to each of the possible combinations.
   *
   * <p>For example, when using CODE for categories and category options and the codes for the
   * categories would be A, B and C and the codes for the options would be A1, A2, B1, B2, C1, C2
   * then the map would be:
   *
   * <pre>
   *   A1 B1 C1 => {aoc-uid1}
   *   A2 B1 C1 => {aoc-uid2}
   *   A1 B2 C1 => {aoc-uid3}
   *   ...
   * </pre>
   *
   * The parts of the key are ordered A,B,C because that is the alphabetical order of the category
   * codes. This allows the caller to take input in form of C=CO (codes) and do the same
   * alphabetically ordered concatenation to get a lookup key for the result map.
   *
   * @param dataSet the dataset for context
   * @param attributeOptions the identifier of a category option that is concatenated into a key
   * @return A map from a concatenated key to an AOC UID. The key is composed of the options of the
   *     AOC in alphabetical order of the categories
   */
  Map<Set<String>, String> getDataSetAocIdMapping(
      @Nonnull UID dataSet, @Nonnull IdProperty attributeOptions);

  /**
   * Fetches the category IDs of the effective category combo for each provided data element.
   *
   * <p>For example, assuming self-explanatory codes are used for both data elements and categories
   *
   * <pre>
   *   DE1 => [C1, C2, C3],
   *   DE2 => [C4, C5, C6],
   *   ...
   * </pre>
   *
   * @param dataSet to check for CC override possible on DE-DS mapping
   * @param categories ID property used for categories
   * @param dataElements ID property used for data elements
   * @param dataElementIds data element IDs for the {@code dataElements} property
   * @return a mapping that for each data element (key) holds a list of the category IDs sorted
   *     alphabetically
   */
  Map<String, List<String>> getDataElementCocCategories(
      @Nonnull UID dataSet,
      @Nonnull IdProperty categories,
      @Nonnull IdProperty dataElements,
      @Nonnull Stream<String> dataElementIds);

  /**
   * Lookup of COC-IDs by CO-IDs of the CCs of multiple DEs.
   *
   * @param dataSet the DS for scope (influences which CC is used by data elements)
   * @param categoryOptions what ID property to extract for the category options
   * @param dataElements what ID to match for the given data element IDs
   * @param dataElementIds the scope of data elements to fetch
   * @return for each data element (key1) a mapping from the options (key2) to the COC (value) for
   *     the key option combination
   */
  Map<String, Map<Set<String>, String>> getDataElementCocIdMapping(
      @Nonnull UID dataSet,
      @Nonnull IdProperty categoryOptions,
      @Nonnull IdProperty dataElements,
      @Nonnull Stream<String> dataElementIds);

  /**
   * Lookup of AOC-IDs by CO-IDs for the CCs provided (as used for AOC).
   *
   * @param categoryCombos the scope of CCs to fetch
   * @return for each CC (key1) a mapping from the options (key2) to the AOC (value) for the key
   *     option combination
   */
  Map<String, Map<Set<String>, String>> getCategoryComboAocIdMapping(
      @Nonnull Stream<String> categoryCombos);

  /**
   * Loading of individual values by key in case a partial update is performed where not all data
   * parts of a data value are provided.
   *
   * @param dataElement DE key
   * @param orgUnit OU key
   * @param categoryOptionCombo COC key (null => default)
   * @param attributeOptionCombo AOC key (null => default)
   * @param period ISO period value
   * @return the current value for the given key or null if no such value exists
   */
  @CheckForNull
  DataEntryValue.Input getPartialDataValue(
      @Nonnull UID dataElement,
      @Nonnull UID orgUnit,
      @CheckForNull UID categoryOptionCombo,
      @CheckForNull UID attributeOptionCombo,
      @Nonnull String period);

  /**
   * Find the datasets a data element can be used with to allow grouping data values into groups of
   * data sets based on the data element.
   *
   * @param dataElements all data elements to check (scope)
   * @return for each data element (key) it lists all datasets it can be used with (ordered most
   *     recently created first, those with active input periods first)
   */
  Map<String, Set<String>> getDataSetsByDataElement(Stream<UID> dataElements);

  /*
  Import support
   */

  int deleteByKeys(List<DataEntryKey> keys);

  int upsertValues(List<DataEntryValue> values);

  /*
  Validation support
   */

  /**
   * Performs data write ACL check on the DS itself.
   *
   * @param dataSet the DS to check
   * @return true, if the current user can write data for the provided DS
   */
  boolean getDataSetCanDataWrite(UID dataSet);

  /**
   * Check is adding a comment for a value is a valid substitute for not providing a value (use an
   * empty value).
   *
   * @param dataSet the DS to check
   * @return true, if the DS allows empty values in case a comment is provided instead
   */
  boolean getDataSetCommentAllowsEmptyValue(UID dataSet);

  int getDataSetExpiryDays(UID dataSet);

  int getDataSetOpenPeriodsOffset(UID dataSet);

  int getDataSetOpenPeriodsAfterCoEndDate(UID dataSet);

  /**
   * Checks if the current user has data write access to the provided AOC/COCs.
   *
   * <p>This sharing checked is those of all CO connected to the AOC/COC. The AOC/COC is only usable
   * if all connected CO allow writing.
   *
   * @param optionCombos AOC or COC UIDs to include in the check
   * @return the provided IDs that the current user does not have data write access to. Meaning in a
   *     successful check the result is empty.
   */
  List<String> getCategoryOptionsCanNotDataWrite(Stream<UID> optionCombos);

  /**
   * Checks if a user can write data for all provided OUs.
   *
   * <p>For this the user must be connected to each of the provided OUs or to one of their parents.
   *
   * @param user current user
   * @param orgUnits the OUs to check
   * @return the OU IDs of from the provided input that are not in the users data capture hierarchy.
   *     Meaning in a successful check the result is empty.
   */
  List<String> getOrgUnitsNotInUserHierarchy(UID user, Stream<UID> orgUnits);

  /**
   * Checks that all the provided OUs are valid to use with the AOC.
   *
   * <p>For an OU to be valid it must be identical or a descended of at least one of the OUs linked
   * to each CO that is linked to the provided AOC.
   *
   * @return the OUs of the provided set that are not in the AOC hierarchy and thus illegal to use.
   *     Meaning in a successful check the result is empty.
   */
  List<String> getOrgUnitsNotInAocHierarchy(UID attrOptionCombo, Stream<UID> orgUnits);

  /**
   * Checks that all provided OUs are explicitly linked to the given DS and thus a valid target for
   * data capture.
   *
   * @param dataSet DS to check (scope)
   * @param orgUnits OUs to checks
   * @return the OUs that are not in scope of the DS and thus illegal to use. Meaning in a
   *     successful check the result is empty.
   */
  List<String> getOrgUnitsNotInDataSet(UID dataSet, Stream<UID> orgUnits);

  /**
   * Checks that all given COCs belong to the CC used by the DE. This CC might be overridden by the
   * DE-DS connection.
   *
   * @param dataSet DS to check (scope)
   * @param dataElement DE to check (scope)
   * @param optionCombos COCs to check
   * @return all COCs that are not connected to the CC for the given DS-DE combination and thus
   *     illegal to use. Meaning in a successful check the result is empty.
   */
  List<String> getCocNotInDataSet(UID dataSet, UID dataElement, Stream<UID> optionCombos);

  /**
   * Checks that all given AOCs belong to the CC defined by the given DS.
   *
   * @param dataSet DS to check (scope)
   * @param optionCombos AOCs to check
   * @return all AOCs that are not connected to the given DS's CC and thus illegal to sue. Meaning
   *     in a successful check the result is empty.
   */
  List<String> getAocNotInDataSet(UID dataSet, Stream<UID> optionCombos);

  /**
   * Finds AOCs in scope of the given AOCs that have explicit restrictions on the OUs that they can
   * be used with.
   *
   * <p>To match an AOC must be connected to a CO that defines a list of OUs (parents) it can be
   * used with. If a CO doesn't have any OU entry any OU can be used.
   *
   * @param attrOptionCombos the AOCs to check
   * @return all AOCs of the input that have restrictions and require checking AOCs for OU
   *     connection. In many cases AOC restriction is not used and the result will be empty.
   */
  List<String> getAocWithOrgUnitHierarchy(Stream<UID> attrOptionCombos);

  /**
   * @return List of unique data set UIDs for the given data elements
   */
  List<String> getDataSets(Stream<UID> dataElements);

  /**
   * Checks that all given DEs can be used with the DS.
   *
   * @param dataSet DS for scope
   * @param dataElements DEs to check
   * @return all DEs of the input that are not connected to the DS and thus are illegal to use.
   *     Meaning in a successful check the result is empty.
   */
  List<String> getDataElementsNotInDataSet(UID dataSet, Stream<UID> dataElements);

  /**
   * Fetches all options (codes) for DEs using options.
   *
   * @param dataElements DEs for which to fetch options
   * @return for each DE using options all the valid options (codes)
   */
  Map<String, Set<String>> getOptionsByDataElements(Stream<UID> dataElements);

  /**
   * Fetches all comment options (codes) for DEs using comment options.
   *
   * @param dataElements DEs for which to fetch comment options
   * @return for each DE using comment options all the valid comment options (codes)
   */
  Map<String, Set<String>> getCommentOptionsByDataElements(Stream<UID> dataElements);

  /**
   * @param dataSet the DS to look up
   * @return the name of the PT used by the DS
   */
  String getDataSetPeriodType(UID dataSet);

  /**
   * Fetch all values types for a set of DEs.
   *
   * @param dataElements DEs for which to fetch their value type
   * @return a map, for each DE (key) it holds their value type
   */
  Map<String, ValueType> getValueTypeByDataElements(Stream<UID> dataElements);

  /**
   * @return The {@link org.hisp.dhis.period.PeriodType} names for the given ISO periods. Does not
   *     contain ISO key entries which do not map to a type.
   */
  List<String> getIsoPeriodsNotUsableInDataSet(UID dataSet, Stream<String> isoPeriods);

  /**
   * @param dataSet DS to check
   * @return AOCs connected to the DS via the approval workflow that have an ongoing or completed
   *     approval and thus need to be checked further. Writing to AOC not in that list is not
   *     blocked by AOC and does not need further approval related checking.
   */
  List<String> getDataSetAocInApproval(UID dataSet);

  /**
   * Fetches data to allow checking if writing is blocked by data approval.
   *
   * @param dataSet DS for scope
   * @param attrOptionCombo AOC for scope
   * @param orgUnits OUs to limit scope
   * @return for each OU (key) all the ISO periods that are in an ongoing or completed approval
   *     process. Writing a value is blocked by approval if its OU is a key in the result and its
   *     period is a value in the ISO period for the OU key. Ideally the result is empty, meaning no
   *     approval is happening and no further check for blockage by approval is needed.
   */
  Map<String, Set<String>> getApprovedIsoPeriodsByOrgUnit(
      UID dataSet, UID attrOptionCombo, Stream<UID> orgUnits);

  /**
   * A.K.A "lock exceptions".
   *
   * @param dataSet DS for scope
   * @return for each OU (key) all the ISO periods that are exempt from having to be entered within
   *     the timeframe of the period + DS expiry days duration.
   */
  Map<String, Set<String>> getExpiryDaysExemptedIsoPeriodsByOrgUnit(UID dataSet);

  /**
   * Fetches the explicitly defined data input periods for the dataset organised by period's ISO
   * value. It is likely that each period just has a single range but the model does allow for
   * multiple and occasionally this is used to add another "extra entry" period after the initial
   * one has been closed and checked.
   *
   * @param dataSet DS for scope
   * @return for each period ISO (key) the map contains all valid input periods for that period
   */
  Map<String, List<DateRange>> getEntrySpansByIsoPeriod(UID dataSet);

  /**
   * Fetches the entry span for each given AOC which is defined by the most restrictive timeframe of
   * all its COs start and end date. AOCs not contained in the result should be assumed to have an
   * infinite or unrestricted entry span.
   *
   * @param attributeOptionCombos the AOCs for scope
   * @return a map containing the entry span (value) for each given AOC (key)
   */
  Map<String, DateRange> getEntrySpanByAoc(Stream<UID> attributeOptionCombos);

  /**
   * Fetches the span in time when each OU is operational only for those OUs in the provided list
   * that do not fully include the start-end range.
   *
   * @param orgUnits OUs for scope
   * @param timeframe the range between most past start of any used period for the given OUs and
   *     most future end of any used period for the given OUs
   * @return for each OU (UID as key) the span in which it is operational (a.k.a. "open")
   */
  Map<String, DateRange> getEntrySpanByOrgUnit(Stream<UID> orgUnits, DateRange timeframe);
}
