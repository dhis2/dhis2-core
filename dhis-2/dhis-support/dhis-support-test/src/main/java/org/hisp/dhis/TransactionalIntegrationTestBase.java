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
package org.hisp.dhis;

import java.lang.reflect.Method;

import org.hisp.dhis.dbms.DbmsManager;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { IntegrationTestConfig.class } )
@Category( IntegrationTest.class )
<<<<<<< HEAD
@ActiveProfiles( profiles = {"test-postgres"} )
=======
@ActiveProfiles( profiles = { "test-postgres" } )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
@Transactional
public abstract class TransactionalIntegrationTestBase
    extends DhisConvenienceTest
    implements ApplicationContextAware
{
    @Autowired
    protected DbmsManager dbmsManager;

    protected ApplicationContext webApplicationContext;

    @Before
    public void before()
        throws Exception
    {
        executeStartupRoutines();
        setUpTest();
    }

    @After
    public void after()
        throws Exception
    {
        tearDownTest();

        if ( emptyDatabaseAfterTest() )
        {
            dbmsManager.emptyDatabase();
        }
    }

    private void executeStartupRoutines()
        throws Exception
    {
        String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

        if ( webApplicationContext.containsBean( id ) )
        {
            Object object = webApplicationContext.getBean( id );
            Method method = object.getClass().getMethod( "executeForTesting", new Class[0] );
            method.invoke( object, new Object[0] );
        }
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.webApplicationContext = applicationContext;
    }

    public abstract boolean emptyDatabaseAfterTest();

    /**
     * Method to override.
     */
    protected void setUpTest()
        throws Exception
    {
    }

    /**
     * Method to override.
     */
    protected void tearDownTest()
        throws Exception
    {
    }
}
