package org.hisp.dhis.dbms;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 */
public class HibernateDbmsManager
    implements DbmsManager
{
    private static final Log log = LogFactory.getLog( HibernateDbmsManager.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    private SessionFactory sessionFactory;

    public void setSessionFactory( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    private HibernateCacheManager cacheManager;

    public void setCacheManager( HibernateCacheManager cacheManager )
    {
        this.cacheManager = cacheManager;
    }

    // -------------------------------------------------------------------------
    // DbmsManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void emptyDatabase()
    {
        emptyTable( "translation" );
        emptyTable( "importobject" );
        emptyTable( "importdatavalue" );
        emptyTable( "constant" );
        emptyTable( "sqlview" );

        emptyTable( "datavalue_audit" );
        emptyTable( "datavalueaudit" );
        emptyTable( "datavalue" );
        emptyTable( "completedatasetregistration" );

        emptyTable( "reporttable_categorydimensions" );
        emptyTable( "reporttable_categoryoptiongroups" );
        emptyTable( "reporttable_dataelements" );
        emptyTable( "reporttable_datasets" );
        emptyTable( "reporttable_indicators" );
        emptyTable( "reporttable_periods" );
        emptyTable( "reporttable_itemorgunitgroups" );
        emptyTable( "reporttable_organisationunits" );
        emptyTable( "reporttable_dataelementgroups" );
        emptyTable( "reporttable_orgunitgroups" );
        emptyTable( "reporttable_columns" );
        emptyTable( "reporttable_rows" );
        emptyTable( "reporttable_filters" );
        emptyTable( "reporttable" );
        
        emptyTable( "chart_periods" );
        emptyTable( "chart_orgunitlevels" );
        emptyTable( "chart_orgunitgroups" );
        emptyTable( "chart_organisationunits" );
        emptyTable( "chart_itemorgunitgroups" );
        emptyTable( "chart_indicators" );
        emptyTable( "chart_filters" );
        emptyTable( "chart_datasets" );
        emptyTable( "chart_dataelements" );
        emptyTable( "chart_dataelementoperands" );
        emptyTable( "chart_dataelementgroups" );
        emptyTable( "chart_categoryoptiongroups" );
        emptyTable( "chart_categorydimensions" );
        emptyTable( "chart" );

        emptyTable( "categoryoptiongroupusergroupaccesses" );
        emptyTable( "categoryoptiongroupsetusergroupaccesses" );
        emptyTable( "dataelementcategoryoptionusergroupaccesses" );
        emptyTable( "usergroupusergroupaccesses" );
        emptyTable( "usergroupaccess" );

        emptyTable( "users_catdimensionconstraints" );
        emptyTable( "userrolemembers" );
        emptyTable( "userroledataset" );
        emptyTable( "userroleauthorities" );
        emptyTable( "usergroupmembers" );
        emptyTable( "usergroup" );
        emptyTable( "userdatavieworgunits" );
        emptyTable( "usermembership" );
        emptyTable( "userrole" );

        emptyTable( "orgunitgroupsetmembers" );
        emptyTable( "orgunitgroupset" );
        emptyTable( "orgunitgroupmembers" );
        emptyTable( "orgunitgroup" );

        emptyTable( "validationrulegroupusergroupstoalert" );
        emptyTable( "validationrulegroupmembers" );
        emptyTable( "validationrulegroup" );
        emptyTable( "validationrule" );

        emptyTable( "dataapproval" );

        emptyTable( "lockexception" );

        emptyTable( "datasetsource" );
        emptyTable( "datasetmembers" );
        emptyTable( "datasetindicators" );
        emptyTable( "datasetoperands" );
        emptyTable( "dataset" );

        emptyTable( "dataapprovalworkflowlevels" );
        emptyTable( "dataapprovalworkflow" );
        emptyTable( "dataapprovallevel" );

        emptyTable( "trackedentitydatavalue" );
        emptyTable( "programstageinstance" );
        emptyTable( "programinstance" );
        emptyTable( "programstage_dataelements" );
        emptyTable( "programstage" );
        emptyTable( "program_organisationunits" );
        emptyTable( "program" );
        emptyTable( "trackedentityinstance" );

        emptyTable( "minmaxdataelement" );
        emptyTable( "expressiondataelement" );
        emptyTable( "expressionsampleelement" );
        emptyTable( "expressionoptioncombo" );
        emptyTable( "calculateddataelement" );
        emptyTable( "dataelementgroupsetmembers" );
        emptyTable( "dataelementgroupset" );
        emptyTable( "dataelementgroupmembers" );
        emptyTable( "dataelementgroup" );
        emptyTable( "dataelementaggregationlevels" );
        emptyTable( "dataelementoperand" );
        emptyTable( "dataelement" );
        emptyTable( "categoryoptioncombos_categoryoptions" );
        emptyTable( "categorycombos_optioncombos" );
        emptyTable( "categorycombos_categories" );
        emptyTable( "categories_categoryoptions" );

        emptyTable( "categoryoption_organisationunits" );
        emptyTable( "orgunitgroupsetmembers" );
        emptyTable( "orgunitgroupmembers" );
        emptyTable( "orgunitgroupset" );
        emptyTable( "orgunitgroup" );
        emptyTable( "organisationunit" );
        
        emptyTable( "version" );
        emptyTable( "mocksource" );
        emptyTable( "period" );

        emptyTable( "indicatorgroupsetmembers" );
        emptyTable( "indicatorgroupset" );
        emptyTable( "indicatorgroupmembers" );
        emptyTable( "indicatorgroup" );
        emptyTable( "indicator" );
        emptyTable( "indicatortype" );

        emptyTable( "categoryoptiongroupsetmembers" );
        emptyTable( "categoryoptiongroupset" );
        emptyTable( "categoryoptiongroupmembers" );
        emptyTable( "categoryoptiongroup" );

        emptyTable( "expression" );
        emptyTable( "categoryoptioncombo" );
        emptyTable( "categorycombo" );
        emptyTable( "dataelementcategory" );
        emptyTable( "dataelementcategoryoption" );

        emptyTable( "optionvalue" );
        emptyTable( "optionset" );

        emptyTable( "systemsetting" );

        emptyTable( "users" );
        emptyTable( "userinfo" );

        dropTable( "aggregateddatavalue" );
        dropTable( "aggregatedindicatorvalue" );
        dropTable( "aggregateddatasetcompleteness" );

        dropTable( "aggregatedorgunitdatavalue" );
        dropTable( "aggregatedorgunitindicatorvalue" );
        dropTable( "aggregatedorgunitdatasetcompleteness" );
        
        dropTable( "_categoryoptioncomboname" );
        dropTable( "_categoryoptiongroupsetstructure" );
        dropTable( "_categorystructure" );
        dropTable( "_dataelementcategoryoptioncombo" );
        dropTable( "_dataelementgroupsetstructure" );
        dropTable( "_dataelementstructure" );
        dropTable( "_dateperiodstructure" );
        dropTable( "_indicatorgroupsetstructure" );
        dropTable( "_organisationunitgroupsetstructure" );
        dropTable( "_orgunitstructure" );
        dropTable( "_periodstructure" );

        log.debug( "Cleared database contents" );

        cacheManager.clearCache();

        log.debug( "Cleared Hibernate cache" );
    }

    @Override
    public void clearSession()
    {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    @Override
    public void emptyTable( String table )
    {
        try
        {
            jdbcTemplate.update( "DELETE FROM " + table );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.debug( "Table " + table + " does not exist" );
        }
    }

    @Override
    public boolean tableExists( String tableName )
    {
        final String sql = 
            "select table_name from information_schema.tables " +
            "where table_name = '" + tableName + "' " +
            "and table_type = 'BASE TABLE'";
        
        List<Object> tables = jdbcTemplate.queryForList( sql, Object.class );
        
        return tables != null && tables.size() > 0;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void dropTable( String table )
    {
        try
        {
            jdbcTemplate.execute( "DROP TABLE " + table );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.debug( "Table " + table + " does not exist" );
        }
    }
}
