<<<<<<< HEAD
package org.hisp.dhis.trackedentitycomment;

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

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Chau Thu Tran
 */
@Service( "org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService" )
public class DefaultTrackedEntityCommentService
    implements TrackedEntityCommentService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final TrackedEntityCommentStore commentStore;

    public DefaultTrackedEntityCommentService( TrackedEntityCommentStore commentStore )
    {
        checkNotNull( commentStore );

        this.commentStore = commentStore;
    }

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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
=======
/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Service( "org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService" )
public class DefaultTrackedEntityCommentService
    implements TrackedEntityCommentService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final TrackedEntityCommentStore commentStore;

    public DefaultTrackedEntityCommentService( TrackedEntityCommentStore commentStore )
    {
        checkNotNull( commentStore );

        this.commentStore = commentStore;
    }

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
    @Transactional( readOnly = true )
    public List<String> filterExistingNotes( List<String> uids )
    {
        return this.commentStore.filterExisting( uids );
    }

    @Override
    @Transactional
    public void updateTrackedEntityComment( TrackedEntityComment comment )
    {
        commentStore.update( comment );
    }

    @Override
    @Transactional( readOnly = true )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    public TrackedEntityComment getTrackedEntityComment( long id )
    {
        return commentStore.get( id );
    }

}
