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
package org.hisp.dhis;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.helpers.extensions.AnalyticsSetupExtension;
import org.hisp.dhis.helpers.extensions.ConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.restassured.http.ContentType;

/**
 * This is the base class responsible for enabling analytics e2e tests. It
 * assumes that there is a DHIS2 instance up and running, so the analytics table
 * generation can take place on the respective instance.
 *
 * Note that this class is @tagged as "analytics-api". Any test that extends
 * this class will automatically execute as part of this group.
 *
 * Also, all tests are expected to execute under the default timeout, which is
 * defined in this class. This value can be overridden at test level when
 * required. The timeout check can be enabled/disabled depending on the
 * situation.
 *
 * ie.: mvn -Djunit.jupiter.execution.timeout.mode=disabled test
 *
 * @author maikel arabori
 */
@TestInstance( PER_CLASS )
@ExtendWith( ConfigurationExtension.class )
@ExtendWith( AnalyticsSetupExtension.class )
@Timeout( AnalyticsApiTest.DEFAULT_LIMIT_EXECUTION_TIME_IN_SECONDS )
@Tag( "analytics-api" )
public abstract class AnalyticsApiTest
{

    protected static final int DEFAULT_LIMIT_EXECUTION_TIME_IN_SECONDS = 2;

    protected final String JSON = ContentType.JSON.toString();

    @BeforeAll
    public void beforeAll()
    {
        login();
    }

    protected void login()
    {
        new LoginActions().loginAsAdmin();
    }
}
