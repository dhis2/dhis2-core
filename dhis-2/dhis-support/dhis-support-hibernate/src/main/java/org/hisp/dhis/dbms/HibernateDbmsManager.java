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
        
        emptyTable( "pushanalysisrecipientusergroups" );
        emptyTable( "pushanalysis" );
        
        emptyTable( "dashboarditem_users" );
        emptyTable( "dashboarditem_resources" );
        emptyTable( "dashboarditem_reports" );
        emptyTable( "dashboard_items" );
        emptyTable( "dashboarditem" );
        emptyTable( "dashboardusergroupaccesses" );
        emptyTable( "dashboard" );

        emptyTable( "delete from interpretation_comments" );
        emptyTable( "delete from interpretationcommenttranslations" );
        emptyTable( "delete from interpretationcomment" );
        emptyTable( "delete from interpretationtranslations" );
        emptyTable( "delete from interpretationusergroupaccesses" );
        emptyTable( "delete from interpretation" );
        
        emptyTable( "delete from reportusergroupaccesses" );
        emptyTable( "delete from report" );
        
        emptyTable( "reporttable_categorydimensions" );
        emptyTable( "reporttable_categoryoptiongroups" );
        emptyTable( "reporttable_columns" );
        emptyTable( "reporttable_datadimensionitems" );
        emptyTable( "reporttable_dataelementgroups" );
        emptyTable( "reporttable_filters" );
        emptyTable( "reporttable_itemorgunitgroups" );
        emptyTable( "reporttable_organisationunits" );
        emptyTable( "reporttable_orgunitgroups" );
        emptyTable( "reporttable_orgunitlevels" );
        emptyTable( "reporttable_periods" );
        emptyTable( "reporttable_rows" );
        emptyTable( "reporttableusergroupaccesses" );
        emptyTable( "reporttabletranslations" );
        emptyTable( "reporttable" );

        emptyTable( "chart_categorydimensions" );
        emptyTable( "chart_categoryoptiongroups" );
        emptyTable( "chart_datadimensionitems" );
        emptyTable( "chart_dataelementgroups" );
        emptyTable( "chart_filters" );
        emptyTable( "chart_itemorgunitgroups" );
        emptyTable( "chart_organisationunits" );
        emptyTable( "chart_orgunitgroups" );
        emptyTable( "chart_orgunitlevels" );
        emptyTable( "chart_periods" );
        emptyTable( "chartusergroupaccesses" );
        emptyTable( "charttranslations" );
        emptyTable( "chart" );
        
        emptyTable( "eventreport_attributedimensions" );
        emptyTable( "eventreport_columns" );
        emptyTable( "eventreport_dataelementdimensions" );
        emptyTable( "eventreport_filters" );
        emptyTable( "eventreport_itemorgunitgroups" );
        emptyTable( "eventreport_organisationunits" );
        emptyTable( "eventreport_orgunitgroups" );
        emptyTable( "eventreport_orgunitlevels" );
        emptyTable( "eventreport_periods" );
        emptyTable( "eventreport_programindicatordimensions" );
        emptyTable( "eventreport_rows" );
        emptyTable( "eventreportusergroupaccesses" );
        emptyTable( "eventreporttranslations" );
        emptyTable( "eventreport" );

        emptyTable( "eventchart_attributedimensions" );
        emptyTable( "eventchart_columns" );
        emptyTable( "eventchart_dataelementdimensions" );
        emptyTable( "eventchart_filters" );
        emptyTable( "eventchart_itemorgunitgroups" );
        emptyTable( "eventchart_organisationunits" );
        emptyTable( "eventchart_orgunitgroups" );
        emptyTable( "eventchart_orgunitlevels" );
        emptyTable( "eventchart_periods" );
        emptyTable( "eventchart_programindicatordimensions" );
        emptyTable( "eventchart_rows" );
        emptyTable( "eventchartusergroupaccesses" );
        emptyTable( "eventcharttranslations" );
        emptyTable( "eventchart" );
        
        emptyTable( "users_catdimensionconstraints" );
        emptyTable( "userrolemembers" );
        emptyTable( "userroledataset" );
        emptyTable( "userroleauthorities" );
        emptyTable( "userdatavieworgunits" );
        emptyTable( "usermembership" );
        emptyTable( "userrole" );

        emptyTable( "orgunitgroupsetmembers" );
        emptyTable( "orgunitgroupset" );
        emptyTable( "orgunitgroupsetusergroupaccesses" );
        
        emptyTable( "orgunitgroupmembers" );
        emptyTable( "orgunitgroup" );
        emptyTable( "orgunitgroupusergroupaccesses" );

        emptyTable( "validationrulegroupusergroupstoalert" );
        emptyTable( "validationrulegroupmembers" );
        emptyTable( "validationrulegroup" );
        emptyTable( "validationrulegroupusergroupaccesses" );
        
        emptyTable( "validationrule" );
        emptyTable( "validationruleusergroupaccesses" );

        emptyTable( "dataapproval" );

        emptyTable( "lockexception" );

        emptyTable( "datasetsource" );
        emptyTable( "datasetelement" );
        emptyTable( "datasetindicators" );
        emptyTable( "datasetoperands" );
        emptyTable( "datasetusergroupaccesses" );
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
        emptyTable( "programusergroupaccesses" );
        emptyTable( "program" );
        emptyTable( "trackedentityinstance" );

        emptyTable( "minmaxdataelement" );
        emptyTable( "expressiondataelement" );
        emptyTable( "expressionsampleelement" );
        emptyTable( "expressionoptioncombo" );
        emptyTable( "calculateddataelement" );
        
        emptyTable( "dataelementgroupsetmembers" );
        emptyTable( "dataelementgroupsetusergroupaccesses" );
        emptyTable( "dataelementgroupset" );
        
        emptyTable( "dataelementgroupmembers" );
        emptyTable( "dataelementgroupusergroupaccesses" );
        emptyTable( "dataelementgroup" );
        
        emptyTable( "dataelementaggregationlevels" );
        emptyTable( "dataelementoperand" );
        emptyTable( "dataelementusergroupaccesses" );
        emptyTable( "dataelement" );
        
        emptyTable( "categoryoptioncombos_categoryoptions" );
        emptyTable( "categorycombos_optioncombos" );
        emptyTable( "categorycombos_categories" );
        emptyTable( "categories_categoryoptions" );

        emptyTable( "categoryoption_organisationunits" );        
        emptyTable( "organisationunit" );
        
        emptyTable( "version" );
        emptyTable( "mocksource" );
        emptyTable( "period" );

        emptyTable( "indicatorgroupsetmembers" );
        emptyTable( "indicatorgroupsetusergroupaccesses" );
        emptyTable( "indicatorgroupset" );
        
        emptyTable( "indicatorgroupmembers" );
        emptyTable( "indicatorgroupusergroupaccesses" );
        emptyTable( "indicatorgroup" );
        
        emptyTable( "indicator" );
        emptyTable( "indicatortype" );

        emptyTable( "categoryoptiongroupsetmembers" );
        emptyTable( "categoryoptiongroupsetusergroupaccesses" );
        emptyTable( "categoryoptiongroupset" );
        
        emptyTable( "categoryoptiongroupmembers" );
        emptyTable( "categoryoptiongroupusergroupaccesses" );
        emptyTable( "categoryoptiongroup" );

        emptyTable( "dataelementcategoryoptionusergroupaccesses" );

        emptyTable( "expression" );
        emptyTable( "categoryoptioncombo" );
        emptyTable( "categorycombo" );
        emptyTable( "dataelementcategory" );
        emptyTable( "dataelementcategoryoption" );

        emptyTable( "optionvalue" );
        emptyTable( "optionset" );

        emptyTable( "systemsetting" );

        emptyTable( "usergroupusergroupaccesses" );
        emptyTable( "usergroupaccess" );
        emptyTable( "usergroupmembers" );
        emptyTable( "usergroup" );

        emptyTable( "users" );
        emptyTable( "userinfo" );

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
            jdbcTemplate.update( "delete from " + table );
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
            jdbcTemplate.execute( "drop table " + table );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.debug( "Table " + table + " does not exist" );
        }
    }
}
