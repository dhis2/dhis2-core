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
package org.hisp.dhis.analytics.common;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.feedback.ErrorCode.E7140;
import static org.hisp.dhis.feedback.ErrorCode.E7141;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.program.EnrollmentStatus;

/**
 * This object wraps the common params/request attributes used across analytics requests/flows. It
 * represents the client request which should remain intact until the very end for further usage.
 * They should not be transformed or overridden after the controller level.
 *
 * <p>Some objects are specified as Set/LinkedHashSet as they might require enforcing ordering and
 * avoid duplications.
 *
 * <p>TODO: This is VERY similar to org.hisp.dhis.analytics.common.params.CommonParams. They should
 * be unified.
 */
@Getter
@Setter
@With
@NoArgsConstructor
@AllArgsConstructor
public class CommonRequestParams {
  /** The list of program uids. */
  private Set<String> program = new LinkedHashSet<>();

  /** The user's organization unit. */
  private String userOrgUnit;

  /** The dimensions to be returned/filtered at. */
  private Set<String> dimension = new LinkedHashSet<>();

  /** The filters to be applied at querying time. */
  private Set<String> filter = new HashSet<>();

  /**
   * When set, the headers in the response object will match the specified headers in the respective
   * order. As the headers should not be duplicated, this is represented as Set.
   */
  private Set<String> headers = new LinkedHashSet<>();

  public static final OrganisationUnitSelectionMode DEFAULT_ORG_UNIT_SELECTION_MODE = DESCENDANTS;

  /**
   * The mode of selecting organisation units. Default is DESCENDANTS, meaning all sub units in the
   * hierarchy. CHILDREN refers to immediate children in the hierarchy; SELECTED refers to the
   * selected organisation units only.
   */
  private OrganisationUnitSelectionMode ouMode = DEFAULT_ORG_UNIT_SELECTION_MODE;

  /**
   * Id scheme to be used for data, more specifically data elements and attributes which have an
   * option set or legend set, e.g. return the name of the option instead of the code, or the name
   * of the legend instead of the legend ID, in the data response.
   */
  private IdScheme dataIdScheme;

  /** The general id scheme, which drives the values in the response object. */
  private IdScheme outputIdScheme;

  /**
   * Scheme used for metadata items in the response. Specific to data items. See {@link IdScheme}
   * for valid values.
   */
  private IdScheme outputDataItemIdScheme;

  /** The id scheme specific for data elements. */
  private IdScheme outputDataElementIdScheme;

  /** The id scheme specific for org units. */
  private IdScheme outputOrgUnitIdScheme;

  /** Overrides the start date of the relative period. e.g: "2016-01-01". */
  private Date relativePeriodDate;

  /** Indicates if the metadata element should be omitted from the response. */
  private boolean skipMeta;

  /** Indicates if the data should be omitted from the response. */
  private boolean skipData;

  /** Indicates if the headers should be omitted from the response. */
  private boolean skipHeaders;

  /** Indicates if full precision should be provided for numeric values. */
  private boolean skipRounding;

  /** Indicates if full metadata details should be provided. */
  private boolean includeMetadataDetails;

  /** Indicates if organization unit hierarchy should be provided. */
  private boolean hierarchyMeta;

  /** Indicates if additional ou hierarchy data should be provided. */
  private boolean showHierarchy;

  /** The page number. Default page is 1. */
  private Integer page = 1;

  /** The page size. */
  private Integer pageSize = 50;

  /**
   * The paging parameter. When set to false we should not paginate. The default is true (always
   * paginate).
   */
  private boolean paging = true;

  /**
   * The paging parameter. When set to false we should not count total pages. The default is false.
   */
  private boolean totalPages = false;

  /** When true, the pageSize can be higher than the system analytics max limit. */
  private boolean ignoreLimit = false;

  /**
   * Dimensions identifier to be sorted ascending, can reference event date, org unit name and code
   * and any item identifiers.
   */
  private Set<String> asc = new LinkedHashSet<>();

  /**
   * Dimensions identifier to be sorted descending, can reference event date, org unit name and code
   * and any item identifiers.
   */
  private Set<String> desc = new LinkedHashSet<>();

  /**
   * The program statuses to filter on.
   *
   * @deprecated use {@link #enrollmentStatus} instead
   */
  @Deprecated private Set<String> programStatus = new LinkedHashSet<>();

  /** The enrollment statuses to filter on. */
  private Set<String> enrollmentStatus = new LinkedHashSet<>();

  /** The event statuses to filter on. */
  private Set<String> eventStatus = new LinkedHashSet<>();

