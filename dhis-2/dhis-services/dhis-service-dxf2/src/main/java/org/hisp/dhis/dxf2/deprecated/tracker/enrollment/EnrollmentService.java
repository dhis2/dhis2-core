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
package org.hisp.dhis.dxf2.deprecated.tracker.enrollment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.EnrollmentParams;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 *
 * @deprecated this is a class related to "old" (deprecated) tracker which will
 *             be removed with "old" tracker. Make sure to plan migrating to new
 *             tracker.
 */
@Deprecated( since = "2.41" )
public interface EnrollmentService
{
    int FLUSH_FREQUENCY = 100;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    List<Enrollment> getEnrollmentsJson( InputStream inputStream )
        throws IOException;

    List<Enrollment> getEnrollmentsXml( InputStream inputStream )
        throws IOException;

    Enrollment getEnrollment( String id, EnrollmentParams params );

    Enrollment getEnrollment( org.hisp.dhis.program.Enrollment enrollment,
        EnrollmentParams params );

    Enrollment getEnrollment( User user,
        org.hisp.dhis.program.Enrollment enrollment, EnrollmentParams params,
        boolean skipOwnershipCheck );

    List<Enrollment> getEnrollments(
        Iterable<org.hisp.dhis.program.Enrollment> programInstances );

    Enrollments getEnrollments( EnrollmentQueryParams params );

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    ImportSummaries addEnrollmentList( List<Enrollment> enrollments,
        ImportOptions importOptions );

    ImportSummaries addEnrollmentsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummaries addEnrollmentsXml( InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummaries addEnrollments( List<Enrollment> enrollments,
        ImportOptions importOptions, boolean clearSession );

    ImportSummaries addEnrollments( List<Enrollment> enrollments,
        ImportOptions importOptions, JobConfiguration jobId );

    ImportSummaries addEnrollments( List<Enrollment> enrollments,
        ImportOptions importOptions,
        TrackedEntity trackedEntity, boolean clearSession );

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    ImportSummary updateEnrollmentJson( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummary updateEnrollmentForNoteJson( String id, InputStream inputStream )
        throws IOException;

    ImportSummary updateEnrollmentXml( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummary addEnrollment( Enrollment enrollment,
        ImportOptions importOptions );

    ImportSummary addEnrollment( Enrollment enrollment,
        ImportOptions importOptions,
        TrackedEntity daoTrackedEntity );

    ImportSummaries updateEnrollments( List<Enrollment> enrollments,
        ImportOptions importOptions,
        boolean clearSession );

    ImportSummary updateEnrollment( Enrollment enrollment,
        ImportOptions importOptions );

    ImportSummary updateEnrollmentForNote( Enrollment enrollment );

    void cancelEnrollment( String uid );

    void completeEnrollment( String uid );

    void incompleteEnrollment( String uid );

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    ImportSummary deleteEnrollment( String uid );

    ImportSummaries deleteEnrollments( List<Enrollment> enrollments,
        ImportOptions importOptions,
        boolean clearSession );
}
