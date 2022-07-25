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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.hooks.AnalyticalObjectObjectBundleHook;
import org.hisp.dhis.dxf2.metadata.objectbundle.hooks.IdentifiableObjectBundleHook;
import org.hisp.dhis.dxf2.metadata.objectbundle.hooks.OrganisationUnitObjectBundleHook;
import org.hisp.dhis.dxf2.metadata.objectbundle.hooks.UserObjectBundleHook;
import org.hisp.dhis.dxf2.metadata.objectbundle.hooks.VersionedObjectObjectBundleHook;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;

/**
 * Tests weather or not the {@link ObjectBundleHooks#getObjectHooks(Object)} and
 * {@link ObjectBundleHooks#getTypeImportHooks(Class)} methods assemble the
 * expected lists of {@link ObjectBundleHook}s.
 *
 * @author Jan Bernitt
 */
class ObjectBundleHooksTest
{

    private final ObjectBundleHooks hooks = new ObjectBundleHooks(
        asList( new OrganisationUnitObjectBundleHook( null, null ),
            new UserObjectBundleHook( null, null, null, null ),
            new IdentifiableObjectBundleHook( null ), new VersionedObjectObjectBundleHook(),
            new AnalyticalObjectObjectBundleHook( null ) ) );

    @Test
    void testMatchingClassBoundIsIncluded()
    {
        assertHasHooksOfType( new OrganisationUnit(), OrganisationUnitObjectBundleHook.class );
        assertHasHooksOfType( new User(), UserObjectBundleHook.class );
    }

    @Test
    void testNonMatchingClassBoundIsNotIncluded()
    {
        assertHasNotHooksOfType( new OrganisationUnit(), UserObjectBundleHook.class );
        assertHasNotHooksOfType( new User(), OrganisationUnitObjectBundleHook.class );
    }

    @Test
    void testMatchingInterfaceBoundIsIncluded()
    {
        assertHasHooksOfType( new OrganisationUnit(), IdentifiableObjectBundleHook.class,
            VersionedObjectObjectBundleHook.class );
        assertHasHooksOfType( new User(), IdentifiableObjectBundleHook.class, VersionedObjectObjectBundleHook.class );
        assertHasHooksOfType( new Visualization(), IdentifiableObjectBundleHook.class,
            VersionedObjectObjectBundleHook.class, AnalyticalObjectObjectBundleHook.class );
    }

    @Test
    void testNonMatchingInterfaceBoundIsNotIncluded()
    {
        assertHasNotHooksOfType( new OrganisationUnit(), AnalyticalObjectObjectBundleHook.class );
        assertHasNotHooksOfType( new User(), AnalyticalObjectObjectBundleHook.class );
    }

    @Test
    void testCommitHooksForObjectTypes()
    {
        assertEquals( singletonList( OrganisationUnitObjectBundleHook.class ),
            getCommitHookTypes( OrganisationUnit.class ) );
        assertEquals( singletonList( UserObjectBundleHook.class ), getCommitHookTypes( User.class ) );
        assertEquals( asList( OrganisationUnitObjectBundleHook.class, UserObjectBundleHook.class ),
            getCommitHookTypes( OrganisationUnit.class, User.class ) );
        assertEquals( asList( OrganisationUnitObjectBundleHook.class, UserObjectBundleHook.class ),
            getCommitHookTypes( Visualization.class, OrganisationUnit.class, User.class ) );
    }

    @SafeVarargs
    @SuppressWarnings( "unchecked" )
    private final <T> void assertHasNotHooksOfType( T object, Class<? extends ObjectBundleHook<?>>... expected )
    {
        assertNoMembers( hooks.getObjectHooks( object ), expected );
        assertNoMembers( hooks.getTypeImportHooks( (Class<T>) object.getClass() ), expected );
    }

    @SafeVarargs
    @SuppressWarnings( "unchecked" )
    private final <T> void assertHasHooksOfType( T object, Class<? extends ObjectBundleHook<? super T>>... expected )
    {
        assertSubset( hooks.getObjectHooks( object ), expected );
        assertSubset( hooks.getTypeImportHooks( (Class<T>) object.getClass() ), expected );
    }

    private <T> void assertSubset( List<ObjectBundleHook<? super T>> actual,
        Class<? extends ObjectBundleHook<? super T>>[] expected )
    {
        Set<Class<?>> actualClasses = actual.stream().map( Object::getClass ).collect( Collectors.toSet() );
        List<Class<? extends ObjectBundleHook<? super T>>> expectedClasses = asList( expected );
        String message = actualClasses + " did not contain all " + expectedClasses;
        assertTrue( actualClasses.containsAll( expectedClasses ), message );
    }

    private <T> void assertNoMembers( List<ObjectBundleHook<? super T>> actual,
        Class<? extends ObjectBundleHook<?>>[] expected )
    {
        Set<Class<?>> actualClasses = actual.stream().map( Object::getClass ).collect( Collectors.toSet() );
        List<Class<? extends ObjectBundleHook<?>>> expectedClasses = asList( expected );
        String message = actualClasses + " did contain at least one of " + expectedClasses;
        assertTrue( actualClasses.stream().noneMatch( expectedClasses::contains ), message );
    }

    @SafeVarargs
    private final List<? extends Class<?>> getCommitHookTypes( Class<? extends IdentifiableObject>... objectTypes )
    {
        return hooks.getCommitHooks( asList( objectTypes ) ).stream().map( Object::getClass ).collect( toList() );
    }
}