  /** The dimensional object for which to produce aggregated data. */
  private DimensionalItemObject value;

  /** Indicates which property to display. */
  private DisplayProperty displayProperty;

  private Set<String> occurredDate = new LinkedHashSet<>();

  // Time fields

  /**
   * @deprecated use {@link #occurredDate} instead. Kept for backward compatibility.
   */
  @Deprecated(since = "2.42")
  private Set<String> eventDate = new LinkedHashSet<>();

  private Set<String> enrollmentDate = new LinkedHashSet<>();

  private Set<String> scheduledDate = new LinkedHashSet<>();

  /**
   * @deprecated use {@link #occurredDate} instead. Kept for backward compatibility.
   */
  @Deprecated(since = "2.42")
  private Set<String> incidentDate = new LinkedHashSet<>();

  private Set<String> lastUpdated = new LinkedHashSet<>();

  private Set<String> created = new LinkedHashSet<>();

  private Internal internal = new Internal();

  /** Whether the query should consider only items with lat/long coordinates. */
  private boolean coordinatesOnly;

  /** Whether the query should consider only items with geometry. */
  private boolean geometryOnly;

  private static final Predicate<String> IS_EVENT_STATUS =
      s -> find(() -> EventStatus.valueOf(s)).isPresent();
  private static final Predicate<String> IS_ENROLLMENT_STATUS =
      s -> find(() -> EnrollmentStatus.valueOf(s)).isPresent();

  /**
   * Checks if there is a program uid in the internal list of programs.
   *
   * @return true if at least one program is found, false otherwise.
   */
  public boolean hasPrograms() {
    return emptyIfNull(program).stream().anyMatch(StringUtils::isNotBlank);
  }

  /**
   * Whether the request has any program status filters.
   *
   * @return true if at least one program status is found, false otherwise.
   */
  public boolean hasProgramStatus() {
    return emptyIfNull(programStatus).stream().anyMatch(StringUtils::isNotBlank);
  }

  /**
   * Whether the request has any enrollment status filters.
   *
   * @return true if at least one enrollment status is found, false otherwise.
   */
  public boolean hasEnrollmentStatus() {
    return emptyIfNull(enrollmentStatus).stream().anyMatch(StringUtils::isNotBlank);
  }

  /**
   * Whether the request has any event status filters.
   *
   * @return true if at least one event status is found, false otherwise.
   */
  public boolean hasEventStatus() {
    return emptyIfNull(eventStatus).stream().anyMatch(StringUtils::isNotBlank);
  }

  public Set<String> getAllDimensions() {
    Set<String> allDimensions = new LinkedHashSet<>(dimension);
    allDimensions.addAll(computeEventStatus(this));
    allDimensions.addAll(computeEnrollmentStatus(this));
    allDimensions.addAll(internal.getEntityTypeAttributes());
    allDimensions.addAll(internal.getProgramAttributes());

    return unmodifiableSet(allDimensions);
  }

  /**
   * Converts the program status into a static dimension example:
   * enrollmentStatus=IpHINAT79UW.COMPLETED;IpHINAT79UW.ACTIVE becomes
   * dimension=IpHINAT79UW.PROGRAM_STATUS:COMPLETED;ACTIVE
   *
   * @param commonRequestParams the {@link CommonRequestParams} to enrich.
   * @return the list of dimension items.
   */
  private Set<String> computeEnrollmentStatus(CommonRequestParams commonRequestParams) {
    if (commonRequestParams.hasProgramStatus() || commonRequestParams.hasEnrollmentStatus()) {
      // Merging programStatus and enrollmentStatus into a single set.
      Set<String> enrollmentStatuses = new LinkedHashSet<>();
      enrollmentStatuses.addAll(commonRequestParams.getProgramStatus());
      enrollmentStatuses.addAll(commonRequestParams.getEnrollmentStatus());

      return enrollmentStatusAsDimension(enrollmentStatuses);
    }

    return Set.of();
  }

  /**
   * Extracts the event status as dimension's filters:
   * eventStatus=IpHINAT79UW.A03MvHHogjR.SCHEDULE;IpHINAT79UW.A03MvHHogjR.ACTIVE becomes
   * dimension=IpHINAT79UW.A03MvHHogjR.EVENT_STATUS:SCHEDULE;ACTIVE
   *
   * @param commonRequestParams the {@link CommonRequestParams}
   * @return the list of dimension items.
   */
  private Set<String> computeEventStatus(CommonRequestParams commonRequestParams) {
    if (commonRequestParams.hasEventStatus()) {
      return eventStatusAsDimension(commonRequestParams.getEventStatus());
    }

    return Set.of();
  }

