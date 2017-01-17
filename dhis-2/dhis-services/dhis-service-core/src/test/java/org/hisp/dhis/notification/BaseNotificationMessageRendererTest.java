package org.hisp.dhis.notification;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.DeliveryChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Halvdan Hoem Grelland
 */
public class BaseNotificationMessageRendererTest
{
    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private BaseNotificationMessageRenderer<Entity> renderer;

    private static final Pair<String, String> ATTR_A = ImmutablePair.of( "a1234567890", "Attribute A value" );
    private static final Pair<String, String> ATTR_B = ImmutablePair.of( "b1234567890", "Attribute B value" );
    private static final Pair<String, String> ATTR_NULL = ImmutablePair.of( "n1234567890", null );

    @Before
    public void setUpTest()
    {
        renderer = new MockNotificationMessageRenderer();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testRenderVariablesRendersVariables()
    {
        Entity e = entity();

        String templateString = "V{a} V{b}";

        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        String expected = format( "%s %s", e.propertyA, e.propertyB );

        assertNotNull( message );
        assertEquals( expected, message.getMessage() );
    }

    @Test
    public void testRenderAttributesRendersAttributes()
    {
        Entity e = entity();
        String templateString = format( "A{%s} A{%s}", ATTR_A.getKey(), ATTR_B.getKey() );

        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        String expected = format( "%s %s", ATTR_A.getValue(), ATTR_B.getValue() );

        assertNotNull( message );
        assertEquals( expected, message.getMessage() );
    }

    @Test
    public void testRenderMixedContentRendersBoth()
    {
        Entity e = entity();
        String templateMessage = format( "Boom V{a} shake A{%s} the room", ATTR_A.getKey() );

        NotificationTemplate template = template( templateMessage, templateMessage );

        NotificationMessage message = renderer.render( e, template );

        String expected = format( "Boom %s shake %s the room", e.propertyA, ATTR_A.getValue() );

        assertNotNull( message );
        assertEquals( expected, message.getMessage() );
    }

    @Test
    public void testRenderNullValuesAreReplacedWithEmptyStrings()
    {
        Entity e = entity( "Halvdan was here", null );

        String templateString = format( "V{a} V{b} A{%s}", ATTR_NULL.getKey() );

        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        String expected = format( "%s  ", e.propertyA );

        assertNotNull( message );
        assertEquals( expected, message.getMessage() );
    }

    @Test
    public void testNonSupportedExpressionsAreIgnored()
    {
        Entity e = entity();
        String templateString = "X{abc} V{a} Y{}";

        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        String expected = format( "X{abc} %s Y{}", e.propertyA );

        assertNotNull( message );
        assertEquals( expected, message.getMessage() );
    }

    @Test
    public void testNonExistingVariablesAreReplacedWithEmptyStrings()
    {
        Entity e = entity();
        String templateString = "V{b} V{does_not_exist} V{a}";

        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        String expected = format( "%s  %s", e.propertyB, e.propertyA );

        assertNotNull( message );
        assertEquals( expected, message.getMessage() );
    }

    @Test
    public void testSubjectLengthIsLimited()
    {
        Entity e = entity();
        String templateString = RandomStringUtils.randomAlphanumeric( 101 );
        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        assertNotNull( message );
        assertEquals( BaseNotificationMessageRenderer.SUBJECT_CHAR_LIMIT, message.getSubject().length() );
    }

    @Test
    public void testMessageLengthIsLimitedWhenHasSmsRecipient()
    {
        Entity e = entity();

        int tooLong = RandomUtils.nextInt(
            BaseNotificationMessageRenderer.SMS_CHAR_LIMIT + 1,
            BaseNotificationMessageRenderer.SMS_CHAR_LIMIT + 100
        );

        String templateString = RandomStringUtils.randomAlphanumeric( tooLong );
        NotificationTemplate template = template( templateString );

        NotificationMessage message = renderer.render( e, template );

        assertNotNull( message );
        assertEquals( BaseNotificationMessageRenderer.SMS_CHAR_LIMIT, message.getMessage().length() );
    }

    @Test
    public void testMessageLengthIsLimitedForEmailRecipients()
    {
        Entity e = entity();

        int tooLong = RandomUtils.nextInt(
            BaseNotificationMessageRenderer.EMAIL_CHAR_LIMIT + 1,
            BaseNotificationMessageRenderer.EMAIL_CHAR_LIMIT + 100
        );

        String templateString = RandomStringUtils.randomAlphanumeric( tooLong );
        NotificationTemplate template = Mockito.spy( template( templateString ) );
        Mockito.when( template.getDeliveryChannels() ).thenReturn( Sets.newHashSet( DeliveryChannel.EMAIL ) );

        NotificationMessage message = renderer.render( e, template );

        assertNotNull( message );
        assertEquals( BaseNotificationMessageRenderer.EMAIL_CHAR_LIMIT, message.getMessage().length() );
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    private static Entity entity( String a, String b )
    {
        return new Entity( a, b );
    }

    private static Entity entity()
    {
        return entity( "A", "B" );
    }

    private static MockNotificationTemplate template( String str )
    {
        return template( str, str );
    }

    private static MockNotificationTemplate template( String sub, String msg )
    {
        return new MockNotificationTemplate( sub, msg );
    }

    // -------------------------------------------------------------------------
    // Mock classes
    // -------------------------------------------------------------------------

    /**
     * Thin mock implementation on top of BaseNotificationMessageRenderer
     */
    static class MockNotificationMessageRenderer extends BaseNotificationMessageRenderer<Entity>
    {
        static final ImmutableMap<TemplateVariable, Function<Entity, String>> VARIABLE_RESOLVERS =
            ImmutableMap.<TemplateVariable, Function<Entity, String>>builder()
                .put( EntityTemplateVariable.a, e -> e.propertyA )
                .put( EntityTemplateVariable.b, e -> e.propertyB )
                .build();

        static final Map<String, String> ATTRIBUTE_VALUES = new HashMap<String, String>() {{
            put( ATTR_A.getKey(), ATTR_A.getValue() );
            put( ATTR_B.getKey(), ATTR_B.getValue() );
            put( ATTR_NULL.getKey(), ATTR_NULL.getValue() );
        }};

        @Override
        protected Map<TemplateVariable, Function<Entity, String>> getVariableResolvers()
        {
            return VARIABLE_RESOLVERS;
        }

        @Override
        protected Map<String, String> resolveAttributeValues( Set<String> attributeKeys, Entity entity )
        {
            return ATTRIBUTE_VALUES;
        }

        @Override
        protected TemplateVariable fromVariableName( String name )
        {
            return EnumSet.allOf( EntityTemplateVariable.class ).stream()
                .filter( tv -> tv.name().equals( name ) )
                .findFirst()
                .orElse( null );
        }

        @Override
        protected Set<ExpressionType> getSupportedExpressionTypes()
        {
            return Sets.newHashSet( BaseNotificationMessageRenderer.ExpressionType.values() );
        }
    }

    static class Entity
    {
        final String propertyA;
        final String propertyB;


        Entity( String propertyA, String propertyB )
        {
            this.propertyA = propertyA;
            this.propertyB = propertyB;
        }
    }

    enum EntityTemplateVariable implements TemplateVariable
    {
        a ( "a" ), b ( "b" );

        final String variableName;

        EntityTemplateVariable( String variableName )
        {
            this.variableName = variableName;
        }

        @Override
        public String getVariableName()
        {
            return variableName;
        }
    }

    static class MockNotificationTemplate implements NotificationTemplate
    {
        final String subjectTemplate, messageTemplate;

        MockNotificationTemplate( String subjectTemplate, String messageTemplate )
        {
            this.subjectTemplate = subjectTemplate;
            this.messageTemplate = messageTemplate;
        }

        @Override
        public String getSubjectTemplate()
        {
            return subjectTemplate;
        }

        @Override
        public String getMessageTemplate()
        {
            return messageTemplate;
        }

        @Override
        public Set<DeliveryChannel> getDeliveryChannels()
        {
            return Sets.newHashSet( DeliveryChannel.values() );
        }
    }
}
