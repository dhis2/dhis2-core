/*
 * Copyright (c) 2004-2024, University of Oslo
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class CommonsMultipartFile implements MultipartFile {
  private static final Log logger = LogFactory.getLog(CommonsMultipartFile.class);
  private final FileItem fileItem;

  private final long size;

  private boolean preserveFilename = false;

  /**
   * Create an instance wrapping the given FileItem.
   *
   * @param fileItem the FileItem to wrap
   */
  public CommonsMultipartFile(FileItem fileItem) {
    this.fileItem = fileItem;
    this.size = this.fileItem.getSize();
  }

  /**
   * Return the underlying {@code org.apache.commons.fileupload.FileItem} instance. There is hardly
   * any need to access this.
   */
  public final FileItem getFileItem() {
    return this.fileItem;
  }

  @Override
  public String getName() {
    return this.fileItem.getFieldName();
  }

  @Override
  public String getOriginalFilename() {
    String filename = this.fileItem.getName();
    if (filename == null) {
      // Should never happen.
      return "";
    }
    if (this.preserveFilename) {
      // Do not try to strip off a path...
      return filename;
    }

    // Check for Unix-style path
    int unixSep = filename.lastIndexOf('/');
    // Check for Windows-style path
    int winSep = filename.lastIndexOf('\\');
    // Cut off at latest possible point
    int pos = Math.max(winSep, unixSep);
    if (pos != -1) {
      // Any sort of path separator found...
      return filename.substring(pos + 1);
    } else {
      // A plain name
      return filename;
    }
  }

  @Override
  public String getContentType() {
    return this.fileItem.getContentType();
  }

  @Override
  public boolean isEmpty() {
    return (this.size == 0);
  }

  @Override
  public long getSize() {
    return this.size;
  }

  @Override
  public byte[] getBytes() {
    if (!isAvailable()) {
      throw new IllegalStateException("File has been moved - cannot be read again");
    }
    byte[] bytes = this.fileItem.get();
    return (bytes != null ? bytes : new byte[0]);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (!isAvailable()) {
      throw new IllegalStateException("File has been moved - cannot be read again");
    }
    InputStream inputStream = this.fileItem.getInputStream();
    return (inputStream != null ? inputStream : StreamUtils.emptyInput());
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    if (!isAvailable()) {
      throw new IllegalStateException("File has already been moved - cannot be transferred again");
    }

    if (dest.exists() && !dest.delete()) {
      throw new IOException(
          "Destination file ["
              + dest.getAbsolutePath()
              + "] already exists and could not be deleted");
    }

    try {
      this.fileItem.write(dest);
      LogFormatUtils.traceDebug(
          logger,
          traceOn -> {
            String action = "transferred";
            if (!this.fileItem.isInMemory()) {
              action = (isAvailable() ? "copied" : "moved");
            }
            return "Part '"
                + getName()
                + "',  filename '"
                + getOriginalFilename()
                + "'"
                + (traceOn ? ", stored " + getStorageDescription() : "")
                + ": "
                + action
                + " to ["
                + dest.getAbsolutePath()
                + "]";
          });
    } catch (FileUploadException ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    } catch (IllegalStateException | IOException ex) {
      // Pass through IllegalStateException when coming from FileItem directly,
      // or propagate an exception from I/O operations within FileItem.write
      throw ex;
    } catch (Exception ex) {
      throw new IOException("File transfer failed", ex);
    }
  }

  /**
   * Determine whether the multipart content is still available. If a temporary file has been moved,
   * the content is no longer available.
   */
  protected boolean isAvailable() {
    // If in memory, it's available.
    if (this.fileItem.isInMemory()) {
      return true;
    }
    // Check actual existence of temporary file.
    if (this.fileItem instanceof DiskFileItem) {
      return ((DiskFileItem) this.fileItem).getStoreLocation().exists();
    }
    // Check whether current file size is different than original one.
    return (this.fileItem.getSize() == this.size);
  }

  /**
   * Return a description for the storage location of the multipart content. Tries to be as specific
   * as possible: mentions the file location in case of a temporary file.
   */
  public String getStorageDescription() {
    if (this.fileItem.isInMemory()) {
      return "in memory";
    } else if (this.fileItem instanceof DiskFileItem) {
      return "at [" + ((DiskFileItem) this.fileItem).getStoreLocation().getAbsolutePath() + "]";
    } else {
      return "on disk";
    }
  }

  @Override
  public String toString() {
    return "MultipartFile[field=\""
        + this.fileItem.getFieldName()
        + "\""
        + (this.fileItem.getName() != null ? ", filename=" + this.fileItem.getName() : "")
        + (this.fileItem.getContentType() != null
            ? ", contentType=" + this.fileItem.getContentType()
            : "")
        + ", size="
        + this.fileItem.getSize()
        + "]";
  }
}
