/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.filter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.commons.jsonfiltering.config.JsonFilteringConfig;
import org.hisp.dhis.commons.jsonfiltering.context.provider.JsonFilteringContextProvider;
import org.hisp.dhis.commons.jsonfiltering.model.Issue;
import org.hisp.dhis.commons.jsonfiltering.model.IssueAction;
import org.hisp.dhis.commons.jsonfiltering.model.Item;
import org.hisp.dhis.commons.jsonfiltering.model.Outer;
import org.hisp.dhis.commons.jsonfiltering.model.User;
import org.hisp.dhis.commons.jsonfiltering.parser.JsonFilteringParser;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Charsets;

@SuppressWarnings( "Duplicates" )
public class JsonFilteringPropertyFilterTest
{

    public static final String BASE_PATH = "org/hisp/dhis/commons/jsonfiltering";

    private Issue issue;

    private ObjectMapper objectMapper;

    private SimpleFilterProvider filterProvider;

    private boolean init = false;

    private ObjectMapper rawObjectMapper = new ObjectMapper();

    public JsonFilteringPropertyFilterTest()
    {
        rawObjectMapper.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
    }

    private static String stringify( ObjectMapper mapper, Object object )
    {
        try
        {
            return mapper.writeValueAsString( object );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    @Before
    public void beforeEachTest()
    {
        if ( !init )
        {
            issue = buildIssue();
            objectMapper = new ObjectMapper();
            filterProvider = new SimpleFilterProvider();
            objectMapper.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
            objectMapper.setFilterProvider( filterProvider );
            objectMapper.addMixIn( Object.class, JsonFilteringPropertyFilterMixin.class );
            init = true;
        }

        filterProvider.removeFilter( JsonFilteringPropertyFilter.FILTER_ID );
    }

    private Issue buildIssue()
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put( "email", "motherofdragons@got.com" );
        properties.put( "priority", "1" );

        Issue issue = new Issue();
        issue.setId( "ISSUE-1" );
        issue.setIssueSummary( "Dragons Need Fed" );
        issue.setIssueDetails( "I need my dragons fed pronto." );
        User assignee = new User( "Jorah", "Mormont" );
        issue.setAssignee( assignee );
        issue.setReporter( new User( "Daenerys", "Targaryen" ) );
        issue.setActions( Arrays.asList(
            new IssueAction( "COMMENT", "I'm going to let Daario get this one..", assignee ),
            new IssueAction( "CLOSE", "All set.", new User( "Daario", "Naharis" ) ) ) );
        issue.setProperties( properties );
        return issue;
    }

    @Test
    public void testAnyDeep()
    {
        filter( "**" );
        assertEquals( stringifyRaw(), stringify() );
    }

    @Test
    public void testEmpty()
    {
        filter( "" );
        assertEquals( "{}", stringify() );
    }

    @Test
    public void testSingleField()
    {
        filter( "id" );
        assertEquals( "{\"id\":\"" + issue.getId() + "\"}", stringify() );
    }

    @Test
    public void testMultipleFields()
    {
        filter( "id,issueSummary" );
        assertEquals( "{\"id\":\"" + issue.getId() + "\",\"issueSummary\":\"" + issue.getIssueSummary() + "\"}",
            stringify() );
    }

    @Test
    public void testRegex()
    {
        filter( "~iss[a-z]e.*~" );
        assertEquals( "{\"issueSummary\":\"" + issue.getIssueSummary() + "\",\"issueDetails\":\""
            + issue.getIssueDetails() + "\"}", stringify() );
    }

    @Test
    public void testRegexCaseInsensitive()
    {
        filter( "~iss[a-z]esumm.*~i" );
        assertEquals( "{\"issueSummary\":\"" + issue.getIssueSummary() + "\"}", stringify() );
    }

    @Test
    public void testRegexTraditional()
    {
        filter( "/iss[a-z]e.*/" );
        assertEquals( "{\"issueSummary\":\"" + issue.getIssueSummary() + "\",\"issueDetails\":\""
            + issue.getIssueDetails() + "\"}", stringify() );
    }

    @Test
    public void testWildCardSingle()
    {
        filter( "issueSummar?" );
        assertEquals( "{\"issueSummary\":\"" + issue.getIssueSummary() + "\"}", stringify() );
    }

    @Test
    public void testWildCardStart()
    {
        filter( "issue*" );
        assertEquals( "{\"issueSummary\":\"" + issue.getIssueSummary() + "\",\"issueDetails\":\""
            + issue.getIssueDetails() + "\"}", stringify() );
    }

    @Test
    public void testWildCardEnd()
    {
        filter( "*d" );
        assertEquals( "{\"id\":\"" + issue.getId() + "\"}", stringify() );
    }

    @Test
    public void testWildCardMiddle()
    {
        filter( "*ue*" );
        assertEquals( "{\"issueSummary\":\"" + issue.getIssueSummary() + "\",\"issueDetails\":\""
            + issue.getIssueDetails() + "\"}", stringify() );
    }

    @Test
    public void testDotPath()
    {
        filter( "id,actions.user.firstName" );
        assertEquals(
            "{\"id\":\"ISSUE-1\",\"actions\":[{\"user\":{\"firstName\":\"Jorah\"}},{\"user\":{\"firstName\":\"Daario\"}}]}",
            stringify() );
    }

    @Test
    public void testNegativeDotPath()
    {
        filter( "id,-actions.user.firstName" );
        assertEquals(
            "{\"id\":\"ISSUE-1\",\"actions\":[{\"id\":null,\"type\":\"COMMENT\",\"text\":\"I'm going to let Daario get this one..\",\"user\":{\"lastName\":\"Mormont\",\"entityType\":\"User\"}},{\"id\":null,\"type\":\"CLOSE\",\"text\":\"All set.\",\"user\":{\"lastName\":\"Naharis\",\"entityType\":\"User\"}}]}",
            stringify() );
    }

    @Test
    public void testNegativeDotPaths()
    {
        filter( "-actions.user.firstName,-actions.user.lastName" );
        assertEquals(
            "{\"id\":\"ISSUE-1\",\"issueSummary\":\"Dragons Need Fed\",\"issueDetails\":\"I need my dragons fed pronto.\",\"reporter\":{\"firstName\":\"Daenerys\",\"lastName\":\"Targaryen\",\"entityType\":\"User\"},\"assignee\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\",\"entityType\":\"User\"},\"actions\":[{\"id\":null,\"type\":\"COMMENT\",\"text\":\"I'm going to let Daario get this one..\",\"user\":{\"entityType\":\"User\"}},{\"id\":null,\"type\":\"CLOSE\",\"text\":\"All set.\",\"user\":{\"entityType\":\"User\"}}]}",
            stringify() );
    }

    @Test
    public void testNestedDotPath()
    {
        filter( "id,actions.user[firstName],issueSummary" );
        assertEquals(
            "{\"id\":\"ISSUE-1\",\"issueSummary\":\"Dragons Need Fed\",\"actions\":[{\"user\":{\"firstName\":\"Jorah\"}},{\"user\":{\"firstName\":\"Daario\"}}]}",
            stringify() );

        filter( "id,actions.user[]" );
        assertEquals( "{\"id\":\"ISSUE-1\",\"actions\":[{\"user\":{}},{\"user\":{}}]}", stringify() );
    }

    @Test
    public void testDeepNestedDotPath()
    {
        filter( "id,items.items[items.id]" );
        assertEquals( "{\"id\":\"ITEM-1\",\"items\":[{\"items\":[{\"items\":[{\"id\":\"ITEM-4\"}]}]}]}",
            stringify( Item.testItem() ) );

        filter( "id,items.items[items.items[id]]" );
        assertEquals( "{\"id\":\"ITEM-1\",\"items\":[{\"items\":[{\"items\":[{\"items\":[{\"id\":\"ITEM-5\"}]}]}]}]}",
            stringify( Item.testItem() ) );

        filter( "id,items.items[-items.id]" );
        assertEquals(
            "{\"id\":\"ITEM-1\",\"items\":[{\"items\":[{\"id\":\"ITEM-3\",\"name\":\"Milkshake\",\"items\":[{\"name\":\"Hoverboard\",\"items\":[{\"id\":\"ITEM-5\",\"name\":\"Binoculars\",\"items\":[]}]}]}]}]}",
            stringify( Item.testItem() ) );

        filter( "id,items.items[items[-id,-name],id]" );
        assertEquals(
            "{\"id\":\"ITEM-1\",\"items\":[{\"items\":[{\"id\":\"ITEM-3\",\"items\":[{\"items\":[{\"id\":\"ITEM-5\",\"name\":\"Binoculars\",\"items\":[]}]}]}]}]}",
            stringify( Item.testItem() ) );

        fileTest( "company-list.json", "deep-nested-01-filter.txt", "deep-nested-01-expected.json" );
        fileTest( "task-list.json", "deep-nested-02-filter.txt", "deep-nested-02-expected.json" );
        fileTest( "task-list.json", "deep-nested-03-filter.txt", "deep-nested-03-expected.json" );
    }

    @Test
    public void testNestedEmpty()
    {
        filter( "assignee[]" );
        assertEquals( "{\"assignee\":{}}", stringify() );
    }

    @Test
    public void testAssignee()
    {
        filter( "assignee" );
        assertEquals( "{\"assignee\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\",\"entityType\":\"User\"}}",
            stringify() );
    }

    @Test
    public void testNestedSingle()
    {
        filter( "assignee[firstName]" );
        assertEquals( "{\"assignee\":{\"firstName\":\"" + issue.getAssignee().getFirstName() + "\"}}", stringify() );
    }

    @Test
    public void testNestedMultiple()
    {
        filter( "actions[type,text]" );
        assertEquals( "{\"actions\":[{\"type\":\"" + issue.getActions().get( 0 ).getType() + "\",\"text\":\""
            + issue.getActions().get( 0 ).getText() + "\"},{\"type\":\"" + issue.getActions().get( 1 ).getType()
            + "\",\"text\":\"" + issue.getActions().get( 1 ).getText() + "\"}]}", stringify() );
    }

    @Test
    public void testMultipleNestedSingle()
    {
        filter( "(reporter,assignee)[lastName]" );
        assertEquals( "{\"reporter\":{\"lastName\":\"" + issue.getReporter().getLastName()
            + "\"},\"assignee\":{\"lastName\":\"" + issue.getAssignee().getLastName() + "\"}}", stringify() );
    }

    @Test
    public void testNestedMap()
    {
        filter( "properties[priority]" );
        assertEquals( "{\"properties\":{\"priority\":\"" + issue.getProperties().get( "priority" ) + "\"}}",
            stringify() );
    }

    @Test
    public void testDeepNested()
    {
        filter( "actions[user[lastName]]" );
        assertEquals(
            "{\"actions\":[{\"user\":{\"lastName\":\"" + issue.getActions().get( 0 ).getUser().getLastName()
                + "\"}},{\"user\":{\"lastName\":\"" + issue.getActions().get( 1 ).getUser().getLastName() + "\"}}]}",
            stringify() );
    }

    @Test
    public void testSameParent()
    {
        filter( "assignee[firstName],assignee[lastName]" );
        assertEquals( "{\"assignee\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\"}}", stringify() );

        filter( "assignee.firstName,assignee.lastName" );
        assertEquals( "{\"assignee\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\"}}", stringify() );

        filter( "actions.user[firstName],actions.user[lastName]" );
        assertEquals(
            "{\"actions\":[{\"user\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\"}},{\"user\":{\"firstName\":\"Daario\",\"lastName\":\"Naharis\"}}]}",
            stringify() );
    }

    @Test
    public void testFilterExcludesBaseFieldsInView()
    {
        String fieldName = "filterImplicitlyIncludeBaseFieldsInView";

        try
        {
            setFieldValue( JsonFilteringConfig.class, fieldName, false );
            filter( "view1" );
            assertEquals( "{\"properties\":" + stringifyRaw( issue.getProperties() ) + "}", stringify() );
        }
        finally
        {
            setFieldValue( JsonFilteringConfig.class, fieldName, true );
        }
    }

    @Test
    public void testFilterSpecificty()
    {
        filter( "**,reporter[lastName,entityType]" );
        String raw = stringifyRaw();
        assertEquals( raw.replace( "\"firstName\":\"" + issue.getReporter().getFirstName() + "\",", "" ), stringify() );

        filter( "**,repo*[lastName,entityType],repo*[firstName,entityType]" );
        assertEquals( raw, stringify() );

        filter( "**,reporter[lastName,entityType],repo*[firstName,entityType]" );
        assertEquals( raw.replace( "\"firstName\":\"" + issue.getReporter().getFirstName() + "\",", "" ), stringify() );

        filter( "**,repo*[firstName,entityType],rep*[lastName,entityType]" );
        assertEquals( raw.replace( ",\"lastName\":\"" + issue.getReporter().getLastName() + "\"", "" ), stringify() );

        filter( "**,reporter[firstName,entityType],reporter[lastName,entityType]" );
        assertEquals( raw, stringify() );
    }

    @Test
    public void testFilterExclusion()
    {
        filter( "**,reporter[-firstName]" );
        assertEquals(
            "{\"id\":\"ISSUE-1\",\"issueSummary\":\"Dragons Need Fed\",\"issueDetails\":\"I need my dragons fed pronto.\",\"reporter\":{\"lastName\":\"Targaryen\",\"entityType\":\"User\"},\"assignee\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\",\"entityType\":\"User\"},\"actions\":[{\"id\":null,\"type\":\"COMMENT\",\"text\":\"I'm going to let Daario get this one..\",\"user\":{\"firstName\":\"Jorah\",\"lastName\":\"Mormont\",\"entityType\":\"User\"}},{\"id\":null,\"type\":\"CLOSE\",\"text\":\"All set.\",\"user\":{\"firstName\":\"Daario\",\"lastName\":\"Naharis\",\"entityType\":\"User\"}}],\"properties\":{\"email\":\"motherofdragons@got.com\",\"priority\":\"1\"}}",
            stringify() );
    }

    @Test
    public void testJsonUnwrapped()
    {
        filter( "innerText" );
        assertEquals( "{\"innerText\":\"innerValue\"}", stringify( new Outer( "outerValue", "innerValue" ) ) );
    }

    @Test
    public void testPropertyWithDash()
    {
        filter( "full-name" );
        assertEquals( "{\"full-name\":\"Fred Flintstone\"}", stringify( new DashObject( "ID-1", "Fred Flintstone" ) ) );
    }

    private void setFieldValue( Class<?> ownerClass, String fieldName, boolean value )
    {
        Field field = getField( ownerClass, fieldName );
        try
        {
            field.setBoolean( null, value );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void removeFinalModifier( Field field )
    {
        try
        {
            Field modifiersField = Field.class.getDeclaredField( "modifiers" );
            modifiersField.setAccessible( true );
            modifiersField.setInt( field, field.getModifiers() & ~Modifier.FINAL );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private Field getField( Class<?> ownerClass, String fieldName )
    {
        try
        {
            Field field = ownerClass.getDeclaredField( fieldName );
            field.setAccessible( true );
            removeFinalModifier( field );
            return field;
        }
        catch ( NoSuchFieldException e )
        {
            throw new RuntimeException( e );
        }
    }

    private String regexRemove( String input, String regex )
    {
        Matcher matcher = regex( input, regex );
        StringBuffer sb = new StringBuffer();

        while ( matcher.find() )
        {
            matcher.appendReplacement( sb, "" );
        }
        matcher.appendTail( sb );
        return sb.toString();
    }

    private Matcher regex( String input, String regex )
    {
        Pattern pattern = Pattern.compile( regex );
        return pattern.matcher( input );
    }

    @SuppressWarnings( "UnusedReturnValue" )
    private String filter( String filter )
    {
        JsonFilteringParser parser = new JsonFilteringParser();
        JsonFilteringContextProvider provider = new SimpleJsonFilteringContextProvider( parser, filter );
        filterProvider.addFilter( JsonFilteringPropertyFilter.FILTER_ID, new JsonFilteringPropertyFilter( provider ) );
        return filter;
    }

    private String stringify()
    {
        return stringify( issue );
    }

    private String stringify( Object object )
    {
        return stringify( objectMapper, object );
    }

    private String stringifyRaw()
    {
        return stringifyRaw( issue );
    }

    private String stringifyRaw( Object object )
    {
        return stringify( rawObjectMapper, object );
    }

    private void fileTest( String inputFile, String filterFile, String expectedFile )
    {
        String input = readFile( BASE_PATH + "/input/" + inputFile );
        String filter = readFile( BASE_PATH + "/tests/" + filterFile );
        String expected = readFile( BASE_PATH + "/tests/" + expectedFile );

        try
        {
            Object inputObject = rawObjectMapper.readValue( input, Object.class );
            Object expectedObject = rawObjectMapper.readValue( expected, Object.class );

            filter( sanitizeFilter( filter ) );
            assertEquals( stringifyRaw( expectedObject ), stringify( inputObject ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private String readFile( String path )
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( path );

        if ( resource == null )
        {
            throw new IllegalArgumentException( "path " + path + " does not exist" );
        }

        try
        {
            return new String( Files.readAllBytes( Paths.get( resource.toURI() ) ), Charsets.UTF_8 );
        }
        catch ( IOException | URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
    }

    private String sanitizeFilter( String filter )
    {
        String[] lines = filter.split( "\n" );
        StringBuilder builder = new StringBuilder( filter.length() );

        for ( String line : lines )
        {
            line = line.trim();

            if ( line.startsWith( "#" ) )
            {
                continue;
            }

            builder.append( line.replaceAll( "\\s", "" ) );
        }

        return builder.toString();
    }

    private static class DashObject
    {

        private String id;

        @JsonProperty( "full-name" )
        private String fullName;

        public DashObject()
        {
        }

        public DashObject( String id, String fullName )
        {
            this.id = id;
            this.fullName = fullName;
        }

        public String getId()
        {
            return id;
        }

        public String getFullName()
        {
            return fullName;
        }
    }
}
