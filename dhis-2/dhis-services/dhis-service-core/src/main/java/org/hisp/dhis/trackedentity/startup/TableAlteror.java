package org.hisp.dhis.trackedentity.startup;

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

import org.amplecode.quick.StatementHolder;
import org.amplecode.quick.StatementManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

/**
 * @author Chau Thu Tran
 */
public class TableAlteror
    extends AbstractStartupRoutine
{
    private static final Log log = LogFactory.getLog( TableAlteror.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private StatementManager statementManager;

    public void setStatementManager( StatementManager statementManager )
    {
        this.statementManager = statementManager;
    }

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    private StatementBuilder statementBuilder;

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void execute()
        throws Exception
    {
        executeSql( "ALTER TABLE relationshiptype RENAME description TO name" );

        executeSql( "ALTER TABLE programstage_dataelements DROP COLUMN showOnReport" );

        executeSql( "ALTER TABLE program DROP COLUMN hidedateofincident" );

        executeSql( "UPDATE program SET type=2 where singleevent=true" );
        executeSql( "UPDATE program SET type=3 where anonymous=true" );
        executeSql( "ALTER TABLE program DROP COLUMN singleevent" );
        executeSql( "ALTER TABLE program DROP COLUMN anonymous" );
        executeSql( "UPDATE program SET type=1 where type is null" );

        executeSql( "DROP TABLE programattributevalue" );
        executeSql( "DROP TABLE programinstance_attributes" );
        executeSql( "DROP TABLE programattributeoption" );
        executeSql( "DROP TABLE programattribute" );

        executeSql( "UPDATE programstage_dataelements SET allowProvidedElsewhere=false WHERE allowProvidedElsewhere is null" );
        executeSql( "ALTER TABLE programstageinstance DROP COLUMN providedbyanotherfacility" );

        executeSql( "ALTER TABLE programstageinstance DROP COLUMN stageInProgram" );

        executeSql( "UPDATE programstage SET reportDateDescription='Report date' WHERE reportDateDescription is null" );
        executeSql( "UPDATE programstage SET autoGenerateEvent=true WHERE autoGenerateEvent is null" );

        executeSql( "update programstage set executiondatelabel = excecutiondatelabel where executiondatelabel is null" );
        executeSql( "alter table programstage drop column excecutiondatelabel" );
        
        executeSql( "UPDATE program SET generatedByEnrollmentDate=false WHERE generatedByEnrollmentDate is null" );

        executeSql( "ALTER TABLE programstage DROP COLUMN stageinprogram" );

        executeSql( "CREATE INDEX index_programinstance ON programinstance( programinstanceid )" );

        executeSql( "ALTER TABLE program DROP COLUMN maxDaysAllowedInputData" );

        executeSql( "UPDATE program SET programstage_dataelements=false WHERE displayInReports is null" );

        executeSql( "ALTER TABLE programvalidation DROP COLUMN leftside" );
        executeSql( "ALTER TABLE programvalidation DROP COLUMN rightside" );
        executeSql( "ALTER TABLE programvalidation DROP COLUMN dateType" );

        executeSql( "UPDATE programstage SET validCompleteOnly=false WHERE validCompleteOnly is null" );
        executeSql( "UPDATE program SET ignoreOverdueEvents=false WHERE ignoreOverdueEvents is null" );

        executeSql( "UPDATE programstage SET displayGenerateEventBox=true WHERE displayGenerateEventBox is null" );

        executeSql( "ALTER TABLE programvalidation RENAME description TO name" );

        executeSql( "UPDATE program SET blockEntryForm=false WHERE blockEntryForm is null" );
        executeSql( "ALTER TABLE dataset DROP CONSTRAINT program_name_key" );
        executeSql( "UPDATE userroleauthorities SET authority='F_PROGRAM_PUBLIC_ADD' WHERE authority='F_PROGRAM_ADD'" );

        executeSql( "UPDATE program SET onlyEnrollOnce='false' WHERE onlyEnrollOnce is null" );
        executeSql( "UPDATE programStage SET captureCoordinates=false WHERE captureCoordinates is null" );

        executeSql( "update prorgam set \"operator\"='times' where \"operator\"='SUM'" );
        executeSql( "update program set remindCompleted=false where remindCompleted is null" );
        executeSql( "UPDATE program SET skipoffline=false where skipoffline is null" );
        executeSql( "UPDATE program SET displayfrontpagelist=false where displayfrontpagelist is null" );
        executeSql( "UPDATE program SET usefirststageduringregistration=false where usefirststageduringregistration is null" );
        executeSql( "UPDATE program SET capturecoordinates=false where capturecoordinates is null" );

        executeSql( "UPDATE programinstance SET followup=false where followup is null" );

        updateProgramInstanceStatus();

        executeSql( "ALTER TABLE program DROP COLUMN disableRegistrationFields" );
        executeSql( "ALTER TABLE program ALTER COLUMN dateofincidentdescription DROP NOT NULL" );
        executeSql( "UPDATE program SET displayOnAllOrgunit=true where displayOnAllOrgunit is null" );
        executeSql( "UPDATE program SET useFormNameDataElement=true where useFormNameDataElement is null" );
        executeSql( "update program set selectEnrollmentDatesInFuture = false where selectEnrollmentDatesInFuture is null" );
        executeSql( "update program set selectIncidentDatesInFuture = false where selectIncidentDatesInFuture is null" );
        executeSql( "update validationcriteria set description = name where description is null or description='' " );
        executeSql( "update programstage set generatedByEnrollmentDate = false where generatedByEnrollmentDate is null " );
        executeSql( "update programstage set blockEntryForm = false where blockEntryForm is null " );
        executeSql( "update programstage set remindCompleted = false where remindCompleted is null " );
        executeSql( "ALTER TABLE program DROP COLUMN generatedByEnrollmentDate" );
        executeSql( "ALTER TABLE program DROP COLUMN blockEntryForm" );
        executeSql( "ALTER TABLE program DROP COLUMN remindCompleted" );
        executeSql( "ALTER TABLE program DROP COLUMN displayProvidedOtherFacility" );
        executeSql( "UPDATE program SET dataEntryMethod=false WHERE dataEntryMethod is null" );
        executeSql( "UPDATE programstage SET allowGenerateNextVisit=false WHERE allowGenerateNextVisit is null" );
        executeSql( "update programstage set openAfterEnrollment=false where openAfterEnrollment is null" );
        executeSql( "update programstage set hideduedate=false where hideduedate is null" );

        executeSql( "ALTER TABLE program DROP COLUMN facilityLB" );
        executeSql( "update programstage_dataelements set allowDateInFuture=false where allowDateInFuture is null" );
        executeSql( "update programstage set autoGenerateEvent=true where programid in ( select programid from program where type=2 )" );

        executeSql( "ALTER TABLE programstageinstance ALTER COLUMN executiondate TYPE timestamp" );

        executeSql( "ALTER TABLE program DROP COLUMN useBirthDateAsIncidentDate" );
        executeSql( "ALTER TABLE program DROP COLUMN useBirthDateAsEnrollmentDate" );

        executeSql( "UPDATE program SET displayIncidentDate=false WHERE displayIncidentDate is null" );
        executeSql( "UPDATE program SET relationshipFromA=false WHERE relationshipFromA is null" );

        executeSql( "update userroleauthorities set \"name\"='trackedEntityExcelTemplateFileName' where \"name\"='patientExcelTemplateFileName'" );
        executeSql( "update systemsetting set \"name\"='autoSavetTrackedEntityForm' where \"name\"='autoSavePatientRegistration'" );

        executeSql( "UPDATE trackedentityattribute SET uniquefield=false WHERE uniquefield is null" );

        executeSql( "INSERT INTO trackedentityattribute "
            + "( trackedentityattributeid, uid, lastUpdated, name, description, valueType, mandatory, inherit, displayOnVisitSchedule, uniquefield, orgunitScope, programScope )"
            + " select "
            + statementBuilder.getAutoIncrementValue()
            + ", uid, lastUpdated, name,  description, type, mandatory, false, false, true, orgunitScope, programScope from patientidentifiertype" );

        executeSql( "INSERT INTO trackedentityattributevalue (trackedentityinstanceid, trackedentityattributeid, value ) "
            + "select trackedentityinstanceid, pa.trackedentityattributeid, identifier "
            + "from patientidentifier pi inner join patientidentifiertype pit "
            + "on pi.patientidentifiertypeid=pit.patientidentifiertypeid inner join trackedentityattribute pa "
            + "on pa.uid=pit.uid where pi.trackedentityinstanceid is not null" );
        executeSql( "DROP TABLE program_identifiertypes" );
        executeSql( "DROP TABLE patientidentifier" );
        executeSql( "DROP TABLE patientidentifiertype" );
        executeSql( "ALTER TABLE trackedentityattribute RENAME CONSTRAINT fk_patientidentifiertype_periodtypeid TO trackedentityattribute_periodtypeid" );

        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_REMOVE_EMPTY_EVENTS' where authority='F_PATIENT_REMOVE_EMPTY_EVENTS'" );
        executeSql( "update userroleauthorities set authority='F_ACCESS_TRACKED_ENTITY_ATTRIBUTES' where authority='F_ACCESS_PATIENT_ATTRIBUTES'" );
        executeSql( "update userroleauthorities set authority='F_ALLOW_EDIT_TRACKED_ENTITY_ATTRIBUTES' where authority='F_ALLOW_EDIT_PATIENT_ATTRIBUTES'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_ATTRIBUTE_ADD' where authority='F_TRACKED_ENTITY_INSTANCEATTRIBUTE_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_ATTRIBUTE_DELETE' where authority='F_TRACKED_ENTITY_INSTANCEATTRIBUTE_DELETE'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_ATTRIBUTEVALUE_ADD' where authority='F_TRACKED_ENTITY_INSTANCEATTRIBUTE_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_CHANGE_LOCATION' where authority='F_PATIENT_CHANGE_LOCATION'" );
        executeSql( "update userroleauthorities set authority='F_SEARCH_TRACKED_ENTITY_INSTANCE_IN_ALL_FACILITIES' where authority='F_SEARCH_PATIENT_IN_ALL_FACILITIES'" );
        executeSql( "update userroleauthorities set authority='F_SEARCH_TRACKED_ENTITY_INSTANCE_IN_OTHER_ORGUNITS' where authority='F_SEARCH_PATIENT_IN_OTHER_ORGUNITS'" );
        executeSql( "update userroleauthorities set authority='F_ADD_TRACKED_ENTITY_FORM' where authority='F_ADD_PATIENT_REGISTRATION_FORM'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_DATAVALUE_ADD' where authority='F_PATIENT_DATAVALUE_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_DATAVALUE_DELETE' where authority='F_PATIENT_DATAVALUE_DELETE'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_ADD' where authority='F_PATIENT_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_DELETE' where authority='F_PATIENT_DELETE'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_SEARCH' where authority='F_PATIENT_SEARCH'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_LIST' where authority='F_PATIENT_LIST'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_HISTORY' where authority='F_PATIENT_HISTORY'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_DASHBOARD' where authority='F_PATIENT_DASHBOARD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_COMMENT_ADD' where authority='F_PATIENT_COMMENT_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_COMMENT_DELETE' where authority='F_PATIENT_COMMENT_DELETE'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_AGGREGATE_REPORT_PUBLIC_ADD' where authority='F_PATIENT_AGGREGATE_REPORT_PUBLIC_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_AGGREGATE_REPORT_PRIVATE_ADD' where authority='F_PATIENT_AGGREGATE_REPORT_PRIVATE_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_TABULAR_REPORT_PUBLIC_ADD' where authority='F_PATIENT_TABULAR_REPORT_PUBLIC_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_TABULAR_REPORT_PRIVATE_ADD' where authority='F_PATIENT_TABULAR_REPORT_PRIVATE_ADD'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_AGGREGATION' where authority='F_PATIENT_AGGREGATION'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_MANAGEMENT' where authority='F_PATIENT_MANAGEMENT'" );
        executeSql( "update userroleauthorities set authority='F_NAME_BASED_DATA_ENTRY' where authority='F_NAME_BASED_DATA_ENTRY'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_ATTRIBUTEVALUE_DELETE' where authority='F_PATIENTATTRIBUTEVALUE_DELETE'" );
        executeSql( "update userroleauthorities set authority='F_TRACKED_ENTITY_INSTANCE_REMINDER_MANAGEMENT' where authority='F_PATIENT_REMINDER_MANAGEMENT'" );

        executeSql( "ALTER TABLE program_attributes RENAME COLUMN programattributeid TO programtrackedentityattributeid" );
        createPersonTrackedEntity();

        executeSql( "ALTER TABLE trackedentityattributevalue DROP COLUMN trackedentityattributeoptionid" );
        executeSql( "DROP TABLE trackedentityattributeoption" );

        executeSql( "UPDATE program_attributes SET mandatory = trackedentityattribute.mandatory "
            + "FROM program_attributes pa " + "INNER JOIN trackedentityattribute  "
            + "ON pa.trackedentityattributeid=trackedentityattribute.trackedentityattributeid  "
            + "where trackedentityattribute.mandatory is not null" );
        executeSql( "ALTER TABLE trackedentityattribute DROP COLUMN mandatory" );
        executeSql( "ALTER TABLE trackedentityattribute DROP COLUMN groupBy" );

        executeSql( "update trackedentityattribute set aggregationype='AVERAGE' where aggregationtype is null" );

        executeSql( "update trackedentityattribute set searchscope='NOT_SEARCHABLE' where confidential=true" );
        executeSql( "update trackedentityattribute set searchscope='SEARCH_ORG_UNITS' where searchscope is null" );
        executeSql( "update trackedentityattribute set searchscope='SEARCH_ORG_UNITS' where searchscope='SEARCH_OUS'" );
        executeSql( "update trackedentityattribute set searchscope='DATA_CAPTURE_ORG_UNITS' where searchscope='OWN_OUS'" );

        executeSql( "DROP TABLE orgunitgroupprograms" );

        executeSql( "ALTER TABLE programstageinstance DROP COLUMN completed" );

        executeSql( "update program_attributes set mandatory = false where mandatory is null;" );

        executeSql( "update trackedentityattribute set confidential = false where confidential is null;" );

        executeSql( "update programstage_dataelements set allowfuturedate = allowdateinfuture where allowfuturedate is null" );
        executeSql( "update programstage_dataelements set allowfuturedate = false where allowfuturedate is null" );
        executeSql( "ALTER TABLE programstage_dataelements DROP COLUMN allowdateinfuture" );

        executeSql( "update program_attributes set allowfuturedate = allowdateinfuture where allowfuturedate is null" );
        executeSql( "update program_attributes set allowfuturedate = false where allowfuturedate is null" );
        executeSql( "ALTER TABLE program_attributes DROP COLUMN allowdateinfuture" );
        executeSql( "UPDATE program_attributes SET allowFutureDate='false' WHERE allowFutureDate is null" );

        executeSql( "DROP SEQUENCE period_periodid_seq" );
        executeSql( "ALTER TABLE programstagesection DROP CONSTRAINT programstagesection_name_key" );

        executeSql( "update eventreport set collapsedatadimensions = false where collapsedatadimensions is null" );
        executeSql( "update eventchart set collapsedatadimensions = false where collapsedatadimensions is null" );

        executeSql( "update programindicator set aggregationtype='AVERAGE' where aggregationtype is null" );
        executeSql( "ALTER TABLE programindicator ALTER description TYPE text" );
        executeSql( "ALTER TABLE programindicator ALTER expression TYPE text" );
        executeSql( "alter table programindicator drop column valuetype" );
        executeSql( "alter table programindicator drop column rootdate" );
        executeSql( "alter table programindicator drop column eventoutputtype" );

        executeSql( "ALTER TABLE programstage ALTER description TYPE text" );

        executeSql( "update programindicator set displayinform = false where displayinform is null" );

        executeSql( "drop index index_patientdatavalue" );

        executeSql( "update program p set dataentryformid = (select dataentryformid from trackedentityform tf where tf.programid=p.programid limit 1)" );
        executeSql( "drop table trackedentityform" );

        updateProgramStageList();
        updateProgramAttributeList();

        executeSql( "update userroleauthorities set authority='F_ADD_TRACKED_ENTITY_FORM' where authority='F_TRACKED_ENTITY_FORM_ADD'" );

        // TODO fix
        // executeSql( "DROP TABLE programstage_programindicators" );
    }

    // -------------------------------------------------------------------------
    // Supporting methods
    // -------------------------------------------------------------------------

    private void updateProgramInstanceStatus()
    {
        // Set active status for events
        executeSql( "UPDATE programinstance SET status=0 WHERE completed=false" );

        // Set un-completed status for events
        executeSql( "UPDATE programinstance SET status=2 WHERE programinstanceid in "
            + "( select psi.programinstanceid from programinstance pi join programstageinstance psi "
            + "on psi.programinstanceid = psi.programstageinstanceid "
            + "where pi.completed=true and psi.completed = false )" );

        // Set completed status for events
        executeSql( "UPDATE programinstance SET status=1 WHERE status is null" );

        // Drop the column with name as completed
        executeSql( "ALTER TABLE programinstance DROP COLUMN completed" );
    }

    private void createPersonTrackedEntity()
    {
        int exist = jdbcTemplate.queryForObject( "SELECT count(*) FROM trackedentity where name='Person'",
            Integer.class );

        if ( exist == 0 )
        {

            String id = statementBuilder.getAutoIncrementValue();
            String uid = "MCPQUTHX1Ze";
            String date = DateUtils.getSqlDateString( new Date() );

            jdbcTemplate.execute( "INSERT INTO trackedentity(trackedentityid,uid, code, created, lastupdated,name, description) values("
                + id + ",'" + uid + "','Person','" + date + "','" + date + "','Person','Person')" );

            jdbcTemplate.execute( "UPDATE program SET trackedentityid="
                + "  (SELECT trackedentityid FROM trackedentity where name='Person') where trackedentityid is null" );

            jdbcTemplate.execute( "UPDATE trackedentityinstance SET trackedentityid="
                + "  (SELECT trackedentityid FROM trackedentity where name='Person') where trackedentityid is null" );
        }
    }

    private void updateProgramStageList()
    {
        int count = jdbcTemplate.queryForObject( "select count(*) from programstage where sort_order is null", Integer.class );

        if ( count > 0 )
        {
            StatementHolder holder = statementManager.getHolder();

            try
            {
                Statement statement = holder.getStatement();

                ResultSet resultSet = statement
                    .executeQuery( "SELECT programstageid, programid, minDaysFromStart FROM programstage order by programid, minDaysFromStart" );

                int index = 1;
                int programId = 0;
                while ( resultSet.next() )
                {
                    if ( programId != resultSet.getInt( "programid" ) )
                    {
                        programId = resultSet.getInt( "programid" );
                        index = 1;
                    }

                    executeSql( "UPDATE programStage SET sort_order=" + index + " WHERE programstageid="
                        + resultSet.getInt( "programstageid" ) );
                    index++;
                }
            }
            catch ( Exception ex )
            {
                log.debug( ex );
            }
            finally
            {
                holder.close();
            }
        }
    }

    private void updateProgramAttributeList()
    {
        StatementHolder holder = statementManager.getHolder();

        try
        {
            Statement statement = holder.getStatement();

            ResultSet resultSet = statement
                .executeQuery( "select programtrackedentityattributeid, programid from program_attributes ORDER BY programid, sort_order" );

            int index = 1;
            int programId = 0;
            while ( resultSet.next() )
            {
                if ( programId != resultSet.getInt( "programid" ) )
                {
                    programId = resultSet.getInt( "programid" );
                    index = 1;
                }

                executeSql( "UPDATE program_attributes SET sort_order=" + index + " WHERE programtrackedentityattributeid="
                    + resultSet.getInt( "programtrackedentityattributeid" ) );
                index++;
            }
        }
        catch ( Exception ex )
        {
            log.debug( ex );
        }
        finally
        {
            holder.close();
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
}
