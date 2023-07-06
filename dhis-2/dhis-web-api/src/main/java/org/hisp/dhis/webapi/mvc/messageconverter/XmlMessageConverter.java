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
package org.hisp.dhis.webapi.mvc.messageconverter;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class XmlMessageConverter extends AbstractRootNodeMessageConverter {
  public static final ImmutableList<MediaType> SUPPORTED_MEDIA_TYPES =
      ImmutableList.<MediaType>builder().add(new MediaType("application", "xml")).build();

  public static final ImmutableList<MediaType> GZIP_SUPPORTED_MEDIA_TYPES =
      ImmutableList.<MediaType>builder().add(new MediaType("application", "xml+gzip")).build();

  public static final ImmutableList<MediaType> ZIP_SUPPORTED_MEDIA_TYPES =
      ImmutableList.<MediaType>builder().add(new MediaType("application", "xml+zip")).build();

  public XmlMessageConverter(@Autowired @Nonnull NodeService nodeService, Compression compression) {
    super(nodeService, "application/xml", "xml", compression);

    switch (getCompression()) {
      case NONE:
        setSupportedMediaTypes(SUPPORTED_MEDIA_TYPES);
        break;
      case GZIP:
        setSupportedMediaTypes(GZIP_SUPPORTED_MEDIA_TYPES);
        break;
      case ZIP:
        setSupportedMediaTypes(ZIP_SUPPORTED_MEDIA_TYPES);
    }
  }
}
