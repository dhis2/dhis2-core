package org.hisp.dhis.program.notification;

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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;

/**
 * @author Halvdan Hoem Grelland
 */
@Ignore( "Work in progress" )
public class NotificationMessageRendererTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private TrackedEntityInstanceService teiService;

    @Autowired
    private TrackedEntityAttributeService teiAttrService;

    @Autowired
    private TrackedEntityAttributeValueService teiAttrValueService;

    @Autowired
    private OrganisationUnitService orgUnitService;

    private OrganisationUnit orgUnitA;

    private TrackedEntityInstance teiA;

    private Program programA;

    private ProgramInstance programInstanceA;

    private ProgramStage programStageA;

    private ProgramStageInstance programStageInstanceA;

    private TrackedEntityAttribute teiAttrX, teiAttrY, teiAttrZ;

    private ProgramNotificationTemplate programNotificationTemplateA;

    @Override
    protected void setUpTest() throws Exception
    {
        /**
         * Orgunit
         */

        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitService.addOrganisationUnit( orgUnitA );

        /**
         * Program
         */

        programA = createProgram( 'A', Sets.newHashSet(), orgUnitA );
        programService.addProgram( programA );

        programStageA = new ProgramStage( "A", programA );
        programStageService.saveProgramStage( programStageA );

        programA.setProgramStages( Collections.singleton( programStageA ) );
        programService.updateProgram( programA );

        programInstanceA.setUid( "UID-PIA" );
        programInstanceService.addProgramInstance( programInstanceA );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, programStageA );
        programStageInstanceA.setDueDate( new Date() );
        programStageInstanceA.setUid( "UID-A" );

        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        /**
         * TEI
         */

        teiA = createTrackedEntityInstance( 'A', orgUnitA );
        teiService.addTrackedEntityInstance( teiA );

        teiAttrX = createTrackedEntityAttribute( 'X', ValueType.TEXT );
        teiAttrY = createTrackedEntityAttribute( 'Y', ValueType.TEXT );
        teiAttrZ = createTrackedEntityAttribute( 'Z', ValueType.TEXT );

        teiAttrService.addTrackedEntityAttribute( teiAttrX );
        teiAttrService.addTrackedEntityAttribute( teiAttrY );
        teiAttrService.addTrackedEntityAttribute( teiAttrZ );

        TrackedEntityAttributeValue
            teiAttrValueX = createTrackedEntityAttributeValue( 'X', teiA, teiAttrX ),
            teiAttrValueY = createTrackedEntityAttributeValue( 'Y', teiA, teiAttrY ),
            teiAttrValueZ = createTrackedEntityAttributeValue( 'Z', teiA, teiAttrZ );

        teiAttrValueService.addTrackedEntityAttributeValue( teiAttrValueX );
        teiAttrValueService.addTrackedEntityAttributeValue( teiAttrValueY );
        teiAttrValueService.addTrackedEntityAttributeValue( teiAttrValueZ );

        teiA.setTrackedEntityAttributeValues(
            new HashSet<>( Arrays.asList( teiAttrValueX, teiAttrValueY, teiAttrValueZ ) )
        );

        teiService.updateTrackedEntityInstance( teiA );

        /**
         * ProgramStageNotification
         */

        programNotificationTemplateA =
            new ProgramNotificationTemplate(
                "Some name",
                "Subject template",
                "Message template",
                NotificationTrigger.SCHEDULED_DAYS_DUE_DATE,
                NotificationRecipient.TRACKED_ENTITY_INSTANCE,
                Collections.singleton( DeliveryChannel.EMAIL ),
                -2,
                null
            );
    }

    @Test
    public void testRenderProgramStageNotification()
    {
        NotificationMessage rendered =
            NotificationMessageRenderer.render( programStageInstanceA, programNotificationTemplateA );

        assertNotNull( rendered );
        // TODO Actually check contents
    }
}
