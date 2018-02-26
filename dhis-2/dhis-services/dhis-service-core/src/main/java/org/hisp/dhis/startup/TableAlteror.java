package org.hisp.dhis.startup;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.hisp.quick.StatementHolder;
import org.hisp.quick.StatementManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public class TableAlteror
    extends AbstractStartupRoutine
{
    private static final Log log = LogFactory.getLog( TableAlteror.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private StatementManager statementManager;

    @Autowired
    private StatementBuilder statementBuilder;

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void execute()
    {
        int defaultCategoryComboId = getDefaultCategoryCombo();
        int defaultOptionComboId = getDefaultOptionCombo();

        // ---------------------------------------------------------------------
        // Drop obsolete tables
        // ---------------------------------------------------------------------

        executeSql( "DROP TABLE categoryoptioncomboname" );
        executeSql( "DROP TABLE orgunitgroupsetstructure" );
        executeSql( "DROP TABLE orgunitstructure" );
        executeSql( "DROP TABLE orgunithierarchystructure" );
        executeSql( "DROP TABLE orgunithierarchy" );
        executeSql( "DROP TABLE columnorder" );
        executeSql( "DROP TABLE roworder" );
        executeSql( "DROP TABLE sectionmembers" );
        executeSql( "DROP TABLE reporttable_categoryoptioncombos" );
        executeSql( "DROP TABLE reporttable_dataelementgroupsets" );
        executeSql( "DROP TABLE dashboardcontent_datamartexports" );
        executeSql( "DROP TABLE dashboardcontent_mapviews" );
        executeSql( "DROP TABLE dashboardcontent_documents" );
        executeSql( "DROP TABLE dashboardcontent_maps" );
        executeSql( "DROP TABLE dashboardcontent_reports" );
        executeSql( "DROP TABLE dashboardcontent_reporttables" );
        executeSql( "DROP TABLE dashboardcontent" );
        executeSql( "DROP TABLE customvalue" );
        executeSql( "DROP TABLE reporttable_displaycolumns" );
        executeSql( "DROP TABLE reportreporttables" );
        executeSql( "DROP TABLE frequencyoverrideassociation" );
        executeSql( "DROP TABLE dataelement_dataelementgroupsetmembers" );
        executeSql( "DROP TABLE dashboardcontent_olapurls" );
        executeSql( "DROP TABLE olapurl" );
        executeSql( "DROP TABLE target" );
        executeSql( "DROP TABLE calculateddataelement" );
        executeSql( "DROP TABLE systemsequence" );
        executeSql( "DROP TABLE reporttablecolumn" );
        executeSql( "DROP TABLE datamartexport" );
        executeSql( "DROP TABLE datamartexportdataelements" );
        executeSql( "DROP TABLE datamartexportindicators" );
        executeSql( "DROP TABLE datamartexportorgunits" );
        executeSql( "DROP TABLE datamartexportperiods" );
        executeSql( "DROP TABLE datasetlockedperiods" );
        executeSql( "DROP TABLE datasetlocksource" );
        executeSql( "DROP TABLE datasetlock" );
        executeSql( "DROP TABLE datasetlockexceptions" );
        executeSql( "DROP TABLE indicator_indicatorgroupsetmembers" );
        executeSql( "DROP TABLE maplegendsetindicator" );
        executeSql( "DROP TABLE maplegendsetdataelement" );
        executeSql( "DROP TABLE loginfailure" );
        executeSql( "DROP TABLE dashboarditem_trackedentitytabularreports" );
        executeSql( "DROP TABLE categoryoptioncombousergroupaccesses" );
        executeSql( "DROP TABLE validationrulegroupuserrolestoalert" );
        executeSql( "DROP TABLE expressionoptioncombo" );
        executeSql( "DROP TABLE orgunitgroupdatasets" );
        executeSql( "DROP TABLE datavalue_audit" );
        executeSql( "DROP TABLE datadictionaryusergroupaccesses" );
        executeSql( "DROP TABLE datadictionaryindicators" );
        executeSql( "DROP TABLE datadictionarydataelements" );
        executeSql( "DROP TABLE datadictionary" );
        executeSql( "DROP TABLE caseaggregationcondition" );
        executeSql( "DROP TABLE trackedentitytabularreportusergroupaccesses" );
        executeSql( "DROP TABLE trackedentitytabularreport_filters" );
        executeSql( "DROP TABLE trackedentitytabularreport_dimensions" );
        executeSql( "DROP TABLE trackedentitytabularreport" );
        executeSql( "DROP TABLE trackedentityaggregatereportusergroupaccesses" );
        executeSql( "DROP TABLE trackedentityaggregatereport_filters" );
        executeSql( "DROP TABLE trackedentityaggregatereport_dimension" );
        executeSql( "DROP TABLE trackedentityaggregatereport" );
        executeSql( "ALTER TABLE categoryoptioncombo drop column userid" );
        executeSql( "ALTER TABLE categoryoptioncombo drop column publicaccess" );
        executeSql( "ALTER TABLE categoryoptioncombo alter column name type text" );
        executeSql( "ALTER TABLE dataelementcategoryoption drop column categoryid" );
        executeSql( "ALTER TABLE reporttable DROP column paramleafparentorganisationunit" );
        executeSql( "ALTER TABLE reporttable DROP column dimension_type" );
        executeSql( "ALTER TABLE reporttable DROP column dimensiontype" );
        executeSql( "ALTER TABLE reporttable DROP column tablename" );
        executeSql( "ALTER TABLE reporttable DROP column existingtablename" );
        executeSql( "ALTER TABLE reporttable DROP column docategoryoptioncombos" );
        executeSql( "ALTER TABLE reporttable DROP column mode" );
        executeSql( "ALTER TABLE categoryoptioncombo DROP COLUMN displayorder" );
        executeSql( "ALTER TABLE section DROP COLUMN label" );
        executeSql( "ALTER TABLE section DROP COLUMN title" );
        executeSql( "ALTER TABLE organisationunit DROP COLUMN polygoncoordinates" );
        executeSql( "ALTER TABLE organisationunit DROP COLUMN geocode" );
        executeSql( "ALTER TABLE indicator DROP COLUMN extendeddataelementid" );
        executeSql( "ALTER TABLE indicator DROP COLUMN numeratoraggregationtype" );
        executeSql( "ALTER TABLE indicator DROP COLUMN denominatoraggregationtype" );
        executeSql( "ALTER TABLE dataset DROP COLUMN locked" );
        executeSql( "ALTER TABLE dataset DROP COLUMN skipaggregation" );
        executeSql( "ALTER TABLE configuration DROP COLUMN completenessrecipientsid" );
        executeSql( "ALTER TABLE dataelement DROP COLUMN alternativename" );
        executeSql( "ALTER TABLE dataelement DROP COLUMN aggregateexportcategoryoptioncombo" );
        executeSql( "ALTER TABLE dataelement DROP COLUMN aggregateexportattributeoptioncombo" );
        executeSql( "ALTER TABLE dataset DROP COLUMN aggregateexportcategoryoptioncombo" );
        executeSql( "ALTER TABLE dataset DROP COLUMN aggregateexportattributeoptioncombo" );
        executeSql( "ALTER TABLE indicator DROP COLUMN alternativename" );
        executeSql( "ALTER TABLE orgunitgroup DROP COLUMN image" );
        executeSql( "ALTER TABLE report DROP COLUMN usingorgunitgroupsets" );
        executeSql( "ALTER TABLE eventchart DROP COLUMN datatype" );
        executeSql( "ALTER TABLE validationrule DROP COLUMN type" );
        executeSql( "ALTER TABLE organisationunit DROP COLUMN active" );
        executeSql( "ALTER TABLE organisationunit DROP COLUMN uuid" );

        executeSql( "DROP INDEX datamart_crosstab" );

        // remove relative period type
        executeSql( "DELETE FROM period WHERE periodtypeid=(select periodtypeid from periodtype where name in ( 'Survey', 'OnChange', 'Relative' ))" );
        executeSql( "DELETE FROM periodtype WHERE name in ( 'Survey', 'OnChange', 'Relative' )" );

        // mapping
        executeSql( "DROP TABLE maporganisationunitrelation" );
        executeSql( "ALTER TABLE mapview DROP COLUMN mapid" );
        executeSql( "ALTER TABLE mapview DROP COLUMN mapsource" );
        executeSql( "ALTER TABLE mapview DROP COLUMN mapsourcetype" );
        executeSql( "ALTER TABLE mapview DROP COLUMN mapdatetype" );
        executeSql( "ALTER TABLE mapview DROP COLUMN featuretype" );
        executeSql( "ALTER TABLE mapview DROP COLUMN bounds" );
        executeSql( "ALTER TABLE mapview DROP COLUMN valuetype" );
        executeSql( "ALTER TABLE mapview DROP COLUMN legendtype" );
        executeSql( "ALTER TABLE mapview ALTER COLUMN opacity TYPE double precision" );

        executeSql( "ALTER TABLE maplegend DROP CONSTRAINT maplegend_name_key" );

        executeSql( "UPDATE mapview SET layer = 'thematic1' WHERE layer IS NULL" );
        executeSql( "UPDATE mapview SET hidden = false WHERE hidden IS NULL" );
        executeSql( "UPDATE mapview SET eventclustering = false WHERE eventclustering IS NULL" );
        executeSql( "UPDATE mapview SET eventpointradius = 0 WHERE eventpointradius IS NULL" );
        executeSql( "UPDATE programnotificationtemplate SET trackedentityattributeid = 0 WHERE trackedentityattributeid IS NULL" );


        executeSql( "DELETE FROM systemsetting WHERE name = 'longitude'" );
        executeSql( "DELETE FROM systemsetting WHERE name = 'latitude'" );

        executeSql( "ALTER TABLE maplayer DROP CONSTRAINT maplayer_mapsource_key" );
        executeSql( "ALTER TABLE maplayer DROP COLUMN mapsource" );
        executeSql( "ALTER TABLE maplayer DROP COLUMN mapsourcetype" );
        executeSql( "ALTER TABLE maplayer DROP COLUMN layer" );

        // extended data element
        executeSql( "ALTER TABLE dataelement DROP CONSTRAINT fk_dataelement_extendeddataelementid" );
        executeSql( "ALTER TABLE dataelement DROP COLUMN extendeddataelementid" );
        executeSql( "ALTER TABLE indicator DROP CONSTRAINT fk_indicator_extendeddataelementid" );
        executeSql( "ALTER TABLE indicator DROP COLUMN extendeddataelementid" );
        executeSql( "DROP TABLE extendeddataelement" );

        executeSql( "ALTER TABLE organisationunit DROP COLUMN hasPatients" );

        // category combo not null
        executeSql( "update dataelement set categorycomboid = " + defaultCategoryComboId + " where categorycomboid is null" );
        executeSql( "alter table dataelement alter column categorycomboid set not null" );

        executeSql( "update dataset set categorycomboid = " + defaultCategoryComboId + " where categorycomboid is null" );
        executeSql( "alter table dataset alter column categorycomboid set not null" );

        executeSql( "update program set categorycomboid = " + defaultCategoryComboId + " where categorycomboid is null" );
        executeSql( "alter table program alter column categorycomboid set not null" );

        // categories_categoryoptions
        // set to 0 temporarily
        int c1 = executeSql( "UPDATE categories_categoryoptions SET sort_order=0 WHERE sort_order is NULL OR sort_order=0" );
        if ( c1 > 0 )
        {
            updateSortOrder( "categories_categoryoptions", "categoryid", "categoryoptionid" );
        }
        executeSql( "ALTER TABLE categories_categoryoptions DROP CONSTRAINT categories_categoryoptions_pkey" );
        executeSql( "ALTER TABLE categories_categoryoptions ADD CONSTRAINT categories_categoryoptions_pkey PRIMARY KEY (categoryid, sort_order)" );

        // categorycombos_categories
        // set to 0 temporarily
        int c2 = executeSql( "update categorycombos_categories SET sort_order=0 where sort_order is NULL OR sort_order=0" );
        if ( c2 > 0 )
        {
            updateSortOrder( "categorycombos_categories", "categorycomboid", "categoryid" );
        }
        executeSql( "ALTER TABLE categorycombos_categories DROP CONSTRAINT categorycombos_categories_pkey" );
        executeSql( "ALTER TABLE categorycombos_categories ADD CONSTRAINT categorycombos_categories_pkey PRIMARY KEY (categorycomboid, sort_order)" );

        // categorycombos_optioncombos
        executeSql( "ALTER TABLE categorycombos_optioncombos DROP CONSTRAINT categorycombos_optioncombos_pkey" );
        executeSql( "ALTER TABLE categorycombos_optioncombos ADD CONSTRAINT categorycombos_optioncombos_pkey PRIMARY KEY (categoryoptioncomboid)" );
        executeSql( "ALTER TABLE categorycombos_optioncombos DROP CONSTRAINT fk4bae70f697e49675" );

        // categoryoptioncombos_categoryoptions
        executeSql( "alter table categoryoptioncombos_categoryoptions drop column sort_order" );
        executeSql( "alter table categoryoptioncombos_categoryoptions add constraint categoryoptioncombos_categoryoptions_pkey primary key(categoryoptioncomboid, categoryoptionid)" );

        // dataelementcategoryoption
        executeSql( "ALTER TABLE dataelementcategoryoption DROP CONSTRAINT fk_dataelement_categoryid" );
        executeSql( "ALTER TABLE dataelementcategoryoption DROP CONSTRAINT dataelementcategoryoption_shortname_key" );

        // minmaxdataelement - If the old, non-unique index exists, drop it, make sure there are no duplicate values (delete the older ones), then create the unique index.
        if ( executeSql( "DROP INDEX index_minmaxdataelement" ) == 0 )
        {
            executeSql( "delete from minmaxdataelement where minmaxdataelementid in (" +
                "select a.minmaxdataelementid from minmaxdataelement a " +
                "join minmaxdataelement b on a.sourceid = b.sourceid and a.dataelementid = b.dataelementid " +
                "and a.categoryoptioncomboid = b.categoryoptioncomboid and a.minmaxdataelementid < b.minmaxdataelementid)" );

            executeSql( "CREATE UNIQUE INDEX minmaxdataelement_unique_key ON minmaxdataelement USING btree (sourceid, dataelementid, categoryoptioncomboid)" );
        }

        // update periodType field to ValidationRule
        executeSql( "UPDATE validationrule SET periodtypeid = (SELECT periodtypeid FROM periodtype WHERE name='Monthly') WHERE periodtypeid is null" );

        // set varchar to text
        executeSql( "ALTER TABLE dataelement ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE dataelementgroupset ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE indicatorgroupset ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE orgunitgroupset ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE indicator ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE validationrule ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE expression ALTER COLUMN expression TYPE text" );
        executeSql( "ALTER TABLE translation ALTER COLUMN value TYPE text" );
        executeSql( "ALTER TABLE organisationunit ALTER COLUMN comment TYPE text" );
        executeSql( "ALTER TABLE program ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE trackedentityattribute ALTER COLUMN description TYPE text" );
        executeSql( "ALTER TABLE programrule ALTER COLUMN condition TYPE text" );
        executeSql( "ALTER TABLE programruleaction ALTER COLUMN content TYPE text" );
        executeSql( "ALTER TABLE programruleaction ALTER COLUMN data TYPE text" );
        executeSql( "ALTER TABLE trackedentitycomment ALTER COLUMN commenttext TYPE text" );
        executeSql( "ALTER TABLE users ALTER COLUMN openid TYPE text" );
        executeSql( "ALTER TABLE users ALTER COLUMN ldapid TYPE text" );
        executeSql( "ALTER TABLE dataentryform ALTER COLUMN htmlcode TYPE text" );

        executeSql( "ALTER TABLE minmaxdataelement RENAME minvalue TO minimumvalue" );
        executeSql( "ALTER TABLE minmaxdataelement RENAME maxvalue TO maximumvalue" );

        executeSql( "update minmaxdataelement set generatedvalue = generated where generatedvalue is null" );
        executeSql( "alter table minmaxdataelement drop column generated" );
        executeSql( "alter table minmaxdataelement alter column generatedvalue set not null" );

        // orgunit shortname uniqueness
        executeSql( "ALTER TABLE organisationunit DROP CONSTRAINT organisationunit_shortname_key" );

        executeSql( "ALTER TABLE section DROP CONSTRAINT section_name_key" );
        executeSql( "UPDATE section SET showrowtotals = false WHERE showrowtotals IS NULL" );
        executeSql( "UPDATE section SET showcolumntotals = false WHERE showcolumntotals IS NULL" );
        executeSql( "UPDATE dataelement SET aggregationtype='avg_sum_org_unit' where aggregationtype='average'" );

        // revert prepare aggregate*Value tables for offline diffs

        executeSql( "ALTER TABLE aggregateddatavalue DROP COLUMN modified" );
        executeSql( "ALTER TABLE aggregatedindicatorvalue DROP COLUMN modified " );
        executeSql( "UPDATE indicatortype SET indicatornumber=false WHERE indicatornumber is null" );

        // program

        executeSql( "ALTER TABLE programinstance ALTER COLUMN patientid DROP NOT NULL" );

        // migrate charts from dimension to category, series, filter

        executeSql( "UPDATE chart SET series='period', category='data', filter='organisationunit' WHERE dimension='indicator'" );
        executeSql( "UPDATE chart SET series='data', category='organisationunit', filter='period' WHERE dimension='organisationUnit'" );
        executeSql( "UPDATE chart SET series='period', category='data', filter='organisationunit' WHERE dimension='dataElement_period'" );
        executeSql( "UPDATE chart SET series='data', category='organisationunit', filter='period' WHERE dimension='organisationUnit_dataElement'" );
        executeSql( "UPDATE chart SET series='data', category='period', filter='organisationunit' WHERE dimension='period'" );
        executeSql( "UPDATE chart SET series='data', category='period', filter='organisationunit' WHERE dimension='period_dataElement'" );

        executeSql( "UPDATE chart SET type='bar' where type='bar3d'" );
        executeSql( "UPDATE chart SET type='stackedbar' where type='stackedBar'" );
        executeSql( "UPDATE chart SET type='stackedbar' where type='stackedBar3d'" );
        executeSql( "UPDATE chart SET type='line' where type='line3d'" );
        executeSql( "UPDATE chart SET type='pie' where type='pie'" );
        executeSql( "UPDATE chart SET type='pie' where type='pie3d'" );

        executeSql( "UPDATE chart SET type=lower(type), series=lower(series), category=lower(category), filter=lower(filter)" );

        executeSql( "ALTER TABLE chart ALTER COLUMN dimension DROP NOT NULL" );
        executeSql( "ALTER TABLE chart DROP COLUMN size" );
        executeSql( "ALTER TABLE chart DROP COLUMN verticallabels" );
        executeSql( "ALTER TABLE chart DROP COLUMN targetline" );
        executeSql( "ALTER TABLE chart DROP COLUMN horizontalplotorientation" );

        executeSql( "ALTER TABLE chart DROP COLUMN monthsLastYear" );
        executeSql( "ALTER TABLE chart DROP COLUMN quartersLastYear" );
        executeSql( "ALTER TABLE chart DROP COLUMN last6BiMonths" );

        executeSql( "ALTER TABLE chart DROP CONSTRAINT chart_title_key" );
        executeSql( "ALTER TABLE chart DROP CONSTRAINT chart_name_key" );

        executeSql( "ALTER TABLE chart DROP COLUMN domainaxixlabel" );
        executeSql( "ALTER TABLE chart DROP COLUMN rewindrelativeperiods" );

        executeSql( "ALTER TABLE chart ALTER hideLegend DROP NOT NULL" );
        executeSql( "ALTER TABLE chart ALTER regression DROP NOT NULL" );
        executeSql( "ALTER TABLE chart ALTER hideSubtitle DROP NOT NULL" );
        executeSql( "ALTER TABLE chart ALTER userOrganisationUnit DROP NOT NULL" );

        // remove outdated relative periods

        executeSql( "ALTER TABLE reporttable DROP COLUMN last6months" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN last9months" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN sofarthisyear" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN sofarthisfinancialyear" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN last3to6months" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN last6to9months" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN last9to12months" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN last12individualmonths" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN individualmonthsthisyear" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN individualquartersthisyear" );
        executeSql( "ALTER TABLE reporttable DROP COLUMN programid" );

        executeSql( "ALTER TABLE chart DROP COLUMN last6months" );
        executeSql( "ALTER TABLE chart DROP COLUMN last9months" );
        executeSql( "ALTER TABLE chart DROP COLUMN sofarthisyear" );
        executeSql( "ALTER TABLE chart DROP COLUMN sofarthisfinancialyear" );
        executeSql( "ALTER TABLE chart DROP COLUMN last3to6months" );
        executeSql( "ALTER TABLE chart DROP COLUMN last6to9months" );
        executeSql( "ALTER TABLE chart DROP COLUMN last9to12months" );
        executeSql( "ALTER TABLE chart DROP COLUMN last12individualmonths" );
        executeSql( "ALTER TABLE chart DROP COLUMN individualmonthsthisyear" );
        executeSql( "ALTER TABLE chart DROP COLUMN individualquartersthisyear" );
        executeSql( "ALTER TABLE chart DROP COLUMN organisationunitgroupsetid" );
        executeSql( "ALTER TABLE chart DROP COLUMN programid" );

        // remove source

        executeSql( "ALTER TABLE datasetsource DROP CONSTRAINT fk766ae2938fd8026a" );
        executeSql( "ALTER TABLE datasetlocksource DROP CONSTRAINT fk582fdf7e8fd8026a" );
        executeSql( "ALTER TABLE completedatasetregistration DROP CONSTRAINT fk_datasetcompleteregistration_sourceid" );
        executeSql( "ALTER TABLE minmaxdataelement DROP CONSTRAINT fk_minmaxdataelement_sourceid" );
        executeSql( "ALTER TABLE datavalue DROP CONSTRAINT fk_datavalue_sourceid" );
        executeSql( "ALTER TABLE organisationunit DROP CONSTRAINT fke509dd5ef1c932ed" );
        executeSql( "DROP TABLE source CASCADE" );
        executeSql( "DROP TABLE datavaluearchive" );

        // message

        executeSql( "ALTER TABLE messageconversation DROP COLUMN messageconversationkey" );
        executeSql( "UPDATE messageconversation SET lastmessage=lastupdated WHERE lastmessage is null" );
        executeSql( "ALTER TABLE message DROP COLUMN messagesubject" );
        executeSql( "ALTER TABLE message DROP COLUMN messagekey" );
        executeSql( "ALTER TABLE message DROP COLUMN sentdate" );
        executeSql( "ALTER TABLE usermessage DROP COLUMN messagedate" );
        executeSql( "UPDATE usermessage SET isfollowup=false WHERE isfollowup is null" );
        executeSql( "DROP TABLE message_usermessages" );

        // create code unique constraints

        executeSql( "ALTER TABLE dataelement ADD CONSTRAINT dataelement_code_key UNIQUE(code)" );
        executeSql( "ALTER TABLE indicator ADD CONSTRAINT indicator_code_key UNIQUE(code)" );
        executeSql( "ALTER TABLE organisationunit ADD CONSTRAINT organisationunit_code_key UNIQUE(code)" );
        executeSql( "ALTER TABLE organisationunit ALTER COLUMN code TYPE varchar(50)" );
        executeSql( "ALTER TABLE indicator ALTER COLUMN code TYPE varchar(50)" );

        // remove uuid

        executeSql( "ALTER TABLE attribute DROP COLUMN uuid" );
        executeSql( "ALTER TABLE categorycombo DROP COLUMN uuid" );
        executeSql( "ALTER TABLE categoryoptioncombo DROP COLUMN uuid" );
        executeSql( "ALTER TABLE chart DROP COLUMN uuid" );
        executeSql( "ALTER TABLE concept DROP COLUMN uuid" );
        executeSql( "ALTER TABLE constant DROP COLUMN uuid" );
        executeSql( "ALTER TABLE dataelement DROP COLUMN uuid" );
        executeSql( "ALTER TABLE dataelementcategory DROP COLUMN uuid" );
        executeSql( "ALTER TABLE dataelementcategoryoption DROP COLUMN uuid" );
        executeSql( "ALTER TABLE dataelementgroup DROP COLUMN uuid" );
        executeSql( "ALTER TABLE dataelementgroupset DROP COLUMN uuid" );
        executeSql( "ALTER TABLE dataset DROP COLUMN uuid" );
        executeSql( "ALTER TABLE indicator DROP COLUMN uuid" );
        executeSql( "ALTER TABLE indicatorgroup DROP COLUMN uuid" );
        executeSql( "ALTER TABLE indicatorgroupset DROP COLUMN uuid" );
        executeSql( "ALTER TABLE indicatortype DROP COLUMN uuid" );
        // executeSql( "ALTER TABLE organisationunit DROP COLUMN uuid" );
        executeSql( "ALTER TABLE orgunitgroup DROP COLUMN uuid" );
        executeSql( "ALTER TABLE orgunitgroupset DROP COLUMN uuid" );
        executeSql( "ALTER TABLE orgunitlevel DROP COLUMN uuid" );
        executeSql( "ALTER TABLE report DROP COLUMN uuid" );
        executeSql( "ALTER TABLE validationrule DROP COLUMN uuid" );
        executeSql( "ALTER TABLE validationrulegroup DROP COLUMN uuid" );

        // replace null with false for boolean fields
        executeSql( "update dataset set fieldcombinationrequired = false where fieldcombinationrequired is null" );
        executeSql( "update chart set hidelegend = false where hidelegend is null" );
        executeSql( "update chart set regression = false where regression is null" );
        executeSql( "update chart set hidesubtitle = false where hidesubtitle is null" );
        executeSql( "update chart set userorganisationunit = false where userorganisationunit is null" );
        executeSql( "update chart set percentstackedvalues = false where percentstackedvalues is null" );
        executeSql( "update chart set cumulativevalues = false where cumulativevalues is null" );
        executeSql( "update chart set nospacebetweencolumns = false where nospacebetweencolumns is null" );
        executeSql( "update indicator set annualized = false where annualized is null" );
        executeSql( "update indicatortype set indicatornumber = false where indicatornumber is null" );
        executeSql( "update dataset set mobile = false where mobile is null" );
        executeSql( "update dataset set allowfutureperiods = false where allowfutureperiods is null" );
        executeSql( "update dataset set validcompleteonly = false where validcompleteonly is null" );
        executeSql( "update dataset set notifycompletinguser = false where notifycompletinguser is null" );
        executeSql( "update dataset set approvedata = false where approvedata is null" );
        executeSql( "update dataelement set zeroissignificant = false where zeroissignificant is null" );
        executeSql( "update organisationunit set haspatients = false where haspatients is null" );
        executeSql( "update organisationunit set openingdate = '1970-01-01' where openingdate is null" );
        executeSql( "update dataset set expirydays = 0 where expirydays is null" );
        executeSql( "update eventchart set hidelegend = false where hidelegend is null" );
        executeSql( "update eventchart set regression = false where regression is null" );
        executeSql( "update eventchart set hidetitle = false where hidetitle is null" );
        executeSql( "update eventchart set hidesubtitle = false where hidesubtitle is null" );
        executeSql( "update eventchart set hidenadata = false where hidenadata is null" );
        executeSql( "update eventchart set percentstackedvalues = false where percentstackedvalues is null" );
        executeSql( "update eventchart set cumulativevalues = false where cumulativevalues is null" );
        executeSql( "update eventchart set nospacebetweencolumns = false where nospacebetweencolumns is null" );
        executeSql( "update reporttable set showdimensionlabels = false where showdimensionlabels is null" );
        executeSql( "update eventreport set showdimensionlabels = false where showdimensionlabels is null" );
        executeSql( "update reporttable set skiprounding = false where skiprounding is null" );
        executeSql( "update validationnotificationtemplate set sendstrategy = 'COLLECTIVE_SUMMARY' where sendstrategy is null" );

        // move timelydays from system setting => dataset property
        executeSql( "update dataset set timelydays = 15 where timelydays is null" );
        executeSql( "delete from systemsetting where name='completenessOffset'" );

        executeSql( "update report set paramreportingmonth = false where paramreportingmonth is null" );
        executeSql( "update report set paramparentorganisationunit = false where paramorganisationunit is null" );

        executeSql( "update reporttable set paramreportingmonth = false where paramreportingmonth is null" );
        executeSql( "update reporttable set paramparentorganisationunit = false where paramparentorganisationunit is null" );
        executeSql( "update reporttable set paramorganisationunit = false where paramorganisationunit is null" );
        executeSql( "update reporttable set paramgrandparentorganisationunit = false where paramgrandparentorganisationunit is null" );

        executeSql( "update reporttable set reportingmonth = false where reportingmonth is null" );
        executeSql( "update reporttable set reportingbimonth = false where reportingbimonth is null" );
        executeSql( "update reporttable set reportingquarter = false where reportingquarter is null" );
        executeSql( "update reporttable set monthsthisyear = false where monthsthisyear is null" );
        executeSql( "update reporttable set quartersthisyear = false where quartersthisyear is null" );
        executeSql( "update reporttable set thisyear = false where thisyear is null" );
        executeSql( "update reporttable set monthslastyear = false where monthslastyear is null" );
        executeSql( "update reporttable set quarterslastyear = false where quarterslastyear is null" );
        executeSql( "update reporttable set lastyear = false where lastyear is null" );
        executeSql( "update reporttable set last5years = false where last5years is null" );
        executeSql( "update reporttable set lastsixmonth = false where lastsixmonth is null" );
        executeSql( "update reporttable set last4quarters = false where last4quarters is null" );
        executeSql( "update reporttable set last12months = false where last12months is null" );
        executeSql( "update reporttable set last3months = false where last3months is null" );
        executeSql( "update reporttable set last6bimonths = false where last6bimonths is null" );
        executeSql( "update reporttable set last4quarters = false where last4quarters is null" );
        executeSql( "update reporttable set last2sixmonths = false where last2sixmonths is null" );
        executeSql( "update reporttable set thisfinancialyear = false where thisfinancialyear is null" );
        executeSql( "update reporttable set lastfinancialyear = false where lastfinancialyear is null" );
        executeSql( "update reporttable set last5financialyears = false where last5financialyears is null" );
        executeSql( "update reporttable set cumulative = false where cumulative is null" );
        executeSql( "update reporttable set userorganisationunit = false where userorganisationunit is null" );
        executeSql( "update reporttable set userorganisationunitchildren = false where userorganisationunitchildren is null" );
        executeSql( "update reporttable set userorganisationunitgrandchildren = false where userorganisationunitgrandchildren is null" );
        executeSql( "update reporttable set subtotals = true where subtotals is null" );
        executeSql( "update reporttable set hideemptyrows = false where hideemptyrows is null" );
        executeSql( "update reporttable set hideemptycolumns = false where hideemptycolumns is null" );
        executeSql( "update reporttable set displaydensity = 'normal' where displaydensity is null" );
        executeSql( "update reporttable set fontsize = 'normal' where fontsize is null" );
        executeSql( "update reporttable set digitgroupseparator = 'space' where digitgroupseparator is null" );
        executeSql( "update reporttable set sortorder = 0 where sortorder is null" );
        executeSql( "update reporttable set toplimit = 0 where toplimit is null" );
        executeSql( "update reporttable set showhierarchy = false where showhierarchy is null" );
        executeSql( "update reporttable set legenddisplaystyle = 'FILL' where legenddisplaystyle is null" );
        executeSql( "update reporttable set legenddisplaystrategy = 'FIXED' where legenddisplaystrategy is null" );
        executeSql( "update reporttable set hidetitle = false where hidetitle is null" );
        executeSql( "update reporttable set hidesubtitle = false where hidesubtitle is null" );

        // reporttable col/row totals = keep existing || copy from totals || true
        executeSql( "update reporttable set totals = true where totals is null" );
        executeSql( "update reporttable set coltotals = totals where coltotals is null" );
        executeSql( "update reporttable set coltotals = true where coltotals is null" );
        executeSql( "update reporttable set rowtotals = totals where rowtotals is null" );
        executeSql( "update reporttable set rowtotals = true where rowtotals is null" );
        executeSql( "alter table reporttable drop column totals" );

        // reporttable col/row subtotals
        executeSql( "update reporttable set colsubtotals = subtotals where colsubtotals is null" );
        executeSql( "update reporttable set rowsubtotals = subtotals where rowsubtotals is null" );

        // reporttable upgrade counttype to outputtype
        executeSql( "update eventreport set outputtype = 'EVENT' where outputtype is null and counttype = 'events'" );
        executeSql( "update eventreport set outputtype = 'TRACKED_ENTITY_INSTANCE' where outputtype is null and counttype = 'tracked_entity_instances'" );
        executeSql( "update eventreport set hidetitle = false where hidetitle is null" );
        executeSql( "update eventreport set hidesubtitle = false where hidesubtitle is null" );
        executeSql( "update eventreport set outputtype = 'EVENT' where outputtype is null" );
        executeSql( "alter table eventreport drop column counttype" );

        executeSql( "update chart set reportingmonth = false where reportingmonth is null" );
        executeSql( "update chart set reportingbimonth = false where reportingbimonth is null" );
        executeSql( "update chart set reportingquarter = false where reportingquarter is null" );
        executeSql( "update chart set monthsthisyear = false where monthsthisyear is null" );
        executeSql( "update chart set quartersthisyear = false where quartersthisyear is null" );
        executeSql( "update chart set thisyear = false where thisyear is null" );
        executeSql( "update chart set monthslastyear = false where monthslastyear is null" );
        executeSql( "update chart set quarterslastyear = false where quarterslastyear is null" );
        executeSql( "update chart set lastyear = false where lastyear is null" );
        executeSql( "update chart set lastsixmonth = false where lastsixmonth is null" );
        executeSql( "update chart set last12months = false where last12months is null" );
        executeSql( "update chart set last3months = false where last3months is null" );
        executeSql( "update chart set last5years = false where last5years is null" );
        executeSql( "update chart set last4quarters = false where last4quarters is null" );
        executeSql( "update chart set last6bimonths = false where last6bimonths is null" );
        executeSql( "update chart set last4quarters = false where last4quarters is null" );
        executeSql( "update chart set last2sixmonths = false where last2sixmonths is null" );
        executeSql( "update chart set showdata = false where showdata is null" );
        executeSql( "update chart set userorganisationunit = false where userorganisationunit is null" );
        executeSql( "update chart set userorganisationunitchildren = false where userorganisationunitchildren is null" );
        executeSql( "update chart set userorganisationunitgrandchildren = false where userorganisationunitgrandchildren is null" );
        executeSql( "update chart set hidetitle = false where hidetitle is null" );
        executeSql( "update chart set sortorder = 0 where sortorder is null" );

        executeSql( "update eventreport set showhierarchy = false where showhierarchy is null" );
        executeSql( "update eventreport set counttype = 'events' where counttype is null" );
        executeSql( "update eventreport set hidenadata = false where hidenadata is null" );

        // eventreport col/rowtotals = keep existing || copy from totals || true
        executeSql( "update eventreport set totals = true where totals is null" );
        executeSql( "update eventreport set coltotals = totals where coltotals is null" );
        executeSql( "update eventreport set coltotals = true where coltotals is null" );
        executeSql( "update eventreport set rowtotals = totals where rowtotals is null" );
        executeSql( "update eventreport set rowtotals = true where rowtotals is null" );
        executeSql( "alter table eventreport drop column totals" );

        // eventreport col/row subtotals
        executeSql( "update eventreport set colsubtotals = subtotals where colsubtotals is null" );
        executeSql( "update eventreport set rowsubtotals = subtotals where rowsubtotals is null" );

        // eventchart upgrade counttype to outputtype
        executeSql( "update eventchart set outputtype = 'EVENT' where outputtype is null and counttype = 'events'" );
        executeSql( "update eventchart set outputtype = 'TRACKED_ENTITY_INSTANCE' where outputtype is null and counttype = 'tracked_entity_instances'" );
        executeSql( "update eventchart set outputtype = 'EVENT' where outputtype is null" );
        executeSql( "alter table eventchart drop column counttype" );

        executeSql( "update eventchart set sortorder = 0 where sortorder is null" );

        // Move chart filters to chart_filters table

        executeSql( "insert into chart_filters (chartid, sort_order, filter) select chartid, 0, filter from chart" );
        executeSql( "alter table chart drop column filter" );

        // Upgrade chart dimension identifiers

        executeSql( "update chart set series = 'dx' where series = 'data'" );
        executeSql( "update chart set series = 'pe' where series = 'period'" );
        executeSql( "update chart set series = 'ou' where series = 'organisationunit'" );
        executeSql( "update chart set category = 'dx' where category = 'data'" );
        executeSql( "update chart set category = 'pe' where category = 'period'" );
        executeSql( "update chart set category = 'ou' where category = 'organisationunit'" );
        executeSql( "update chart_filters set filter = 'dx' where filter = 'data'" );
        executeSql( "update chart_filters set filter = 'pe' where filter = 'period'" );
        executeSql( "update chart_filters set filter = 'ou' where filter = 'organisationunit'" );

        executeSql( "update dataentryform set format = 1 where format is null" );

        executeSql( "update dataelementgroup set shortname=name where shortname is null and length(name)<=50" );
        executeSql( "update orgunitgroup set shortname=name where shortname is null and length(name)<=50" );

        // report, reporttable, chart groups

        executeSql( "DROP TABLE reportgroupmembers" );
        executeSql( "DROP TABLE reportgroup" );
        executeSql( "DROP TABLE reporttablegroupmembers" );
        executeSql( "DROP TABLE reporttablegroup" );
        executeSql( "DROP TABLE chartgroupmembers" );
        executeSql( "DROP TABLE chartgroup" );

        executeSql( "delete from usersetting where name='currentStyle' and value like '%blue/blue.css'" );
        executeSql( "delete from systemsetting where name='currentStyle' and value like '%blue/blue.css'" );

        executeSql( "update dataentryform set style='regular' where style is null" );

        executeSql( "UPDATE dataset SET skipaggregation = false WHERE skipaggregation IS NULL" );
        executeSql( "UPDATE dataset SET skipoffline = false WHERE skipoffline IS NULL" );
        executeSql( "UPDATE dataset SET renderastabs = false WHERE renderastabs IS NULL" );
        executeSql( "UPDATE dataset SET renderhorizontally = false WHERE renderhorizontally IS NULL" );
        executeSql( "UPDATE dataset SET novaluerequirescomment = false WHERE novaluerequirescomment IS NULL" );
        executeSql( "UPDATE dataset SET openfutureperiods = 12 where allowfutureperiods is true" );
        executeSql( "UPDATE dataset SET openfutureperiods = 0 where allowfutureperiods is false" );
        executeSql( "ALTER TABLE dataset DROP COLUMN allowfutureperiods" );

        executeSql( "UPDATE categorycombo SET skiptotal = false WHERE skiptotal IS NULL" );

        // short names
        executeSql( "ALTER TABLE dataelement ALTER COLUMN shortname TYPE character varying(50)" );
        executeSql( "ALTER TABLE indicator ALTER COLUMN shortname TYPE character varying(50)" );
        executeSql( "ALTER TABLE dataset ALTER COLUMN shortname TYPE character varying(50)" );
        executeSql( "ALTER TABLE organisationunit ALTER COLUMN shortname TYPE character varying(50)" );

        executeSql( "update report set type='jasperReportTable' where type is null and reporttableid is not null" );
        executeSql( "update report set type='jasperJdbc' where type is null and reporttableid is null" );

        // upgrade authorities
        executeSql( "UPDATE userroleauthorities SET authority='F_DOCUMENT_PUBLIC_ADD' WHERE authority='F_DOCUMENT_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_REPORT_PUBLIC_ADD' WHERE authority='F_REPORT_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_REPORTTABLE_PUBLIC_ADD' WHERE authority='F_REPORTTABLE_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_DATASET_PUBLIC_ADD' WHERE authority='F_DATASET_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_DATAELEMENT_PUBLIC_ADD' WHERE authority='F_DATAELEMENT_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_DATAELEMENTGROUP_PUBLIC_ADD' WHERE authority='F_DATAELEMENTGROUP_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_DATAELEMENTGROUPSET_PUBLIC_ADD' WHERE authority='F_DATAELEMENTGROUPSET_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_ORGUNITGROUP_PUBLIC_ADD' WHERE authority='F_ORGUNITGROUP_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_ORGUNITGROUPSET_PUBLIC_ADD' WHERE authority='F_ORGUNITGROUPSET_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_INDICATOR_PUBLIC_ADD' WHERE authority='F_INDICATOR_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_INDICATORGROUP_PUBLIC_ADD' WHERE authority='F_INDICATORGROUP_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_INDICATORGROUPSET_PUBLIC_ADD' WHERE authority='F_INDICATORGROUPSET_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_USERROLE_PUBLIC_ADD' WHERE authority='F_USERROLE_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_USERGROUP_PUBLIC_ADD' WHERE authority='F_USER_GRUP_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_USERGROUP_UPDATE' WHERE authority='F_USER_GRUP_UPDATE'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_USERGROUP_DELETE' WHERE authority='F_USER_GRUP_DELETE'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_USERGROUP_LIST' WHERE authority='F_USER_GRUP_LIST'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_SQLVIEW_PUBLIC_ADD' WHERE authority='F_SQLVIEW_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_OPTIONSET_PUBLIC_ADD' WHERE authority='F_OPTIONSET_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_VALIDATIONRULEGROUP_PUBLIC_ADD' WHERE authority='F_VALIDATIONRULEGROUP_ADD'" );
        executeSql( "UPDATE userroleauthorities SET authority='F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD' WHERE authority='F_TRACKED_ENTITY_ATTRIBUTE_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_PROGRAM_INDICATOR_PUBLIC_ADD' WHERE authority='F_ADD_PROGRAM_INDICATOR'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_LEGEND_SET_PUBLIC_ADD' WHERE authority='F_LEGEND_SET_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_VALIDATIONRULE_PUBLIC_ADD' WHERE authority='F_VALIDATIONRULE_ADD'" );

        executeSql( "UPDATE userroleauthorities SET authority='F_ATTRIBUTE_PUBLIC_ADD' WHERE authority='F_ATTRIBUTE_ADD'" );

        // remove unused authorities
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_CONCEPT_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_CONSTANT_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_DATAELEMENT_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_DATAELEMENTGROUP_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_DATAELEMENTGROUPSET_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_DATAELEMENT_MINMAX_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_DATASET_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_SECTION_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_DATAVALUE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_INDICATOR_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_INDICATORTYPE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_INDICATORGROUP_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_INDICATORGROUPSET_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_ORGANISATIONUNIT_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_ORGUNITGROUP_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_ORGUNITGROUPSET_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_USERROLE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_USERGROUP_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_VALIDATIONRULE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_VALIDATIONRULEGROUP_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_REPORT_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_SQLVIEW_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_VALIDATIONCRITERIA_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_OPTIONSET_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_ATTRIBUTE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PATIENTATTRIBUTE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PATIENT_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_UPDATE_PROGRAM_INDICATOR'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PROGRAM_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PROGRAMSTAGE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PROGRAMSTAGE_SECTION_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PATIENTIDENTIFIERTYPE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PROGRAM_ATTRIBUTE_UPDATE'" );
        executeSql( "DELETE FROM userroleauthorities WHERE authority='F_PATIENT_DATAVALUE_UPDATE'" );

        // remove unused configurations
        executeSql( "delete from systemsetting where name='keySmsConfig'" );
        executeSql( "delete from systemsetting where name='keySmsConfiguration'" );
        executeSql( "delete from systemsetting where name='keySmsConfigurations'" );
        executeSql( "UPDATE incomingsms SET userid = 0 WHERE userid IS NULL" );

        // update denominator of indicator which has indicatortype as 'number'
        executeSql( "UPDATE indicator SET denominator = 1, denominatordescription = '' WHERE indicatortypeid IN (SELECT DISTINCT indicatortypeid FROM indicatortype WHERE indicatornumber = true) AND denominator IS NULL" );

        // remove name/shortName uniqueness
        executeSql( "ALTER TABLE organisationunit DROP CONSTRAINT organisationunit_name_key" );
        executeSql( "ALTER TABLE orgunitgroup ADD CONSTRAINT orgunitgroup_name_key UNIQUE (name)" );
        executeSql( "ALTER TABLE orgunitgroupset ADD CONSTRAINT orgunitgroupset_name_key UNIQUE (name)" );
        executeSql( "ALTER TABLE indicator DROP CONSTRAINT indicator_name_key" );
        executeSql( "ALTER TABLE indicator DROP CONSTRAINT indicator_shortname_key" );
        executeSql( "ALTER TABLE indicatorgroup DROP CONSTRAINT indicatorgroup_name_key" );
        executeSql( "ALTER TABLE indicatorgroupset DROP CONSTRAINT indicatorgroupset_name_key" );
        executeSql( "ALTER TABLE dataset DROP CONSTRAINT dataset_name_key" );
        executeSql( "ALTER TABLE dataset DROP CONSTRAINT dataset_shortname_key" );
        executeSql( "ALTER TABLE document DROP CONSTRAINT document_name_key" );
        executeSql( "ALTER TABLE reporttable DROP CONSTRAINT reporttable_name_key" );
        executeSql( "ALTER TABLE report DROP CONSTRAINT report_name_key" );
        executeSql( "ALTER TABLE usergroup DROP CONSTRAINT usergroup_name_key" );

        executeSql( "ALTER TABLE dataelementcategory DROP COLUMN conceptid" );
        executeSql( "ALTER TABLE dataelementcategoryoption DROP COLUMN conceptid" );

        // upgrade system charts/maps to public read-only sharing
        executeSql( "UPDATE chart SET publicaccess='r-------' WHERE user IS NULL AND publicaccess IS NULL;" );
        executeSql( "UPDATE map SET publicaccess='r-------' WHERE user IS NULL AND publicaccess IS NULL;" );

        executeSql( "UPDATE chart SET publicaccess='--------' WHERE user IS NULL AND publicaccess IS NULL;" );
        executeSql( "UPDATE map SET publicaccess='-------' WHERE user IS NULL AND publicaccess IS NULL;" );

        executeSql( "update dataelementcategory set datadimension = false where datadimension is null" );

        executeSql( "UPDATE dataset SET dataelementdecoration=false WHERE dataelementdecoration is null" );

        executeSql( "update sqlview set sqlviewid=viweid" );
        executeSql( "alter table sqlview drop column viewid" );
        executeSql( "update sqlview set type = 'QUERY' where query is true" );
        executeSql( "update sqlview set type = 'VIEW' where type is null" );
        executeSql( "alter table sqlview drop column query" );

        executeSql( "UPDATE dashboard SET publicaccess='--------' WHERE publicaccess is null" );

        executeSql( "UPDATE optionset SET version=0 WHERE version IS NULL" );
        executeSql( "UPDATE dataset SET version=0 WHERE version IS NULL" );
        executeSql( "UPDATE program SET version=0 WHERE version IS NULL" );
        executeSql( "update program set shortname = substring(name,0,50) where shortname is null" );

        executeSql( "update programstageinstance set attributeoptioncomboid = " + defaultOptionComboId + " where attributeoptioncomboid is null" );
        executeSql( "update programstageinstance set storedby=completedby where storedby is null and completedby is not null" );

        executeSql( "ALTER TABLE datavalue ALTER COLUMN lastupdated TYPE timestamp" );
        executeSql( "ALTER TABLE completedatasetregistration ALTER COLUMN date TYPE timestamp" );
        executeSql( "ALTER TABLE message ALTER COLUMN userid DROP NOT NULL" );
        executeSql( "ALTER TABLE message ALTER COLUMN messagetext TYPE text" );
        executeSql( "drop index crosstab" );

        executeSql( "delete from usersetting where name = 'dashboardConfig' or name = 'dashboardConfiguration'" );
        executeSql( "update usersetting set name = 'keyUiLocale' where name = 'currentLocale'" );
        executeSql( "update usersetting set name = 'keyDbLocale' where name = 'keyLocaleUserSetting'" );
        executeSql( "update usersetting set name = 'keyStyle' where name = 'currentStyle'" );
        executeSql( "ALTER TABLE interpretation ALTER COLUMN userid DROP NOT NULL" );
        executeSql( "UPDATE interpretation SET publicaccess='r-------' WHERE publicaccess IS NULL;" );

        executeSql( "ALTER TABLE dataset DROP COLUMN symbol" );
        executeSql( "ALTER TABLE users ALTER COLUMN password DROP NOT NULL" );


        // set default dataDimension on orgUnitGroupSet and deGroupSet
        executeSql( "UPDATE dataelementgroupset SET datadimension=true WHERE datadimension IS NULL" );
        executeSql( "ALTER TABLE dataelementgroupset ALTER COLUMN datadimension SET NOT NULL" );
        executeSql( "UPDATE orgunitgroupset SET datadimension=true WHERE datadimension IS NULL" );
        executeSql( "ALTER TABLE orgunitgroupset ALTER COLUMN datadimension SET NOT NULL" );
        executeSql( "ALTER TABLE validationnotificationtemplate ALTER COLUMN sendstrategy SET NOT NULL" );

        // set attribute defaults
        executeSql( "UPDATE attribute SET dataelementattribute=false WHERE dataelementattribute IS NULL" );
        executeSql( "UPDATE attribute SET dataelementgroupattribute=false WHERE dataelementgroupattribute IS NULL" );
        executeSql( "UPDATE attribute SET indicatorattribute=false WHERE indicatorattribute IS NULL" );
        executeSql( "UPDATE attribute SET indicatorgroupattribute=false WHERE indicatorgroupattribute IS NULL" );
        executeSql( "UPDATE attribute SET organisationunitattribute=false WHERE organisationunitattribute IS NULL" );
        executeSql( "UPDATE attribute SET organisationunitgroupattribute=false WHERE organisationunitgroupattribute IS NULL" );
        executeSql( "UPDATE attribute SET organisationunitgroupsetattribute=false WHERE organisationunitgroupsetattribute IS NULL" );
        executeSql( "UPDATE attribute SET userattribute=false WHERE userattribute IS NULL" );
        executeSql( "UPDATE attribute SET usergroupattribute=false WHERE usergroupattribute IS NULL" );
        executeSql( "UPDATE attribute SET datasetattribute=false WHERE datasetattribute IS NULL" );
        executeSql( "UPDATE attribute SET programattribute=false WHERE programattribute IS NULL" );
        executeSql( "UPDATE attribute SET programstageattribute=false WHERE programstageattribute IS NULL" );
        executeSql( "UPDATE attribute SET trackedentityattribute=false WHERE trackedentityattribute IS NULL" );
        executeSql( "UPDATE attribute SET trackedentityattributeattribute=false WHERE trackedentityattributeattribute IS NULL" );
        executeSql( "UPDATE attribute SET categoryoptionattribute=false WHERE categoryoptionattribute IS NULL" );
        executeSql( "UPDATE attribute SET categoryoptiongroupattribute=false WHERE categoryoptiongroupattribute IS NULL" );
        executeSql( "UPDATE attribute SET documentattribute=false WHERE documentattribute IS NULL" );
        executeSql( "UPDATE attribute SET optionattribute=false WHERE optionattribute IS NULL" );
        executeSql( "UPDATE attribute SET optionsetattribute=false WHERE optionsetattribute IS NULL" );
        executeSql( "UPDATE attribute SET constantattribute=false WHERE constantattribute IS NULL" );
        executeSql( "UPDATE attribute SET legendsetattribute=false WHERE legendsetattribute IS NULL" );
        executeSql( "UPDATE attribute SET programindicatorattribute=false WHERE programindicatorattribute IS NULL" );
        executeSql( "UPDATE attribute SET sqlViewAttribute=false WHERE sqlViewAttribute IS NULL" );
        executeSql( "UPDATE attribute SET sectionAttribute=false WHERE sectionAttribute IS NULL" );
        executeSql( "UPDATE attribute SET categoryoptioncomboattribute=false WHERE categoryoptioncomboattribute IS NULL" );

        executeSql( "update attribute set isunique=false where isunique is null" );

        executeSql( "ALTER TABLE trackedentityattributedimension DROP COLUMN operator" );
        executeSql( "ALTER TABLE trackedentitydataelementdimension DROP COLUMN operator" );

        // update attribute.code, set to null if code=''
        executeSql( "UPDATE attribute SET code=NULL WHERE code=''" );

        //update programruleaction:
        executeSql( "ALTER TABLE programruleaction DROP COLUMN name" );

        //update programrule
        executeSql( "UPDATE programrule SET rulecondition = condition WHERE rulecondition IS NULL" );
        executeSql( "ALTER TABLE programrule DROP COLUMN condition" );

        // data approval
        executeSql( "UPDATE dataapproval SET accepted=false WHERE accepted IS NULL" );
        executeSql( "ALTER TABLE dataapproval ALTER COLUMN accepted SET NOT NULL" );
        executeSql( "DELETE FROM dataapproval WHERE categoryoptiongroupid IS NOT NULL" );
        executeSql( "ALTER TABLE dataapproval DROP COLUMN categoryoptiongroupid" );
        executeSql( "UPDATE dataapproval SET attributeoptioncomboid=categoryoptioncomboid WHERE categoryoptioncomboid IS NOT NULL" );
        executeSql( "ALTER TABLE dataapproval DROP COLUMN categoryoptioncomboid" );
        executeSql( "UPDATE dataapproval SET attributeoptioncomboid=" + defaultCategoryComboId + " WHERE attributeoptioncomboid IS NULL" );
        executeSql( "ALTER TABLE dataapproval ALTER COLUMN attributeoptioncomboid SET NOT NULL" );

        // validation rule group, new column alertbyorgunits
        executeSql( "UPDATE validationrulegroup SET alertbyorgunits=false WHERE alertbyorgunits IS NULL" );

        executeSql( "update expression set missingvaluestrategy = 'SKIP_IF_ANY_VALUE_MISSING' where missingvaluestrategy is null and (nullifblank is true or nullifblank is null)" );
        executeSql( "update expression set missingvaluestrategy = 'NEVER_SKIP' where missingvaluestrategy is null nullifblank is false" );
        executeSql( "alter table expression alter column missingvaluestrategy set not null" );
        executeSql( "alter table expression drop column nullifblank" );
        executeSql( "drop table expressiondataelement" );
        executeSql( "drop table expressionsampleelement" );

        executeSql( "alter table dataelementcategoryoption alter column startdate type date" );
        executeSql( "alter table dataelementcategoryoption alter column enddate type date" );

        executeSql( "alter table dataelement drop column sortorder" );
        executeSql( "alter table indicator drop column sortorder" );
        executeSql( "alter table dataset drop column sortorder" );

        executeSql( "alter table dataelement drop column active" );

        executeSql( "alter table datavalue alter column value type varchar(50000)" );
        executeSql( "alter table datavalue alter column comment type varchar(50000)" );
        executeSql( "alter table datavalueaudit alter column value type varchar(50000)" );
        executeSql( "alter table trackedentitydatavalue alter column value type varchar(50000)" );
        executeSql( "alter table trackedentityattributevalue alter column value type varchar(50000)" );

        executeSql( "update trackedentitydatavalue set providedelsewhere=false where providedelsewhere is null" );

        executeSql( "update datavalueaudit set attributeoptioncomboid = " + defaultOptionComboId + " where attributeoptioncomboid is null" );
        executeSql( "alter table datavalueaudit alter column attributeoptioncomboid set not null;" );

        executeSql( "update dataelementcategoryoption set shortname = substring(name,0,50) where shortname is null" );

        // AttributeValue
        executeSql( "UPDATE attributevalue SET created=now() WHERE created IS NULL" );
        executeSql( "UPDATE attributevalue SET lastupdated=now() WHERE lastupdated IS NULL" );
        executeSql( "ALTER TABLE attributevalue ALTER value TYPE text" );
        executeSql( "DELETE FROM attributevalue where value IS NULL or value=''" );

        executeSql( "update dashboarditem set shape = 'normal' where shape is null" );

        executeSql( "update categoryoptioncombo set ignoreapproval = false where ignoreapproval is null" );

        executeSql( "alter table version alter column versionkey set not null" );
        executeSql( "alter table version add constraint version_versionkey_key unique(versionkey)" );

        // Cacheable
        executeSql( "UPDATE report set cachestrategy='RESPECT_SYSTEM_SETTING' where cachestrategy is null" );
        executeSql( "UPDATE sqlview set cachestrategy='RESPECT_SYSTEM_SETTING' where cachestrategy is null" );

        executeSql( "update categorycombo set datadimensiontype = 'DISAGGREGATION' where dimensiontype = 'disaggregation'" );
        executeSql( "update categorycombo set datadimensiontype = 'ATTRIBUTE' where dimensiontype = 'attribute'" );
        executeSql( "update categorycombo set datadimensiontype = 'DISAGGREGATION' where datadimensiontype is null" );
        executeSql( "alter table categorycombo drop column dimensiontype" );
        executeSql( "update dataelementcategory set datadimensiontype = 'DISAGGREGATION' where dimensiontype = 'disaggregation'" );
        executeSql( "update dataelementcategory set datadimensiontype = 'ATTRIBUTE' where dimensiontype = 'attribute'" );
        executeSql( "update dataelementcategory set datadimensiontype = 'DISAGGREGATION' where datadimensiontype is null" );
        executeSql( "alter table dataelementcategory drop column dimensiontype" );

        executeSql( "update categoryoptiongroupset set datadimensiontype = 'ATTRIBUTE' where datadimensiontype is null" );
        executeSql( "update categoryoptiongroup set datadimensiontype = 'ATTRIBUTE' where datadimensiontype is null" );

        executeSql( "update reporttable set completedonly = false where completedonly is null" );
        executeSql( "update chart set completedonly = false where completedonly is null" );
        executeSql( "update eventreport set completedonly = false where completedonly is null" );
        executeSql( "update eventchart set completedonly = false where completedonly is null" );

        executeSql( "update program set enrollmentdatelabel = dateofenrollmentdescription where enrollmentdatelabel is null" );
        executeSql( "update program set incidentdatelabel = dateofincidentdescription where incidentdatelabel is null" );
        executeSql( "update programinstance set incidentdate = dateofincident where incidentdate is null" );
        executeSql( "alter table programinstance alter column incidentdate drop not null" );
        executeSql( "alter table program drop column dateofenrollmentdescription" );
        executeSql( "alter table program drop column dateofincidentdescription" );
        executeSql( "alter table programinstance drop column dateofincident" );

        executeSql( "update programstage set reportdatetouse = 'indicentDate' where reportdatetouse='dateOfIncident'" );
        executeSql( "update programstage set repeatable = irregular where repeatable is null" );
        executeSql( "update programstage set repeatable = false where repeatable is null" );
        executeSql( "alter table programstage drop column reportdatedescription" );
        executeSql( "alter table programstage drop column irregular" );

        executeSql( "update smscodes set compulsory = false where compulsory is null" );

        executeSql( "alter table programmessage drop column storecopy" );

        executeSql( "alter table programindicator drop column missingvaluereplacement" );

        executeSql( "update keyjsonvalue set namespacekey = key where namespacekey is null" );
        executeSql( "alter table keyjsonvalue alter column namespacekey set not null" );
        executeSql( "alter table keyjsonvalue drop column key" );
        executeSql( "alter table trackedentityattributevalue drop column encrypted_value" );
        executeSql( "alter table predictor drop column predictororglevels" );

        // Remove data mart
        executeSql( "drop table aggregateddatasetcompleteness" );
        executeSql( "drop table aggregateddatasetcompleteness_temp" );
        executeSql( "drop table aggregateddatavalue" );
        executeSql( "drop table aggregateddatavalue_temp" );
        executeSql( "drop table aggregatedindicatorvalue" );
        executeSql( "drop table aggregatedindicatorvalue_temp" );
        executeSql( "drop table aggregatedorgunitdatasetcompleteness" );
        executeSql( "drop table aggregatedorgunitdatasetcompleteness_temp" );
        executeSql( "drop table aggregatedorgunitdatavalue" );
        executeSql( "drop table aggregatedorgunitdatavalue_temp" );
        executeSql( "drop table aggregatedorgunitindicatorvalue" );
        executeSql( "drop table aggregatedorgunitindicatorvalue_temp" );

        executeSql( "alter table trackedentitydatavalue alter column storedby TYPE character varying(255)" );
        executeSql( "alter table datavalue alter column storedby TYPE character varying(255)" );

        executeSql( "alter table datastatisticsevent alter column eventtype type character varying" );
        executeSql( "alter table orgunitlevel drop constraint orgunitlevel_name_key" );

        executeSql( "update interpretation set likes = 0 where likes is null" );

        executeSql( "update chart set regressiontype = 'NONE' where regression is false or regression is null" );
        executeSql( "update chart set regressiontype = 'LINEAR' where regression is true" );
        executeSql( "alter table chart alter column regressiontype set not null" );
        executeSql( "alter table chart drop column regression" );

        executeSql( "update eventchart set regressiontype = 'NONE' where regression is false or regression is null" );
        executeSql( "update eventchart set regressiontype = 'LINEAR' where regression is true" );
        executeSql( "alter table eventchart alter column regressiontype set not null" );
        executeSql( "alter table eventchart drop column regression" );

        executeSql( "alter table validationrule drop column ruletype" );
        executeSql( "alter table validationrule drop column skiptestexpressionid" );
        executeSql( "alter table validationrule drop column organisationunitlevel" );
        executeSql( "alter table validationrule drop column sequentialsamplecount" );
        executeSql( "alter table validationrule drop column annualsamplecount" );
        executeSql( "alter table validationrule drop column sequentialskipcount" );

        // remove TrackedEntityAttributeGroup
        executeSql( "alter table trackedentityattribute drop column trackedentityattributegroupid" );
        executeSql( "ALTER TABLE trackedentityattribute DROP CONSTRAINT fk_trackedentityattribute_attributegroupid" );

        // remove id object parts from embedded objects
        upgradeEmbeddedObject( "datainputperiod" );
        upgradeEmbeddedObject( "datasetelement" );

        updateEnums();

        upgradeDataValueSoftDelete();

        initOauth2();

        upgradeDataValuesWithAttributeOptionCombo();
        upgradeCompleteDataSetRegistrationsWithAttributeOptionCombo();
        upgradeMapViewsToAnalyticalObject();
        upgradeTranslations();

        upgradeToDataApprovalWorkflows();
        executeSql( "alter table dataapproval alter column workflowid set not null" );
        executeSql( "alter table dataapproval add constraint dataapproval_unique_key unique (dataapprovallevelid,workflowid,periodid,organisationunitid,attributeoptioncomboid)" );

        upgradeImplicitAverageMonitoringRules();
        updateOptions();

        upgradeAggregationType( "reporttable" );
        upgradeAggregationType( "chart" );

        updateRelativePeriods();
        updateNameColumnLengths();

        upgradeMapViewsToColumns();
        upgradeDataDimensionsToEmbeddedOperand();
        upgradeDataDimensionItemsToReportingRateMetric();
        upgradeDataDimensionItemToEmbeddedProgramAttribute();
        upgradeDataDimensionItemToEmbeddedProgramDataElement();

        updateObjectTranslation();
        upgradeDataSetElements();

        removeOutdatedTranslationProperties();

        updateLegendRelationship();
        updateHideEmptyRows();

        executeSql( "update programindicator set analyticstype = 'EVENT' where analyticstype is null" );
        executeSql( "alter table programindicator alter column analyticstype set not null" );

        //TODO: remove - not needed in release 2.26.
        executeSql( "update programindicator set analyticstype = programindicatoranalyticstype" );
        executeSql( "alter table programindicator drop programindicatoranalyticstype" );
       
        updateDimensionFilterToText();

        log.info( "Tables updated" );
    }

    private void upgradeEmbeddedObject( String table )
    {
        executeSql( "ALTER TABLE " + table + " DROP COLUMN uid" );
        executeSql( "ALTER TABLE " + table + " DROP COLUMN created" );
        executeSql( "ALTER TABLE " + table + " DROP COLUMN lastupdated" );
        executeSql( "ALTER TABLE " + table + " DROP COLUMN code" );
    }

    private void removeOutdatedTranslationProperties()
    {
        executeSql( "delete from indicatortranslations where objecttranslationid in (select objecttranslationid from objecttranslation where property in ('numeratorDescription', 'denominatorDescription'))" );
        executeSql( "delete from objecttranslation where property in ('numeratorDescription', 'denominatorDescription')" );
    }

    private void upgradeDataValueSoftDelete()
    {
        executeSql( "update datavalue set deleted = false where deleted is null" );
        executeSql( "alter table datavalue alter column deleted set not null" );
        executeSql( "create index in_datavalue_deleted on datavalue(deleted)" );
    }

    private void initOauth2()
    {
        // OAuth2
        executeSql( "CREATE TABLE oauth_code (" +
            "  code VARCHAR(256), authentication " + statementBuilder.getLongVarBinaryType() +
            ")" );

        executeSql( "CREATE TABLE oauth_access_token (" +
            "  token_id VARCHAR(256)," +
            "  token " + statementBuilder.getLongVarBinaryType() + "," +
            "  authentication_id VARCHAR(256) PRIMARY KEY," +
            "  user_name VARCHAR(256)," +
            "  client_id VARCHAR(256)," +
            "  authentication " + statementBuilder.getLongVarBinaryType() + "," +
            "  refresh_token VARCHAR(256)" +
            ")" );

        executeSql( "CREATE TABLE oauth_refresh_token (" +
            "  token_id VARCHAR(256)," +
            "  token " + statementBuilder.getLongVarBinaryType() + "," +
            "  authentication " + statementBuilder.getLongVarBinaryType() +
            ")" );
    }

    private void updateEnums()
    {
        executeSql( "update report set type='JASPER_REPORT_TABLE' where type='jasperReportTable'" );
        executeSql( "update report set type='JASPER_JDBC' where type='jasperJdbc'" );
        executeSql( "update report set type='HTML' where type='html'" );

        executeSql( "update dashboarditem set shape='NORMAL' where shape ='normal'" );
        executeSql( "update dashboarditem set shape='DOUBLE_WIDTH' where shape ='double_width'" );
        executeSql( "update dashboarditem set shape='FULL_WIDTH' where shape ='full_width'" );

        executeSql( "update reporttable set displaydensity='COMFORTABLE' where displaydensity='comfortable'" );
        executeSql( "update reporttable set displaydensity='NORMAL' where displaydensity='normal'" );
        executeSql( "update reporttable set displaydensity='COMPACT' where displaydensity='compact'" );

        executeSql( "update eventreport set displaydensity='COMFORTABLE' where displaydensity='comfortable'" );
        executeSql( "update eventreport set displaydensity='NORMAL' where displaydensity='normal'" );
        executeSql( "update eventreport set displaydensity='COMPACT' where displaydensity='compact'" );

        executeSql( "update reporttable set fontsize='LARGE' where fontsize='large'" );
        executeSql( "update reporttable set fontsize='NORMAL' where fontsize='normal'" );
        executeSql( "update reporttable set fontsize='SMALL' where fontsize='small'" );

        executeSql( "update eventreport set fontsize='LARGE' where fontsize='large'" );
        executeSql( "update eventreport set fontsize='NORMAL' where fontsize='normal'" );
        executeSql( "update eventreport set fontsize='SMALL' where fontsize='small'" );

        executeSql( "update reporttable set digitgroupseparator='NONE' where digitgroupseparator='none'" );
        executeSql( "update reporttable set digitgroupseparator='SPACE' where digitgroupseparator='space'" );
        executeSql( "update reporttable set digitgroupseparator='COMMA' where digitgroupseparator='comma'" );

        executeSql( "update eventreport set digitgroupseparator='NONE' where digitgroupseparator='none'" );
        executeSql( "update eventreport set digitgroupseparator='SPACE' where digitgroupseparator='space'" );
        executeSql( "update eventreport set digitgroupseparator='COMMA' where digitgroupseparator='comma'" );

        executeSql( "update eventreport set datatype='AGGREGATED_VALUES' where datatype='aggregated_values'" );
        executeSql( "update eventreport set datatype='EVENTS' where datatype='individual_cases'" );

        executeSql( "update chart set type='COLUMN' where type='column'" );
        executeSql( "update chart set type='STACKED_COLUMN' where type='stackedcolumn'" );
        executeSql( "update chart set type='STACKED_COLUMN' where type='stackedColumn'" );
        executeSql( "update chart set type='BAR' where type='bar'" );
        executeSql( "update chart set type='STACKED_BAR' where type='stackedbar'" );
        executeSql( "update chart set type='STACKED_BAR' where type='stackedBar'" );
        executeSql( "update chart set type='LINE' where type='line'" );
        executeSql( "update chart set type='AREA' where type='area'" );
        executeSql( "update chart set type='PIE' where type='pie'" );
        executeSql( "update chart set type='RADAR' where type='radar'" );
        executeSql( "update chart set type='GAUGE' where type='gauge'" );

        executeSql( "update eventchart set type='COLUMN' where type='column'" );
        executeSql( "update eventchart set type='STACKED_COLUMN' where type='stackedcolumn'" );
        executeSql( "update eventchart set type='STACKED_COLUMN' where type='stackedColumn'" );
        executeSql( "update eventchart set type='BAR' where type='bar'" );
        executeSql( "update eventchart set type='STACKED_BAR' where type='stackedbar'" );
        executeSql( "update eventchart set type='STACKED_BAR' where type='stackedBar'" );
        executeSql( "update eventchart set type='LINE' where type='line'" );
        executeSql( "update eventchart set type='AREA' where type='area'" );
        executeSql( "update eventchart set type='PIE' where type='pie'" );
        executeSql( "update eventchart set type='RADAR' where type='radar'" );
        executeSql( "update eventchart set type='GAUGE' where type='gauge'" );

        executeSql( "update dataentryform set style='COMFORTABLE' where style='comfortable'" );
        executeSql( "update dataentryform set style='NORMAL' where style='regular'" );
        executeSql( "update dataentryform set style='COMPACT' where style='compact'" );
        executeSql( "update dataentryform set style='NONE' where style='none'" );
    }

    private void upgradeDataSetElements()
    {
        String autoIncr = statementBuilder.getAutoIncrementValue();
        String uid = statementBuilder.getUid();

        String insertSql =
            "insert into datasetelement(datasetelementid,uid,datasetid,dataelementid,created,lastupdated) " +
                "select " + autoIncr + "  as datasetelementid, " +
                uid + " as uid, " +
                "dsm.datasetid as datasetid, " +
                "dsm.dataelementid as dataelementid, " +
                "now() as created, " +
                "now() as lastupdated " +
                "from datasetmembers dsm; " +
                "drop table datasetmembers; ";

        executeSql( insertSql );

        executeSql( "alter table datasetelement alter column uid set not null" );
        executeSql( "alter table datasetelement alter column created set not null" );
        executeSql( "alter table datasetelement alter column lastupdated set not null" );
        executeSql( "alter table datasetelement alter column datasetid drop not null" );
    }

    private void upgradeAggregationType( String table )
    {
        executeSql( "update " + table + " set aggregationtype='SUM' where aggregationtype='sum'" );
        executeSql( "update " + table + " set aggregationtype='COUNT' where aggregationtype='count'" );
        executeSql( "update " + table + " set aggregationtype='STDDEV' where aggregationtype='stddev'" );
        executeSql( "update " + table + " set aggregationtype='VARIANCE' where aggregationtype='variance'" );
        executeSql( "update " + table + " set aggregationtype='MIN' where aggregationtype='min'" );
        executeSql( "update " + table + " set aggregationtype='MAX' where aggregationtype='max'" );
        executeSql( "update " + table + " set aggregationtype='DEFAULT' where aggregationtype='default' or aggregationtype is null" );
    }

    private void updateRelativePeriods()
    {
        executeSql( "update relativeperiods set thismonth=reportingmonth" );
        executeSql( "update relativeperiods set thisbimonth=reportingbimonth" );
        executeSql( "update relativeperiods set thisquarter=reportingquarter" );

        executeSql( "update relativeperiods set lastweek = false where lastweek is null" );
        executeSql( "update relativeperiods set weeksthisyear = false where weeksthisyear is null" );
        executeSql( "update relativeperiods set bimonthsthisyear = false where bimonthsthisyear is null" );
        executeSql( "update relativeperiods set last4weeks = false where last4weeks is null" );
        executeSql( "update relativeperiods set last12weeks = false where last12weeks is null" );
        executeSql( "update relativeperiods set last6months = false where last6months is null" );

        executeSql( "update relativeperiods set thismonth = false where thismonth is null" );
        executeSql( "update relativeperiods set thisbimonth = false where thisbimonth is null" );
        executeSql( "update relativeperiods set thisquarter = false where thisquarter is null" );
        executeSql( "update relativeperiods set thissixmonth = false where thissixmonth is null" );
        executeSql( "update relativeperiods set thisweek = false where thisweek is null" );

        executeSql( "update relativeperiods set lastmonth = false where lastmonth is null" );
        executeSql( "update relativeperiods set lastbimonth = false where lastbimonth is null" );
        executeSql( "update relativeperiods set lastquarter = false where lastquarter is null" );
        executeSql( "update relativeperiods set lastsixmonth = false where lastsixmonth is null" );
        executeSql( "update relativeperiods set lastweek = false where lastweek is null" );

        executeSql( "update relativeperiods set thisday = false where thisday is null" );
        executeSql( "update relativeperiods set yesterday = false where yesterday is null" );
        executeSql( "update relativeperiods set last3days = false where last3days is null" );
        executeSql( "update relativeperiods set last7days = false where last7days is null" );
        executeSql( "update relativeperiods set last14days = false where last14days is null" );


        // Set non-null constraint on fields
        executeSql( "alter table relativeperiods alter column thisday set not null" );
        executeSql( "alter table relativeperiods alter column yesterday set not null" );
        executeSql( "alter table relativeperiods alter column last3Days set not null" );
        executeSql( "alter table relativeperiods alter column last7Days set not null" );
        executeSql( "alter table relativeperiods alter column last14Days set not null" );
        executeSql( "alter table relativeperiods alter column thisMonth set not null" );
        executeSql( "alter table relativeperiods alter column lastMonth set not null" );
        executeSql( "alter table relativeperiods alter column thisBimonth set not null" );
        executeSql( "alter table relativeperiods alter column lastBimonth set not null" );
        executeSql( "alter table relativeperiods alter column thisQuarter set not null" );
        executeSql( "alter table relativeperiods alter column lastQuarter set not null" );
        executeSql( "alter table relativeperiods alter column thisSixMonth set not null" );
        executeSql( "alter table relativeperiods alter column lastSixMonth set not null" );
        executeSql( "alter table relativeperiods alter column monthsThisYear set not null" );
        executeSql( "alter table relativeperiods alter column quartersThisYear set not null" );
        executeSql( "alter table relativeperiods alter column thisYear set not null" );
        executeSql( "alter table relativeperiods alter column monthsLastYear set not null" );
        executeSql( "alter table relativeperiods alter column quartersLastYear set not null" );
        executeSql( "alter table relativeperiods alter column lastYear set not null" );
        executeSql( "alter table relativeperiods alter column last5Years set not null" );
        executeSql( "alter table relativeperiods alter column last12Months set not null" );
        executeSql( "alter table relativeperiods alter column last6Months set not null" );
        executeSql( "alter table relativeperiods alter column last3Months set not null" );
        executeSql( "alter table relativeperiods alter column last6BiMonths set not null" );
        executeSql( "alter table relativeperiods alter column last4Quarters set not null" );
        executeSql( "alter table relativeperiods alter column last2SixMonths set not null" );
        executeSql( "alter table relativeperiods alter column thisFinancialYear set not null" );
        executeSql( "alter table relativeperiods alter column lastFinancialYear set not null" );
        executeSql( "alter table relativeperiods alter column last5FinancialYears set not null" );
        executeSql( "alter table relativeperiods alter column thisWeek set not null" );
        executeSql( "alter table relativeperiods alter column lastWeek set not null" );
        executeSql( "alter table relativeperiods alter column last4Weeks set not null" );
        executeSql( "alter table relativeperiods alter column last12Weeks set not null" );
        executeSql( "alter table relativeperiods alter column last52Weeks set not null" );
    }

    private void updateNameColumnLengths()
    {
        List<String> tables = Lists.newArrayList( "user", "usergroup", "organisationunit", "orgunitgroup", "orgunitgroupset",
            "section", "dataset", "sqlview", "dataelement", "dataelementgroup", "dataelementgroupset", "categorycombo",
            "dataelementcategory", "dataelementcategoryoption", "indicator", "indicatorgroup", "indicatorgroupset", "indicatortype",
            "validationrule", "validationrulegroup", "constant", "attribute", "attributegroup",
            "program", "programstage", "programindicator", "trackedentity", "trackedentityattribute" );

        for ( String table : tables )
        {
            executeSql( "alter table " + table + " alter column name type character varying(230)" );
        }
    }

    private void upgradeDataValuesWithAttributeOptionCombo()
    {
        final String sql = statementBuilder.getNumberOfColumnsInPrimaryKey( "datavalue" );

        Integer no = statementManager.getHolder().queryForInteger( sql );

        if ( no >= 5 )
        {
            return; // attributeoptioncomboid already part of pkey
        }

        int optionComboId = getDefaultOptionCombo();

        executeSql( "alter table datavalue drop constraint datavalue_pkey;" );

        executeSql( "alter table datavalue add column attributeoptioncomboid integer;" );
        executeSql( "update datavalue set attributeoptioncomboid = " + optionComboId + " where attributeoptioncomboid is null;" );
        executeSql( "alter table datavalue alter column attributeoptioncomboid set not null;" );
        executeSql( "alter table datavalue add constraint fk_datavalue_attributeoptioncomboid foreign key (attributeoptioncomboid) references categoryoptioncombo (categoryoptioncomboid) match simple;" );
        executeSql( "alter table datavalue add constraint datavalue_pkey primary key(dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid);" );

        log.info( "Data value table upgraded with attributeoptioncomboid column" );
    }

    private void upgradeCompleteDataSetRegistrationsWithAttributeOptionCombo()
    {
        final String sql = statementBuilder.getNumberOfColumnsInPrimaryKey( "completedatasetregistration" );

        Integer no = statementManager.getHolder().queryForInteger( sql );

        if ( no >= 4 )
        {
            return; // attributeoptioncomboid already part of pkey
        }

        int optionComboId = getDefaultOptionCombo();

        executeSql( "alter table completedatasetregistration drop constraint completedatasetregistration_pkey" );
        executeSql( "alter table completedatasetregistration add column attributeoptioncomboid integer;" );
        executeSql( "update completedatasetregistration set attributeoptioncomboid = " + optionComboId
            + " where attributeoptioncomboid is null;" );
        executeSql( "alter table completedatasetregistration alter column attributeoptioncomboid set not null;" );
        executeSql( "alter table completedatasetregistration add constraint fk_completedatasetregistration_attributeoptioncomboid foreign key (attributeoptioncomboid) references categoryoptioncombo (categoryoptioncomboid) match simple;" );
        executeSql( "alter table completedatasetregistration add constraint completedatasetregistration_pkey primary key(datasetid, periodid, sourceid, attributeoptioncomboid);" );

        log.info( "Complete data set registration table upgraded with attributeoptioncomboid column" );
    }

    private void upgradeMapViewsToAnalyticalObject()
    {
        executeSql( "insert into mapview_dataelements ( mapviewid, sort_order, dataelementid ) select mapviewid, 0, dataelementid from mapview where dataelementid is not null" );
        executeSql( "alter table mapview drop column dataelementid" );

        executeSql( "insert into mapview_dataelementoperands ( mapviewid, sort_order, dataelementoperandid ) select mapviewid, 0, dataelementoperandid from mapview where dataelementoperandid is not null" );
        executeSql( "alter table mapview drop column dataelementoperandid" );

        executeSql( "insert into mapview_indicators ( mapviewid, sort_order, indicatorid ) select mapviewid, 0, indicatorid from mapview where indicatorid is not null" );
        executeSql( "alter table mapview drop column indicatorid" );

        executeSql( "insert into mapview_organisationunits ( mapviewid, sort_order, organisationunitid ) select mapviewid, 0, parentorganisationunitid from mapview where parentorganisationunitid is not null" );
        executeSql( "alter table mapview drop column parentorganisationunitid" );

        executeSql( "insert into mapview_periods ( mapviewid, sort_order, periodid ) select mapviewid, 0, periodid from mapview where periodid is not null" );
        executeSql( "alter table mapview drop column periodid" );

        executeSql( "insert into mapview_orgunitlevels ( mapviewid, sort_order, orgunitlevel ) select m.mapviewid, 0, o.level "
            + "from mapview m join orgunitlevel o on (m.organisationunitlevelid=o.orgunitlevelid) where m.organisationunitlevelid is not null" );
        executeSql( "alter table mapview drop column organisationunitlevelid" );

        executeSql( "alter table mapview drop column dataelementgroupid" );
        executeSql( "alter table mapview drop column indicatorgroupid" );

        executeSql( "update mapview set userorganisationunit = false where userorganisationunit is null" );
        executeSql( "update mapview set userorganisationunitchildren = false where userorganisationunitchildren is null" );
        executeSql( "update mapview set userorganisationunitgrandchildren = false where userorganisationunitgrandchildren is null" );
    }

    private void upgradeTranslations()
    {
        final String sql = statementBuilder.getNumberOfColumnsInPrimaryKey( "translation" );

        Integer no = statementManager.getHolder().queryForInteger( sql );

        if ( no == 1 )
        {
            return; // translationid already set as single pkey
        }

        executeSql( statementBuilder.getDropPrimaryKey( "translation" ) );
        executeSql( statementBuilder.getAddPrimaryKeyToExistingTable( "translation", "translationid" ) );
        executeSql( statementBuilder.getDropNotNullConstraint( "translation", "objectid", "integer" ) );
    }

    /**
     * Convert from older releases where dataApproval referenced dataset
     * instead of workflow:
     * <p>
     * For every dataset that has either ("approve data" == true) *or*
     * (existing data approval database records referencing it), a workflow will
     * be created with the same name as the data set. This workflow will be
     * associated with all approval levels in the system and have a period type
     * equal to the data set's period type. If the data set's approvedata ==
     * true, then the data set will be associated with this workflow.
     * If there are existing data approval records that reference this data set,
     * then they will be changed to reference the associated workflow instead.
     */
    private void upgradeToDataApprovalWorkflows()
    {
        if ( executeSql( "update dataset set approvedata = approvedata where datasetid < 0" ) < 0 )
        {
            return; // Already converted because dataset.approvedata no longer exists.
        }

        executeSql( "insert into dataapprovalworkflow ( workflowid, uid, created, lastupdated, name, periodtypeid, userid, publicaccess ) "
            + "select " + statementBuilder.getAutoIncrementValue() + ", " + statementBuilder.getUid() + ", now(), now(), ds.name, ds.periodtypeid, ds.userid, ds.publicaccess "
            + "from (select datasetid from dataset where approvedata = true union select distinct datasetid from dataapproval) as a "
            + "join dataset ds on ds.datasetid = a.datasetid" );

        executeSql( "insert into dataapprovalworkflowlevels (workflowid, dataapprovallevelid) "
            + "select w.workflowid, l.dataapprovallevelid from dataapprovalworkflow w "
            + "cross join dataapprovallevel l" );

        executeSql( "update dataset set workflowid = ( select w.workflowid from dataapprovalworkflow w where w.name = dataset.name)" );
        executeSql( "alter table dataset drop column approvedata cascade" ); // Cascade to SQL Views, if any.

        executeSql( "update dataapproval set workflowid = ( select ds.workflowid from dataset ds where ds.datasetid = dataapproval.datasetid)" );
        executeSql( "alter table dataapproval drop constraint dataapproval_unique_key" );
        executeSql( "alter table dataapproval drop column datasetid cascade" ); // Cascade to SQL Views, if any.

        log.info( "Added any workflows needed for approvble datasets and/or approved data." );
    }

    /**
     * Convert from pre-2.22 releases where the right hand sides of surveillance rules were
     * implicitly averaged.  This just wraps the previous expression in a call to AVG().
     * <p>
     * We use the presence of the lowoutliers column to determine whether we need to make the
     * change.  Just to be extra sure, our rewrite SQL won't rewrite rules which already have
     * references to AVG or STDDEV.
     */
    private void upgradeImplicitAverageMonitoringRules()
    {
        if ( executeSql( "update validationrule set lowoutliers = lowoutliers where validationruleid < 0" ) < 0 )
        {
            return; // Already converted because lowoutlier fields are gone
        }

        // Just to be extra sure, we don't modify any expressions which already contain a call to AVG or STDDEV
        executeSql( "INSERT INTO expressionsampleelement (expressionid, dataelementid) " +
            "SELECT ede.expressionid, ede.dataelementid " +
            "FROM expressiondataelement ede " +
            "JOIN expression e ON e.expressionid = ede.expressionid " +
            "JOIN validationrule v ON v.rightexpressionid = e.expressionid " +
            "WHERE v.ruletype='SURVEILLANCE' " +
            "AND e.expression NOT LIKE '%AVG%' and e.expression NOT LIKE '%STDDEV%';" );

        executeSql( "update expression set expression=" + statementBuilder.concatenate( "'AVG('", "expression", "')'" ) +
            " from validationrule where ruletype='SURVEILLANCE' AND rightexpressionid=expressionid " +
            "AND expression NOT LIKE '%AVG%' and expression NOT LIKE '%STDDEV%';" );

        executeSql( "ALTER TABLE validationrule DROP COLUMN highoutliers" );
        executeSql( "ALTER TABLE validationrule DROP COLUMN lowoutliers" );

        log.info( "Added explicit AVG calls to olid-style implicit average surveillance rules" );
    }

    private List<Integer> getDistinctIdList( String table, String col1 )
    {
        StatementHolder holder = statementManager.getHolder();

        List<Integer> distinctIds = new ArrayList<>();

        try
        {
            Statement statement = holder.getStatement();

            ResultSet resultSet = statement.executeQuery( "SELECT DISTINCT " + col1 + " FROM " + table );

            while ( resultSet.next() )
            {
                distinctIds.add( resultSet.getInt( 1 ) );
            }
        }
        catch ( Exception ex )
        {
            log.error( ex );
        }
        finally
        {
            holder.close();
        }

        return distinctIds;
    }

    private Map<Integer, List<Integer>> getIdMap( String table, String col1, String col2, List<Integer> distinctIds )
    {
        StatementHolder holder = statementManager.getHolder();

        Map<Integer, List<Integer>> idMap = new HashMap<>();

        try
        {
            Statement statement = holder.getStatement();

            for ( Integer distinctId : distinctIds )
            {
                List<Integer> foreignIds = new ArrayList<>();

                ResultSet resultSet = statement.executeQuery( "SELECT " + col2 + " FROM " + table + " WHERE " + col1
                    + "=" + distinctId );

                while ( resultSet.next() )
                {
                    foreignIds.add( resultSet.getInt( 1 ) );
                }

                idMap.put( distinctId, foreignIds );
            }
        }
        catch ( Exception ex )
        {
            log.error( ex );
        }
        finally
        {
            holder.close();
        }

        return idMap;
    }
    
    private void updateHideEmptyRows()
    {
        executeSql( 
            "update chart set hideemptyrowitems = 'NONE' where hideemptyrows is false or hideemptyrows is null; " +
            "update chart set hideemptyrowitems = 'ALL' where hideemptyrows is true; " +
            "alter table chart alter column hideemptyrowitems set not null; " +
            "alter table chart drop column hideemptyrows;" );
        
        executeSql(
            "update eventchart set hideemptyrowitems = 'NONE' where hideemptyrows is false or hideemptyrows is null; " +
            "update eventchart set hideemptyrowitems = 'ALL' where hideemptyrows is true; " +
            "alter table eventchart alter column hideemptyrowitems set not null; " +
            "alter table eventchart drop column hideemptyrows;" );        
    }

    private void updateSortOrder( String table, String col1, String col2 )
    {
        List<Integer> distinctIds = getDistinctIdList( table, col1 );

        log.info( "Got distinct ids: " + distinctIds.size() );

        Map<Integer, List<Integer>> idMap = getIdMap( table, col1, col2, distinctIds );

        log.info( "Got id map: " + idMap.size() );

        for ( Integer distinctId : idMap.keySet() )
        {
            int sortOrder = 1;

            for ( Integer foreignId : idMap.get( distinctId ) )
            {
                String sql = "UPDATE " + table + " SET sort_order=" + sortOrder++ + " WHERE " + col1 + "=" + distinctId
                    + " AND " + col2 + "=" + foreignId;

                int count = executeSql( sql );

                log.info( "Executed: " + count + " - " + sql );
            }
        }
    }

    private Integer getDefaultOptionCombo()
    {
        String sql = "select coc.categoryoptioncomboid from categoryoptioncombo coc "
            + "inner join categorycombos_optioncombos cco on coc.categoryoptioncomboid=cco.categoryoptioncomboid "
            + "inner join categorycombo cc on cco.categorycomboid=cc.categorycomboid " + "where cc.name='default';";

        return statementManager.getHolder().queryForInteger( sql );
    }

    private Integer getDefaultCategoryCombo()
    {
        String sql = "select categorycomboid from categorycombo where name = 'default'";

        return statementManager.getHolder().queryForInteger( sql );
    }

    private void updateOptions()
    {
        String sql = "insert into optionvalue(optionvalueid, code, name, optionsetid, sort_order) "
            + "select " + statementBuilder.getAutoIncrementValue() + ", optionvalue, optionvalue, optionsetid, ( sort_order + 1 ) "
            + "from optionsetmembers";

        int result = executeSql( sql );

        if ( result != -1 )
        {
            executeSql( "drop table optionsetmembers" );
        }
    }

    /**
     * Upgrades existing map views to use mapview_columns for multiple column
     * dimensions.
     */
    private void upgradeMapViewsToColumns()
    {
        String sql =
            "insert into mapview_columns(mapviewid, sort_order, dimension) " +
                "select mapviewid, 0, 'dx' " +
                "from mapview mv " +
                "where not exists (" +
                "select mc.mapviewid " +
                "from mapview_columns mc " +
                "where mv.mapviewid = mc.mapviewid)";

        executeSql( sql );
    }

    /**
     * Upgrades data dimension items to use embedded data element operands.
     */
    private void upgradeDataDimensionsToEmbeddedOperand()
    {
        String sql =
            "update datadimensionitem di " +
            "set dataelementoperand_dataelementid = ( " +
                "select op.dataelementid " +
                "from dataelementoperand op " +
                "where di.dataelementoperandid=op.dataelementoperandid " +
            "), " +
            "dataelementoperand_categoryoptioncomboid = ( " +
                "select op.categoryoptioncomboid " +
                "from dataelementoperand op " +
                "where di.dataelementoperandid=op.dataelementoperandid " +
            ") " +
            "where di.dataelementoperandid is not null; " +
            "alter table datadimensionitem drop column dataelementoperandid;";
        
        executeSql( sql );
    }
    
    /**
     * Upgrade data dimension items for legacy data sets to use REPORTING_RATE
     * as metric.
     */
    private void upgradeDataDimensionItemsToReportingRateMetric()
    {
        String sql = "update datadimensionitem " +
            "set metric='REPORTING_RATE' " +
            "where datasetid is not null " +
            "and metric is null;";

        executeSql( sql );
    }
    
    /**
     * Upgrades data dimension items to use embedded 
     * ProgramTrackedEntityAttributeDimensionItem class.
     */
    private void upgradeDataDimensionItemToEmbeddedProgramAttribute()
    {
        String sql =
            "update datadimensionitem di " +
            "set programattribute_programid = (select programid from program_attributes where programtrackedentityattributeid=di.programattributeid), " +
                "programattribute_attributeid = (select trackedentityattributeid from program_attributes where programtrackedentityattributeid=di.programattributeid) " +
            "where programattributeid is not null " +
            "and (programattribute_programid is null and programattribute_attributeid is null); " +
            "alter table datadimensionitem drop column programattributeid;";
        
        executeSql( sql );
    }

    /**
     * Upgrades data dimension items to use embedded 
     * ProgramDataElementDimensionItem class.
     */
    private void upgradeDataDimensionItemToEmbeddedProgramDataElement()
    {
        String sql =
            "update datadimensionitem di " +
            "set programdataelement_programid = (select programid from programdataelement where programdataelementid=di.programdataelementid), " +
                "programdataelement_dataelementid = (select dataelementid from programdataelement where programdataelementid=di.programdataelementid) " +
            "where di.programdataelementid is not null " +
            "and (programdataelement_programid is null and programdataelement_dataelementid is null); " +
            "alter table datadimensionitem drop column programdataelementid; " +
            "drop table programdataelementtranslations; " +
            "drop table programdataelement;"; // Remove if program data element is to be reintroduced
        
        executeSql( sql );
    }
    
    private int executeSql( String sql )
    {
        try
        {
            // TODO use jdbcTemplate

            return statementManager.getHolder().executeUpdate( sql );
        }
        catch ( Exception ex )
        {
            log.debug( ex );

            return -1;
        }
    }

    private void addTranslationTable( List<Map<String, String>> listTables,
        String className, String translationTable, String objectTable, String objectId )
    {
        Map<String, String> mapTables = new HashMap<>();
        mapTables.put( "className", className );
        mapTables.put( "translationTable", translationTable );
        mapTables.put( "objectTable", objectTable );
        mapTables.put( "objectId", objectId );
        listTables.add( mapTables );
    }

    private void updateObjectTranslation()
    {
        List<Map<String, String>> listTables = new ArrayList<>();

        addTranslationTable( listTables, "DataElement", "dataelementtranslations", "dataelement", "dataelementid" );
        addTranslationTable( listTables, "DataElementCategory", "dataelementcategorytranslations", "dataelementcategory", "categoryid" );
        addTranslationTable( listTables, "Attribute", "attributetranslations", "attribute", "attributeid" );
        addTranslationTable( listTables, "Indicator", "indicatortranslations", "indicator", "indicatorid" );
        addTranslationTable( listTables, "OrganisationUnit", "organisationUnittranslations", "organisationunit", "organisationunitid" );
        addTranslationTable( listTables, "DataElementCategoryCombo", "categorycombotranslations", "categorycombo", "categorycomboid" );
        addTranslationTable( listTables, "OrganisationUnit", "organisationUnittranslations", "organisationunit", "organisationunitid" );
        addTranslationTable( listTables, "DataElementGroup", "dataelementgrouptranslations", "dataelementgroup", "dataelementgroupid" );
        addTranslationTable( listTables, "DataSet", "datasettranslations", "dataset", "datasetid" );
        addTranslationTable( listTables, "IndicatorType", "indicatortypetranslations", "indicatortype", "indicatortypeid" );
        addTranslationTable( listTables, "Section", "datasetsectiontranslations", "section", "sectionid" );
        addTranslationTable( listTables, "Chart", "charttranslations", "chart", "chartid" );
        addTranslationTable( listTables, "Color", "colortranslations", "color", "colorid" );
        addTranslationTable( listTables, "ColorSet", "colorsettranslations", "colorset", "colorsetid" );
        addTranslationTable( listTables, "Constant", "constanttranslations", "constant", "constantid" );
        addTranslationTable( listTables, "Dashboard", "dashboardtranslations", "dashboard", "dashboardid" );
        addTranslationTable( listTables, "DashboardItem", "dashboarditemtranslations", "dashboarditemid", "dashboarditemid" );
        addTranslationTable( listTables, "DataApprovalLevel", "dataapprovalleveltranslations", "dataapprovallevel", "dataapprovallevelid" );
        addTranslationTable( listTables, "DataApprovalWorkflow", "dataapprovalworkflowtranslations", "dataapprovalworkflow", "workflowid" );
        addTranslationTable( listTables, "CategoryOptionGroup", "categoryoptiongrouptranslations", "categoryoptiongroup", "categoryoptiongroupid" );
        addTranslationTable( listTables, "CategoryOptionGroupSet", "categoryoptiongroupsettranslations", "categoryoptiongroupset", "categoryoptiongroupsetid" );
        addTranslationTable( listTables, "DataElementCategoryOption", "categoryoptiontranslations", "dataelementcategoryoption", "categoryoptionid" );
        addTranslationTable( listTables, "DataElementCategoryOptionCombo", "categoryoptioncombotranslations", "categoryoptioncombo", "categoryoptioncomboid" );
        addTranslationTable( listTables, "DataElementGroupSet", "dataelementgroupsettranslations", "dataelementgroupset", "dataelementgroupsetid" );
        addTranslationTable( listTables, "DataElementOperand", "dataelementoperandtranslations", "dataelementoperand", "dataelementoperandid" );
        addTranslationTable( listTables, "DataEntryForm", "dataentryformtranslations", "dataentryform", "dataentryformid" );
        addTranslationTable( listTables, "DataStatistics", "statisticstranslations", "datastatistics", "statisticsid" );
        addTranslationTable( listTables, "Document", "documenttranslations", "document", "documentid" );
        addTranslationTable( listTables, "EventChart", "eventcharttranslations", "eventchart", "eventchartid" );
        addTranslationTable( listTables, "EventReport", "eventreporttranslations", "eventreport", "eventreportid" );
        addTranslationTable( listTables, "IndicatorGroup", "indicatorgrouptranslations", "indicatorgroup", "indicatorgroupid" );
        addTranslationTable( listTables, "IndicatorGroupSet", "indicatorgroupsettranslations", "indicatorgroupset", "indicatorgroupsetid" );
        addTranslationTable( listTables, "Interpretation", "interpretationtranslations", "interpretation", "interpretationid" );
        addTranslationTable( listTables, "InterpretationComment", "interpretationcommenttranslations", "interpretationcomment", "interpretationcommentid" );
        addTranslationTable( listTables, "Legend", "maplegendtranslations", "maplegend", "maplegendid" );
        addTranslationTable( listTables, "LegendSet", "maplegendsettranslations", "maplegendset", "maplegendsetid" );
        addTranslationTable( listTables, "Map", "maptranslations", "map", "mapid" );
        addTranslationTable( listTables, "MapLayer", "maplayertranslations", "maplayer", "maplayerid" );
        addTranslationTable( listTables, "MapView", "mapviewtranslations", "mapview", "mapviewid" );
        addTranslationTable( listTables, "Message", "messagetranslations", "message", "messageid" );
        addTranslationTable( listTables, "MessageConversation", "messageconversationtranslations", "messageconversation", "messageconversationid" );
        addTranslationTable( listTables, "Option", "optionvaluetranslations", "optionvalue", "optionvalueid" );
        addTranslationTable( listTables, "OptionSet", "optionsettranslations", "optionset", "optionsetid" );
        addTranslationTable( listTables, "OrganisationUnit", "organisationunittranslations", "organisationunit", "organisationunitid" );
        addTranslationTable( listTables, "OrganisationUnitGroup", "orgunitgrouptranslations", "orgunitgroup", "orgunitgroupid" );
        addTranslationTable( listTables, "OrganisationUnitGroupSet", "orgunitgroupsettranslations", "orgunitgroupset", "orgunitgroupsetid" );
        addTranslationTable( listTables, "OrganisationUnitLevel", "orgunitleveltranslations", "orgunitlevel", "orgunitlevelid" );
        addTranslationTable( listTables, "Period", "periodtranslations", "period", "periodid" );
        addTranslationTable( listTables, "Program", "programtranslations", "program", "programid" );
        addTranslationTable( listTables, "ProgramDataElement", "programdataelementtranslations", "programdataelement", "programdataelementid" );
        addTranslationTable( listTables, "ProgramIndicator", "programindicatortranslations", "programindicator", "programindicatorid" );
        addTranslationTable( listTables, "ProgramInstance", "programinstancetranslations", "programinstance", "programinstanceid" );
        addTranslationTable( listTables, "ProgramMessage", "programmessagetranslations", "programmessage", "id" );
        addTranslationTable( listTables, "ProgramStage", "programstagetranslations", "programstage", "programstageid" );
        addTranslationTable( listTables, "ProgramStageDataElement", "programstagedataelementtranslations", "programstagedataelement", "programstagedataelementid" );
        addTranslationTable( listTables, "ProgramStageInstance", "programstageinstancetranslations", "programstageinstance", "programstageinstanceid" );
        addTranslationTable( listTables, "ProgramStageSection", "programstagesectiontranslations", "programstagesection", "programstagesectionid" );
        addTranslationTable( listTables, "ProgramTrackedEntityAttribute", "programattributestranslations", "programtrackedentityattribute", "programtrackedentityattributeid" );
        addTranslationTable( listTables, "ProgramRule", "programruletranslations", "programrule", "programruleid" );
        addTranslationTable( listTables, "ProgramRuleAction", "programruleactiontranslations", "programruleaction", "programruleactionid" );
        addTranslationTable( listTables, "ProgramRuleVariable", "programrulevariabletranslations", "programrulevariable", "programrulevariableid" );
        addTranslationTable( listTables, "RelationshipType", "relationshiptypetranslations", "relationshiptype", "relationshiptypeid" );
        addTranslationTable( listTables, "Report", "reporttranslations", "report", "reportid" );
        addTranslationTable( listTables, "ReportTable", "reporttabletranslations", "reporttable", "reporttableid" );
        addTranslationTable( listTables, "TrackedEntity", "trackedentitytranslations", "trackedentity", "trackedentityid" );
        addTranslationTable( listTables, "TrackedEntityAttribute", "trackedentityattributetranslations", "trackedentityattribute", "trackedentityattributeid" );
        addTranslationTable( listTables, "TrackedEntityInstance", "trackedentityinstancetranslations", "trackedentityinstance", "trackedentityinstanceid" );
        addTranslationTable( listTables, "User", "userinfotranslations", "userinfo", "userinfoid" );
        addTranslationTable( listTables, "UserAuthorityGroup", "userroletranslations", "userrole", "userroleid" );
        addTranslationTable( listTables, "UserCredentials", "usertranslations", "users", "userid" );
        addTranslationTable( listTables, "UserGroup", "usergrouptranslations", "usergroup", "usergroupid" );
        addTranslationTable( listTables, "ValidationCriteria", "validationcriteriatranslations", "validationcriteria", "validationcriteriaid" );
        addTranslationTable( listTables, "ValidationRule", "validationruletranslations", "validationrule", "validationruleid" );
        addTranslationTable( listTables, "ValidationRuleGroup", "validationrulegrouptranslations", "validationrulegroup", "validationrulegroupid" );

        executeSql( "alter table translation add column objectid integer;" );

        String sql;

        for ( Map<String, String> table : listTables )
        {
            sql =
                " insert into objecttranslation ( objecttranslationid, locale , property , value )  " +
                    " select t.translationid, t.locale,  " +
                    " case when t.objectproperty = 'shortName' then 'SHORT_NAME' " +
                    " when t.objectproperty = 'formName' then 'FORM_NAME'   " +
                    " when t.objectproperty = 'name' then 'NAME'  " +
                    " when t.objectproperty = 'description' then'DESCRIPTION'" +
                    " else t.objectproperty " +
                    " end ," +
                    " t.value " +
                    " from  translation as t " +
                    " where t.objectclass = '" + table.get( "className" ) + "'" +
                    " and t.objectproperty is not null " +
                    " and t.locale is not null " +
                    " and t.value is not null " +
                    " and not exists ( select 1 from objecttranslation where objecttranslationid = t.translationid )  " +
                    " and ( " +
                    " exists ( select 1 from " + table.get( "objectTable" ) + "  where " + table.get( "objectId" ) + " = t.objectid )  " +
                    " or  exists ( select 1 from " + table.get( "objectTable" ) + " where uid  = t.objectuid ) " +
                    " ) ;";

            executeSql( sql );

            sql =
                " insert into " + table.get( "translationTable" ) + " ( " + table.get( "objectId" ) + ", objecttranslationid ) " +
                    " select " +
                    " case when t.objectid is not null then t.objectid " +
                    " else ( select " + table.get( "objectId" ) + " from " + table.get( "objectTable" ) + " where uid = t.objectuid ) " +
                    " end," +
                    " o.objecttranslationid  " +
                    " from objecttranslation o inner join translation t on o.objecttranslationid = t.translationid and t.objectclass = '" + table.get( "className" ) + "'" +
                    " and not exists ( select 1 from " + table.get( "translationTable" ) + " where objecttranslationid = o.objecttranslationid) ;";

            executeSql( sql );

        }
    }

    private void updateLegendRelationship()
    {
        String sql = "update maplegend l set maplegendsetid = (select legendsetid from maplegendsetmaplegend m where m.maplegendid = l.maplegendid);";
        executeSql( sql );

        sql = " drop table maplegendsetmaplegend";
        executeSql( sql );
    }
    
    private void updateDimensionFilterToText()
    {
        executeSql( "alter table trackedentityattributedimension alter column \"filter\" type text;" );
        executeSql( "alter table trackedentitydataelementdimension alter column \"filter\" type text;" );
        executeSql( "alter table trackedentityprogramindicatordimension alter column \"filter\" type text;" );
    }
}
