package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.domain.Note;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Luciano Fiandesio
 */
public class NoteValidationUtils {

    private NoteValidationUtils()
    {
    }

    /**
     * Filters out from a List of {@see Note}, notes that have no value (empty note)
     * and notes with an uid that already exist in the database.
     *
     * @param commentService an instance of {@see TrackedEntityCommentService},
     *        required to check if a note uid already exist in the db
     * @param notes the list of {@see Note} to filter
     * @return a filtered list of {@see Note}}
     */
    static List<Note> getPersistableNotes( TrackedEntityCommentService commentService, List<Note> notes )
    {
        // Check which notes are already on the DB and skip them
        // Only check notes that are marked NOT marked as "new note"
        // FIXME: do we really need this? Currently trackedentitycomment uid is a unique
        // key in the db, can't we simply catch the exception
        List<String> nonExistingUid = commentService.filterExistingNotes( notes.stream()
            .filter( n -> StringUtils.isNotEmpty( n.getValue() ) )
            .filter( n -> !n.isNewNote() )
            .map( Note::getNote )
            .collect( Collectors.toList() ) );

        return Streams.concat(
            notes.stream()
                .filter( Note::isNewNote )
                .filter( n -> StringUtils.isNotEmpty( n.getValue() ) ),
            notes.stream()
                .filter( n -> nonExistingUid.contains( n.getNote() ) ) )
            .collect( Collectors.toList() );
    }
}
