/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dataanalysis.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.query.JpaQueryUtils.generateHqlQueryForSharingCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.collection.PaginatedList;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataanalysis.DataAnalysisMeasures;
import org.hisp.dhis.dataanalysis.DataAnalysisStore;
import org.hisp.dhis.dataanalysis.FollowupAnalysisRequest;
import org.hisp.dhis.dataanalysis.FollowupValue;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.objectmapper.DeflatedDataValueNameMinMaxRowMapper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Repository( "org.hisp.dhis.dataanalysis.DataAnalysisStore" )
public class JdbcDataAnalysisStore implements DataAnalysisStore
{
    private final StatementBuilder statementBuilder;

    /**
     * Read only JDBC template.
     */
    private final JdbcTemplate jdbcTemplate;

    private final SessionFactory sessionFactory;

    public JdbcDataAnalysisStore( StatementBuilder statementBuilder,
        @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate,
        SessionFactory sessionFactory )
    {
        checkNotNull( statementBuilder );
        checkNotNull( jdbcTemplate );
        checkNotNull( sessionFactory );

        this.statementBuilder = statementBuilder;
        this.jdbcTemplate = jdbcTemplate;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<DataAnalysisMeasures> getDataAnalysisMeasures( DataElement dataElement,
        Collection<CategoryOptionCombo> categoryOptionCombos, Collection<String> parentPaths, Date from )
    {
        List<DataAnalysisMeasures> measures = new ArrayList<>();

        if ( categoryOptionCombos.isEmpty() || parentPaths.isEmpty() )
        {
            return measures;
        }

        String catOptionComboIds = TextUtils.getCommaDelimitedString( getIdentifiers( categoryOptionCombos ) );

        String matchPaths = "(";
        for ( String path : parentPaths )
        {
            matchPaths += "ou.path like '" + path + "%' or ";
        }
        matchPaths = TextUtils.removeLastOr( matchPaths ) + ") ";

        String sql = "select dv.sourceid, dv.categoryoptioncomboid, " +
            "avg(cast(dv.value as " + statementBuilder.getDoubleColumnType() + ")) as average, " +
            "stddev_pop(cast(dv.value as " + statementBuilder.getDoubleColumnType() + ")) as standarddeviation " +
            "from datavalue dv " +
            "inner join organisationunit ou on ou.organisationunitid = dv.sourceid " +
            "inner join period pe on dv.periodid = pe.periodid " +
            "where dv.dataelementid = " + dataElement.getId() + " " +
            "and dv.categoryoptioncomboid in (" + catOptionComboIds + ") " +
            "and pe.startdate >= '" + DateUtils.getMediumDateString( from ) + "' " +
            "and " + matchPaths +
            "and dv.deleted is false " +
            "group by dv.sourceid, dv.categoryoptioncomboid";

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            int orgUnitId = rowSet.getInt( 1 );
            int categoryOptionComboId = rowSet.getInt( 2 );
            double average = rowSet.getDouble( 3 );
            double stdDev = rowSet.getDouble( 4 );

            if ( stdDev != 0.0 )
            {
                measures.add( new DataAnalysisMeasures(
                    orgUnitId, categoryOptionComboId, average, stdDev ) );
            }
        }

        return measures;
    }

