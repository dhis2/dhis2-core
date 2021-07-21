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
package org.hisp.dhis.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.sqlview.SqlView;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class SchemaServiceTest
    extends DhisSpringTest
{
    @Autowired
    private SchemaService schemaService;

    @Test
    public void testHaveSchemas()
    {
        assertFalse( schemaService.getSchemas().isEmpty() );
    }

    @Test
    public void testOrganisationUnit()
    {
        Schema schema = schemaService.getSchema( OrganisationUnit.class );
        assertNotNull( schema );
        assertNotNull( schema.getFieldNameMapProperties() );
    }

    @Test
    public void testProgramTrackedEntityAttribute()
    {
        Schema schema = schemaService.getSchema( ProgramTrackedEntityAttribute.class );
        assertNotNull( schema );
        Property groups = schema.getProperty( "programTrackedEntityAttributeGroups" );
        assertNotNull( groups );
        assertFalse( groups.isSimple() );
        assertTrue( groups.isCollection() );
    }

    @Test
    public void testSqlViewSchema()
    {
        Schema schema = schemaService.getSchema( SqlView.class );
        assertNotNull( schema );
        assertFalse( schema.isDataWriteShareable() );
    }

    @Test
    public void testProgramSchema()
    {
        Schema schema = schemaService.getSchema( Program.class );
        assertNotNull( schema );
        assertTrue( schema.isDataShareable() );
        assertTrue( schema.isDataWriteShareable() );
        assertTrue( schema.isDataReadShareable() );
    }
}
