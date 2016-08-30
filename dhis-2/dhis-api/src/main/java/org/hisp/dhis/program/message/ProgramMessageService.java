package org.hisp.dhis.program.message;

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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.sms.BatchResponseStatus;
import org.hisp.dhis.user.User;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

public interface ProgramMessageService
{
    ProgramMessageQueryParams getFromUrl( Set<String> ou, String programInstance, String programStageInstance,
        ProgramMessageStatus messageStatus, Integer page, Integer pageSize, Date afterDate, Date beforeDate );

    boolean exists( String uid );

    void hasAccess( ProgramMessageQueryParams params, User user );

    void validateQueryParameters( ProgramMessageQueryParams params );

    void validatePayload( ProgramMessage message );

    // -------------------------------------------------------------------------
    // Transport Service methods
    // -------------------------------------------------------------------------
    
    BatchResponseStatus sendMessages( List<ProgramMessage> programMessages );

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    ProgramMessage getProgramMessage( int id );

    ProgramMessage getProgramMessage( String uid );

    List<ProgramMessage> getAllProgramMessages();

    List<ProgramMessage> getProgramMessages( ProgramMessageQueryParams params );

    // -------------------------------------------------------------------------
    // Save OR Update
    // -------------------------------------------------------------------------

    int saveProgramMessage( ProgramMessage programMessage );
    
    void updateProgramMessage( ProgramMessage programMessage );

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    void deleteProgramMessage( ProgramMessage programMessage );
}
