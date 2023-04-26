/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.hisp.dhis.common.IllegalQueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith( MockitoExtension.class )
class FileResourceUtilsTest
{

    @Mock
    private MultipartFile multipartFile;

    @Test
    void shouldWorkWhenCustomIconIsValid()
    {
        when( multipartFile.getOriginalFilename() ).thenReturn( "OU_profile_image.png" );
        when( multipartFile.getSize() ).thenReturn( 10L );

        Assertions.assertDoesNotThrow( () -> FileResourceUtils.validateCustomIconFile( multipartFile ) );
    }

    @Test
    void shouldFailWhenCustomIconHasInvalidFormat()
    {
        MockMultipartFile jpgImage = new MockMultipartFile( "file", "OU_profile_image.jpg", "image/jpg",
            "<<jpg data>>".getBytes() );

        Exception ex = assertThrows( IllegalQueryException.class,
            () -> FileResourceUtils.validateCustomIconFile( jpgImage ) );
        assertContains( "Wrong file extension", ex.getMessage() );
    }

    @Test
    void shouldFailWhenCustomIconIsTooLarge()
    {
        when( multipartFile.getOriginalFilename() ).thenReturn( "OU_profile_image.png" );
        when( multipartFile.getSize() ).thenReturn( 100000000L );

        Exception ex = assertThrows( IllegalQueryException.class,
            () -> FileResourceUtils.validateCustomIconFile( multipartFile ) );
        assertContains( "File size can't be bigger than", ex.getMessage() );
    }

    @Test
    void shouldResizeImageTo48x48WhenCustomIconIsValid()
        throws IOException
    {
        InputStream in = getClass().getResourceAsStream( "/icon/test-image.png" );
        MockMultipartFile mockMultipartFile = new MockMultipartFile( "file", "test-image.png", "image/png", in );
        MultipartFile file = FileResourceUtils.resizeToDefaultIconSize( mockMultipartFile );
        BufferedImage bufferedImage = ImageIO.read( file.getInputStream() );

        Assertions.assertEquals( 48, bufferedImage.getWidth() );
        Assertions.assertEquals( 48, bufferedImage.getHeight() );
    }
}
