package org.hisp.dhis.program.hibernate;

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

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.SchedulingProgramObject;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * @author Abyot Asalefew
 */
public class HibernateProgramStageInstanceStore
    extends HibernateIdentifiableObjectStore<ProgramStageInstance>
    implements ProgramStageInstanceStore
{
    @Autowired
    private TrackedEntityInstanceReminderService reminderService;

    // -------------------------------------------------------------------------
    // Implemented methods
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    public ProgramStageInstance get( ProgramInstance programInstance, ProgramStage programStage )
    {
        List<ProgramStageInstance> list = getCriteria(
            Restrictions.eq( "programInstance", programInstance ),
            Restrictions.eq( "programStage", programStage ) ).
            addOrder( Order.asc( "id" ) ).list();

        return list.isEmpty() ? null : list.get( list.size() - 1 );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramStageInstance> get( Collection<ProgramInstance> programInstances, EventStatus status )
    {
        return getCriteria(
            Restrictions.in( "programInstance", programInstances ),
            Restrictions.eq( "status", status ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramStageInstance> get( TrackedEntityInstance entityInstance, EventStatus status )
    {
        Criteria criteria = getCriteria();
        criteria.createAlias( "programInstance", "programInstance" );
        criteria.add( Restrictions.eq( "programInstance.entityInstance", entityInstance ) );
        criteria.add( Restrictions.eq( "status", status ) );
        return criteria.list();
    }

    @Override
    public Collection<SchedulingProgramObject> getSendMessageEvents()
    {
        String sql = " ( " + sendMessageToTrackedEntityInstanceSql() + " ) ";

        sql += " UNION ( " + sendMessageToHealthWorkerSql() + " ) ";

        sql += " UNION ( " + sendMessageToOrgunitRegisteredSql() + " ) ";

        sql += " UNION ( " + sendMessageToUsersSql() + " ) ";

        sql += " UNION ( " + sendMessageToUserGroupsSql() + " ) ";
        
        SqlRowSet rs = jdbcTemplate.queryForRowSet( sql );

        Collection<SchedulingProgramObject> schedulingProgramObjects = new HashSet<>();

        while ( rs.next() )
        {
            String message = rs.getString( "templatemessage" );

            List<String> attributeUids = reminderService.getAttributeUids( message );
            int programstageinstanceid = rs.getInt( "programstageinstanceid" );
            SqlRowSet attributeValueRow = jdbcTemplate
                .queryForRowSet( "select tea.uid ,teav.value from trackedentityattributevalue teav "
                    + " INNER JOIN trackedentityattribute tea on tea.trackedentityattributeid=teav.trackedentityattributeid "
                    + " INNER JOIN programinstance ps on teav.trackedentityinstanceid=ps.trackedentityinstanceid "
                    + " INNER JOIN programstageinstance psi on ps.programinstanceid=psi.programinstanceid "
                    + " where tea.uid in ( " + TextUtils.getQuotedCommaDelimitedString( attributeUids ) + ")  "
                    + " and psi.programstageinstanceid=" + programstageinstanceid );

            while ( attributeValueRow.next() )
            {
                String uid = attributeValueRow.getString( "uid" );
                String value = attributeValueRow.getString( "value" );
                String key = "\\{(" + TrackedEntityInstanceReminder.ATTRIBUTE + ")=(" + uid + ")\\}";
                message = message.replaceAll( key, value );
            }

            String organisationunitName = rs.getString( "orgunitName" );
            String programName = rs.getString( "programName" );
            String programStageName = rs.getString( "programStageName" );
            String daysSinceDueDate = rs.getString( "days_since_due_date" );
            String dueDate = rs.getString( "duedate" ).split( " " )[0];

            message = message.replace( TrackedEntityInstanceReminder.TEMPLATE_MESSSAGE_PROGRAM_NAME, programName );
            message = message.replace( TrackedEntityInstanceReminder.TEMPLATE_MESSSAGE_PROGAM_STAGE_NAME, programStageName );
            message = message.replace( TrackedEntityInstanceReminder.TEMPLATE_MESSSAGE_DUE_DATE, dueDate );
            message = message.replace( TrackedEntityInstanceReminder.TEMPLATE_MESSSAGE_ORGUNIT_NAME, organisationunitName );
            message = message.replace( TrackedEntityInstanceReminder.TEMPLATE_MESSSAGE_DAYS_SINCE_DUE_DATE, daysSinceDueDate );

            SchedulingProgramObject schedulingProgramObject = new SchedulingProgramObject();
            schedulingProgramObject.setProgramStageInstanceId( programstageinstanceid );
            schedulingProgramObject.setPhoneNumber( rs.getString( "PHONE_NUMBER" ) );
            schedulingProgramObject.setMessage( message );

            schedulingProgramObjects.add( schedulingProgramObject );
        }

        return schedulingProgramObjects;
    }

    @Override
    public int getOverDueCount( ProgramStage programStage, Collection<Integer> orgunitIds, Date startDate, Date endDate )
    {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add( Calendar.DATE, -1 );
        PeriodType.clearTimeOfDay( yesterday );
        Date now = yesterday.getTime();

        if ( endDate.before( now ) )
        {
            now = endDate;
        }

        Criteria criteria = getCriteria();
        criteria.createAlias( "programInstance", "programInstance" );
        criteria.createAlias( "programInstance.entityInstance", "entityInstance" );
        criteria.createAlias( "entityInstance.organisationUnit", "regOrgunit" );
        criteria.add( Restrictions.eq( "programStage", programStage ) );
        criteria.add( Restrictions.isNull( "programInstance.endDate" ) );
        criteria.add( Restrictions.isNull( "executionDate" ) );
        criteria.add( Restrictions.between( "dueDate", startDate, now ) );
        criteria.add( Restrictions.in( "regOrgunit.id", orgunitIds ) );
        criteria.setProjection( Projections.rowCount() ).uniqueResult();

        Number rs = (Number) criteria.setProjection( Projections.rowCount() ).uniqueResult();

        return rs != null ? rs.intValue() : 0;
    }

    @Override
    public int count( ProgramStage programStage, Collection<Integer> orgunitIds, Date startDate, Date endDate,
        Boolean completed )
    {
        Number rs = (Number) getCriteria( programStage, orgunitIds, startDate, endDate, completed ).setProjection(
            Projections.rowCount() ).uniqueResult();

        return rs != null ? rs.intValue() : 0;
    }

    @Override
    public long getProgramStageInstanceCountLastUpdatedAfter( Date time )
    {
        Number rs = (Number) getCriteria()
            .add( Restrictions.ge( "lastUpdated", time ) )
            .setProjection( Projections.rowCount() )
            .uniqueResult();

        return rs != null ? rs.longValue() : 0;
    }

    @Override
    public boolean exists( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from programstageinstance where uid=?", Integer.class, uid );
        return result != null && result > 0;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Criteria getCriteria( ProgramStage programStage, Collection<Integer> orgunitIds, Date startDate,
        Date endDate, Boolean completed )
    {
        Criteria criteria = getCriteria();
        criteria.createAlias( "programInstance", "programInstance" );
        criteria.add( Restrictions.eq( "programStage", programStage ) );

        if ( completed == null )
        {
            criteria.createAlias( "programInstance.entityInstance", "entityInstance" );
            criteria.createAlias( "entityInstance.organisationUnit", "regOrgunit" );
            criteria.add( Restrictions.or( Restrictions.and( Restrictions.eq( "status", EventStatus.COMPLETED ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ), Restrictions.and(
                    Restrictions.eq( "status", EventStatus.ACTIVE ), Restrictions.isNotNull( "executionDate" ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ),
                Restrictions.and( Restrictions.eq( "status", EventStatus.ACTIVE ), Restrictions.isNull( "executionDate" ),
                    Restrictions.between( "dueDate", startDate, endDate ),
                    Restrictions.in( "regOrgunit.id", orgunitIds ) ), Restrictions.and(
                    Restrictions.eq( "status", EventStatus.SKIPPED ),
                    Restrictions.between( "dueDate", startDate, endDate ),
                    Restrictions.in( "regOrgunit.id", orgunitIds ) ) ) );
        }
        else
        {
            if ( completed )
            {
                criteria.add( Restrictions.and( Restrictions.eq( "status", EventStatus.COMPLETED ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ) );
            }
            else
            {
                criteria.createAlias( "programInstance.entityInstance", "entityInstance" );
                criteria.createAlias( "entityInstance.organisationUnit", "regOrgunit" );
                criteria.add( Restrictions.and( Restrictions.eq( "status", EventStatus.ACTIVE ),
                    Restrictions.isNotNull( "executionDate" ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ) );
            }
        }

        return criteria;
    }

    //TODO this must be re-written

    private String sendMessageToTrackedEntityInstanceSql()
    {
        return "select psi.programstageinstanceid, pav.value as PHONE_NUMBER, prm.templatemessage, org.name as orgunitName "
            + ",pg.name as programName, ps.name as programStageName, psi.duedate,(DATE(now()) - DATE(psi.duedate) ) as days_since_due_date "
            + "from trackedentityinstance p INNER JOIN programinstance pi "
            + "     ON p.trackedentityinstanceid=pi.trackedentityinstanceid "
            + " INNER JOIN programstageinstance psi  "
            + "     ON psi.programinstanceid=pi.programinstanceid "
            + " INNER JOIN program pg  "
            + "     ON pg.programid=pi.programid "
            + " INNER JOIN programstage ps  "
            + "     ON ps.programstageid=psi.programstageid "
            + " INNER JOIN organisationunit org  "
            + "     ON org.organisationunitid = p.organisationunitid "
            + " INNER JOIN trackedentityinstancereminder prm  "
            + "     ON prm.programstageid = ps.programstageid "
            + " INNER JOIN trackedentityattributevalue pav "
            + "     ON pav.trackedentityinstanceid=p.trackedentityinstanceid "
            + " INNER JOIN trackedentityattribute pa "
            + "     ON pa.trackedentityattributeid=pav.trackedentityattributeid "
            + "WHERE pi.status='"
            + EventStatus.ACTIVE.name()
            + "'     and prm.templatemessage is not NULL and prm.templatemessage != '' "
            + "     and pg.type='" + ProgramType.WITH_REGISTRATION.name() + "' and prm.daysallowedsendmessage is not null  "
            + "     and psi.executiondate is null and pa.valuetype='PHONE_NUMBER' "
            + "     and (  DATE(now()) - DATE(psi.duedate) ) = prm.daysallowedsendmessage "
            + "     and prm.whentosend is null and prm.sendto = " + TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE;
    }

    private String sendMessageToHealthWorkerSql()
    {
        return "SELECT psi.programstageinstanceid, uif.phonenumber, prm.templatemessage, org.name as orgunitName, "
            + "pg.name as programName, ps.name as programStageName, psi.duedate, "
            + "         (DATE(now()) - DATE(psi.duedate) ) as days_since_due_date "
            + " FROM trackedentityinstance p INNER JOIN programinstance pi "
            + "          ON p.trackedentityinstanceid=pi.trackedentityinstanceid "
            + "           INNER JOIN programstageinstance psi  "
            + "                ON psi.programinstanceid=pi.programinstanceid "
            + "             INNER JOIN program pg  "
            + "               ON pg.programid=pi.programid "
            + "           INNER JOIN programstage ps  "
            + "               ON ps.programstageid=psi.programstageid "
            + "           INNER JOIN organisationunit org  "
            + "               ON org.organisationunitid = p.organisationunitid "
            + "           INNER JOIN trackedentityinstancereminder prm  "
            + "               ON prm.programstageid = ps.programstageid "
            + "           INNER JOIN trackedentityattributevalue pav "
            + "               ON pav.trackedentityinstanceid=p.trackedentityinstanceid "
            + "           INNER JOIN trackedentityattribute pa "
            + "               ON pa.trackedentityattributeid=pav.trackedentityattributeid "
            + "           INNER JOIN userinfo uif "
            + "               ON pav.value=concat(uif.userinfoid ,'') "
            + " WHERE pi.status='"
            + EventStatus.ACTIVE.name()
            + "' and pa.valueType='users' and uif.phonenumber is not NULL and uif.phonenumber != '' "
            + "               and prm.templatemessage is not NULL and prm.templatemessage != '' "
            + "               and pg.type='" + ProgramType.WITH_REGISTRATION.name() + "' and prm.daysallowedsendmessage is not null "
            + "               and psi.executiondate is null "
            + "               and (  DATE(now()) - DATE(psi.duedate) ) = prm.daysallowedsendmessage "
            + "               and prm.whentosend is null and prm.sendto = " + TrackedEntityInstanceReminder.SEND_TO_ATTRIBUTE_TYPE_USERS;
    }

    private String sendMessageToOrgunitRegisteredSql()
    {
        return "select psi.programstageinstanceid, ou.phonenumber, prm.templatemessage, org.name as orgunitName, "
            + "pg.name as programName, ps.name as programStageName, psi.duedate,"
            + "(DATE(now()) - DATE(psi.duedate) ) as days_since_due_date "
            + "            from trackedentityinstance p INNER JOIN programinstance pi "
            + "               ON p.trackedentityinstanceid=pi.trackedentityinstanceid "
            + "           INNER JOIN programstageinstance psi "
            + "               ON psi.programinstanceid=pi.programinstanceid "
            + "           INNER JOIN program pg "
            + "               ON pg.programid=pi.programid "
            + "           INNER JOIN programstage ps "
            + "               ON ps.programstageid=psi.programstageid "
            + "           INNER JOIN organisationunit org "
            + "               ON org.organisationunitid = p.organisationunitid "
            + "           INNER JOIN trackedentityinstancereminder prm "
            + "               ON prm.programstageid = ps.programstageid "
            + "           INNER JOIN organisationunit ou "
            + "               ON ou.organisationunitid=p.organisationunitid "
            + "WHERE pi.status= '"
            + EventStatus.ACTIVE.name()
            + "'               and ou.phonenumber is not NULL and ou.phonenumber != '' "
            + "               and prm.templatemessage is not NULL and prm.templatemessage != '' "
            + "               and pg.type='" + ProgramType.WITH_REGISTRATION.name() + "' and prm.daysallowedsendmessage is not null "
            + "               and psi.executiondate is null "
            + "               and (  DATE(now()) - DATE(psi.duedate) ) = prm.daysallowedsendmessage "
            + "               and prm.whentosend is null and prm.sendto = "
            + +TrackedEntityInstanceReminder.SEND_TO_REGISTERED_ORGUNIT;
    }

    private String sendMessageToUsersSql()
    {
        return "select psi.programstageinstanceid, uif.phonenumber,prm.templatemessage, org.name as orgunitName ,"
            + " pg.name as programName, ps.name as programStageName, psi.duedate, "
            + "(DATE(now()) - DATE(psi.duedate) ) as days_since_due_date "
            + "  from trackedentityinstance p INNER JOIN programinstance pi "
            + "       ON p.trackedentityinstanceid=pi.trackedentityinstanceid "
            + "   INNER JOIN programstageinstance psi "
            + "       ON psi.programinstanceid=pi.programinstanceid "
            + "   INNER JOIN program pg "
            + "       ON pg.programid=pi.programid "
            + "   INNER JOIN programstage ps "
            + "       ON ps.programstageid=psi.programstageid "
            + "   INNER JOIN trackedentityinstancereminder prm "
            + "       ON prm.programstageid = ps.programstageid "
            + "   INNER JOIN organisationunit org "
            + "       ON org.organisationunitid = p.organisationunitid "
            + "   INNER JOIN usermembership ums "
            + "       ON ums.organisationunitid = p.organisationunitid "
            + "   INNER JOIN userinfo uif "
            + "       ON uif.userinfoid = ums.userinfoid "
            + "  WHERE pi.status= '"
            + EventStatus.ACTIVE.name()
            + "'       and uif.phonenumber is not NULL and uif.phonenumber != '' "
            + "       and prm.templatemessage is not NULL and prm.templatemessage != '' "
            + "       and pg.type='" + ProgramType.WITH_REGISTRATION.name() + "' and prm.daysallowedsendmessage is not null "
            + "       and psi.executiondate is null "
            + "       and (  DATE(now()) - DATE(psi.duedate) ) = prm.daysallowedsendmessage "
            + "       and prm.whentosend is null and prm.sendto = "
            + TrackedEntityInstanceReminder.SEND_TO_ALL_USERS_AT_REGISTERED_ORGUNIT;
    }

    private String sendMessageToUserGroupsSql()
    {
        return "select psi.programstageinstanceid, uif.phonenumber,prm.templatemessage, org.name as orgunitName ,"
            + " pg.name as programName, ps.name as programStageName, psi.duedate, "
            + "(DATE(now()) - DATE(psi.duedate) ) as days_since_due_date "
            + "  from trackedentityinstance p INNER JOIN programinstance pi "
            + "       ON p.trackedentityinstanceid=pi.trackedentityinstanceid "
            + "   INNER JOIN programstageinstance psi "
            + "       ON psi.programinstanceid=pi.programinstanceid "
            + "   INNER JOIN program pg "
            + "       ON pg.programid=pi.programid "
            + "   INNER JOIN programstage ps "
            + "       ON ps.programstageid=psi.programstageid "
            + "   INNER JOIN trackedentityinstancereminder prm "
            + "       ON prm.programstageid = ps.programstageid "
            + "   INNER JOIN organisationunit org "
            + "       ON org.organisationunitid = p.organisationunitid "
            + "   INNER JOIN usergroupmembers ugm "
            + "       ON ugm.usergroupid = prm.usergroupid "
            + "   INNER JOIN userinfo uif "
            + "       ON uif.userinfoid = ugm.userid "
            + "  WHERE pi.status= '"
            + EventStatus.ACTIVE.name()
            + "'       and uif.phonenumber is not NULL and uif.phonenumber != '' "
            + "       and prm.templatemessage is not NULL and prm.templatemessage != '' "
            + "       and pg.type='" + ProgramType.WITH_REGISTRATION.name() + "' and prm.daysallowedsendmessage is not null "
            + "       and psi.executiondate is not null "
            + "       and (  DATE(now()) - DATE(psi.duedate) ) = prm.daysallowedsendmessage "
            + "       and prm.whentosend is null " + "       and prm.sendto = " + TrackedEntityInstanceReminder.SEND_TO_USER_GROUP;
    }
}