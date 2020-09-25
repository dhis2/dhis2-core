package org.hisp.dhis.trackedentity;

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

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author David Katuscak
 */
public class TrackedEntityAttributeServiceTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackedEntityAttributeStore trackedEntityAttributeStore;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private ProgramService programService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AclService aclService;

    @Mock
    private TrackedEntityAttributeStore attributeStore;

    @Mock
    private FileResourceService fileResourceService;

    @Mock
    private UserService userService;

    @Mock
    private TrackedEntityTypeAttributeStore entityTypeAttributeStore;

    @Mock
    private ProgramTrackedEntityAttributeStore programAttributeStore;

    private TrackedEntityAttributeService trackedEntityAttributeService;

    private TrackedEntityInstance teiPassedInPayload;

    private final String identicalTeiUid = "TeiUid12345";

    private final String differentTeiUid = "TeiUid54321";

    private OrganisationUnit orgUnit;

    private TrackedEntityAttribute tea;

    @Before
    public void setUp()
    {
        trackedEntityAttributeService = new DefaultTrackedEntityAttributeService( attributeStore, programService,
            trackedEntityTypeService, fileResourceService, userService, currentUserService, aclService,
            trackedEntityAttributeStore, entityTypeAttributeStore, programAttributeStore );

        orgUnit = new OrganisationUnit( "orgUnitA" );

        teiPassedInPayload = new TrackedEntityInstance();
        teiPassedInPayload.setUid( identicalTeiUid );
        teiPassedInPayload.setOrganisationUnit( orgUnit );

        tea = new TrackedEntityAttribute();
        tea.setUid( "TeaUid12345" );
        tea.setUnique( true );
        tea.setValueType( ValueType.TEXT );
        tea.setOrgunitScope( false );
    }

    @Test
    public void identicalTeiWithTheSameUniqueAttributeExistsInSystem()
    {
        when( trackedEntityAttributeStore
            .getTrackedEntityInstanceUidWithUniqueAttributeValue( any( TrackedEntityInstanceQueryParams.class ) ) )
            .thenReturn( Optional.of( identicalTeiUid ) );

        String teaValue = "Firstname";

        String result = trackedEntityAttributeService.validateAttributeUniquenessWithinScope( tea, teaValue, teiPassedInPayload, orgUnit );
        assertNull( result );
    }

    @Test
    public void differentTeiWithTheSameUniqueAttributeExistsInSystem()
    {
        when( trackedEntityAttributeStore
            .getTrackedEntityInstanceUidWithUniqueAttributeValue( any( TrackedEntityInstanceQueryParams.class ) ) )
            .thenReturn( Optional.of( differentTeiUid ) );

        String teaValue = "Firstname";

        String result = trackedEntityAttributeService.validateAttributeUniquenessWithinScope( tea, teaValue, teiPassedInPayload, orgUnit );
        assertNotNull( result );
    }

    @Test
    public void attributeIsUniqueWithinTheSystem()
    {
        when( trackedEntityAttributeStore
            .getTrackedEntityInstanceUidWithUniqueAttributeValue( any( TrackedEntityInstanceQueryParams.class ) ) )
            .thenReturn( Optional.empty() );

        String teaValue = "Firstname";

        String result = trackedEntityAttributeService.validateAttributeUniquenessWithinScope( tea, teaValue, teiPassedInPayload, orgUnit );
        assertNull( result );
    }

    @Test
    public void wrongValueToValueType()
    {
        tea.setValueType( ValueType.NUMBER );
        String teaValue = "Firstname";

        String result = trackedEntityAttributeService.validateValueType( tea, teaValue );
        assertNotNull( result );

        tea.setValueType( ValueType.BOOLEAN );
        result = trackedEntityAttributeService.validateValueType( tea, teaValue );
        assertNotNull( result );
    }

    @Test( expected = IllegalArgumentException.class )
    public void wrongValueToDateValueType()
    {
        tea.setValueType( ValueType.DATE );
        String teaValue = "Firstname";
        trackedEntityAttributeService.validateValueType( tea, teaValue );
    }

    @Test
    public void correctValueToValueType()
    {
        String teaValue = "Firstname";
        tea.setValueType( ValueType.TEXT );

        String result = trackedEntityAttributeService.validateValueType( tea, teaValue );
        assertNull( result );

        tea.setValueType( ValueType.NUMBER );
        teaValue = "123";
        result = trackedEntityAttributeService.validateValueType( tea, teaValue );
        assertNull( result );

        tea.setValueType( ValueType.BOOLEAN );
        teaValue = String.valueOf( true );
        result = trackedEntityAttributeService.validateValueType( tea, teaValue );
        assertNull( result );

        tea.setValueType( ValueType.DATE );
        teaValue = "2019-01-01";
        result = trackedEntityAttributeService.validateValueType( tea, teaValue );
        assertNull( result );
    }
}