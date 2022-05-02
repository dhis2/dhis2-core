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
package org.hisp.dhis.maintenance.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.maintenance.MaintenanceStore;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.maintenance.MaintenanceStore" )
public class JdbcMaintenanceStore
    implements MaintenanceStore
{
    private static final ImmutableMap<Class<? extends SoftDeletableObject>, SoftDeletableObject> ENTITY_MAPPER = new ImmutableMap.Builder<Class<? extends SoftDeletableObject>, SoftDeletableObject>()
        .put( ProgramInstance.class, new ProgramInstance() )
        .put( ProgramStageInstance.class, new ProgramStageInstance() )
        .put( TrackedEntityInstance.class, new TrackedEntityInstance() )
        .build();
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private JdbcTemplate jdbcTemplate;

    private AuditManager auditManager;

    public JdbcMaintenanceStore( JdbcTemplate jdbcTemplate, AuditManager auditManager )
    {
        checkNotNull( jdbcTemplate );
        checkNotNull( auditManager );

        this.jdbcTemplate = jdbcTemplate;
        this.auditManager = auditManager;
    }

    // -------------------------------------------------------------------------
    // MaintenanceStore implementation
    // -------------------------------------------------------------------------

    @Override
    public int deleteZeroDataValues()
    {
        String sql = "delete from datavalue dv " + "where dv.dataelementid in ( " + "select de.dataelementid "
            + "from dataelement de " + "where de.aggregationtype = 'SUM' " + "and de.zeroissignificant is false ) "
            + "and dv.value = '0';";

        return jdbcTemplate.update( sql );
    }

    @Override
    public int deleteSoftDeletedDataValues()
    {
        String sql = "delete from datavalue dv " + "where dv.deleted is true;";

        return jdbcTemplate.update( sql );
    }

    @Override
    public int deleteSoftDeletedProgramStageInstances()
    {
        List<String> deletedEvents = getDeletionEntities(
            "(select uid from programstageinstance where deleted is true)" );

        String psiSelect = "(select programstageinstanceid from programstageinstance where deleted is true)";

        String pmSelect = "(select id from programmessage where programstageinstanceid in "
            + psiSelect + " )";

        /*
         * Delete event values, event value audits, event comments, events
         *
         */
        String[] sqlStmts = new String[] {
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + pmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + pmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + pmSelect,
            "delete from programmessage where programstageinstanceid in " + psiSelect,
            "delete from trackedentitydatavalueaudit where programstageinstanceid in " + psiSelect,
            "delete from programstageinstancecomments where programstageinstanceid in " + psiSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            "delete from programstageinstance where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 && deletedEvents.size() > 0 )
        {
            auditHardDeletedEntity( deletedEvents, ProgramStageInstance.class );

        }

        return result;
    }

    @Override
    public int deleteSoftDeletedProgramInstances()
    {
        String piSelect = "(select programinstanceid from programinstance where deleted is true)";

        List<String> deletedEnrollments = getDeletionEntities(
            "select uid from programinstance where deleted is true" );

        List<String> associatedEvents = getDeletionEntities(
            "select uid from programstageinstance where programinstanceid in " + piSelect );

        String psiSelect = "(select programstageinstanceid from programstageinstance where programinstanceid in "
            + piSelect + " )";

        String pmSelect = "(select id from programmessage where programinstanceid in "
            + piSelect + " )";

        /*
         * Delete event values, event value audits, event comments, events,
         * enrollment comments, enrollments
         *
         */
        String[] sqlStmts = new String[] {
            "delete from trackedentitydatavalueaudit where programstageinstanceid in " + psiSelect,
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + pmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + pmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + pmSelect,
            "delete from programmessage where programinstanceid in " + piSelect,
            "delete from programmessage where programstageinstanceid in " + psiSelect,
            "delete from trackedentitycomment where trackedentitycommentid in (select trackedentitycommentid from programstageinstancecomments where programstageinstanceid in "
                + psiSelect + ")",
            "delete from programstageinstancecomments where programstageinstanceid in " + psiSelect,
            "delete from programstageinstance where programinstanceid in " + piSelect,
            "delete from programinstancecomments where programinstanceid in " + piSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            "delete from programinstance where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 && deletedEnrollments.size() > 0 )
        {
            auditHardDeletedEntity( deletedEnrollments, ProgramInstance.class );

            if ( associatedEvents.size() > 0 )
            {
                auditHardDeletedEntity( associatedEvents, ProgramStageInstance.class );
            }
        }

        return result;
    }

    @Override
    public int deleteSoftDeletedTrackedEntityInstances()
    {
        String teiSelect = "(select trackedentityinstanceid from trackedentityinstance where deleted is true)";

        String piSelect = "(select programinstanceid from programinstance where trackedentityinstanceid in " + teiSelect
            + " )";

        List<String> deletedTeiUids = getDeletionEntities(
            "select uid from trackedentityinstance where deleted is true" );

        List<String> associatedEnrollments = getDeletionEntities(
            "select uid from programinstance where trackedentityinstanceid in " + teiSelect );

        List<String> associatedEvents = getDeletionEntities(
            "select uid from programstageinstance where programinstanceid in " + piSelect );

        /*
         * Prepare filter queries for hard delete
         */

        String psiSelect = "(select programstageinstanceid from programstageinstance where programinstanceid in "
            + piSelect + " )";

        String teiPmSelect = "(select id from programmessage where trackedentityinstanceid in " + teiSelect + " )";
        String piPmSelect = "(select id from programmessage where programinstanceid in " + piSelect + " )";
        String psiPmSelect = "(select id from programmessage where programstageinstanceid in " + psiSelect + " )";

        /*
         * Delete event values, event audits, event comments, events, enrollment
         * comments, enrollments, tei attribtue values, tei attribtue value
         * audits, teis
         *
         */
        String[] sqlStmts = new String[] {
            "delete from trackedentitydatavalueaudit where programstageinstanceid in " + psiSelect,
            "delete from programstageinstancecomments where programstageinstanceid in " + psiSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            "delete from programstageinstance where programinstanceid in " + piSelect,
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + teiPmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + teiPmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + teiPmSelect,
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + piPmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + piPmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + piPmSelect,
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + psiPmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + psiPmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + psiPmSelect,
            "delete from programmessage where programinstanceid in " + piSelect,
            "delete from programmessage where trackedentityinstanceid in " + teiSelect,
            "delete from programinstancecomments where programinstanceid in " + piSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            "delete from programinstance where programinstanceid in " + piSelect,
            "delete from trackedentityattributevalue where trackedentityinstanceid in " + teiSelect,
            "delete from trackedentityattributevalueaudit where trackedentityinstanceid in " + teiSelect,
            "delete from trackedentityprogramowner where trackedentityinstanceid in " + teiSelect,
            "delete from programtempowner where trackedentityinstanceid in " + teiSelect,
            "delete from programtempownershipaudit where trackedentityinstanceid in " + teiSelect,
            "delete from programownershiphistory where trackedentityinstanceid in " + teiSelect,
            "delete from trackedentityinstance where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 && deletedTeiUids.size() > 0 )
        {
            auditHardDeletedEntity( deletedTeiUids, TrackedEntityInstance.class );

            if ( associatedEnrollments.size() > 0 )
            {
                auditHardDeletedEntity( associatedEnrollments, ProgramInstance.class );
            }

            if ( associatedEvents.size() > 0 )
            {
                auditHardDeletedEntity( associatedEvents, ProgramStageInstance.class );
            }
        }

        return result;
    }

    private List<String> getDeletionEntities( String entitySql )
    {
        /*
         * Get all soft deleted entities before they are hard deleted from
         * database
         */
        List<String> deletedUids = new ArrayList<>();

        SqlRowSet softDeletedEntitiesUidRows = jdbcTemplate.queryForRowSet( entitySql );

        while ( softDeletedEntitiesUidRows.next() )
        {
            deletedUids.add( softDeletedEntitiesUidRows.getString( "uid" ) );
        }

        return deletedUids;
    }

    private void auditHardDeletedEntity( List<String> deletedEnrollments, Class<? extends SoftDeletableObject> entity )
    {
        deletedEnrollments.forEach( enrolment -> {

            SoftDeletableObject object = ENTITY_MAPPER.getOrDefault( entity, new SoftDeletableObject() );

            object.setUid( enrolment );
            object.setDeleted( true );
            auditManager.send( Audit.builder()
                .auditType( AuditType.DELETE )
                .auditScope( AuditScope.TRACKER )
                .createdAt( LocalDateTime.now() )
                .object( object )
                .uid( enrolment )
                .auditableEntity( new AuditableEntity( entity, object ) )
                .build() );
        } );
    }
}
