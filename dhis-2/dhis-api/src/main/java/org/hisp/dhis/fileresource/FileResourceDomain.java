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
package org.hisp.dhis.fileresource;

import java.util.Set;

/**
 * @author Halvdan Hoem Grelland
 */
public enum FileResourceDomain
{
    DATA_VALUE( "dataValue", ImageFileDimension.getPictureDimensions() ),
    PUSH_ANALYSIS( "pushAnalysis", Set.of() ),
    DOCUMENT( "document", Set.of() ),
    MESSAGE_ATTACHMENT( "messageAttachment", Set.of() ),
    USER_AVATAR( "userAvatar", ImageFileDimension.getPictureDimensions() ),
    ORG_UNIT( "organisationUnit", ImageFileDimension.getPictureDimensions() );

    /**
     * Container name to use when storing blobs of this FileResourceDomain
     */
    private final String containerName;

    /**
     * If not empty, any images will be copied and resized into the specified
     * image dimensions.
     */
    private final Set<ImageFileDimension> imageDimensions;

    FileResourceDomain( String containerName, Set<ImageFileDimension> imageDimensions )
    {
        this.containerName = containerName;
        this.imageDimensions = imageDimensions;
    }

    public String getContainerName()
    {
        return containerName;
    }

    public Set<ImageFileDimension> getImageDimensions()
    {
        return imageDimensions;
    }

    public boolean hasImageDimensions()
    {
        return !imageDimensions.isEmpty();
    }
}
