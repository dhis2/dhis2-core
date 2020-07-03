package org.hisp.dhis.common.adapter;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.type.RenderingObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractDeviceRenderTypeMapDeserializer<T extends RenderingObject>
    extends JsonDeserializer<DeviceRenderTypeMap<T>>
{
    private Supplier<T> serializeObject;

    public AbstractDeviceRenderTypeMapDeserializer( Supplier<T> serializeObject )
    {
        this.serializeObject = serializeObject;
    }

    @Override
    public DeviceRenderTypeMap deserialize( JsonParser jsonParser, DeserializationContext deserializationContext )
        throws IOException
    {
        DeviceRenderTypeMap<T>  deviceRenderTypeMap = new DeviceRenderTypeMap<>();
        LinkedHashMap<String, LinkedHashMap<String,String>> map = jsonParser
            .readValueAs( new TypeReference<LinkedHashMap<String, LinkedHashMap<String,String>>>() {} );

        for( String renderDevice : map.keySet() )
        {
            LinkedHashMap<String,String> renderObjectMap = map.get( renderDevice );
            T renderingObject = serializeObject.get();
            renderingObject.setType( Enum.valueOf( renderingObject.getRenderTypeClass(), renderObjectMap.get( RenderingObject._TYPE ) ) );
            deviceRenderTypeMap.put( RenderDevice.valueOf( renderDevice ), renderingObject );
        }

        return deviceRenderTypeMap;
    }
}
