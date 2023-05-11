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
package org.hisp.dhis.dxf2.events.event;

import static java.util.stream.Collectors.toList;

import java.sql.PreparedStatement;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcEventCommentStore implements EventCommentStore
{

    private final JdbcTemplate jdbcTemplate;

    private final String INSERT_EVENT_NOTE_SQL = "INSERT INTO TRACKEDENTITYCOMMENT (trackedentitycommentid, " + // 0
        "uid, " + // 1
        "commenttext, " + // 2
        "created, " + // 3
        "creator," + // 4
        "lastUpdated" + // 5
        ")  values ( nextval('hibernate_sequence'), ?, ?, ?, ?, ?)";

    private final static String INSERT_EVENT_COMMENT_LINK = "INSERT INTO programstageinstancecomments (programstageinstanceid, "
        + "sort_order, trackedentitycommentid) values (?, ?, ?)";

    /**
     * Save all the comments ({@see TrackedEntityComment} for the list of
     * {@see Event}
     *
     * @param batch a List of {@see Event}
     */
    public void saveAllComments( List<Event> batch )
    {
        try
        {
            // List of PSI that has at least one non empty comment (i.e. PSI
            // having comments
            // that can actually be saved)
            // In resulting PSI list, all comments without text are removed.
            List<Event> events = batch.stream()
                .map( this::withoutEmptyComments )
                .filter( this::hasComments )
                .collect( toList() );

            for ( Event psi : events )
            {
                Integer sortOrder = getInitialSortOrder( psi );

                for ( TrackedEntityComment comment : psi.getComments() )
                {
                    Long commentId = saveComment( comment );
                    if ( commentId != null && commentId != 0 )
                    {
                        saveCommentToEvent( psi.getId(), commentId, sortOrder );
                        sortOrder++;
                    }
                }
            }
        }
        catch ( DataAccessException dae )
        {
            log.error( "An error occurred saving a Program Stage Instance comment", dae );
            throw dae;
        }
    }

    private boolean hasComments( Event event )
    {
        return CollectionUtils.isNotEmpty( event.getComments() );
    }

    Integer getInitialSortOrder( Event psi )
    {
        if ( psi.getId() > 0 )
        {
            // if the PSI is already in the db, fetch the latest sort order for
            // the
            // notes, to avoid conflicts
            return jdbcTemplate.queryForObject(
                "select coalesce(max(sort_order) + 1, 1) from programstageinstancecomments where programstageinstanceid = "
                    + psi.getId(),
                Integer.class );
        }
        return 1;
    }

    private Event withoutEmptyComments( Event event )
    {
        event.setComments( getNonEmptyComments( event ) );
        return event;
    }

    private List<TrackedEntityComment> getNonEmptyComments( Event event )
    {
        return event.getComments().stream()
            .filter( this::hasCommentText )
            .collect( toList() );
    }

    private boolean hasCommentText( TrackedEntityComment trackedEntityComment )
    {
        return StringUtils.isNotEmpty( trackedEntityComment.getCommentText() );
    }

    Long saveComment( TrackedEntityComment comment )
    {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try
        {
            jdbcTemplate.update( connection -> {
                PreparedStatement ps = connection.prepareStatement( INSERT_EVENT_NOTE_SQL,
                    new String[] { "trackedentitycommentid" } );

                ps.setString( 1, comment.getUid() );
                ps.setString( 2, comment.getCommentText() );
                ps.setTimestamp( 3, JdbcEventSupport.toTimestamp( comment.getCreated() ) );
                ps.setString( 4, comment.getCreator() );
                ps.setTimestamp( 5, JdbcEventSupport.toTimestamp( comment.getLastUpdated() ) );

                return ps;
            }, keyHolder );
        }
        catch ( DataAccessException e )
        {
            log.error( "An error occurred saving a TrackedEntityComment", e );
            return null;
        }

        return (Long) keyHolder.getKey();
    }

    void saveCommentToEvent( Long programStageInstanceId, Long commentId, int sortOrder )
    {
        try
        {
            jdbcTemplate.update( connection -> {
                PreparedStatement ps = connection.prepareStatement( INSERT_EVENT_COMMENT_LINK );

                ps.setLong( 1, programStageInstanceId );
                ps.setInt( 2, sortOrder );
                ps.setLong( 3, commentId );

                return ps;
            } );
        }
        catch ( DataAccessException e )
        {
            log.error(
                "An error occurred saving a link between a TrackedEntityComment and an Event with primary key: "
                    + programStageInstanceId,
                e );
            throw e;
        }
    }
}
