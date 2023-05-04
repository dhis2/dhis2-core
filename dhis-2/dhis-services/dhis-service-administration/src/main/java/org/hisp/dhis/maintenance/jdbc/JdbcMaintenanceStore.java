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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.maintenance.MaintenanceStore;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service
@RequiredArgsConstructor
public class JdbcMaintenanceStore implements MaintenanceStore
{
    private static final Map<Class<? extends SoftDeletableObject>, SoftDeletableObject> ENTITY_MAPPER = Map.of(
        Enrollment.class, new Enrollment(),
        Event.class, new Event(),
        TrackedEntityInstance.class, new TrackedEntityInstance(),
        Relationship.class, new Relationship() );

    private final JdbcTemplate jdbcTemplate;

    private final AuditManager auditManager;

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
    public int deleteSoftDeletedEvents()
    {
        List<String> deletedEvents = getDeletionEntities(
            "(select uid from programstageinstance where deleted is true)" );

        if ( deletedEvents.isEmpty() )
        {
            return 0;
        }

        String eventSelect = "(select programstageinstanceid from programstageinstance where deleted is true)";

        String pmSelect = "(select id from programmessage where programstageinstanceid in " + eventSelect + " )";

        /*
         * Delete event values, event value audits, event comments, events
         *
         */
        String[] sqlStmts = new String[] {
            // delete objects related to messages that are related to PSIs
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + pmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + pmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + pmSelect,
            // delete related PSIs comments
            "delete from programstageinstancecomments where programstageinstanceid in " + eventSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            // delete other objects related to PSIs
            "delete from relationshipitem where programstageinstanceid in " + eventSelect,
            "delete from trackedentitydatavalueaudit where programstageinstanceid in " + eventSelect,
            "delete from programmessage where programstageinstanceid in " + eventSelect,
            // finally delete the PSIs
            "delete from programstageinstance where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 && !deletedEvents.isEmpty() )
        {
            auditHardDeletedEntity( deletedEvents, Event.class );

        }

        return result;
    }

    @Override
    public int deleteSoftDeletedRelationships()
    {

        List<String> deletedRelationships = getDeletionEntities(
            "(select uid from relationship where deleted is true)" );

        if ( deletedRelationships.isEmpty() )
        {
            return 0;
        }

        /*
         * Delete relationship items and relationships. There is a `on cascade
         * delete` constraints between relationship and relationshipitem tables
         */
        String[] sqlStmts = { "delete from relationship where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 )
        {
            auditHardDeletedEntity( deletedRelationships, Relationship.class );
        }

        return result;
    }

    @Override
    public int deleteSoftDeletedEnrollments()
    {
        String piSelect = "(select programinstanceid from programinstance where deleted is true)";

        List<String> deletedEnrollments = getDeletionEntities(
            "select uid from programinstance where deleted is true" );

        if ( deletedEnrollments.isEmpty() )
        {
            return 0;
        }

        List<String> associatedEvents = getDeletionEntities(
            "select uid from programstageinstance where programinstanceid in " + piSelect );

        String eventSelect = "(select programstageinstanceid from programstageinstance where programinstanceid in "
            + piSelect + " )";

        String pmSelect = "(select id from programmessage where programinstanceid in " + piSelect + " )";

        /*
         * Delete event values, event value audits, event comments, events,
         * enrollment comments, enrollments
         *
         */
        String[] sqlStmts = new String[] {
            // delete objects linked to messages that are linked to PIs
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + pmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + pmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + pmSelect,
            // delete comments linked to both PIs and PSIs
            "delete from programstageinstancecomments where programstageinstanceid in " + eventSelect,
            "delete from programinstancecomments where programinstanceid in " + piSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            // delete other entries linked to PSIs
            "delete from relationshipitem where programstageinstanceid in " + eventSelect,
            "delete from trackedentitydatavalueaudit where programstageinstanceid in " + eventSelect,
            "delete from programmessage where programstageinstanceid in " + eventSelect,
            // delete other entries linked to PIs
            "delete from relationshipitem where programinstanceid in " + piSelect,
            "delete from programmessage where programinstanceid in " + piSelect,
            "delete from programstageinstance where programinstanceid in " + piSelect,
            // finally delete the PIs themselves
            "delete from programinstance where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 )
        {
            auditHardDeletedEntity( associatedEvents, Event.class );
            auditHardDeletedEntity( deletedEnrollments, Enrollment.class );
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
        if ( deletedTeiUids.isEmpty() )
        {
            return 0;
        }

        List<String> associatedEnrollments = getDeletionEntities(
            "select uid from programinstance where trackedentityinstanceid in " + teiSelect );

        List<String> associatedEvents = getDeletionEntities(
            "select uid from programstageinstance where programinstanceid in " + piSelect );

        /*
         * Prepare filter queries for hard delete
         */

        String eventSelect = "(select programstageinstanceid from programstageinstance where programinstanceid in "
            + piSelect + " )";

        String teiPmSelect = "(select id from programmessage where trackedentityinstanceid in " + teiSelect + " )";
        String piPmSelect = "(select id from programmessage where programinstanceid in " + piSelect + " )";
        String eventPmSelect = "(select id from programmessage where programstageinstanceid in " + eventSelect + " )";

        /*
         * Delete event values, event audits, event comments, events, enrollment
         * comments, enrollments, tei attribtue values, tei attribtue value
         * audits, teis
         *
         */
        String[] sqlStmts = new String[] {
            // delete objects related to any message related to obsolete TEIs
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + teiPmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + teiPmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + teiPmSelect,
            // delete objects related to any message related to obsolete PIs
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + piPmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + piPmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + piPmSelect,
            // delete objects related to any message related to obsolete PSIs
            "delete from programmessage_deliverychannels where programmessagedeliverychannelsid in " + eventPmSelect,
            "delete from programmessage_emailaddresses where programmessageemailaddressid in " + eventPmSelect,
            "delete from programmessage_phonenumbers where programmessagephonenumberid in " + eventPmSelect,
            // delete comments related to any obsolete PIs or PSIs
            "delete from programstageinstancecomments where programstageinstanceid in " + eventSelect,
            "delete from programinstancecomments where programinstanceid in " + piSelect,
            "delete from trackedentitycomment where trackedentitycommentid not in (select trackedentitycommentid from programstageinstancecomments union all select trackedentitycommentid from programinstancecomments)",
            // delete other objects related to obsolete PSIs
            "delete from trackedentitydatavalueaudit where programstageinstanceid in " + eventSelect,
            // delete other objects related to obsolete PIs
            "delete from programmessage where programinstanceid in " + piSelect,
            "delete from programstageinstance where programinstanceid in " + piSelect,
            // delete other objects related to obsolete TEIs
            "delete from programmessage where trackedentityinstanceid in " + teiSelect,
            "delete from relationshipitem where trackedentityinstanceid in " + teiSelect,
            "delete from trackedentityattributevalue where trackedentityinstanceid in " + teiSelect,
            "delete from trackedentityattributevalueaudit where trackedentityinstanceid in " + teiSelect,
            "delete from trackedentityprogramowner where trackedentityinstanceid in " + teiSelect,
            "delete from programtempowner where trackedentityinstanceid in " + teiSelect,
            "delete from programtempownershipaudit where trackedentityinstanceid in " + teiSelect,
            "delete from programownershiphistory where trackedentityinstanceid in " + teiSelect,
            "delete from programinstance where trackedentityinstanceid in " + teiSelect,
            // finally delete the TEIs
            "delete from trackedentityinstance where deleted is true" };

        int result = jdbcTemplate.batchUpdate( sqlStmts )[sqlStmts.length - 1];

        if ( result > 0 )
        {
            auditHardDeletedEntity( associatedEvents, Event.class );
            auditHardDeletedEntity( associatedEnrollments, Enrollment.class );
            auditHardDeletedEntity( deletedTeiUids, TrackedEntityInstance.class );
        }

        return result;
    }

    /**
     * This SQL needs to consider any other table that has a foreign key
     * reference to the period table. Following the DHIS2 convention this should
     * mean they have a column named {@code periodid}. When such tables are
     * dropped they need to be removed from the union here. When such new tables
     * are added they need to be added to the union here.
     */
    @Override
    public void prunePeriods()
    {
        String sql = "with period_ids(periodid) as ("
            + "    select distinct periodid from completedatasetregistration"
            + "    union select distinct periodid from dataapproval"
            + "    union select distinct periodid from dataapprovalaudit"
            + "    union select distinct periodid from datainputperiod"
            + "    union select distinct periodid from datavalue"
            + "    union select distinct periodid from datavalueaudit"
            + "    union select distinct periodid from eventchart_periods"
            + "    union select distinct periodid from eventreport_periods"
            + "    union select distinct periodid from eventvisualization_periods"
            + "    union select distinct periodid from interpretation where periodid is not null"
            + "    union select distinct periodid from lockexception where periodid is not null"
            + "    union select distinct periodid from mapview_periods"
            + "    union select distinct periodid from validationresult where periodid is not null"
            + "    union select distinct periodid from visualization_periods)"
            + "delete from period where periodid not in ("
            + "    select periodid from period_ids)";
        jdbcTemplate.batchUpdate( sql );
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

    private void auditHardDeletedEntity( List<String> deletedEntities, Class<? extends SoftDeletableObject> entity )
    {
        if ( deletedEntities == null || deletedEntities.isEmpty() )
        {
            return;
        }
        deletedEntities.forEach( deletedEntity -> {

            SoftDeletableObject object = ENTITY_MAPPER.getOrDefault( entity, new SoftDeletableObject() );

            object.setUid( deletedEntity );
            object.setDeleted( true );
            auditManager.send( Audit.builder()
                .auditType( AuditType.DELETE )
                .auditScope( AuditScope.TRACKER )
                .createdAt( LocalDateTime.now() )
                .object( object )
                .uid( deletedEntity )
                .auditableEntity( new AuditableEntity( entity, object ) )
                .build() );
        } );
    }
}
