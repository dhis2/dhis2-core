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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

/**
 * @Author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {
  private static final int SMALL_IMAGE_WIDTH = 256;

  private static final int MEDIUM_IMAGE_WIDTH = 512;

  private static final int LARGE_IMAGE_WIDTH = 1024;

  private ImageProcessingService subject;

  @BeforeEach
  public void setUp() {
    subject = new DefaultImageProcessingService();
  }

  @Test
  void test_create_images_with_null_values() {
    Map<ImageFileDimension, File> images = subject.createImages(new FileResource(), null);

    assertTrue(images.isEmpty());
  }

  @Test
  void test_create_images_with_wrong_file_content_type() throws IOException {
    FileResource fileResource = new FileResource();
    fileResource.setName("test");
    fileResource.setContentType("image/png");

    File file = new File("complex.pdf");

    Map<ImageFileDimension, File> images = subject.createImages(fileResource, file);

    assertTrue(images.isEmpty());

    Files.deleteIfExists(file.toPath());
  }

  @Test
  void test_create_image() throws IOException {
    FileResource fileResource = new FileResource();
    fileResource.setName("test");
    fileResource.setContentType("image/png");

    File file = new ClassPathResource("images/dhis2.png").getFile();

    Map<ImageFileDimension, File> images = subject.createImages(fileResource, file);

    assertNotNull(images);
    assertEquals(4, images.size());

    File smallImage = images.get(ImageFileDimension.SMALL);
    File mediumImage = images.get(ImageFileDimension.MEDIUM);
    File largeImage = images.get(ImageFileDimension.LARGE);

    assertEquals(SMALL_IMAGE_WIDTH, ImageIO.read(smallImage).getWidth());

    assertEquals(MEDIUM_IMAGE_WIDTH, ImageIO.read(mediumImage).getWidth());

    assertEquals(LARGE_IMAGE_WIDTH, ImageIO.read(largeImage).getWidth());

    Files.deleteIfExists(smallImage.toPath());
    Files.deleteIfExists(mediumImage.toPath());
    Files.deleteIfExists(largeImage.toPath());
  }
}
