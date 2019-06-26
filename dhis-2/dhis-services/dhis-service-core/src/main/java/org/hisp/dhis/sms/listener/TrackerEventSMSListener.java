package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrackerEventSMSListener
    extends
    NewSMSListener
{

    @Autowired
    private UserService userService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CategoryService categoryService;

    @Override
    protected SMSResponse postProcess( IncomingSms sms, SMSSubmission submission )
        throws SMSProcessingException
    {
        TrackerEventSMSSubmission subm = (TrackerEventSMSSubmission) submission;

        String ouid = subm.getOrgUnit();
        String stageid = subm.getProgramStage();
        String teiid = subm.getTrackedEntityInstance();
        String aocid = subm.getAttributeOptionCombo();

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ouid );
        User user = userService.getUser( subm.getUserID() );

        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( teiid );
        if ( tei == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_TEI.set( teiid ) );
        }

        ProgramStage programStage = programStageService.getProgramStage( stageid );
        if ( programStage == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_STAGE.set( teiid ) );
        }

        CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo( aocid );
        if ( aoc == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_AOC.set( aocid ) );
        }

        Optional<ProgramInstance> res = tei.getProgramInstances().stream()
            .filter( pi -> pi.getStatus() == ProgramStatus.ACTIVE )
            .filter( pi -> pi.getProgram().getProgramStages().contains( programStage ) ).findFirst();

        if ( !res.isPresent() )
        {
            throw new SMSProcessingException( SMSResponse.NO_ENROLL.set( teiid, stageid ) );
        }
        ProgramInstance programInstance = res.get();

        List<String> errorUIDs = saveNewEvent( subm.getEvent(), orgUnit, programStage, programInstance, sms, aoc, user,
            subm.getValues() );
        if ( !errorUIDs.isEmpty() )
        {
            return SMSResponse.WARN_DVERR.set( errorUIDs );
        }
        else if ( subm.getValues().isEmpty() )
        {
            // TODO: Should we save the event if there are no data values?
            return SMSResponse.WARN_DVEMPTY;
        }

        return SMSResponse.SUCCESS;
    }

    @Override
    protected boolean handlesType( SubmissionType type )
    {
        return (type == SubmissionType.TRACKER_EVENT);
    }

}