    @Override
    public List<DeflatedDataValue> getMinMaxViolations( Collection<DataElement> dataElements,
        Collection<CategoryOptionCombo> categoryOptionCombos, Collection<Period> periods,
        Collection<OrganisationUnit> parents, int limit )
    {
        if ( dataElements.isEmpty() || categoryOptionCombos.isEmpty() || periods.isEmpty() || parents.isEmpty() )
        {
            return new ArrayList<>();
        }

        String dataElementIds = getCommaDelimitedString( getIdentifiers( dataElements ) );
        String periodIds = getCommaDelimitedString( getIdentifiers( periods ) );
        String categoryOptionComboIds = getCommaDelimitedString( getIdentifiers( categoryOptionCombos ) );

        String sql = "select dv.dataelementid, dv.periodid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, dv.storedby, dv.lastupdated, "
            +
            "dv.created, dv.comment, dv.followup, ou.name as sourcename, de.name as dataelementname, " +
            "pt.name as periodtypename, pe.startdate, pe.enddate, coc.name as categoryoptioncomboname, mm.minimumvalue, mm.maximumvalue "
            +
            "from datavalue dv " +
            "left join minmaxdataelement mm on dv.dataelementid = mm.dataelementid and dv.categoryoptioncomboid = mm.categoryoptioncomboid and dv.sourceid = mm.sourceid "
            +
            "inner join dataelement de on dv.dataelementid = de.dataelementid " +
            "inner join period pe on dv.periodid = pe.periodid " +
            "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "where dv.dataelementid in (" + dataElementIds + ") " + "and dv.categoryoptioncomboid in (" +
            categoryOptionComboIds + ") " + "and dv.periodid in (" + periodIds + ") " + "and (" +
            "cast(dv.value as " + statementBuilder.getDoubleColumnType() + ") < mm.minimumvalue " +
            "or cast(dv.value as " + statementBuilder.getDoubleColumnType() + ") > mm.maximumvalue) " + "and (";

        for ( OrganisationUnit parent : parents )
        {
            sql += "ou.path like '" + parent.getPath() + "%' or ";
        }

        sql = TextUtils.removeLastOr( sql ) + ") ";
        sql += "and dv.deleted is false ";

        sql += statementBuilder.limitRecord( 0, limit );

        return jdbcTemplate.query( sql, new DeflatedDataValueNameMinMaxRowMapper( null, null ) );
    }

    @Override
    public List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement,
        CategoryOptionCombo categoryOptionCombo, Collection<Period> periods, Map<Long, Integer> lowerBoundMap,
        Map<Long, Integer> upperBoundMap )
    {
        if ( lowerBoundMap == null || lowerBoundMap.isEmpty() || periods.isEmpty() )
        {
            return new ArrayList<>();
        }

        List<List<Long>> organisationUnitPages = new PaginatedList<>( lowerBoundMap.keySet() ).setPageSize( 1000 )
            .getPages();

        log.debug( "No of pages: " + organisationUnitPages.size() );

        List<DeflatedDataValue> dataValues = new ArrayList<>();

        for ( List<Long> unitPage : organisationUnitPages )
        {
            dataValues.addAll( getDeflatedDataValues( dataElement, categoryOptionCombo, periods, unitPage,
                lowerBoundMap, upperBoundMap ) );
        }

        return dataValues;
    }

