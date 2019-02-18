package org.hisp.dhis.orgunitdistribution.jdbc;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionManager;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionParams;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.util.List;
import java.util.stream.Collectors;

import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;

public class JdbcOrgUnitDistributionManager
    implements OrgUnitDistributionManager
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Grid getOrgUnitDistribution( OrgUnitDistributionParams params )
    {
        String sql = getDistributionSql( params );

        Grid grid = new ListGrid();

        addHeaders( params, grid );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();

            for ( GridHeader header : grid.getMetadataHeaders() )
            {
                grid.addValue( rowSet.getString( header.getName() ) );
            }

            grid.addValue( rowSet.getInt( "count" ) );
        }

        return grid;
    }

    private String getDistributionSql( OrgUnitDistributionParams params )
    {
        String levelCol = String.format( "ous.uidlevel%d", params.getOrgUnitLevel() );

        List<String> orgUnits = params.getOrgUnits().stream()
            .map( OrganisationUnit::getUid )
            .collect( Collectors.toList() );

        List<String> quotedGroupSets = params.getOrgUnitGroupSets().stream()
            .map( OrganisationUnitGroupSet::getUid )
            .map( uid -> quote( "ougs", uid ) )
            .collect( Collectors.toList() );

        String sql = "select " + levelCol + " as orgunit, " + getCommaDelimitedString( quotedGroupSets ) + ", " +
            "count(ougs.organisationunitid) as count " +
            "from " + quote( "_orgunitstructure" ) + " ous " +
            "inner join " + quote( "_organisationunitgroupsetstructure" ) + " ougs " +
            "where " + levelCol + " in (" + getQuotedCommaDelimitedString( orgUnits ) + ") " +
            "group by " + levelCol + ", " + getCommaDelimitedString( quotedGroupSets ) + ";";

        return sql;
    }

    private void addHeaders( OrgUnitDistributionParams params, Grid grid )
    {
        grid.addHeader( new GridHeader( "orgunit", "Organisation unit", ValueType.TEXT, null, false, true ) );
        params.getOrgUnitGroupSets().forEach( ougs ->
            grid.addHeader( new GridHeader( ougs.getUid(), ougs.getDisplayName(), ValueType.TEXT, null, false, true ) ) );
        grid.addHeader( new GridHeader( "count", "Count", ValueType.INTEGER, null, false, false ) );
    }
}
