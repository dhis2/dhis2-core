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
package org.hisp.dhis.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataexchange.analytics.AnalyticsDataExchange;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class SchemaServiceTest extends DhisSpringTest
{
    @Autowired
    private SchemaService schemaService;

    @Test
    void testHaveSchemas()
    {
        assertFalse( schemaService.getSchemas().isEmpty() );
    }

    @Test
    void testOrganisationUnit()
    {
        Schema schema = schemaService.getSchema( OrganisationUnit.class );
        assertNotNull( schema );
        assertNotNull( schema.getFieldNameMapProperties() );
    }

    @Test
    void testProgramTrackedEntityAttribute()
    {
        Schema schema = schemaService.getSchema( ProgramTrackedEntityAttribute.class );
        assertNotNull( schema );
        Property groups = schema.getProperty( "programTrackedEntityAttributeGroups" );
        assertNotNull( groups );
        assertFalse( groups.isSimple() );
        assertTrue( groups.isCollection() );
    }

    @Test
    void testGetSchema()
    {
        Schema schema = schemaService.getSchema( AnalyticsDataExchange.class );
        assertNotNull( schema );
        assertEquals( AnalyticsDataExchange.class, schema.getKlass() );
    }

    @Test
    void testGetSchemaByPluralName()
    {
        Schema schema = schemaService.getSchemaByPluralName( "analyticsDataExchanges" );
        assertNotNull( schema );
        assertEquals( AnalyticsDataExchange.class, schema.getKlass() );
    }

    @Test
    void testSqlViewSchema()
    {
        Schema schema = schemaService.getSchema( SqlView.class );
        assertNotNull( schema );
        assertFalse( schema.isDataWriteShareable() );
    }

    @Test
    void testProgramSchema()
    {
        Schema schema = schemaService.getSchema( Program.class );
        assertNotNull( schema );
        assertTrue( schema.isDataShareable() );
        assertTrue( schema.isDataWriteShareable() );
        assertTrue( schema.isDataReadShareable() );
    }

    @Test
    void testCanScanJobParameters()
    {
        JobParameters parameters = new AnalyticsJobParameters( 10, new HashSet<>(), new HashSet<>(), true );
        Schema schema = schemaService.getDynamicSchema( parameters.getClass() );

        assertNotNull( schema );
        assertFalse( schema.getProperties().isEmpty() );
        assertEquals( 4, schema.getProperties().size() );
    }

    @Test
    void testCanScanJobConfigurationWithJobParameters()
    {
        JobConfiguration configuration = new JobConfiguration();
        configuration.setJobParameters( new AnalyticsJobParameters( 10, new HashSet<>(), new HashSet<>(), true ) );

        Schema schema = schemaService.getDynamicSchema( configuration.getClass() );
        assertNotNull( schema );
        assertFalse( schema.getProperties().isEmpty() );

        Property property = schema.getProperty( "jobParameters" );
        assertNotNull( property );

        Object jobParameters = ReflectionUtils.invokeMethod( configuration, property.getGetterMethod() );
        assertNotNull( jobParameters );

        schema = schemaService.getDynamicSchema( jobParameters.getClass() );

        assertNotNull( schema );
        assertFalse( schema.getProperties().isEmpty() );
        assertEquals( 4, schema.getProperties().size() );
    }
}
