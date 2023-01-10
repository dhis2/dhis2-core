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
package org.hisp.dhis.trackedentitycomment;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService" )
public class DefaultTrackedEntityCommentService
    implements TrackedEntityCommentService
{
    private final TrackedEntityCommentStore commentStore;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addTrackedEntityComment( TrackedEntityComment comment )
    {
        commentStore.save( comment );

        return comment.getId();
    }

    @Override
    @Transactional
    public void deleteTrackedEntityComment( TrackedEntityComment comment )
    {
        commentStore.delete( comment );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean trackedEntityCommentExists( String uid )
    {
        return commentStore.exists( uid );
    }

    @Override
    @Transactional
    public void updateTrackedEntityComment( TrackedEntityComment comment )
    {
        commentStore.update( comment );
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityComment getTrackedEntityComment( long id )
    {
        return commentStore.get( id );
    }

}
