/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_HEADER_VALUE;
import static org.imgscalr.Scalr.resize;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.scheduling.JobRunner;
import org.imgscalr.Scalr.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lars Helge Overland
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FileResourceUtils {

  @Autowired private final JobRunner jobRunner;
  @Autowired private final FileResourceService fileResourceService;

  private static final List<String> CUSTOM_ICON_VALID_ICON_EXTENSIONS = List.of("png");

  private static final long CUSTOM_ICON_FILE_SIZE_LIMIT_IN_BYTES = 25_000_000;

  private static final int CUSTOM_ICON_TARGET_HEIGHT = 48;
  private static final int CUSTOM_ICON_TARGET_WIDTH = 48;

  private static final int AVATAR_TARGET_HEIGHT = 200;
  private static final int AVATAR_TARGET_WIDTH = 200;

  private static final int ORGUNIT_TARGET_HEIGHT = 800;
  private static final int ORGUNIT_TARGET_WIDTH = 800;

  private static final long MAX_AVATAR_IMAGE_SIZE_IN_BYTES = 2_000_000;
  private static final long MAX_ORGUNIT_IMAGE_SIZE_IN_BYTES = 8_000_000;

  private static final List<String> ALLOWED_IMAGE_FILE_EXTENSIONS =
      List.of("jpg", "jpeg", "png", "gif");
  private static final List<String> ALLOWED_IMAGE_MIME_TYPES =
      List.of("image/jpeg", "image/png", "image/gif");

  private static class MultipartFileByteSource extends ByteSource {
    private MultipartFile file;

    public MultipartFileByteSource(MultipartFile file) {
      this.file = file;
    }

    @Override
    public InputStream openStream() {
      try {
        return file.getInputStream();
      } catch (IOException ioe) {
        return new NullInputStream(0);
      }
    }
  }

  /**
   * Transfers the given multipart file content to a local temporary file.
   *
   * @param multipartFile the multipart file.
   * @return a temporary local file.
   * @throws IOException if the file content could not be transferred.
   */
  public static File toTempFile(MultipartFile multipartFile) throws IOException {
    File tmpFile = Files.createTempFile("org.hisp.dhis", ".tmp").toFile();
    tmpFile.deleteOnExit();
    multipartFile.transferTo(tmpFile);
    return tmpFile;
  }

  /**
   * Indicates whether the content type represented by the given string is a valid, known content
   * type.
   *
   * @param contentType the content type string.
   * @return true if the content is valid, false if not.
   */
  public static boolean isValidContentType(String contentType) {
    try {
      MimeTypeUtils.parseMimeType(contentType);
    } catch (InvalidMimeTypeException ignored) {
      return false;
    }

    return true;
  }

  /**
   * Builds a {@link FileResource} from a {@link MultipartFile}.
   *
   * @param key the key to associate to the {@link FileResource}
   * @param file a {@link MultipartFile}
   * @param domain a {@link FileResourceDomain}
   * @return a valid {@link FileResource} populated with data from the provided file
   * @throws IOException if hashing fails
   */
  public static FileResource build(String key, MultipartFile file, FileResourceDomain domain)
      throws IOException {
    return new FileResource(
        key,
        file.getName(),
        file.getContentType(),
        file.getSize(),
        ByteSource.wrap(file.getBytes()).hash(Hashing.md5()).toString(),
        domain);
  }

  public static void setImageFileDimensions(
      FileResource fileResource, ImageFileDimension dimension) {
    if (FileResource.IMAGE_CONTENT_TYPES.contains(fileResource.getContentType())
        && FileResourceDomain.isDomainForMultipleImages(fileResource.getDomain())) {
      if (fileResource.isHasMultipleStorageFiles()) {
        fileResource.setStorageKey(
            StringUtils.join(fileResource.getStorageKey(), dimension.getDimension()));
      }
    }
  }

  public void configureFileResourceResponse(
      HttpServletResponse response, FileResource fileResource, DhisConfigurationProvider dhisConfig)
      throws WebMessageException {
    response.setContentType(fileResource.getContentType());
    response.setContentLengthLong(fileResource.getContentLength());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());
    HeaderUtils.setSecurityHeaders(response, dhisConfig.getProperty(CSP_HEADER_VALUE));

    try {
      fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
    } catch (IOException e) {
      throw new WebMessageException(
          error(
              "Failed fetching the file from storage",
              "There was an exception when trying to fetch the file from the storage backend. "
                  + "Depending on the provider the root cause could be network or file system related."));
    }
  }

  public FileResource saveFileResource(String uid, MultipartFile file, FileResourceDomain domain)
      throws IOException, ConflictException {
    String filename =
        StringUtils.defaultIfBlank(
            FilenameUtils.getName(file.getOriginalFilename()), FileResource.DEFAULT_FILENAME);

    String contentType = file.getContentType();
    contentType =
        FileResourceUtils.isValidContentType(contentType)
            ? contentType
            : FileResource.DEFAULT_CONTENT_TYPE;

    long contentLength = file.getSize();

    log.info(
        "File uploaded with filename: '{}', original filename: '{}', content type: '{}', content length: {}",
        filename,
        file.getOriginalFilename(),
        file.getContentType(),
        contentLength);

    if (contentLength <= 0) {
      throw new ConflictException("Could not read file or file is empty.");
    }

    ByteSource bytes = new MultipartFileByteSource(file);

    String contentMd5 = bytes.hash(Hashing.md5()).toString();

    FileResource fileResource =
        new FileResource(filename, contentType, contentLength, contentMd5, domain);
    fileResource.setUid(uid);

    File tmpFile = toTempFile(file);

    if (uid != null && fileResourceService.fileResourceExists(uid)) {
      throw new ConflictException(ErrorCode.E1119, FileResource.class.getSimpleName(), uid);
    }
    if (!jobRunner.isScheduling()) {
      fileResourceService.syncSaveFileResource(fileResource, Files.readAllBytes(tmpFile.toPath()));
    } else {
      fileResourceService.asyncSaveFileResource(fileResource, tmpFile);
    }
    return fileResource;
  }

  public void validateOrgUnitImage(MultipartFile file) {
    validateContentType(file.getContentType(), ALLOWED_IMAGE_MIME_TYPES);
    validateFileExtension(file.getOriginalFilename(), ALLOWED_IMAGE_FILE_EXTENSIONS);
    validateFileSize(file, MAX_ORGUNIT_IMAGE_SIZE_IN_BYTES);
  }

  public void validateUserAvatar(@Nonnull MultipartFile file) {
    validateContentType(file.getContentType(), ALLOWED_IMAGE_MIME_TYPES);
    validateFileExtension(file.getOriginalFilename(), ALLOWED_IMAGE_FILE_EXTENSIONS);
    validateFileSize(file, MAX_AVATAR_IMAGE_SIZE_IN_BYTES);
  }

  private void validateContentType(String contentType, @Nonnull List<String> validExtensions) {
    if (contentType == null) {
      throw new IllegalQueryException("Invalid content type, content type is NULL");
    }
    contentType = contentType.split(";")[0].trim();
    if (!validExtensions.contains(contentType)) {
      throw new IllegalQueryException(
          "Invalid content type, valid content types are: " + String.join(",", validExtensions));
    }
  }

  public static void validateCustomIconFile(MultipartFile file) {
    validateFileExtension(file.getOriginalFilename(), CUSTOM_ICON_VALID_ICON_EXTENSIONS);
    validateFileSize(file, CUSTOM_ICON_FILE_SIZE_LIMIT_IN_BYTES);
  }

  private static void validateFileExtension(String fileName, List<String> validExtension) {
    if (getExtension(fileName) == null || !validExtension.contains(getExtension(fileName))) {
      throw new IllegalQueryException(
          "Wrong file extension, valid extensions are: " + String.join(",", validExtension));
    }
  }

  public static void validateFileSize(@Nonnull MultipartFile file, long maxFileSizeInBytes) {
    if (file.getSize() > maxFileSizeInBytes) {
      throw new IllegalQueryException(
          String.format(
              "File size can't be bigger than %d, current file size %d",
              maxFileSizeInBytes, file.getSize()));
    }
  }

  public static MultipartFile resizeImageToCustomSize(
      MultipartFile multipartFile, int targetWidth, int targetHeight, Mode resizeMode)
      throws IOException {
    File tmpFile = null;

    try {
      BufferedImage resizedImage =
          resize(
              ImageIO.read(multipartFile.getInputStream()), resizeMode, targetWidth, targetHeight);

      tmpFile = Files.createTempFile("org.hisp.dhis", ".tmp").toFile();

      ImageIO.write(
          resizedImage,
          Objects.requireNonNull(getExtension(multipartFile.getOriginalFilename())),
          tmpFile);

      FileItem fileItem =
          new DiskFileItemFactory()
              .createItem(
                  "file",
                  Files.probeContentType(tmpFile.toPath()),
                  false,
                  multipartFile.getOriginalFilename());

      try (InputStream in = new FileInputStream(tmpFile);
          OutputStream out = fileItem.getOutputStream()) {
        in.transferTo(out);
      }

      return new CommonsMultipartFile(fileItem);
    } catch (Exception e) {
      throw new IOException("Failed to resize image: " + e.getMessage());
    } finally {
      if (tmpFile != null && tmpFile.exists()) {
        Files.delete(tmpFile.toPath());
      }
    }
  }

  public static MultipartFile resizeIconToDefaultSize(MultipartFile multipartFile)
      throws IOException {
    return resizeImageToCustomSize(
        multipartFile, CUSTOM_ICON_TARGET_WIDTH, CUSTOM_ICON_TARGET_HEIGHT, Mode.FIT_EXACT);
  }

  public static MultipartFile resizeAvatarToDefaultSize(MultipartFile multipartFile)
      throws IOException {
    return resizeImageToCustomSize(
        multipartFile, AVATAR_TARGET_WIDTH, AVATAR_TARGET_HEIGHT, Mode.AUTOMATIC);
  }

  public MultipartFile resizeOrgToDefaultSize(MultipartFile multipartFile) throws IOException {
    return resizeImageToCustomSize(
        multipartFile, ORGUNIT_TARGET_WIDTH, ORGUNIT_TARGET_HEIGHT, Mode.AUTOMATIC);
  }
}
