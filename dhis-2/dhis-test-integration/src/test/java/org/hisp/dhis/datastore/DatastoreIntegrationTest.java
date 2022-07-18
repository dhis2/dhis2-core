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
package org.hisp.dhis.datastore;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.utils.JavaToJson.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the features of the {@link DatastoreService} that do require a postgres
 * database.
 * <p>
 * The test dimensions are:
 * <ul>
 * <li>filter operator</li>
 * <li>root path (.) or property path (name or a.b)</li>
 * <li>type of JSON value: numeric, text, ...</li>
 * </ul>
 *
 * @author Jan Bernitt
 */
class DatastoreIntegrationTest extends IntegrationTestBase
{
    @Autowired
    private DatastoreService datastore;

    @Override
    protected void setUpTest()
        throws Exception
    {
        datastore.deleteNamespace( "pets" );

        addEntry( "dog", toJson( false ) );
        addEntry( "bat", toJson( true ) );
        addEntry( "pidgin", toJson( 42 ) );
        addEntry( "horse", toJson( "Fury" ) );
        addEntry( "snake", toJson( (Object) null ) );

        addEntry( "mole", toJson( "" ) );
        addEntry( "duck", toJson( List.of() ) );
        addEntry( "eagle", toJson( Map.of() ) );

        addPet( "cat", "Miao", 9, List.of( "tuna", "mice", "birds" ) );
        addPet( "cow", "Muuhh", 5, List.of( "gras" ) );
        addPet( "hamster", "", 2, List.of() );
        addPet( "pig", "Oink", 6, List.of( "carrots", "potatoes" ) );
    }

    @Override
    protected void tearDownTest()
        throws Exception
    {
        datastore.deleteNamespace( "pets" );
    }

    private DatastoreEntry addEntry( String key, String value )
    {
        DatastoreEntry entry = new DatastoreEntry( "pets", key, value.replace( '\'', '"' ), false );
        datastore.addEntry( entry );
        return entry;
    }

    private DatastoreEntry addPet( String key, String name, int age, List<String> eats )
    {
        return addEntry( key,
            toJson( Map.of(
                "name", name,
                "age", age,
                "cute", true,
                "eats", eats == null ? List.of() : eats.stream().map( food -> Map.of( "name", food ) ) ) ) );
    }

    /*
     * Boolean Equality Filters
     */

    @Test
    void test_Filter_RootEq_Boolean()
    {
        assertEntries( ".:eq:true", "bat" );
    }

    @Test
    void test_Filter_RootNotEq_Boolean()
    {
        assertEntries( ".:!eq:true", "dog" );
    }

    /*
     * String Equality Filters
     */

    @Test
    void test_Filter_RootEq_String()
    {
        assertEntries( ".:eq:Fury", "horse" );
    }

    @Test
    void test_Filter_RootNotEq_String()
    {
        assertEntries( ".:!eq:Fury", "mole" );
    }

    @Test
    void test_Filter_PathEq_String()
    {
        assertEntries( "name:eq:Miao", "cat" );
    }

    @Test
    void test_Filter_PathNotEq_String()
    {
        assertEntries( "name:!eq:Miao", "cow", "hamster", "pig" );
    }

    /*
     * Numeric Filters (decided by value)
     */

    @Test
    void test_Filter_RootEq_Numeric()
    {
        assertEntries( ".:eq:42", "pidgin" );
    }

    @Test
    void test_Filter_RootNotEq_Numeric()
    {
        assertEntries( ".:!eq:13", "pidgin" );
    }

    @Test
    void test_Filter_RootLt_Numeric()
    {
        assertEntries( ".:lt:44", "pidgin" );
    }

    @Test
    void test_Filter_RootLe_Numeric()
    {
        assertEntries( ".:le:42", "pidgin" );
    }

    @Test
    void test_Filter_RootGt_Numeric()
    {
        assertEntries( ".:gt:41", "pidgin" );
    }

