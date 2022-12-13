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
package org.hisp.dhis.webapi.openapi;

import java.util.function.UnaryOperator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.OpenApi;

/**
 * Uses {@link Descriptions} to supply CommonMark description texts to an
 * {@link Api} model.
 *
 * @see Descriptions
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
final class ApiDescribe
{
    static void describeApi( Api api )
    {
        describeTags( api );
    }

    private static void describeTags( Api api )
    {
        Descriptions tags = Descriptions.of( OpenApi.Tags.class );
        api.getUsedTags().forEach( name -> {
            Api.Tag tag = new Api.Tag( name );
            tag.getDescription().setValue( tags.get( name + ".description" ) );
            tag.getExternalDocsUrl().setValue( tags.get( name + ".externalDocs.url" ) );
            tag.getExternalDocsDescription().setValue( tags.get( name + ".externalDocs.description" ) );
            api.getTags().put( name, tag );
        } );

        api.getControllers().forEach( ApiDescribe::describeController );
    }

    private static void describeController( Api.Controller controller )
    {
        Descriptions descriptions = Descriptions.of( controller.getSource() );

        controller.getEndpoints().forEach( endpoint -> {
            String name = endpoint.getSource().getName();
            UnaryOperator<String> subst = desc -> desc.replace( "{entityType}", endpoint.getEntityTypeName() );
            endpoint.getDescription().setValue( descriptions.get( name + ".description", subst ) );
            endpoint.getParameters().values().forEach( parameter -> {
                Api.Maybe<String> description = parameter.getDescription();
                String paramName = parameter.isShared() ? parameter.getGlobalName() : parameter.getName();
                String key = name + ".parameter." + paramName + ".description";
                description.setValue( descriptions.get( key, subst ) );
                if ( !description.isPresent() )
                {
                    key = "*" + key.substring( key.indexOf( '.' ) );
                    description.setValue( descriptions.get( key, subst ) );
                }
            } );
            if ( endpoint.getRequestBody().isPresent() )
            {
                Api.Maybe<String> description = endpoint.getRequestBody().getValue().getDescription();
                String key = name + "request.description";
                description.setValue( descriptions.get( key, subst ) );
            }
            endpoint.getResponses().values().forEach( response -> {
                String key = name + ".response." + response.getStatus().value() + ".description";
                Api.Maybe<String> description = response.getDescription();
                description.setValue( descriptions.get( key, subst ) );
                if ( !description.isPresent() )
                {
                    key = "*" + key.substring( key.indexOf( '.' ) );
                    description.setValue( descriptions.get( key, subst ) );
                }
            } );
        } );
    }

}
