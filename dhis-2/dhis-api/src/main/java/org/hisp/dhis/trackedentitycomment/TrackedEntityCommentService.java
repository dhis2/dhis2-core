package org.hisp.dhis.trackedentitycomment;

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


/**
 * @author Chau Thu Tran
 * 
 * @version TrackedEntityCommentService.java 9:27:31 AM Aug 17, 2012 $
 */
public interface TrackedEntityCommentService
{
    String ID = TrackedEntityCommentService.class.getName();

    /**
     * Adds an {@link TrackedEntityComment}
     * 
     * @param comment The to TrackedEntityComment add.
     * 
     * @return A generated unique id of the added {@link TrackedEntityComment}.
     */
    int addTrackedEntityComment( TrackedEntityComment comment );

    /**
     * Deletes a {@link TrackedEntityComment}.
     * 
     * @param comment the TrackedEntityComment to delete.
     */
    void deleteTrackedEntityComment( TrackedEntityComment comment );

    /**
     * Updates an {@link TrackedEntityComment}.
     * 
     * @param comment the TrackedEntityComment to update.
     */
    void updateTrackedEntityComment( TrackedEntityComment comment );

    /**
     * Returns a {@link TrackedEntityComment}.
     * 
     * @param id the id of the TrackedEntityComment to return.
     * 
     * @return the TrackedEntityComment with the given id
     */
    TrackedEntityComment getTrackedEntityComment( int id );

}
