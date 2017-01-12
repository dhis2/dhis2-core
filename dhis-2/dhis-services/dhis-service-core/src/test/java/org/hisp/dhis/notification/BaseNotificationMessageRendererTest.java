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
import org.hisp.dhis.common.DeliveryChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

    private NotificationTemplate template;

    static final ImmutableMap<TemplateVariable, Function<Entity, String>> VARIABLE_RESOLVERS =
        ImmutableMap.<TemplateVariable, Function<Entity, String>>builder()
            .put( EntityTemplateVariable.a, e -> e.propertyA )
            .put( EntityTemplateVariable.b, e -> e.propertyB )
            .build();

    static final ImmutableMap<String, String> ATTRIBUTE_VALUES =
        ImmutableMap.<String, String>builder()
            .put( "a1234567890", "Attribute A" )
            .put( "b1234567890", "Attribute B" )
            .build();

    @SuppressWarnings( "unchecked" )
    @Before
    public void setUpTest()
    {
        // Mock

        renderer = Mockito.spy( new MockNotificationMessageRenderer() );
        template = new MockNotificationTemplate( "Subject V{a} V{b}", "Message V{a} V{b}" );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testRenderVariableRendersVariable()
    {
        Entity e = new Entity( "Something A", "Something B" );

        NotificationMessage message = renderer.render( e, template );

        assertNotNull( message );
        assertEquals( "Subject Something A Something B", message.getSubject() );
        assertEquals( "Message Something A Something B", message.getMessage() );
    }

    @Test
    public void testRenderAttributesRendersAttributes()
    {
        Entity e = new Entity( "A", "B" );
        template = new MockNotificationTemplate( "A{a1234567890}", "A{b1234567890}" );

        NotificationMessage message = renderer.render( e, template );

        assertNotNull( message );

        assertEquals( ATTRIBUTE_VALUES.get( "a1234567890" ), message.getSubject() );
        assertEquals( ATTRIBUTE_VALUES.get( "b1234567890" ), message.getMessage() );
    }

    // -------------------------------------------------------------------------
    // Mock classes
    // -------------------------------------------------------------------------

    /**
     * Thin mock implementation on top of BaseNotificationMessageRenderer
     */
    static class MockNotificationMessageRenderer extends BaseNotificationMessageRenderer<Entity>
    {
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
            return EntityTemplateVariable.valueOf( name );
        }

        @Override
        protected Set<ExpressionType> getSupportedExpressionTypes()
        {
            return Sets.newHashSet( BaseNotificationMessageRenderer.ExpressionType.values() );
        }
    }

    /**
     * Our fake entity
     */
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
        a ( "a" ),
        b ( "b" );

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
