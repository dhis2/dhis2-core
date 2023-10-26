/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.analytics.AggregationType.MAX;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.DataType.fromValueType;
import static org.hisp.dhis.analytics.data.JdbcAnalyticsManager.AO;
import static org.hisp.dhis.analytics.data.JdbcAnalyticsManager.CO;
import static org.hisp.dhis.analytics.data.JdbcAnalyticsManager.DX;
import static org.hisp.dhis.analytics.data.JdbcAnalyticsManager.OU;
import static org.hisp.dhis.analytics.data.JdbcAnalyticsManager.VALUE;
import static org.hisp.dhis.analytics.data.SubexpressionPeriodOffsetUtils.DELTA;
import static org.hisp.dhis.analytics.data.SubexpressionPeriodOffsetUtils.REPORTPERIOD;
import static org.hisp.dhis.analytics.data.SubexpressionPeriodOffsetUtils.SHIFT;
import static org.hisp.dhis.analytics.data.SubexpressionPeriodOffsetUtils.getParamsWithOffsetPeriodsWithoutData;
import static org.hisp.dhis.analytics.data.SubexpressionPeriodOffsetUtils.joinPeriodOffsetValues;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.encode;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.commons.collection.CollectionUtils.addUnique;
import static org.hisp.dhis.parser.expression.ParserUtils.castSql;
import static org.hisp.dhis.subexpression.SubexpressionDimensionItem.getItemColumnName;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.subexpression.SubexpressionDimensionItem;

/**
 * This class is responsible for generating an analytics query to fetch data from a subexpression.
 *
 * <p>A subexpression is evaluated before aggregating across organisation units but after
 * aggregating over any other dimensions. This can be illustrated with a simple subexpression:
 *
 * <pre>
 *       subExpression(#{A2VfEfPflHV} / #{AFM5H0wNq3t})
 * </pre>
 *
 * This example subexpression is not useful in practice (since it's just adding the values and then
 * summing them) but it can demonstrate the query logic.
 *
 * <p>The following simplified query shows the basic logic of how subexpression values are computed.
 * (The actual query has more quotes, aliases, etc.)
 *
 * <pre>
 * select uidlevel2, monthly, 'subExpreUid' as dx, sum("A2VfEfPflHV" + "AFM5H0wNq3t") as value
 * from (select uidlevel2,
 *              monthly,
 *              sum(case when dx = 'A2VfEfPflHV' then value else null end) as "A2VfEfPflHV",
 *              sum(case when dx = 'AFM5H0wNq3t' then value else null end) as "AFM5H0wNq3t"
 *       from analytics
 *       where monthly in ('202305', '202306')
 *       and dx in ('A2VfEfPflHV', 'AFM5H0wNq3t') // (greatly improves performance)
 *       group by uidlevel2, monthly, ou) as ax
 * where "A2VfEfPflHV" + "AFM5H0wNq3t" is not null
 * group by uidlevel2, monthly;
 * </pre>
 *
 * If there are data elements (or data element operands) inside the subexpresion having a
 * .periodOffset query modifier (with a non-zero value), then an inline table is joined which allows
 * for mapping between data periods (the period in the database) and reporting periods (the period
 * for which the data is reported, after applying the period offset). For example, consider the
 * subexpression:
 *
 * <pre>
 *       subExpression( #{A2VfEfPflHV} + #{A2VfEfPflHV}.periodOffset(-1) )
 * </pre>
 *
 * <p>This would generate the following query logic (again, simplified here) when evaluated for the
 * two months 202309 and 202310:
 *
 * <pre>
 * select uidlevel2, monthly, 'subExpreUid' as dx, sum("A2VfEfPflHV" + "A2VfEfPflHV_minus_1") as value
 * from (select uidlevel2,
 *              shift.reportperiod as monthly,
 *              sum(case when dx = 'A2VfEfPflHV' and shift.delta = 0 then value else null end) as "A2VfEfPflHV",
 *              sum(case when dx = 'A2VfEfPflHV' and shift.delta = -1 then value else null end) as "A2VfEfPflHV_minus_1"
 *       from analytics
 *       join (values(-1,'202309','202308'),(-1,'202310','202309'),
 *                   (0,'202309','202309'),(0,'202310','202310'))
 *              as shift (delta, reportperiod, dataperiod) on dataperiod = monthly
 *       where monthly in ('202308', '202309', '202310')
 *       and dx in ('A2VfEfPflHV') // (greatly improves performance)
 *       group by uidlevel2, monthly, ou) as ax
 * where "A2VfEfPflHV" is not null
 * group by uidlevel2, monthly;
 * </pre>
 *
 * @author Jim Grace
 */
public class JdbcSubexpressionQueryGenerator {
  /** {@link JdbcAnalyticsManager} for callbacks. */
  private final JdbcAnalyticsManager jam;

