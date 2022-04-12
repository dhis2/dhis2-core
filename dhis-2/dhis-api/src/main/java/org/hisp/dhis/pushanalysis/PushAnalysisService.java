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
package org.hisp.dhis.pushanalysis;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.user.User;

/**
 * @author Stian Sandvold
 */
public interface PushAnalysisService
{
    /**
     * Returns a PushAnalysis with the given UID
     *
     * @param uid uid of the PushAnalysis
     * @return PushAnalysis
     */
    PushAnalysis getByUid( String uid );

    /**
     * Returns all PushAnalysis
     *
     * @return List of PushAnalysis
     */
    List<PushAnalysis> getAll();

    /**
     * Returns a String, consisting of HTML representing the PushAnalysis
     * report. This report is generated based on the associated Dashboard, as
     * well as the user supplied
     *
     * @param pushAnalysis PushAnalysis to generate report from
     * @param user User to base data on
     * @return String containing a HTML report
     * @throws IOException if the upload of report content failed.
     */
    String generateHtmlReport( PushAnalysis pushAnalysis, User user )
        throws IOException;

    /**
     * Used to Generate and send reports to all UserGroups assigned to the
     * PushAnalysis, using generateHtmlReport to generate the reports for each
     * individual user in the UserGroups.
     *
     * @param uids UIDs of the PushAnalysis to run
     * @param progress tracking of the processing
     */
    void runPushAnalysis( List<String> uids, JobProgress progress );
}
