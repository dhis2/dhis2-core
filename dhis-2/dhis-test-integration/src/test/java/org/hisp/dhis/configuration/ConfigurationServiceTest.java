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
package org.hisp.dhis.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class ConfigurationServiceTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private UserService userService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    void testConfiguration()
    {
        User userA = makeUser( "A" );
        User userB = makeUser( "B" );
        UserGroup group = new UserGroup( "UserGroupA" );
        group.getMembers().add( userA );
        group.getMembers().add( userB );
        userService.addUser( userA );
        userService.addUser( userB );
        userGroupService.addUserGroup( group );
        OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet( 'A' );
        idObjectManager.save( groupSet );
        Configuration config = configurationService.getConfiguration();
        assertNull( config.getFeedbackRecipients() );
        assertNull( config.getFacilityOrgUnitGroupSet() );
        config.setFeedbackRecipients( group );
        config.setFacilityOrgUnitGroupSet( groupSet );
        configurationService.setConfiguration( config );
        config = configurationService.getConfiguration();
        assertNotNull( config.getFeedbackRecipients() );
        assertNotNull( config.getFacilityOrgUnitGroupSet() );
        assertEquals( group, config.getFeedbackRecipients() );
        assertEquals( groupSet, config.getFacilityOrgUnitGroupSet() );
    }

    @Test
    void testCorsWhitelist()
    {
        Configuration config = configurationService.getConfiguration();
        Set<String> cors = new HashSet<>();
        cors.add( "http://localhost:3000/" );
        cors.add( "http://*.local.tld:3000/" );
        cors.add( "*.remote.tld/" );
        config.setCorsWhitelist( cors );
        configurationService.setConfiguration( config );
        assertTrue( configurationService.isCorsWhitelisted( "http://localhost:3000/" ) );
        assertTrue( configurationService.isCorsWhitelisted( "http://foobar.local.tld:3000/" ) );
        assertTrue( configurationService.isCorsWhitelisted( "http://magic.remote.tld/" ) );
        assertFalse( configurationService.isCorsWhitelisted( "http://localhost:9000/" ) );
        assertFalse( configurationService.isCorsWhitelisted( "http://another.local.tld/" ) );
        assertFalse( configurationService.isCorsWhitelisted( "http://some.other.tld/" ) );
    }
}
