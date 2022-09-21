/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
<<<<<<< HEAD

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleServiceUserTest
    extends DhisSpringTest
{
    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void testCreateUsers()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/users.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsByCode( UserAuthorityGroup.class, ErrorCode.E5003 ).size() );
        objectBundleService.commit( bundle );

        List<User> users = manager.getAll( User.class );
        assertEquals( 4, users.size() );

        User userA = manager.get( User.class, "sPWjoHSY03y" );
        User userB = manager.get( User.class, "MwhEJUnTHkn" );

        assertNotNull( userA );
        assertNotNull( userB );

        assertNotNull( userA.getUserCredentials().getUserInfo() );
        assertNotNull( userB.getUserCredentials().getUserInfo() );
        assertNotNull( userA.getUserCredentials().getUserInfo().getUserCredentials() );
        assertNotNull( userB.getUserCredentials().getUserInfo().getUserCredentials() );
        assertEquals( "UserA", userA.getUserCredentials().getUserInfo().getUserCredentials().getUsername() );
        assertEquals( "UserB", userB.getUserCredentials().getUserInfo().getUserCredentials().getUsername() );

        assertNotNull( userA.getUserCredentials().getUser() );
        assertNotNull( userB.getUserCredentials().getUser() );
        assertNotNull( userA.getUserCredentials().getUser().getUserCredentials() );
        assertNotNull( userB.getUserCredentials().getUser().getUserCredentials() );
        assertEquals( "admin", userA.getUserCredentials().getUser().getUserCredentials().getUsername() );
        assertEquals( "admin", userB.getUserCredentials().getUser().getUserCredentials().getUsername() );

        assertEquals( 1, userA.getOrganisationUnits().size() );
        assertEquals( 1, userB.getOrganisationUnits().size() );
    }

    @Test
    public void testUpdateUsers()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/users.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsByCode( UserAuthorityGroup.class, ErrorCode.E5003 ).size() );
        objectBundleService.commit( bundle );

        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/users_update.json" ).getInputStream(),
            RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsByCode( UserAuthorityGroup.class, ErrorCode.E5001 ).size() );
        objectBundleService.commit( bundle );

        List<User> users = manager.getAll( User.class );
        assertEquals( 4, users.size() );

        User userA = manager.get( User.class, "sPWjoHSY03y" );
        User userB = manager.get( User.class, "MwhEJUnTHkn" );

        assertNotNull( userA );
        assertNotNull( userB );

        assertNotNull( userA.getUserCredentials().getUserInfo() );
        assertNotNull( userB.getUserCredentials().getUserInfo() );
        assertNotNull( userA.getUserCredentials().getUserInfo().getUserCredentials() );
        assertNotNull( userB.getUserCredentials().getUserInfo().getUserCredentials() );
        assertEquals( "UserAA", userA.getUserCredentials().getUserInfo().getUserCredentials().getUsername() );
        assertEquals( "UserBB", userB.getUserCredentials().getUserInfo().getUserCredentials().getUsername() );

        assertNotNull( userA.getUserCredentials().getUser() );
        assertNotNull( userB.getUserCredentials().getUser() );
        assertNotNull( userA.getUserCredentials().getUser().getUserCredentials() );
        assertNotNull( userB.getUserCredentials().getUser().getUserCredentials() );
        assertEquals( "admin", userA.getUserCredentials().getUser().getUserCredentials().getUsername() );
        assertEquals( "admin", userB.getUserCredentials().getUser().getUserCredentials().getUsername() );
    }

    @Test
    public void testCreateMetadataWithDuplicateUsername()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/user_duplicate_username.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );

        assertEquals( 1, manager.getAll( User.class ).size() );
    }

    @Test
    public void testCreateMetadataWithDuplicateUsernameAndInjectedUser()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/user_duplicate_username.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );

        objectBundleService.commit( bundle );
        assertEquals( 2, manager.getAll( User.class ).size() );
    }

    @Test
    public void testUpdateAdminUser()
        throws IOException
    {
        createAndInjectAdminUser();

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/user_admin.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setAtomicMode( AtomicMode.ALL );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        assertEquals( 0, objectBundleValidationService.validate( bundle ).getErrorReports().size() );
    }

    @Test
    public void testCreateUsersWithInvalidPasswords()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/users_passwords.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsByCode( User.class, ErrorCode.E4005 ).size() );
    }

    @Test
    public void testUpdateUserWithNoAccessUserRole()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/user_userrole.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );

        objectBundleService.commit( bundle );

        User userB = manager.get( User.class, "MwhEJUnTHkn" );
        User userA = manager.get( User.class, "sPWjoHSY03y" );

        assertEquals( 2, userA.getUserCredentials().getUserAuthorityGroups().size() );
        assertEquals( 2, userB.getUserCredentials().getUserAuthorityGroups().size() );

        UserAuthorityGroup userManagerRole = manager.get( UserAuthorityGroup.class, "xJZBzAHI88H" );
<<<<<<< HEAD
        assertNotNull(  userManagerRole );
        userManagerRole.getUserAccesses().clear();
        userManagerRole.getUserAccesses().add( new UserAccess( userB, "rw------" ) );
        userManagerRole.setPublicAccess( "--------" );
        userManagerRole.setUser( userB );
        manager.update( userManagerRole );

        SecurityContextHolder.clearContext();
        userA.getUserCredentials().setPassword( "passwordUserA" );
        manager.update( userA );
        injectSecurityContext( userA );

       metadata = renderService.fromMetadata(
=======
        assertNotNull( userManagerRole );
        userManagerRole.getUserAccesses().clear();
        userManagerRole.getUserAccesses().add( new UserAccess( userB, "rw------" ) );
        userManagerRole.setPublicAccess( "--------" );
        userManagerRole.setUser( userB );
        manager.update( userManagerRole );

        SecurityContextHolder.clearContext();
        userA.getUserCredentials().setPassword( "passwordUserA" );
        manager.update( userA );
        injectSecurityContext( userA );

        metadata = renderService.fromMetadata(
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
            new ClassPathResource( "dxf2/user_userrole_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        objectBundleService.commit( bundle );

        assertEquals( 2, userA.getUserCredentials().getUserAuthorityGroups().size() );
        assertEquals( 2, userB.getUserCredentials().getUserAuthorityGroups().size() );

    }
}
