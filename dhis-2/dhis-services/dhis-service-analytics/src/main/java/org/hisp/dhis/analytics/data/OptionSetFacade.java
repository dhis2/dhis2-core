/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.data;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;
import static org.hisp.dhis.analytics.OptionSetSelectionMode.AGGREGATED;
import static org.hisp.dhis.analytics.OptionSetSelectionMode.DISAGGREGATED;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getFirstIdentifier;
import static org.hisp.dhis.common.DimensionalObjectUtils.getOptionsParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getSecondIdentifier;
import static org.hisp.dhis.common.DimensionalObjectUtils.getThirdIdentifier;
import static org.hisp.dhis.common.DimensionalObjectUtils.getValueFromDimensionParam;
import static org.hisp.dhis.common.collection.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OptionSetSelection;
import org.hisp.dhis.analytics.OptionSetSelectionCriteria;
import org.hisp.dhis.analytics.OptionSetSelectionMode;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OptionSetFacade {
  private final IdentifiableObjectManager idObjectManager;

  /**
   * Add queries to the given list of {@link EventQueryParams} based on the params {@link
   * EventQueryParams}. It respects rules related to AGGREGATED option set ({@link AGGREGATED}).
   *
   * @param params the {@link EventQueryParams}.
   * @param queries the list of {@link EventQueryParams}.
   * @param item the {@link QueryItem}.
   */
  public void handleAggregatedOptionSet(
      EventQueryParams params, List<EventQueryParams> queries, QueryItem item) {

    OptionSetSelectionCriteria optionSetSelectionCriteria = params.getOptionSetSelectionCriteria();
    OptionSetSelectionMode optionSetSelectionMode =
        optionSetSelectionCriteria.getOptionSetSelections().entrySet().stream()
            .toList()
            .get(0)
            .getValue()
            .getOptionSetSelectionMode();

    if (optionSetSelectionMode == AGGREGATED) {
      AnalyticsAggregationType aggregationType =
          firstNonNull(params.getAggregationType(), fromAggregationType(item.getAggregationType()));

      EventQueryParams.Builder query =
          new EventQueryParams.Builder(params)
              .removeItems()
              .removeItemProgramIndicators()
              .withValue(item.getItem())
              .withAggregationType(aggregationType);

      if (item.hasProgram()) {
        query.withProgram(item.getProgram());
      }

      queries.add(query.build());
    }
  }

  /**
   * Add queries to the given list of {@link EventQueryParams} based on the params {@link
   * EventQueryParams}. It respects rules related to DISAGGREGATED option set ({@link
   * OptionSetSelectionMode.DISAGGREGATED}).
   *
   * @param params the {@link EventQueryParams}.
   * @param queries the list of {@link EventQueryParams}.
   */
  public void handleDisaggregatedOptionSet(
      EventQueryParams params, List<EventQueryParams> queries) {
    if (params.hasOptionSetSelections()) {
      for (Map.Entry<String, OptionSetSelection> entry :
          params.getOptionSetSelectionCriteria().getOptionSetSelections().entrySet()) {
        if (entry.getValue() != null
            && entry.getValue().getOptionSetSelectionMode() == DISAGGREGATED) {
          for (String option : entry.getValue().getOptions()) {
            Set<String> optionSet = new LinkedHashSet<>();
            optionSet.add(option);

            OptionSetSelection optionSetSelection =
                new OptionSetSelection(
                    entry.getValue().getQualifiedUid(),
                    entry.getValue().getOptionSetUid(),
                    optionSet,
                    DISAGGREGATED);

            Map<String, OptionSetSelection> optionSetSelectionMap = new HashMap<>();
            optionSetSelectionMap.put(entry.getKey(), optionSetSelection);

            OptionSetSelectionCriteria optionSetSelectionCriteria =
                new OptionSetSelectionCriteria(optionSetSelectionMap);

            Program program = getProgram(params.getItems(), entry.getValue().getQualifiedUid());

            EventQueryParams query =
                new EventQueryParams.Builder(params)
                    .withAggregationType(params.getAggregationType())
                    .withOptionSetSelectionCriteria(optionSetSelectionCriteria)
                    .withProgram(program)
                    .build();

            queries.add(query);
          }
        }
      }
    }
  }

  /**
   * Creates a {@link OptionSetSelectionCriteria} object based on the given collection of
   * dimensions.
   *
   * @param dimensions the collection of dimensions.
   * @return the {@link OptionSetSelectionCriteria} object.
   */
  public OptionSetSelectionCriteria getOptionSetSelectionCriteria(Set<String> dimensions) {
    Map<String, OptionSetSelection> optionSetSelections = new HashMap<>();
    Set<String> splitDimensions = new LinkedHashSet<>();

    for (String dimension : dimensions) {
      if (dimension.startsWith("dx:")) {
        splitDimensions.addAll(getDimensionItemsFromParam(dimension));
      }
    }

    for (String dimension : splitDimensions) {
      String dimValue = getValueFromDimensionParam(dimension);

      if (hasOptionSet(dimValue)) {
        OptionSetSelectionMode mode = DimensionalObjectUtils.getOptionSetSelectionMode(dimValue);
        String dimIdentifier = getDimensionIdentifier(dimValue);
        String optionsParam = getOptionsParam(dimValue);
        Set<String> options = extractOptions(optionsParam);

        if (mode == DISAGGREGATED && isEmpty(options)) {
          DataElement dataElement =
              this.idObjectManager.get(DataElement.class, substringBefore(dimIdentifier, "."));
          if (dataElement.getOptionSet() != null) {
            options = dataElement.getOptionSet().getOptionUids();
          }
        }

        OptionSetSelection optionSetSelection =
            new OptionSetSelection(dimValue, dimIdentifier, options, mode);

        optionSetSelections.put(dimIdentifier, optionSetSelection);
      }
    }

    return new OptionSetSelectionCriteria(optionSetSelections);
  }

  /**
   * Returns a "where" clause for options selected, if any.
   *
   * @param params the {@link DataQueryParams}.
   * @param sqlHelper the {@link SqlHelper}.
   * @param sqlBuilder the {@link SqlBuilder}.
   * @param sql the {@link StringBuilder}.
   */
  static void addWhereClauseForOptions(
      DataQueryParams params,
      SqlHelper sqlHelper,
      SqlBuilder sqlBuilder,
      StringBuilder sql,
      String items) {
    if (params.hasOptionSetSelections()) {
      params
          .getOptionSetSelectionCriteria()
          .getOptionSetSelections()
          .forEach(
              (key, value) -> {
                Set<String> options = value.getOptions();
                if (isNotEmpty(options)
                    && items.contains(value.getOptionSetUid())) { // TODO: MAIKEL revisit
                  // OptionSetSelectionCriteria thing.
                  sql.append(" ")
                      .append(sqlHelper.whereAnd())
                      .append(" ")
                      .append(sqlBuilder.quote("optionvalueuid"))
                      .append(" in ('")
                      .append(String.join("','", options))
                      .append("') ");
                }
              });
    }
  }

  static OptionSetSelectionMode getOptionSetSelectionMode(@Nonnull DataQueryParams params) {
    Optional<OptionSetSelectionMode> optionSetSelectionMode = Optional.empty();

    for (DimensionalItemObject de : params.getDataElements()) {
      if (params.hasOptionSetSelections() && ((DataElement) de).getOptionSet() != null) {
        OptionSetSelectionMode setSelectionMode =
            params
                .getOptionSetSelectionCriteria()
                .getOptionSetSelections()
                .get(
                    de.getUid()
                        + DIMENSION_IDENTIFIER_SEP
                        + ((DataElement) de).getOptionSet().getUid())
                .getOptionSetSelectionMode();
        optionSetSelectionMode = Optional.of(setSelectionMode);
        break;
      }
    }

    return optionSetSelectionMode.orElse(AGGREGATED);
  }

  static String getAggregatedOptionValueClause(DataQueryParams params) {
    if (params.hasOptionSetInDimensionItemsTypeDataElement()) {
      return ", count(" + params.getValueColumn() + ") as valuecount ";
    }

    return EMPTY;
  }

  /**
   * Extracts the dimension uid based on the given argument and internal rules, depending on the
   * composition of the value.
   *
   * @param composedDimension ie: WSGAb5XwJ3Y.QFX1FLWBwtq.R3ShQczKnI9[l8S7SjnQ58G;x7H1HjJ0R64]
   * @return the respective dimension uid.
   */
  private String getDimensionIdentifier(String composedDimension) {
    String dimIdentifier = getThirdIdentifier(composedDimension);

    if (dimIdentifier == null) {
      dimIdentifier =
          getFirstIdentifier(composedDimension)
              + DIMENSION_IDENTIFIER_SEP
              + getSecondIdentifier(composedDimension);
    } else {
      dimIdentifier =
          getSecondIdentifier(composedDimension) + DIMENSION_IDENTIFIER_SEP + dimIdentifier;
    }

    return dimIdentifier;
  }

  /**
   * Extracts the options uids specified in the URL param, if any.
   *
   * @param options the URL param options.
   * @return the options uids found, or empty.
   */
  private Set<String> extractOptions(String options) {
    if (isNotBlank(options)) {
      Set<String> optionSet = new LinkedHashSet<>();

      for (String uid : options.split(OPTION_SEP)) {
        Option option = this.idObjectManager.get(Option.class, uid);
        if (option != null) {
          optionSet.add(option.getUid());
        }
      }

      return optionSet;
    }

    return Set.of();
  }

  private boolean hasOptionSet(String param) {
    String uid = getThirdIdentifier(param);

    if (uid == null) {
      uid = getSecondIdentifier(param);
    }

    return uid != null && idObjectManager.exists(OptionSet.class, uid);
  }

  private Program getProgram(List<QueryItem> items, String dimension) {
    for (QueryItem item : items) {
      if (item.hasProgram() && dimension.startsWith(item.getProgram().getUid())) {
        return item.getProgram();
      }
    }

    return null;
  }
}
