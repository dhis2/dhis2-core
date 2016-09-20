package org.hisp.dhis.startup;

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

import org.hisp.quick.StatementManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
public class InitTableAlteror
    extends AbstractStartupRoutine
{
    private static final Log log = LogFactory.getLog( InitTableAlteror.class );

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
        executeSql( "update dataelement set domaintype='AGGREGATE' where domaintype='aggregate' or domaintype is null;" );
        executeSql( "update dataelement set domaintype='TRACKER' where domaintype='patient';" );
        executeSql( "update users set invitation = false where invitation is null" );
        executeSql( "update users set selfregistered = false where selfregistered is null" );
        executeSql( "update users set externalauth = false where externalauth is null" );
        executeSql( "update users set disabled = false where disabled is null" );
        executeSql( "alter table dataelement alter column domaintype set not null;" );
        executeSql( "alter table programstageinstance alter column  status  type varchar(25);" );
        executeSql( "UPDATE programstageinstance SET status='ACTIVE' WHERE status='0';" );
        executeSql( "UPDATE programstageinstance SET status='COMPLETED' WHERE status='1';" );
        executeSql( "UPDATE programstageinstance SET status='SKIPPED' WHERE status='5';" );        
        executeSql( "ALTER TABLE program DROP COLUMN displayonallorgunit" );

        upgradeProgramStageDataElements();
        updateValueTypes();
        updateAggregationTypes();
        updateFeatureTypes();
        updateValidationRuleEnums();
        updateProgramStatus();
        removeDeprecatedConfigurationColumns();
        updateTimestamps();
        updateCompletedBy();

        executeSql( "ALTER TABLE program ALTER COLUMN \"type\" TYPE varchar(255);" );
        executeSql( "update program set \"type\"='WITH_REGISTRATION' where type='1' or type='2'" );
        executeSql( "update program set \"type\"='WITHOUT_REGISTRATION' where type='3'" );


        // Update userkeyjsonvalue and keyjsonvalue to set new encrypted column to false.

        executeSql( "UPDATE keyjsonvalue SET encrypted = false WHERE encrypted IS NULL" );
        executeSql( "UPDATE userkeyjsonvalue SET encrypted = false WHERE encrypted IS NULL" );

        // Set messages "ticket" properties to non-null values

        executeSql( "UPDATE message SET internal = FALSE WHERE internal IS NULL" );
        executeSql( "UPDATE messageconversation SET priority = 'NONE' WHERE priority IS NULL" );
        executeSql( "UPDATE messageconversation SET status = 'NONE' WHERE status IS NULL" );

        updateMessageConversationMessageCount();
    }

    private void updateMessageConversationMessageCount() {

        Integer nullCounts = statementManager.getHolder().queryForInteger( "SELECT count(*) from messageconversation WHERE messagecount IS NULL" );

        if(nullCounts > 0)
        {
            // Count messages in messageConversations
            executeSql(
                "update messageconversation MC SET messagecount = (SELECT count(MCM.messageconversationid) FROM messageconversation_messages MCM WHERE messageconversationid=MC.messageconversationid) " );
        }
    }

    private void updateCompletedBy()
    {
        executeSql( "update programinstance set completedby=completeduser where completedby is null" );
        executeSql( "update programstageinstance set completedby=completeduser where completedby is null" );

        executeSql( "alter table programinstance drop column completeduser" );
        executeSql( "alter table programstageinstance drop column completeduser" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void removeDeprecatedConfigurationColumns()
    {
        try
        {
            executeSql( "ALTER TABLE configuration DROP COLUMN smptpassword" );
            executeSql( "ALTER TABLE configuration DROP COLUMN smtppassword" );
            executeSql( "ALTER TABLE configuration DROP COLUMN remoteserverurl" );
            executeSql( "ALTER TABLE configuration DROP COLUMN remoteserverusername" );
            executeSql( "ALTER TABLE configuration DROP COLUMN remotepassword" );
            executeSql( "ALTER TABLE configuration DROP COLUMN remoteserverpassword" );

        }
        catch ( Exception ex )
        {
            log.debug( ex );
        }
    }

    private void updateTimestamps()
    {
        executeSql( "update datavalueaudit set created=timestamp where created is null" );
        executeSql( "update datavalueaudit set created=now() where created is null" );
        executeSql( "alter table datavalueaudit drop column timestamp" );

        executeSql( "update trackedentitydatavalue set created=timestamp where created is null" );
        executeSql( "update trackedentitydatavalue set lastupdated=timestamp where lastupdated is null" );
        executeSql( "update trackedentityattributevalue set created=now() where created is null" );
        executeSql( "update trackedentityattributevalue set lastupdated=now() where lastupdated is null" );
        executeSql( "alter table trackedentitydatavalue drop column timestamp" );
    }

    private void updateProgramStatus()
    {
        executeSql( "alter table programinstance alter column status type varchar(50)" );

        executeSql( "update programinstance set status='ACTIVE' where status='0'" );
        executeSql( "update programinstance set status='COMPLETED' where status='1'" );
        executeSql( "update programinstance set status='CANCELLED' where status='2'" );

        executeSql( "update programinstance set status='ACTIVE' where status is null" );
    }

    private void updateValidationRuleEnums()
    {
        executeSql( "alter table validationrule alter column ruletype type varchar(50)" );
        executeSql( "alter table validationrule alter column importance type varchar(50)" );

        executeSql( "update validationrule set ruletype='VALIDATION' where ruletype='validation'" );
        executeSql( "update validationrule set ruletype='SURVEILLANCE' where ruletype='surveillance'" );
        executeSql( "update validationrule set ruletype='VALIDATION' where ruletype='' or ruletype is null" );

        executeSql( "update validationrule set importance='HIGH' where importance='high'" );
        executeSql( "update validationrule set importance='MEDIUM' where importance='medium'" );
        executeSql( "update validationrule set importance='LOW' where importance='low'" );
        executeSql( "update validationrule set importance='MEDIUM' where importance='' or importance is null" );
    }

    private void updateFeatureTypes()
    {
        executeSql( "update organisationunit set featuretype='NONE' where featuretype='None'" );
        executeSql( "update organisationunit set featuretype='MULTI_POLYGON' where featuretype='MultiPolygon'" );
        executeSql( "update organisationunit set featuretype='POLYGON' where featuretype='Polygon'" );
        executeSql( "update organisationunit set featuretype='POINT' where featuretype='Point'" );
        executeSql( "update organisationunit set featuretype='SYMBOL' where featuretype='Symbol'" );
        executeSql( "update organisationunit set featuretype='NONE' where featuretype is null" );
    }

    private void updateAggregationTypes()
    {
        executeSql( "alter table dataelement alter column aggregationtype type varchar(50)" );

        executeSql( "update dataelement set aggregationtype='SUM' where aggregationtype='sum'" );
        executeSql( "update dataelement set aggregationtype='AVERAGE' where aggregationtype='avg'" );
        executeSql( "update dataelement set aggregationtype='AVERAGE_SUM_ORG_UNIT' where aggregationtype='avg_sum_org_unit'" );
        executeSql( "update dataelement set aggregationtype='AVERAGE_SUM_ORG_UNIT' where aggregationtype='average'" );
        executeSql( "update dataelement set aggregationtype='COUNT' where aggregationtype='count'" );
        executeSql( "update dataelement set aggregationtype='STDDEV' where aggregationtype='stddev'" );
        executeSql( "update dataelement set aggregationtype='VARIANCE' where aggregationtype='variance'" );
        executeSql( "update dataelement set aggregationtype='MIN' where aggregationtype='min'" );
        executeSql( "update dataelement set aggregationtype='MAX' where aggregationtype='max'" );
        executeSql( "update dataelement set aggregationtype='NONE' where aggregationtype='none'" );
        executeSql( "update dataelement set aggregationtype='DEFAULT' where aggregationtype='default'" );
        executeSql( "update dataelement set aggregationtype='CUSTOM' where aggregationtype='custom'" );

        executeSql( "update dataelement set aggregationtype='SUM' where aggregationtype is null" );
    }

    private void updateValueTypes()
    {
        executeSql( "alter table dataelement alter column valuetype type varchar(50)" );

        executeSql( "update dataelement set valuetype='NUMBER' where valuetype='int' and numbertype='number'" );
        executeSql( "update dataelement set valuetype='INTEGER' where valuetype='int' and numbertype='int'" );
        executeSql( "update dataelement set valuetype='INTEGER_POSITIVE' where valuetype='int' and numbertype='posInt'" );
        executeSql( "update dataelement set valuetype='INTEGER_POSITIVE' where valuetype='int' and numbertype='positiveNumber'" );
        executeSql( "update dataelement set valuetype='INTEGER_NEGATIVE' where valuetype='int' and numbertype='negInt'" );
        executeSql( "update dataelement set valuetype='INTEGER_NEGATIVE' where valuetype='int' and numbertype='negativeNumber'" );
        executeSql( "update dataelement set valuetype='INTEGER_ZERO_OR_POSITIVE' where valuetype='int' and numbertype='zeroPositiveInt'" );
        executeSql( "update dataelement set valuetype='PERCENTAGE' where valuetype='int' and numbertype='percentage'" );
        executeSql( "update dataelement set valuetype='UNIT_INTERVAL' where valuetype='int' and numbertype='unitInterval'" );
        executeSql( "update dataelement set valuetype='NUMBER' where valuetype='int' and numbertype is null" );

        executeSql( "alter table dataelement drop column numbertype" );

        executeSql( "update dataelement set valuetype='TEXT' where valuetype='string' and texttype='text'" );
        executeSql( "update dataelement set valuetype='LONG_TEXT' where valuetype='string' and texttype='longText'" );
        executeSql( "update dataelement set valuetype='TEXT' where valuetype='string' and texttype is null" );

        executeSql( "alter table dataelement drop column texttype" );

        executeSql( "update dataelement set valuetype='DATE' where valuetype='date'" );
        executeSql( "update dataelement set valuetype='DATETIME' where valuetype='datetime'" );
        executeSql( "update dataelement set valuetype='BOOLEAN' where valuetype='bool'" );
        executeSql( "update dataelement set valuetype='TRUE_ONLY' where valuetype='trueOnly'" );
        executeSql( "update dataelement set valuetype='USERNAME' where valuetype='username'" );

        executeSql( "update dataelement set valuetype='NUMBER' where valuetype is null" );

        executeSql( "update trackedentityattribute set valuetype='TEXT' where valuetype='string'" );
        executeSql( "update trackedentityattribute set valuetype='PHONE_NUMBER' where valuetype='phoneNumber'" );
        executeSql( "update trackedentityattribute set valuetype='EMAIL' where valuetype='email'" );
        executeSql( "update trackedentityattribute set valuetype='NUMBER' where valuetype='number'" );
        executeSql( "update trackedentityattribute set valuetype='NUMBER' where valuetype='int'" );
        executeSql( "update trackedentityattribute set valuetype='LETTER' where valuetype='letter'" );
        executeSql( "update trackedentityattribute set valuetype='BOOLEAN' where valuetype='bool'" );
        executeSql( "update trackedentityattribute set valuetype='TRUE_ONLY' where valuetype='trueOnly'" );
        executeSql( "update trackedentityattribute set valuetype='DATE' where valuetype='date'" );
        executeSql( "update trackedentityattribute set valuetype='TEXT' where valuetype='optionSet'" );
        executeSql( "update trackedentityattribute set valuetype='TEXT' where valuetype='OPTION_SET'" );
        executeSql( "update trackedentityattribute set valuetype='TRACKER_ASSOCIATE' where valuetype='trackerAssociate'" );
        executeSql( "update trackedentityattribute set valuetype='USERNAME' where valuetype='users'" );
        executeSql( "update trackedentityattribute set valuetype='TEXT' where valuetype is null" );

        executeSql( "update optionset set valuetype='TEXT' where valuetype is null" );

        executeSql( "update attribute set valuetype='TEXT' where valuetype='string'" );
        executeSql( "update attribute set valuetype='LONG_TEXT' where valuetype='text'" );
        executeSql( "update attribute set valuetype='BOOLEAN' where valuetype='bool'" );
        executeSql( "update attribute set valuetype='DATE' where valuetype='date'" );
        executeSql( "update attribute set valuetype='NUMBER' where valuetype='number'" );
        executeSql( "update attribute set valuetype='INTEGER' where valuetype='integer'" );
        executeSql( "update attribute set valuetype='INTEGER_POSITIVE' where valuetype='positive_integer'" );
        executeSql( "update attribute set valuetype='INTEGER_NEGATIVE' where valuetype='negative_integer'" );
        executeSql( "update attribute set valuetype='TEXT' where valuetype='option_set'" );
        executeSql( "update attribute set valuetype='TEXT' where valuetype is null" );
    }

    private void upgradeProgramStageDataElements()
    {
        if ( tableExists( "programstage_dataelements" ) )
        {
            String autoIncr = statementBuilder.getAutoIncrementValue();

            String insertSql =
                "insert into programstagedataelement(programstagedataelementid,programstageid,dataelementid,compulsory,allowprovidedelsewhere," +
                    "sort_order,displayinreports,programstagesectionid,allowfuturedate,section_sort_order) " + "select " + autoIncr +
                    ",programstageid,dataelementid,compulsory,allowprovidedelsewhere,sort_order,displayinreports,programstagesectionid,allowfuturedate,section_sort_order from programstage_dataelements";


            executeSql( insertSql );

            String dropSql = "drop table programstage_dataelements";

            executeSql( dropSql );

            log.info( "Upgraded program stage data elements" );
        }
    }

    private int executeSql( String sql )
    {
        try
        {
            return statementManager.getHolder().executeUpdate( sql );
        }
        catch ( Exception ex )
        {
            log.debug( ex );

            return -1;
        }
    }

    private boolean tableExists( String table )
    {
        try
        {
            statementManager.getHolder().queryForInteger( "select 1 from " + table );
            return true;
        }
        catch ( Exception ex )
        {
            return false;
        }
    }
}
