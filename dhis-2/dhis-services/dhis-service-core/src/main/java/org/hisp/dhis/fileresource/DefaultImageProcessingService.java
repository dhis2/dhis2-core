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

import com.google.common.collect.ImmutableMap;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.DebugUtils;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;

/**
 * @Author Zubair Asghar.
 */
@Slf4j
@Service("org.hisp.dhis.fileresource.ImageProcessingService")
public class DefaultImageProcessingService implements ImageProcessingService {
  private static final ImmutableMap<ImageFileDimension, ImageSize> IMAGE_FILE_SIZES =
      ImmutableMap.of(
          ImageFileDimension.SMALL, new ImageSize(256, 256),
          ImageFileDimension.MEDIUM, new ImageSize(512, 512),
          ImageFileDimension.LARGE, new ImageSize(1024, 1024));

  @Override
  public Map<ImageFileDimension, File> createImages(FileResource fileResource, File file) {
    if (!isInputValid(fileResource, file)) {
      return new HashMap<>();
    }

    Map<ImageFileDimension, File> images = new HashMap<>();

    try {
      BufferedImage image = ImageIO.read(file);

      for (ImageFileDimension dimension : ImageFileDimension.values()) {
        if (ImageFileDimension.ORIGINAL == dimension) {
          images.put(dimension, file);
          continue;
        }

        ImageSize size = IMAGE_FILE_SIZES.get(dimension);

        BufferedImage resizedImage = resize(image, size);

        File tempFile = new File(file.getPath() + dimension.getDimension());

        ImageIO.write(resizedImage, fileResource.getFormat(), tempFile);

        images.put(dimension, tempFile);
      }
    } catch (IOException e) {
      log.error("Image file resource cannot be processed");
      DebugUtils.getStackTrace(e);
      return new HashMap<>();
    }

    return images;
  }

  private BufferedImage resize(BufferedImage image, ImageSize dimensions) {
    return Scalr.resize(
        image, Scalr.Method.BALANCED, Scalr.Mode.FIT_TO_WIDTH, dimensions.width, dimensions.height);
  }

  private boolean isInputValid(FileResource fileResource, File file) {
    if (fileResource == null || file == null) {
      log.error("FileResource and associated File must not be null");
      return false;
    }

    if (file.exists()) {
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
        String mimeType = URLConnection.guessContentTypeFromStream(is);
        return FileResource.IMAGE_CONTENT_TYPES.contains(mimeType);
      } catch (IOException e) {
        DebugUtils.getStackTrace(e);
        log.error("Error while getting content type", e);
        return false;
      }
    } else {
      return false;
    }
  }

  private static class ImageSize {
    int width;

    int height;

    ImageSize(int width, int height) {
      this.width = width;
      this.height = height;
    }
  }
}
