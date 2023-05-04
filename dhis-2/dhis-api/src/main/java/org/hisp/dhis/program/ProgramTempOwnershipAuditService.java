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
package org.hisp.dhis.program;

import java.util.List;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
public interface ProgramTempOwnershipAuditService
{

    String ID = ProgramTempOwnershipAuditService.class.getName();

    /**
     * Adds program temp ownership audit
     *
     * @param programTempOwnershipAudit the audit to add
     */
    void addProgramTempOwnershipAudit( ProgramTempOwnershipAudit programTempOwnershipAudit );

    /**
     * Deletes program temp ownership audit for the given enrollment
     *
     * @param program the program
     */
    void deleteProgramTempOwnershipAudit( Program program );

    /**
     * Returns program temp ownership audits matching query params
     *
     * @param params program temp ownership audit query params
     * @return matching ProgramTempOwnershipAuditQueryParams
     */
    List<ProgramTempOwnershipAudit> getProgramTempOwnershipAudits( ProgramTempOwnershipAuditQueryParams params );

    /**
     * Returns count of program temp ownership audits matching query params
     *
     * @param params program temp ownership audit query params
     * @return count of ProgramTempOwnershipAuditQueryParams
     */
    int getProgramTempOwnershipAuditsCount( ProgramTempOwnershipAuditQueryParams params );

}
