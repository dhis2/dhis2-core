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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This tests the {@link RelativePropertyContext}. We use an integration test,
 * so we can use DHIS2 model to test the {@link RelativePropertyContext} with.
 *
 * @author Jan Bernitt
 */
class RelativePropertyContextTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private SchemaService schemaService;

    @Test
    void testResolve_DirectProperty()
    {
        assertPropertyDoesExist( UserGroup.class, "users", User.class );
        assertPropertyDoesExist( UserGroup.class, "managedGroups", UserGroup.class );
        assertPropertyDoesExist( OrganisationUnit.class, "ancestors", OrganisationUnit.class );
        assertPropertyDoesExist( OrganisationUnit.class, "level", Integer.class );
        assertPropertyDoesNotExist( UserGroup.class, "members" );
    }

    @Test
    void testResolve_ChildProperty()
    {
        assertPropertyDoesExist( UserGroup.class, "users.userGroups", UserGroup.class );
        assertPropertyDoesExist( UserGroup.class, "users.organisationUnits", OrganisationUnit.class );
        assertPropertyDoesExist( UserGroup.class, "users.whatsApp", String.class );
        assertPropertyDoesNotExist( OrganisationUnit.class, "ancestors.pony" );
        assertPropertyDoesNotExist( OrganisationUnit.class, "pony.ancestors" );
    }

    @Test
    void testResolve_GrantChildProperty()
    {
        assertPropertyDoesExist( UserGroup.class, "users.userGroups.users", User.class );
        assertPropertyDoesExist( UserGroup.class, "users.organisationUnits.level", Integer.class );
        assertPropertyDoesExist( UserGroup.class, "users.dataViewOrganisationUnits.ancestors", OrganisationUnit.class );
        assertPropertyDoesNotExist( OrganisationUnit.class, "ancestors.ancestors.pony" );
        assertPropertyDoesNotExist( OrganisationUnit.class, "pony.ancestors.ancestors" );
    }

    private void assertPropertyDoesExist( Class<?> type, String path, Class<?> expectedType )
    {
        RelativePropertyContext context = new RelativePropertyContext( type, schemaService::getDynamicSchema );
        // test "resolve"
        Property property = context.resolve( path );
        assertNotNull( property, path + " not found" );
        assertSame( expectedType, property.isCollection() ? property.getItemKlass() : property.getKlass() );
        // test "resolveMandatory"
        Property mandatory = context.resolveMandatory( path );
        assertNotNull( mandatory );
        assertSame( property, mandatory );
        // test "resolvePath"
        List<Property> elements = context.resolvePath( path );
        assertEquals( path.split( "\\." ).length, elements.size() );
        assertSame( property, elements.get( elements.size() - 1 ) );
        assertEquals( path, elements.stream().map( Property::key ).collect( joining( "." ) ) );
    }

    private void assertPropertyDoesNotExist( Class<?> type, String path )
    {
        RelativePropertyContext context = new RelativePropertyContext( type, schemaService::getDynamicSchema );
        assertNull( context.resolve( path ) );
        assertThrows( NoSuchElementException.class, () -> context.resolveMandatory( path ) );
    }
}