  private Set<String> eventStatusAsDimension(Set<String> eventStatuses) {
    // Builds a map of [program,program stage] with a list of event statuses.
    Map<Pair<String, String>, List<EventStatus>> statusesByProgramAndProgramStage =
        eventStatuses.stream()
            .map(evStatus -> splitAndValidate(evStatus, IS_EVENT_STATUS, 3, E7141))
            .map(
                parts ->
                    Pair.of(
                        Pair.of(parts[0], parts[1]),
                        find(() -> EventStatus.valueOf(parts[2])).orElse(null)))
            .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, Collectors.toList())));

    return statusesByProgramAndProgramStage.keySet().stream()
        .map(
            programWithStage ->
                programWithStage.getLeft()
                    + // Program
                    DIMENSION_IDENTIFIER_SEP
                    + programWithStage.getRight()
                    + // Program stage
                    DIMENSION_IDENTIFIER_SEP
                    + "EVENT_STATUS"
                    + // "EVENT_STATUS"
                    DIMENSION_NAME_SEP
                    +
                    // ";" concatenated values - for example "COMPLETED;SKIPPED"
                    statusesByProgramAndProgramStage.get(programWithStage).stream()
                        .filter(Objects::nonNull)
                        .map(EventStatus::name)
                        .collect(Collectors.joining(";")))
        .collect(Collectors.toSet());
  }

  private Set<String> enrollmentStatusAsDimension(Set<String> enrollmentStatuses) {
    // Builds a map of [program] with a list of enrollment statuses.
    Map<String, List<EnrollmentStatus>> statusesByProgram =
        enrollmentStatuses.stream()
            .map(status -> splitAndValidate(status, IS_ENROLLMENT_STATUS, 2, E7140))
            .map(
                parts ->
                    Pair.of(parts[0], find(() -> EnrollmentStatus.valueOf(parts[1])).orElse(null)))
            .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, Collectors.toList())));

    return statusesByProgram.keySet().stream()
        .map(
            p ->
                p
                    + DIMENSION_IDENTIFIER_SEP
                    + "ENROLLMENT_STATUS"
                    + DIMENSION_NAME_SEP
                    + statusesByProgram.get(p).stream()
                        .filter(Objects::nonNull)
                        .map(EnrollmentStatus::name)
                        .collect(Collectors.joining(";")))
        .collect(Collectors.toSet());
  }

  /**
   * Splits the given parameter by the dot character and validates that the resulting array has the
   * given length. If the length is not correct, an {@link IllegalQueryException} is thrown.
   *
   * @param parameter the parameter to split
   * @param allowedLength the allowed length of the resulting array
   * @param errorCode the error code to use in case of error
   * @return the resulting array
   */
  private String[] splitAndValidate(
      String parameter, Predicate<String> valueValidator, int allowedLength, ErrorCode errorCode) {
    String[] parts = parameter.split("\\.");

    // If the number of parts is the same as the allowed length, we should validate the last part.
    if (parts.length == allowedLength) {
      // In this case the last part is the value and should be validated.
      if (valueValidator.test(parts[parts.length - 1])) {
        return parts;
      }
      throw new IllegalQueryException(new ErrorMessage(errorCode));
    }

    // If the number of parts is less than the allowed length, we ensure the last part is
    // not valid enum value.
    if (parts.length == allowedLength - 1 && !valueValidator.test(parts[parts.length - 1])) {
      return parts;
    }

    throw new IllegalQueryException(new ErrorMessage(errorCode));
  }

  /**
   * Tries to get a value from the given supplier, and returns an Optional with the value if
   * successful, or an empty Optional if an exception is thrown.
   *
   * @param supplier the supplier to get the value from
   * @return an Optional with the value if successful, or an empty Optional
   * @param <T>
   */
  private static <T> Optional<T> find(Supplier<T> supplier) {
    try {
      return Optional.of(supplier.get());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @OpenApi.Ignore
  public DimensionalItemObject getValue() {
    return value;
  }

  /**
   * Encapsulates all internal objects/flags that cannot be exposed externally, but are necessary
   * internally. It holds states that are required by analytics flows at some point.
   */
  @Data
  public static class Internal {
    /** The dimensions of the given entity type. Internal only. */
    private Set<String> entityTypeAttributes = new LinkedHashSet<>();

    /** The dimensions of the given program. Internal only. */
    private Set<String> programAttributes = new LinkedHashSet<>();

    /** Indicates if this request contains one or more programs as URL params. Internal use only. */
    private boolean requestPrograms;
  }
}
