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
package org.hisp.dhis.datavalue.hibernate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataExportStoreParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Torgeir Lorange Ostby
 */
@Slf4j
@Repository
public class HibernateDataValueStore extends HibernateGenericStore<DataValue>
    implements DataValueStore {

  private static final String DELETED = "deleted";

  private static final String LAST_UPATED = "lastUpdated";

  public HibernateDataValueStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataValue.class, false);
  }

  // -------------------------------------------------------------------------
  // Basic DataValue
  // -------------------------------------------------------------------------

  @Override
  public void deleteDataValues(OrganisationUnit organisationUnit) {
    String hql = "delete from DataValue d where d.source = :source";

    entityManager.createQuery(hql).setParameter("source", organisationUnit).executeUpdate();
  }

  @Override
  public void deleteDataValues(DataElement dataElement) {
    String hql = "delete from DataValue d where d.dataElement = :dataElement";

    entityManager.createQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  @Override
  public void deleteDataValues(@Nonnull Collection<DataElement> dataElements) {
    String hql = "delete from DataValue d where d.dataElement in :dataElements";

    entityManager.createQuery(hql).setParameter("dataElements", dataElements).executeUpdate();
  }

  @Override
  public void deleteDataValuesByCategoryOptionCombo(
      @Nonnull Collection<CategoryOptionCombo> categoryOptionCombos) {
    String hql = "delete from DataValue d where d.categoryOptionCombo in :categoryOptionCombos";

    entityManager
        .createQuery(hql)
        .setParameter("categoryOptionCombos", categoryOptionCombos)
        .executeUpdate();
  }

  @Override
  public void deleteDataValuesByAttributeOptionCombo(
      @Nonnull Collection<CategoryOptionCombo> attributeOptionCombos) {
    String hql = "delete from DataValue d where d.attributeOptionCombo in :attributeOptionCombos";

    entityManager
        .createQuery(hql)
        .setParameter("attributeOptionCombos", attributeOptionCombos)
        .executeUpdate();
  }

  @Override
  public int getDataValueCountLastUpdatedBetween(
      Date startDate, Date endDate, boolean includeDeleted) {
    if (startDate == null && endDate == null) {
      throw new IllegalArgumentException("Start date or end date must be specified");
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    List<Function<Root<DataValue>, Predicate>> predicateList = new ArrayList<>();

    if (!includeDeleted) {
      predicateList.add(root -> builder.equal(root.get(DELETED), false));
    }

    if (startDate != null) {
      predicateList.add(root -> builder.greaterThanOrEqualTo(root.get(LAST_UPATED), startDate));
    }

    if (endDate != null) {
      predicateList.add(root -> builder.lessThanOrEqualTo(root.get(LAST_UPATED), endDate));
    }

    return getCount(
            builder, newJpaParameters().addPredicates(predicateList).count(builder::countDistinct))
        .intValue();
  }

  @Override
  public boolean dataValueExists(CategoryCombo combo) {
    String cocIdsSql =
        "select distinct categoryoptioncomboid from categorycombos_optioncombos where categorycomboid = :cc";
    List<?> cocIds =
        getSession()
            .createNativeQuery(cocIdsSql)
            .addSynchronizedEntityClass(CategoryOptionCombo.class)
            .setParameter("cc", combo.getId())
            .list();
    String anyDataValueSql =
        "select 1 from datavalue dv "
            + "where dv.categoryoptioncomboid in :cocIds or dv.attributeoptioncomboid in :cocIds limit 1";
    return !nativeSynchronizedQuery(anyDataValueSql)
        .setParameter("cocIds", cocIds)
        .list()
        .isEmpty();
  }

  @Override
  public boolean dataValueExistsForDataElement(String uid) {
    return !getQuery("select 1 from DataValue dv where dv.dataElement.uid = :uid")
        .setParameter("uid", uid)
        .setMaxResults(1)
        .getResultList()
        .isEmpty();
  }

  @Override
  public void mergeDataValuesWithCategoryOptionCombos(long target, @Nonnull Set<Long> sources) {
    String sql =
        getSqlForMergingDataValues(target, sources, DataValueMergeType.CATEGORY_OPTION_COMBO);
    log.debug("SQL query to be used for merging category option combos: \n{}", sql);
    jdbcTemplate.update(sql);
  }

  @Override
  public void mergeDataValuesWithAttributeOptionCombos(long target, @Nonnull Set<Long> sources) {
    String sql =
        getSqlForMergingDataValues(target, sources, DataValueMergeType.ATTRIBUTE_OPTION_COMBO);
    log.debug("SQL query to be used for merging attribute option combos: \n{}", sql);
    jdbcTemplate.update(sql);
  }

  @Override
  public void mergeDataValuesWithDataElements(long target, @Nonnull Set<Long> sources) {
    String sql = getSqlForMergingDataValues(target, sources, DataValueMergeType.DATA_ELEMENT);
    log.debug("SQL query to be used for merging data elements: \n{}", sql);
    jdbcTemplate.update(sql);
  }

  // -------------------------------------------------------------------------
  // getDeflatedDataValues and related supportive methods
  // -------------------------------------------------------------------------

  @Override
  public List<DeflatedDataValue> getDeflatedDataValues(DataExportStoreParams params) {
    SqlHelper sqlHelper = new SqlHelper(true);

    StringBuilder sql = new StringBuilder();
    StringBuilder where = new StringBuilder();

    getDdvSelectFrom(params, sql);
    getDdvDataElementsAndOperands(params, sql, where, sqlHelper);
    getDdvPeriods(params, sql, where, sqlHelper);
    getDdvOrgUnits(params, sql, where, sqlHelper);
    getDdvAttributeOptionCombos(params, where, sqlHelper);
    getDdvDimensionConstraints(params, sql, where, sqlHelper);
    getDdvLastUpdated(params, where, sqlHelper);
    getDdvIncludeDeleted(params, where, sqlHelper);

    sql.append(where);

    getDdvOrderBy(params, sql);

    List<DeflatedDataValue> result = new ArrayList<>();

    jdbcTemplate.query(
        sql.toString(),
        resultSet -> {
          DeflatedDataValue ddv = getDdvFromResultSet(resultSet, params.needsOrgUnitDetails());
          if (params.hasBlockingQueue()) {
            getDdvAddToBlockingQueue(params.getBlockingQueue(), ddv);
          } else {
            result.add(ddv);
          }
        });

    if (params.hasBlockingQueue()) {
      getDdvAddToBlockingQueue(params.getBlockingQueue(), END_OF_DDV_DATA);
    }

    log.debug(result.size() + " DeflatedDataValues returned from: " + sql);

    return result;
  }

  /** getDeflatedDataValues - Adds SELECT clause and starts FROM clause. */
  private void getDdvSelectFrom(DataExportStoreParams params, StringBuilder sql) {
    sql.append(
            """
            select dv.dataelementid, dv.periodid, dv.sourceid, \
            dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, \
            dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup, dv.deleted""")
        .append(params.needsOrgUnitDetails() ? ", ou.path" : "")
        .append(" from datavalue dv");
  }

  /** getDeflatedDataValues - Chooses data elements and data element operands. */
  private void getDdvDataElementsAndOperands(
      DataExportStoreParams params, StringBuilder sql, StringBuilder where, SqlHelper sqlHelper) {
    List<Long> deIds = new ArrayList<>();
    List<Long> cocIds = new ArrayList<>();
    getDdvDataElementLists(params, deIds, cocIds);

    if (!cocIds.isEmpty()) {
      sql.append(" join ")
          .append(literalLongLongTable(deIds, cocIds, "deo", "deid", "cocid"))
          .append(
              " on deo.deid = dv.dataelementid and (deo.cocid is null or deo.cocid::bigint = dv.categoryoptioncomboid)");
    } else if (!deIds.isEmpty()) {
      String dataElementIdList = getCommaDelimitedString(deIds);

      where
          .append(sqlHelper.whereAnd())
          .append("dv.dataelementid in (")
          .append(dataElementIdList)
          .append(")");
    }
  }

  /** getDeflatedDataValues - Chooses periods. */
  private void getDdvPeriods(
      DataExportStoreParams params, StringBuilder sql, StringBuilder where, SqlHelper sqlHelper) {
    if (params.hasPeriods()) {
      String periodIdList = getCommaDelimitedString(getIdentifiers(params.getPeriods()));

      where
          .append(sqlHelper.whereAnd())
          .append("dv.periodid in (")
          .append(periodIdList)
          .append(")");
    } else if (params.hasPeriodTypes() || params.hasStartEndDate() || params.hasIncludedDate()) {
      sql.append(" join period p on p.periodid = dv.periodid");

      if (params.hasPeriodTypes()) {
        sql.append(" join periodtype pt on pt.periodtypeid = p.periodtypeid");

        String periodTypeIdList =
            getCommaDelimitedString(
                params.getPeriodTypes().stream().map(PeriodType::getId).collect(toList()));

        where
            .append(sqlHelper.whereAnd())
            .append("pt.periodtypeid in (")
            .append(periodTypeIdList)
            .append(")");
      }

      if (params.hasStartEndDate()) {
        where
            .append(sqlHelper.whereAnd())
            .append("p.startdate >= '")
            .append(DateUtils.toMediumDate(params.getStartDate()))
            .append("'")
            .append(" and p.enddate <= '")
            .append(DateUtils.toMediumDate(params.getEndDate()))
            .append("'");
      } else if (params.hasIncludedDate()) {
        where
            .append(sqlHelper.whereAnd())
            .append("p.startdate <= '")
            .append(DateUtils.toMediumDate(params.getIncludedDate()))
            .append("'")
            .append(" and p.enddate >= '")
            .append(DateUtils.toMediumDate(params.getIncludedDate()))
            .append("'");
      }
    }
  }

  /** getDeflatedDataValues - Chooses organisation units. */
  private void getDdvOrgUnits(
      DataExportStoreParams params, StringBuilder sql, StringBuilder where, SqlHelper sqlHelper) {
    if (params.needsOrgUnitDetails()) {
      sql.append(" join organisationunit ou on ou.organisationunitid = dv.sourceid");
    }

    if (params.hasOrgUnitLevel()) {
      where
          .append(sqlHelper.whereAnd())
          .append("ou.hierarchylevel ")
          .append(params.isIncludeDescendants() ? ">" : "")
          .append("= ")
          .append(params.getOrgUnitLevel());
    }

    if (params.hasOrganisationUnits()) {
      if (params.isIncludeDescendantsForOrganisationUnits()) {
        where.append(sqlHelper.whereAnd()).append("(");

        for (OrganisationUnit parent : params.getOrganisationUnits()) {
          where
              .append(sqlHelper.or())
              .append("ou.path like '")
              .append(parent.getStoredPath())
              .append("%'");
        }

        where.append(" )");
      } else {
        String orgUnitIdList =
            getCommaDelimitedString(getIdentifiers(params.getOrganisationUnits()));

        where
            .append(sqlHelper.whereAnd())
            .append("dv.sourceid in (")
            .append(orgUnitIdList)
            .append(")");
      }
    }
  }

  /** getDeflatedDataValues - Chooses attribute option combinations. */
  private void getDdvAttributeOptionCombos(
      DataExportStoreParams params, StringBuilder where, SqlHelper sqlHelper) {
    if (params.hasAttributeOptionCombos()) {
      String aocIdList = getCommaDelimitedString(getIdentifiers(params.getAttributeOptionCombos()));

      where
          .append(sqlHelper.whereAnd())
          .append("dv.attributeoptioncomboid in (")
          .append(aocIdList)
          .append(")");
    }
  }

  /** getDeflatedDataValues - Adds user dimension constraints. */
  private void getDdvDimensionConstraints(
      DataExportStoreParams params, StringBuilder sql, StringBuilder where, SqlHelper sqlHelper) {
    if (params.hasCogDimensionConstraints() || params.hasCoDimensionConstraints()) {
      sql.append(
          " join categoryoptioncombos_categoryoptions cc on dv.attributeoptioncomboid = cc.categoryoptioncomboid");

      if (params.hasCoDimensionConstraints()) {
        String coDimConstraintsList =
            getCommaDelimitedString(getIdentifiers(params.getCoDimensionConstraints()));

        where
            .append(sqlHelper.whereAnd())
            .append("cc.categoryoptionid in (")
            .append(coDimConstraintsList)
            .append(") ");
      }

      if (params.hasCogDimensionConstraints()) {
        String cogDimConstraintsList =
            getCommaDelimitedString(getIdentifiers(params.getCogDimensionConstraints()));

        sql.append(
            " join categoryoptiongroupmembers cogm on cc.categoryoptionid = cogm.categoryoptionid");

        where
            .append(sqlHelper.whereAnd())
            .append("cogm.categoryoptiongroupid in (")
            .append(cogDimConstraintsList)
            .append(")");
      }
    }
  }

  /** getDeflatedDataValues - Adds LastUpdated constraint. */
  private void getDdvLastUpdated(
      DataExportStoreParams params, StringBuilder where, SqlHelper sqlHelper) {
    if (params.hasLastUpdated()) {
      where
          .append(sqlHelper.whereAnd())
          .append("dv.lastupdated >= ")
          .append(DateUtils.toMediumDate(params.getLastUpdated()));
    }
  }

  /** getDeflatedDataValues - Adds deleted constraint. */
  private void getDdvIncludeDeleted(
      DataExportStoreParams params, StringBuilder where, SqlHelper sqlHelper) {
    if (!params.isIncludeDeleted()) {
      where.append(sqlHelper.whereAnd()).append("dv.deleted is false");
    }
  }

  /** getDeflatedDataValues - Adds ORDER BY. */
  private void getDdvOrderBy(DataExportStoreParams params, StringBuilder sql) {
    if (params.isOrderByOrgUnitPath()) {
      sql.append(" order by ou.path");
    }
  }

  /**
   * getDeflatedDataValues - Gets data element / category option combo lists.
   *
   * <p>There are two ways that all the category option combos of a data element may be requested:
   * either as a data element (returns the sum of all COCs) or as a wildcard data element operand
   * (having COC == null, returns each individual data element operand.)
   *
   * <p>If the parameters have any non-wildcard data element operands, then this method fills the
   * lists of data element ids and COC ids with equal numbers of values. For a non-wildcard data
   * element operand, the COC will be not null. For a data element or a wildcard DEO, the COC will
   * be null.
   *
   * <p>If there are no non-wildcard data element operands, then only the list of data element ids
   * is populated.
   */
  private void getDdvDataElementLists(
      DataExportStoreParams params, List<Long> deIds, List<Long> cocIds) {
    // Get a collection of unique DataElement ids.
    Collection<Long> dataElementIds =
        union(
            params.getDataElements().stream().map(DataElement::getId).collect(toSet()),
            params.getDataElementOperands().stream()
                .filter(deo -> deo.getCategoryOptionCombo() == null)
                .map(deo -> deo.getDataElement().getId())
                .collect(toSet()));

    deIds.addAll(dataElementIds);

    // Get a set of unique DataElement/CategoryOptionCombo id pairs.
    Set<DeflatedDataValue> dataElementOperands =
        params.getDataElementOperands().stream()
            .filter(deo -> !dataElementIds.contains(deo.getDataElement().getId()))
            .filter(deo -> deo.getCategoryOptionCombo() != null)
            .map(
                deo ->
                    new DeflatedDataValue(
                        deo.getDataElement().getId(), deo.getCategoryOptionCombo().getId()))
            .collect(toSet());

    if (!dataElementOperands.isEmpty()) {
      cocIds.addAll(Collections.nCopies(deIds.size(), null));

      for (DeflatedDataValue ddv : dataElementOperands) {
        deIds.add(ddv.getDataElementId());
        cocIds.add(ddv.getCategoryOptionComboId());
      }
    }
  }

  /**
   * Generates a derived table containing literals in two columns: long and long.
   *
   * @param long1Values (non-empty) 1st long column values for the table
   * @param long2Values (same size) 2nd long column values for the table
   * @param table the desired table name alias
   * @param long1Column the desired 1st long column name
   * @param long2Column the desired 2nd long column name
   * @return the derived literal table
   *     <p>The generic implementation, which works in all supported database types, returns a
   *     subquery in the following form: <code>
   *     (values (i1_1, i2_1),(i1_2, i2_2),(i1_3, i2_3)) table (int1Column, int2Column)
   * </code>
   */
  private String literalLongLongTable(
      List<Long> long1Values,
      List<Long> long2Values,
      String table,
      String long1Column,
      String long2Column) {
    StringBuilder sb = new StringBuilder("(values ");

    for (int i = 0; i < long1Values.size(); i++) {
      sb.append("(")
          .append(long1Values.get(i))
          .append(", ")
          .append(long2Values.get(i))
          .append("),");
    }

    return sb.deleteCharAt(sb.length() - 1)
        .append(") ")
        .append(table)
        .append(" (")
        .append(long1Column)
        .append(", ")
        .append(long2Column)
        .append(")")
        .toString();
  }

  /** getDeflatedDataValues - Creates a {@link DeflatedDataValue} from a query result row. */
  private DeflatedDataValue getDdvFromResultSet(ResultSet resultSet, boolean joinOrgUnit)
      throws SQLException {
    Long dataElementId = resultSet.getLong(1);
    Long periodId = resultSet.getLong(2);
    Long organisationUnitId = resultSet.getLong(3);
    Long categoryOptionComboId = resultSet.getLong(4);
    Long attributeOptionComboId = resultSet.getLong(5);
    String value = resultSet.getString(6);
    String storedBy = resultSet.getString(7);
    Date created = resultSet.getDate(8);
    Date lastUpdated = resultSet.getDate(9);
    String comment = resultSet.getString(10);
    boolean followup = resultSet.getBoolean(11);
    boolean deleted = resultSet.getBoolean(12);
    String sourcePath = joinOrgUnit ? resultSet.getString(13) : null;

    DeflatedDataValue ddv =
        new DeflatedDataValue(
            dataElementId,
            periodId,
            organisationUnitId,
            categoryOptionComboId,
            attributeOptionComboId,
            value,
            storedBy,
            created,
            lastUpdated,
            comment,
            followup,
            deleted);

    ddv.setSourcePath(sourcePath);

    return ddv;
  }

  /** getDeflatedDataValues - Adds {@link DeflatedDataValue} to blocking queue. */
  private void getDdvAddToBlockingQueue(
      BlockingQueue<DeflatedDataValue> blockingQueue, DeflatedDataValue ddv) {
    try {
      if (!blockingQueue.offer(ddv, DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT)) {
        log.error("HibernateDataValueStore failed to add to BlockingQueue.");
      }
    } catch (InterruptedException ex) {
      log.error("HibernateDataValueStore BlockingQueue InterruptedException: " + ex.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private String getSqlForMergingDataValues(
      long targetId, Set<Long> sourceIds, DataValueMergeType mergeType) {
    String sql =
        """
        do
        $$
        declare
          source_dv record;
          target_duplicate record;
          target_id bigint default %s;
        begin

          -- loop through each record with a matching source id
          for source_dv in
            select * from datavalue where source_column in (%s)
            loop

            -- check if target Data Value exists with same unique key
            select dv.*
              into target_duplicate
              from datavalue dv
              where dv.dataelementid = data_element
              and dv.periodid = source_dv.periodid
              and dv.sourceid = source_dv.sourceid
              and dv.attributeoptioncomboid = attr_opt_combo
              and dv.categoryoptioncomboid = cat_opt_combo;

            -- target duplicate found and target has latest lastUpdated value
            if (target_duplicate.source_column is not null
                and target_duplicate.lastupdated >= source_dv.lastupdated)
              then
              -- delete source
              delete from datavalue
                where dataelementid = source_dv.dataelementid
                and periodid = source_dv.periodid
                and sourceid = source_dv.sourceid
                and attributeoptioncomboid = source_dv.attributeoptioncomboid
                and categoryoptioncomboid = source_dv.categoryoptioncomboid;

            -- target duplicate found and source has latest lastUpdated value
            elsif (target_duplicate.source_column is not null
                and target_duplicate.lastupdated < source_dv.lastupdated)
              then
              -- delete target
              delete from datavalue
                where dataelementid = target_duplicate.dataelementid
                and periodid = target_duplicate.periodid
                and sourceid = target_duplicate.sourceid
                and attributeoptioncomboid = target_duplicate.attributeoptioncomboid
                and categoryoptioncomboid = target_duplicate.categoryoptioncomboid;

              -- update source with target
              update datavalue
                set source_column = target_id
                where dataelementid = source_dv.dataelementid
                and periodid = source_dv.periodid
                and sourceid = source_dv.sourceid
                and attributeoptioncomboid = source_dv.attributeoptioncomboid
                and categoryoptioncomboid = source_dv.categoryoptioncomboid;

            else
              -- no target duplicate found, update source with target id
              update datavalue
                set source_column = target_id
                where dataelementid = source_dv.dataelementid
                and periodid = source_dv.periodid
                and sourceid = source_dv.sourceid
                and attributeoptioncomboid = source_dv.attributeoptioncomboid
                and categoryoptioncomboid = source_dv.categoryoptioncomboid;

            end if;

            end loop;
        end;
        $$
        language plpgsql;
        """
            .formatted(
                targetId, sourceIds.stream().map(String::valueOf).collect(Collectors.joining(",")));

    if (mergeType.equals(DataValueMergeType.DATA_ELEMENT)) {
      return sql.replace("source_column", "dataelementid")
          .replace("data_element", "target_id")
          .replace("cat_opt_combo", "source_dv.categoryoptioncomboid")
          .replace("attr_opt_combo", "source_dv.attributeoptioncomboid");
    } else if (mergeType.equals(DataValueMergeType.CATEGORY_OPTION_COMBO)) {
      return sql.replace("source_column", "categoryoptioncomboid")
          .replace("data_element", "source_dv.dataelementid")
          .replace("cat_opt_combo", "target_id")
          .replace("attr_opt_combo", "source_dv.attributeoptioncomboid");
    } else if (mergeType.equals(DataValueMergeType.ATTRIBUTE_OPTION_COMBO)) {
      return sql.replace("source_column", "attributeoptioncomboid")
          .replace("data_element", "source_dv.dataelementid")
          .replace("cat_opt_combo", "source_dv.categoryoptioncomboid")
          .replace("attr_opt_combo", "target_id");
    }
    // if SQL params haven't been replaced there's no point trying to execute a bad SQL query
    throw new IllegalArgumentException(
        "Error while trying to construct SQL for data value merge for " + mergeType);
  }

  private enum DataValueMergeType {
    DATA_ELEMENT,
    CATEGORY_OPTION_COMBO,
    ATTRIBUTE_OPTION_COMBO
  }
}
