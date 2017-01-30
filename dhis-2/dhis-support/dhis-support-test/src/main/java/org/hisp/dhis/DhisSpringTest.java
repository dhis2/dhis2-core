package org.hisp.dhis;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * @author Trygve Laugst&oslash;l
 * @author Lars Helge Overland
 */
@RunWith( SpringRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/dhis/beans.xml", "classpath*:/META-INF/dhis/security.xml" } )
@Transactional
public abstract class DhisSpringTest
    extends DhisConvenienceTest
{
    // -------------------------------------------------------------------------
    // ApplicationContextAware implementation
    // -------------------------------------------------------------------------

    @Autowired
    protected ApplicationContext context;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public final void before()
        throws Exception
    {
        executeStartupRoutines();

        setUpTest();
    }

    /**
     * Method to override.
     */
    protected void setUpTest()
        throws Exception
    {
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    /**
     * Retrieves a bean from the application context.
     *
     * @param beanId the identifier of the bean.
     */
    protected Object getBean( String beanId )
    {
        return context.getBean( beanId );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void executeStartupRoutines()
        throws Exception
    {
        String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

        if ( context != null && context.containsBean( id ) )
        {
            Object object = context.getBean( id );

            Method method = object.getClass().getMethod( "executeForTesting", new Class[0] );

            method.invoke( object, new Object[0] );
        }
    }
}
