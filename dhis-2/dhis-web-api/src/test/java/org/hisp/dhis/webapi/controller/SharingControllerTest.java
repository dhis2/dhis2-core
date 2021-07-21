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
package org.hisp.dhis.webapi.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link SharingController}.
 *
 * @author Volker Schmidt
 */
public class SharingControllerTest
{
    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private AclService aclService;

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private MockHttpServletResponse response = new MockHttpServletResponse();

    @InjectMocks
    private SharingController sharingController;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test( expected = AccessDeniedException.class )
    public void notSystemDefaultMetadataNoAccess()
        throws Exception
    {
        final OrganisationUnit organisationUnit = new OrganisationUnit();

        doReturn( OrganisationUnit.class ).when( aclService ).classForType( eq( "organisationUnit" ) );
        when( aclService.isClassShareable( eq( OrganisationUnit.class ) ) ).thenReturn( true );
        doReturn( organisationUnit ).when( manager ).get( eq( OrganisationUnit.class ), eq( "kkSjhdhks" ) );

        sharingController.postSharing( "organisationUnit", "kkSjhdhks", response, request );
    }

    @Test( expected = AccessDeniedException.class )
    public void systemDefaultMetadataNoAccess()
        throws Exception
    {
        final Category category = new Category();
        category.setName( Category.DEFAULT_NAME + "x" );

        doReturn( Category.class ).when( aclService ).classForType( eq( "category" ) );
        when( aclService.isClassShareable( eq( Category.class ) ) ).thenReturn( true );
        when( manager.get( eq( Category.class ), eq( "kkSjhdhks" ) ) ).thenReturn( category );

        sharingController.postSharing( "category", "kkSjhdhks", response, request );
    }

    @Test( expected = WebMessageException.class )
    public void systemDefaultMetadata()
        throws Exception
    {
        final Category category = new Category();
        category.setName( Category.DEFAULT_NAME );

        doReturn( Category.class ).when( aclService ).classForType( eq( "category" ) );
        when( aclService.isClassShareable( eq( Category.class ) ) ).thenReturn( true );
        when( manager.get( eq( Category.class ), eq( "kkSjhdhks" ) ) ).thenReturn( category );

        try
        {
            sharingController.postSharing( "category", "kkSjhdhks", response, request );
        }
        catch ( WebMessageException e )
        {
            assertThat( e.getWebMessage().getMessage(),
                containsString( "Sharing settings of system default metadata object" ) );
            throw e;
        }
    }
}
