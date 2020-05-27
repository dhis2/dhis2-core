package org.hisp.dhis.webapi.mvc.messageconverter;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.serializers.Jackson2JsonNodeSerializer;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class JsonPMessageConverter extends AbstractRootNodeMessageConverter
{
    public static final String DEFAULT_CALLBACK_PARAMETER = "callback";

    public static final ImmutableList<MediaType> SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType>builder()
        .add( new MediaType( "application", "javascript" ) )
        .add( new MediaType( "application", "x-javascript" ) )
        .add( new MediaType( "text", "javascript" ) )
        .build();

    private ContextService contextService;

    public JsonPMessageConverter( @Autowired @Nonnull NodeService nodeService, @Autowired @Nonnull ContextService contextService )
    {
        super( nodeService, "application/json", "jsonp", Compression.NONE );
        this.contextService = contextService;
        setSupportedMediaTypes( SUPPORTED_MEDIA_TYPES );
    }

    @Override
    protected void writeInternal( RootNode rootNode, HttpOutputMessage outputMessage ) throws IOException, HttpMessageNotWritableException
    {
        List<String> callbacks = Lists.newArrayList( contextService.getParameterValues( DEFAULT_CALLBACK_PARAMETER ) );

        String callbackParam;

        if ( callbacks.isEmpty() )
        {
            callbackParam = DEFAULT_CALLBACK_PARAMETER;
        }
        else
        {
            callbackParam = callbacks.get( 0 );
        }

        rootNode.getConfig().getProperties().put( Jackson2JsonNodeSerializer.JSONP_CALLBACK, callbackParam );

        super.writeInternal( rootNode, outputMessage );
    }
}