    private List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement,
        CategoryOptionCombo categoryOptionCombo, Collection<Period> periods, List<Long> organisationUnits,
        Map<Long, Integer> lowerBoundMap, Map<Long, Integer> upperBoundMap )
    {
        String periodIds = TextUtils.getCommaDelimitedString( getIdentifiers( periods ) );

        String sql = "select dv.dataelementid, dv.periodid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, dv.storedby, dv.lastupdated, "
            +
            "dv.created, dv.comment, dv.followup, ou.name as sourcename, " +
            "? as dataelementname, pt.name as periodtypename, pe.startdate, pe.enddate, " +
            "? as categoryoptioncomboname " +
            "from datavalue dv " +
            "inner join period pe on dv.periodid = pe.periodid " +
            "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "where dv.dataelementid = " + dataElement.getId() + " " +
            "and dv.categoryoptioncomboid = " + categoryOptionCombo.getId() + " " +
            "and dv.periodid in (" + periodIds + ") and (";

        for ( Long orgUnitUid : organisationUnits )
        {
            sql += "(dv.sourceid = " + orgUnitUid + " " +
                "and (cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) < "
                + lowerBoundMap.get( orgUnitUid ) + " " +
                "or cast(dv.value as " + statementBuilder.getDoubleColumnType() + ") > "
                + upperBoundMap.get( orgUnitUid ) + ")) or ";
        }

        sql = TextUtils.removeLastOr( sql ) + ") ";
        sql += "and dv.deleted is false ";

        PreparedStatementSetter pss = ( ps ) -> {
            ps.setString( 1, dataElement.getName() );
            ps.setString( 2, categoryOptionCombo.getName() );
        };

        return jdbcTemplate.query( sql, pss, new DeflatedDataValueNameMinMaxRowMapper( lowerBoundMap, upperBoundMap ) );
    }

    @Override
    public List<DeflatedDataValue> getFollowupDataValues( Collection<DataElement> dataElements,
        Collection<CategoryOptionCombo> categoryOptionCombos, Collection<Period> periods,
        Collection<OrganisationUnit> parents, int limit )
    {
        if ( dataElements.isEmpty() || categoryOptionCombos.isEmpty() || periods.isEmpty() || parents.isEmpty() )
        {
            return new ArrayList<>();
        }

        String dataElementIds = getCommaDelimitedString( getIdentifiers( dataElements ) );
        String periodIds = getCommaDelimitedString( getIdentifiers( periods ) );
        String categoryOptionComboIds = getCommaDelimitedString( getIdentifiers( categoryOptionCombos ) );

        String sql = "select dv.dataelementid, dv.periodid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, dv.storedby, dv.lastupdated, "
            +
            "dv.created, dv.comment, dv.followup, ou.name as sourcename, de.name as dataelementname, " +
            "pt.name as periodtypename, pe.startdate, pe.enddate, coc.name as categoryoptioncomboname, mm.minimumvalue, mm.maximumvalue "
            +
            "from datavalue dv " +
            "left join minmaxdataelement mm on (dv.dataelementid = mm.dataelementid and dv.categoryoptioncomboid = mm.categoryoptioncomboid and dv.sourceid = mm.sourceid) "
            +
            "inner join dataelement de on dv.dataelementid = de.dataelementid " +
            "inner join period pe on dv.periodid = pe.periodid " +
            "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "where dv.dataelementid in (" + dataElementIds + ") " +
            "and dv.categoryoptioncomboid in (" + categoryOptionComboIds + ") " +
            "and dv.periodid in (" + periodIds + ") " + "and (";

        for ( OrganisationUnit parent : parents )
        {
            sql += "ou.path like '" + parent.getPath() + "%' or ";
        }

        sql = TextUtils.removeLastOr( sql ) + ") ";
        sql += "and dv.followup = true and dv.deleted is false ";

        sql += statementBuilder.limitRecord( 0, limit );

        return jdbcTemplate.query( sql, new DeflatedDataValueNameMinMaxRowMapper( null, null ) );
    }

    /**
     * HQL query used to select {@link FollowupValue}s.
     */
    private static final String FOLLOWUP_VALUE_HQL = "select new org.hisp.dhis.dataanalysis.FollowupValue(" +
        "de.uid, de.name, " +
        "pt.class, " +
        "pe.startDate, pe.endDate, " +
        "ou.uid, ou.name, ou.path, " +
        "coc.uid, coc.name, " +
        "aoc.uid, aoc.name, " +
        "dv.value, dv.storedBy, dv.lastUpdated, dv.created, dv.comment, " +
        "mm.min, mm.max " +
        ")" +
        "from DataValue dv " +
        "left join MinMaxDataElement mm on (dv.dataElement.id = mm.dataElement.id and dv.categoryOptionCombo.id = mm.optionCombo.id and dv.source.id = mm.source.id) "
        +
        "inner join DataElement de on dv.dataElement.id = de.id " +
        "inner join Period pe on dv.period.id = pe.id " +
        "inner join PeriodType pt on pe.periodType.id = pt.id " +
        "inner join OrganisationUnit ou on dv.source.id = ou.id " +
        "inner join CategoryOptionCombo coc on dv.categoryOptionCombo.id = coc.id " +
        "inner join CategoryOptionCombo aoc on dv.attributeOptionCombo.id = aoc.id " +
        "where de.uid in (:de_ids) and (<<sharing>>)" +
        "and dv.followup = true and dv.deleted is false " +
        "and coc.uid in (:coc_ids) " +
        "and pe.startDate >= :startDate and pe.endDate <= :endDate " +
        "and exists (select 1 from OrganisationUnit parent where parent.uid in (:ou_ids) and ou.path like concat(parent.path, '%'))";

    /**
     * HQL query potentially used to resolve all
     * {@link org.hisp.dhis.dataelement.DataElement} UIDS from
     * {@link org.hisp.dhis.dataset.DataSet} UIDs in context of
     * {@link #FOLLOWUP_VALUE_HQL} query.
     */
    private static final String DATA_ELEMENT_UIDS_BY_DATA_SET_UIDS_HQL = "select dse.dataElement.uid " +
        "from DataSetElement dse " +
        "where dse.dataSet.uid in (:ds_ids)" +
        "and dse.dataElement.valueType in ('INTEGER', 'INTEGER_POSITIVE', 'INTEGER_NEGATIVE', 'INTEGER_ZERO_OR_POSITIVE', 'NUMBER', 'UNIT_INTERVAL', 'PERCENTAGE')";

    private static final String CATEGORY_OPTION_COMBO_UIDS_BY_DATE_ELEMENT_UIDS_HQL = "select coc.uid " +
        "from CategoryOptionCombo coc " +
        "where coc.categoryCombo.id in (select de.categoryCombo.id from DataElement de where de.uid in (:de_ids))";

    @Override
    public List<FollowupValue> getFollowupDataValues( User currentUser, FollowupAnalysisRequest request )
    {
        if ( isEmpty( request.getDe() ) && !isEmpty( request.getDs() ) )
        {
            request.setDe( sessionFactory.getCurrentSession()
                .createQuery( DATA_ELEMENT_UIDS_BY_DATA_SET_UIDS_HQL, String.class )
                .setParameter( "ds_ids", request.getDs() ).list() );
        }
        if ( !isEmpty( request.getDe() ) && isEmpty( request.getCoc() ) )
        {
            request.setCoc( sessionFactory.getCurrentSession()
                .createQuery( CATEGORY_OPTION_COMBO_UIDS_BY_DATE_ELEMENT_UIDS_HQL, String.class )
                .setParameter( "de_ids", request.getDe() ).list() );
        }
        if ( isEmpty( request.getDe() ) || isEmpty( request.getCoc() )
            || isEmpty( request.getOu() ) )
        {
            return emptyList();
        }
        if ( request.getStartDate() == null && request.getPe() != null )
        {
            request.setStartDate( PeriodType.getPeriodFromIsoString( request.getPe() ).getStartDate() );
        }
        if ( request.getEndDate() == null && request.getPe() != null )
        {
            request.setEndDate( PeriodType.getPeriodFromIsoString( request.getPe() ).getEndDate() );
        }

        Query<FollowupValue> query = sessionFactory.getCurrentSession().createQuery(
            FOLLOWUP_VALUE_HQL.replace( "<<sharing>>",
                generateHqlQueryForSharingCheck( "de", currentUser, AclService.LIKE_READ_METADATA ) ),
            FollowupValue.class );

        query.setParameter( "ou_ids", request.getOu() );
        query.setParameter( "de_ids", request.getDe() );
        query.setParameter( "coc_ids", request.getCoc() );
        query.setParameter( "startDate", request.getStartDate() );
        query.setParameter( "endDate", request.getEndDate() );
        query.setMaxResults( request.getMaxResults() );
        return query.setCacheable( false ).list();
    }
}
