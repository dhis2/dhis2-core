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
package org.hisp.dhis.test.integration;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.hisp.dhis.BaseSpringTest;
import org.hisp.dhis.config.IntegrationTestConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@ContextConfiguration( classes = { IntegrationTestConfig.class } )
@IntegrationTest
@ActiveProfiles( profiles = { "test-postgres" } )
@Transactional
@Slf4j
// TODO lets not catch exceptions if we fail to clean up. lets fail and fix
// these issues when they occur
// TODO reduce visibility of methods
// TODO add javadoc explaining when this class should be used and refer to other
// class in case it should not be used
public abstract class TransactionalIntegrationTest extends BaseSpringTest
{
    @BeforeEach
    public final void before()
        throws Exception
    {
        // TODO can we run startup routines only on BeforeEach/BeforeAll for all
        // tests? This would simplify test setup
        TestUtils.executeStartupRoutines( applicationContext );

        // TODO dislike messing with logging in code; can we move this into the
        // log4j2-test.xml?
        boolean enableQueryLogging = dhisConfigurationProvider.isEnabled( ConfigurationKey.ENABLE_QUERY_LOGGING );
        // Enable to query logger to log only what's happening inside the test
        // method
        if ( enableQueryLogging )
        {
            Configurator.setLevel( ORG_HISP_DHIS_DATASOURCE_QUERY, Level.INFO );
            Configurator.setRootLevel( Level.INFO );
        }
    }

    @AfterEach
    public final void after()
    {
        clearSecurityContext();
        try
        {
            dbmsManager.clearSession();
        }
        catch ( Exception e )
        {
            log.info( "Failed to clear hibernate session, reason:" + e.getMessage() );
        }
    }

    // TODO add docs to why this is necessary. In case a TransactionalTest uses
    // @BeforeAll on a method this method
    // will not run in a transaction so we need to make sure to clean that up.
    // As devs will likely forget I think it
    // would be good to do that for them.
    @AfterAll
    public final void afterAll()
    {
        try
        {
            dbmsManager.emptyDatabase();
        }
        catch ( Exception e )
        {
            log.info( "Failed to empty db, reason:" + e.getMessage() );
        }
    }
}