    @Test
    void test_Filter_RootGe_Numeric()
    {
        assertEntries( ".:ge:42", "pidgin" );
    }

    @Test
    void test_Filter_PathEq_Numeric()
    {
        assertEntries( "age:eq:9", "cat" );
    }

    @Test
    void test_Filter_PathNotEq_Numeric()
    {
        assertEntries( "age:!eq:9", "cow", "hamster", "pig" );
    }

    @Test
    void test_Filter_PathLt_Numeric()
    {
        assertEntries( "age:lt:6", "cow", "hamster" );
    }

    @Test
    void test_Filter_PathLe_Numeric()
    {
        assertEntries( "age:le:6", "cow", "hamster", "pig" );
    }

    @Test
    void test_Filter_PathGt_Numeric()
    {
        assertEntries( "age:gt:6", "cat" );
    }

    @Test
    void test_Filter_PathGe_Numeric()
    {
        assertEntries( "age:ge:6", "cat", "pig" );
    }

    /*
     * Emptiness Filters
     *
     * These match empty/non-empty objects or arrays or strings of length zero
     */

    @Test
    void test_Filter_RootEmpty()
    {
        assertEntries( ".:empty", "duck", "eagle", "mole" );
    }

    @Test
    void test_Filter_RootNotEmpty()
    {
        assertEntries( ".:!empty", "cat", "cow", "hamster", "horse", "pig" );
    }

    @Test
    void test_Filter_PathEmpty()
    {
        assertEntries( "eats:empty", "hamster" );
    }

    @Test
    void test_Filter_PathNotEmpty()
    {
        assertEntries( "eats:!empty", "cat", "cow", "pig" );
    }

    /*
     * String Pattern Filters
     *
     * We test ilike and !ilike since they represent the most complicated
     * combination. If those work it is very likely the other variants work as
     * well.
     */

    @Test
    void test_Filter_RootILike_String()
    {
        assertEntries( ".:ilike:uRy", "horse" );
    }

    @Test
    void test_Filter_RootNotILike_String()
    {
        assertEntries( ".:!ilike:ur", "mole" );
    }

    @Test
    void test_Filter_PathILike_String()
    {
        assertEntries( "name:ilike:IA", "cat" );
    }

    @Test
    void test_Filter_PathNotILike_String()
    {
        assertEntries( "name:!ilike:IA", "cow", "hamster", "pig" );
    }

    /*
     * Nullness Filters
     */

    @Test
    void test_Filter_RootNull()
    {
        assertEntries( ".:null", "snake" );
    }

    @Test
    void test_Filter_RootNotNull()
    {
        assertEntries( ".:!null",
            "bat", "cat", "cow", "dog", "duck", "eagle", "hamster", "horse", "mole", "pidgin", "pig" );
    }

    @Test
    void test_Filter_RootEqNull()
    {
        assertEntries( ".:eq:null", "snake" );
    }

    @Test
    void test_Filter_RootNotEqNull()
    {
        assertEntries( ".:!eq:null",
            "bat", "cat", "cow", "dog", "duck", "eagle", "hamster", "horse", "mole", "pidgin", "pig" );
    }

    private DatastoreQuery createQuery( String... filters )
    {
        return DatastoreQuery.builder()
            .namespace( "pets" )
            .fields( List.of() )
            .filters( DatastoreQuery.parseFilters( List.of( filters ) ) )
            .build();
    }

    private List<DatastoreFields> queryAsList( DatastoreQuery query )
    {
        return datastore.getFields( query, stream -> stream.collect( toList() ) );
    }

    private void assertEntries( String filter, String... expectedKeys )
    {
        assertEntries( List.of( expectedKeys ),
            queryAsList( createQuery( filter ) ) );
    }

    private static void assertEntries( List<String> expectedKeys, List<DatastoreFields> actual )
    {
        List<String> actualKeys = actual.stream().map( DatastoreFields::getKey ).collect( toList() );
        assertEquals( expectedKeys, actualKeys );
    }
}
