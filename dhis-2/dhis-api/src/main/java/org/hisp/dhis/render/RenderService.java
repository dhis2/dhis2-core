package org.hisp.dhis.render;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.metadata.version.MetadataVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface RenderService
{
    void toJson( OutputStream output, Object value ) throws IOException;

    void toJson( OutputStream output, Object value, Class<?> klass ) throws IOException;

    void toJsonP( OutputStream output, Object value, String callback ) throws IOException;

    void toJsonP( OutputStream output, Object value, Class<?> klass, String callback ) throws IOException;

    <T> T fromJson( InputStream input, Class<T> klass ) throws IOException;

    <T> T fromJson( String input, Class<T> klass ) throws IOException;

    <T> void toXml( OutputStream output, T value ) throws IOException;

    <T> void toXml( OutputStream output, T value, Class<?> klass ) throws IOException;

    <T> T fromXml( InputStream input, Class<T> klass ) throws IOException;

    <T> T fromXml( String input, Class<T> klass ) throws IOException;

    boolean isValidJson( String json ) throws IOException;

    /**
     * Parses metadata stream and automatically finds collections of id object based on root properties.
     * <p>
     * i.e. A property called "dataElements" would be tried to parsed as a collection of data elements.
     *
     * @param inputStream Stream to read from
     * @param format      Payload format (only JSON is supported)
     * @return Map of all id object types that were found
     */
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> fromMetadata( InputStream inputStream, RenderFormat format ) throws IOException;

    /**
     * Parses the input stream for the collection of MetadataVersion objects.
     *
     * @param inputStream
     * @param format
     * @return List of MetadataVersion objects.
     * @throws IOException
     */
    List<MetadataVersion> fromMetadataVersion(InputStream inputStream, RenderFormat format) throws IOException;
}