  /** Query parameters. */
  private final DataQueryParams params;

  /** Analytics table type. */
  private final AnalyticsTableType tableType;

  /** Copy of parameters but without the data dimension. */
  private final DataQueryParams paramsWithoutData;

  /** The subexpression being processed, from the parameters. */
  private final SubexpressionDimensionItem subex;

  /** Whether this subexpression has any period offsets. */
  private final boolean hasPeriodOffsets;

  public JdbcSubexpressionQueryGenerator(
      JdbcAnalyticsManager jam, DataQueryParams params, AnalyticsTableType tableType) {
    this.jam = jam;
    this.params = params;
    this.tableType = tableType;
    this.paramsWithoutData =
        DataQueryParams.newBuilder(params).removeDimension(DATA_X_DIM_ID).build();
    this.subex = params.getSubexpression();
    this.hasPeriodOffsets = subex.hasPeriodOffsets();
  }

  /**
   * Gets the SQL for an analytics query containing a subexpression.
   *
   * @return the SQL query.
   */
  public String getSql() {
    String select = getSelect();

    String from = getFrom();

    String where = "where " + subex.getSubexSql() + " is not null ";

    String groupBy = jam.getGroupByClause(paramsWithoutData);

    return select + from + where + groupBy;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Gets the select SQL. */
  private String getSelect() {
    String dimensions =
        jam.getCommaDelimitedQuotedDimensionColumns(paramsWithoutData.getDimensions());

    String data = encode(subex.getDimensionItemWithQueryModsId()) + " as " + quote(DX);

    String aggregate = getHighLevelAggregateFunction();

    String value = subex.getSubexSql();

    return "select "
        + dimensions
        + ","
        + data
        + ","
        + aggregate
        + "("
        + value
        + ") as "
        + quote(VALUE)
        + " ";
  }

  /** Gets the from clause for the main query, which is a subquery. */
  private String getFrom() {
    String selectSub = getSelectSubquery();

    String fromSub = "from " + jam.getFromSourceClause(params) + " as " + ANALYTICS_TBL_ALIAS + " ";

    String joinSub = (hasPeriodOffsets) ? joinPeriodOffsetValues(params) : "";

    String whereSub = getWhereSubquery();

    String groupBySub = getGroupBySubquery();

    return "from ("
        + selectSub
        + fromSub
        + joinSub
        + whereSub
        + groupBySub
        + ") as "
        + ANALYTICS_TBL_ALIAS
        + " ";
  }

  /** Gets the subquery select clause. */
  private String getSelectSubquery() {
    String dimensionColumns =
        (hasPeriodOffsets)
            ? getPeriodOffsetSelectDimensionColumns()
            : jam.getCommaDelimitedQuotedDimensionColumns(paramsWithoutData.getDimensions());

    String subexItemColumns =
        subex.getItems().stream().map(this::getItemSql).distinct().collect(joining(","));

    return "select " + dimensionColumns + ", " + subexItemColumns + " ";
  }

  private String getPeriodOffsetSelectDimensionColumns() {
    String nonPeriodDimensions =
        jam.getCommaDelimitedQuotedDimensionColumns(paramsWithoutData.getNonPeriodDimensions());
    return SHIFT
        + "."
        + REPORTPERIOD
        + " as "
        + paramsWithoutData.getPeriodType().toLowerCase()
        + (nonPeriodDimensions.isEmpty() ? "" : "," + nonPeriodDimensions);
  }

  /** Gets the subquery where clause. */
  private String getWhereSubquery() {
    DataQueryParams whereSubQueryParams =
        hasPeriodOffsets ? getParamsWithOffsetPeriodsWithoutData(params) : paramsWithoutData;

    whereSubQueryParams = getParamsWithPeriodType(whereSubQueryParams);

    String sql = jam.getWhereClause(whereSubQueryParams, tableType);

    if (!sql.isEmpty()) {
      sql += "and ";
    }

    sql += quote(ANALYTICS_TBL_ALIAS, DX) + " in (" + getSubexpressionDataElementList() + ") ";

    return sql;
  }

  /** Gets the subquery group by clause. */
  private String getGroupBySubquery() {
    DataQueryParams groupByParams =
        hasPeriodOffsets
            ? DataQueryParams.newBuilder(paramsWithoutData).removeDimension(PERIOD_DIM_ID).build()
            : getParamsWithPeriodType(paramsWithoutData);

    List<String> cols = jam.getQuotedDimensionColumns(groupByParams.getDimensions());

    addUnique(cols, quote(ANALYTICS_TBL_ALIAS, OU));

    if (hasPeriodOffsets) {
      cols.add(SHIFT + "." + REPORTPERIOD);
    }

    return " group by " + join(",", cols);
  }

  /**
   * Gets parameters where the period type will be the selected column for the period (so a query
   * for this column will also data from enclosed, shorter periods).
   */
  private DataQueryParams getParamsWithPeriodType(DataQueryParams query) {
    String periodType = query.getPeriodType();
    if (periodType != null) {
      BaseDimensionalObject periodDim =
          new BaseDimensionalObject(
              DimensionalObject.PERIOD_DIM_ID,
              DimensionType.PERIOD,
              periodType.toLowerCase(),
              null,
              query.getPeriods());
      query = DataQueryParams.newBuilder(query).replaceDimension(periodDim).build();
    }
    return query;
  }

  /** Gets a comma-separated list of the quoted UIDs of the data elements in the subexpression. */
  private String getSubexpressionDataElementList() {
    return subex.getItems().stream()
        .map(this::getQuotedDataElementUid)
        .distinct()
        .collect(joining(","));
  }

  /** Gets the data element UID from a data element or data element operand. */
  private String getQuotedDataElementUid(DimensionalItemObject item) {
    return "'"
        + ((item instanceof DataElementOperand deo) ? deo.getDataElement().getUid() : item.getUid())
        + "'";
  }

  /**
   * Gets a SQL fragment that can be used to return the value of a {@link DataElement} or {@link
   * DataElementOperand} aggregated over the analytics rows withing an organisation unit.
   */
  private String getItemSql(DimensionalItemObject item) {
    DataElement dataElement =
        (item instanceof DataElementOperand deo) ? deo.getDataElement() : (DataElement) item;

    String deUid = dataElement.getUid();
    String cocUid = null;
    String aocUid = null;

    if (item instanceof DataElementOperand deo) {
      cocUid = (deo.getCategoryOptionCombo() != null) ? deo.getCategoryOptionCombo().getUid() : "";
      aocUid =
          (deo.getAttributeOptionCombo() != null) ? deo.getAttributeOptionCombo().getUid() : "";
    }

    String fun = getLowLevelAggregateFunction(dataElement);

    String conditional =
        quote(ANALYTICS_TBL_ALIAS, DX)
            + "='"
            + deUid
            + "'"
            + (isEmpty(cocUid)
                ? ""
                : " and " + quote(ANALYTICS_TBL_ALIAS, CO) + "='" + cocUid + "'")
            + (isEmpty(aocUid)
                ? ""
                : " and " + quote(ANALYTICS_TBL_ALIAS, AO) + "='" + aocUid + "'")
            + (hasPeriodOffsets
                ? " and " + SHIFT + "." + DELTA + " = " + item.getPeriodOffset()
                : "");

    String value = quote(dataElement.getValueColumn());

    DataType dataType = fromValueType(dataElement.getValueType());
    if (dataType == BOOLEAN) {
      dataType = NUMERIC; // Booleans are always aggregated as numeric in subex.
    }
    String cast = castSql("", dataType);

    String column = getItemColumnName(deUid, cocUid, aocUid, dataElement.getQueryMods());

    return fun
        + "(case when "
        + conditional
        + " then "
        + value
        + cast
        + " else null end)"
        + " as "
        + column;
  }

  /**
   * Gets the aggregation type to be used at the lower level to aggregate data within an
   * organisation unit before subexpression evaluation.
   *
   * <p>For numeric data types, gets the aggregationType configured for the data element unless
   * overridden in the expression.
   *
   * <p>For non-numeric data types, returns MAX unless overridden in the expression.
   */
  private String getLowLevelAggregateFunction(DataElement dataElement) {
    AggregationType dataElementAggType =
        (dataElement.getValueType().isNumeric()
                || dataElement.getQueryMods() != null
                    && dataElement.getQueryMods().getAggregationType() != null)
            ? dataElement.getAggregationType()
            : MAX;

    AnalyticsAggregationType analyticsAggType = fromAggregationType(dataElementAggType);

    return aggregateFunctionFromType(analyticsAggType.getPeriodAggregationType());
  }

  /**
   * Gets the aggregate function for the higher level of aggregating the subexpression result by
   * organisation unit.
   */
  private String getHighLevelAggregateFunction() {
    AggregationType subexAggType = subex.getAggregationType();

    AnalyticsAggregationType analyticsAggType = fromAggregationType(subexAggType);

    return aggregateFunctionFromType(analyticsAggType.getAggregationType());
  }

  /**
   * Gets the SQL aggregation function to be used for a simple aggregation type. Note that all the
   * compound aggregation types have already been broken down to a simple type for lower level
   * aggregation within an organisation unit or higher level aggregation among organisation units.
   */
  private String aggregateFunctionFromType(AggregationType aggType) {
    return switch (aggType) {
      case AVERAGE -> "avg";
      case MAX, MIN, COUNT, STDDEV, VARIANCE -> aggType.name().toLowerCase();
      default -> "sum";
    };
  }
}
