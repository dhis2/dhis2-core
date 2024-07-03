/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier.DimensionIdentifierType.TEI;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.hasRestrictions;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.isOfType;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.TeiAttributeCondition;
import org.hisp.dhis.analytics.tei.query.TeiFields;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilderAdaptor;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.springframework.stereotype.Service;

/**
 * This builder is responsible for building the SQL query for the TEI table. It will generate the
 * relevant SQL parts related to dimensions/filters/sortingParameters having one the following
 * structure: - {teiField} - {programUid}.{programAttribute}
 */
@Service
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class TeiQueryBuilder extends SqlQueryBuilderAdaptor {
  private static final String JSON_AGGREGATION_QUERY =
      """
        coalesce( (select json_agg(json_build_object('programUid', pr.uid,
                                              'enrollmentUid', en.programinstanceuid,
                                              'enrollmentDate', en.enrollmentdate,
                                              'incidentDate', en.incidentdate,
                                              'endDate', en.enddate,
                                              'orgUnitUid', en.ou,
                                              'orgUnitName', en.ouname,
                                              'orgUnitCode', en.oucode,
                                              'orgUnitNameHierarchy', en.ounamehierarchy,
                                              'enrollmentStatus', en.enrollmentstatus,
                                              'events',
                                              coalesce( (select json_agg(json_build_object('programStageUid', ps.uid,
                                                                                 'eventUid', ev.programstageuid,
                                                                                 'occurredDate', ev.occurreddate,
                                                                                 'dueDate', ev.scheduleddate,
                                                                                 'orgUnitUid', ev.ou,
                                                                                 'orgUnitName', ev.ouname,
                                                                                 'orgUnitCode', ev.oucode,
                                                                                 'orgUnitNameHierarchy', ev.ounamehierarchy,
                                                                                 'eventStatus', ev.status,
                                                                                 'eventDataValues', ev.eventdatavalues))
                                               from analytics_tei_events_%s ev,
                                                    programstage ps
                                               where ev.programinstanceuid = en.programinstanceuid
                                                 and ps.uid = ev.programstageuid), '[]'::json)))
            from analytics_tei_enrollments_%s en,
                 program pr
            where en.trackedentityinstanceuid = t_1.trackedentityinstanceuid
              and pr.uid = en.programuid), '[]'::json)""";

  private final IdentifiableObjectManager identifiableObjectManager;

  @Override
  public boolean alwaysRun() {
    return true;
  }

  @Getter
  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(
          SqlQueryBuilders::isNotPeriodDimension,
          OrgUnitQueryBuilder::isNotOuDimension,
          TeiQueryBuilder::isTeiRestriction);

  @Getter
  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(
          sortingParams -> SqlQueryBuilders.isNotPeriodDimension(sortingParams.getOrderBy()),
          sortingParams -> OrgUnitQueryBuilder.isNotOuDimension(sortingParams.getOrderBy()),
          TeiQueryBuilder::isTeiOrder);

  @Override
  protected Stream<Field> getSelect(QueryContext queryContext) {
    String aggregationQuery =
        JSON_AGGREGATION_QUERY.formatted(
            queryContext.getTetTableSuffix(), queryContext.getTetTableSuffix());

    Field aggregatedEnrollments =
        Field.ofUnquoted(EMPTY, () -> aggregationQuery, "enrollments").withUsedInHeaders(false);

    return Stream.of(
            // Organisation unit group set columns.
            identifiableObjectManager
                .getDataDimensionsNoAcl(OrganisationUnitGroupSet.class)
                .stream()
                .map(OrganisationUnitGroupSet::getUid)
                .map(attr -> Field.of(TEI_ALIAS, () -> attr, attr)),
            // Static fields column.
            TeiFields.getStaticFields(),
            // Tei/Program attributes.
            TeiFields.getDimensionFields(queryContext.getTeiQueryParams()),
            // Enrollments
            Stream.of(aggregatedEnrollments))
        .flatMap(Function.identity());
  }

  @Override
  protected Stream<GroupableCondition> getWhereClauses(
      QueryContext queryContext, List<DimensionIdentifier<DimensionParam>> acceptedDimensions) {
    return acceptedDimensions.stream()
        .map(
            dimId ->
                GroupableCondition.of(
                    dimId.getGroupId(), TeiAttributeCondition.of(dimId, queryContext)));
  }

  /**
   * Returns true if the given dimension identifier has restrictions and is either a TEI dimension
   * or a Program indicator
   *
   * @param dimensionIdentifier the dimension identifier
   * @return true if the given dimension identifier has restrictions and is either a TEI dimension
   *     or a Program indicator
   */
  private static boolean isTeiRestriction(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return hasRestrictions(dimensionIdentifier) && isTei(dimensionIdentifier);
  }

  /**
   * Returns true if the given dimension identifier is either a TEI dimension or a Program indicator
   *
   * @param dimensionIdentifier the dimension identifier
   * @return true if the given dimension identifier is either a TEI dimension or a Program indicator
   */
  private static boolean isTei(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return
    // Will match all dimensionIdentifiers like {dimensionUid}.
    dimensionIdentifier.getDimensionIdentifierType() == TEI
        ||
        // Will match all dimensionIdentifiers whose type is PROGRAM_ATTRIBUTE.
        // e.g. {attributeUid}
        isOfType(dimensionIdentifier, PROGRAM_ATTRIBUTE);
  }

  @Override
  protected Stream<IndexedOrder> getOrderClauses(
      QueryContext queryContext, List<AnalyticsSortingParams> acceptedSortingParams) {
    return acceptedSortingParams.stream().map(this::toIndexedOrder);
  }

  private IndexedOrder toIndexedOrder(AnalyticsSortingParams param) {
    // Here, we can assume that param is either a static dimension or
    // a TEI/Program attribute in the form asc=pUid.dimension (or desc=pUid.dimension)
    // in both cases the column for the select is the same.
    String column = param.getOrderBy().getDimension().getUid();
    return IndexedOrder.of(param.getIndex(), Order.of(Field.of(column), param.getSortDirection()));
  }

  private static boolean isTeiOrder(AnalyticsSortingParams analyticsSortingParams) {
    return isTei(analyticsSortingParams.getOrderBy());
  }
}