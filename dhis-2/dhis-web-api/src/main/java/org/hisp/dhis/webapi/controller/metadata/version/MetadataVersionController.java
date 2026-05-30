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
package org.hisp.dhis.webapi.controller.metadata.version;

import static org.hisp.dhis.security.Authorities.F_METADATA_MANAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.webapi.controller.exception.MetadataVersionException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for MetadataVersion
 *
 * @author aamerm
 */
@OpenApi.Document(entity = MetadataVersion.class)
@Controller
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequestMapping("/api/metadata")
public class MetadataVersionController {
  @Autowired private SystemSettingsProvider settingsProvider;

  @Autowired private MetadataVersionService versionService;

  @Autowired private ContextUtils contextUtils;

  // Gets the version by versionName or latest system version
  @GetMapping(value = "/version", produces = ContextUtils.CONTENT_TYPE_JSON)
  public @ResponseBody MetadataVersion getMetaDataVersion(
      @RequestParam(value = "versionName", required = false) String versionName)
      throws MetadataVersionException, BadRequestException {
    MetadataVersion versionToReturn = null;
    boolean enabled = isMetadataVersioningEnabled();

    try {
      if (!enabled) {
        throw new BadRequestException("Metadata versioning is not enabled for this instance.");
      }

      if (StringUtils.isNotEmpty(versionName)) {
        versionToReturn = versionService.getVersionByName(versionName);

        if (versionToReturn == null) {
          throw new MetadataVersionException(
              "No metadata version with name "
                  + versionName
                  + " exists. Please check again later.");
        }

      } else {
        versionToReturn = versionService.getCurrentVersion();

        if (versionToReturn == null) {
          throw new MetadataVersionException(
              "No metadata versions exist. Please check again later.");
        }
      }

      return versionToReturn;
    } catch (MetadataVersionServiceException ex) {
      throw new MetadataVersionException(
          "Exception occurred while getting metadata version."
              + (StringUtils.isNotEmpty(versionName) ? versionName : " ")
              + ex.getMessage(),
          ex);
    }
  }

  // Gets the list of all versions in between the passed version name and
  // latest system version
  @GetMapping(value = "/version/history", produces = ContextUtils.CONTENT_TYPE_JSON)
  public @ResponseBody RootNode getMetaDataVersionHistory(
      @RequestParam(value = "baseline", required = false) String versionName)
      throws MetadataVersionException, BadRequestException {
    List<MetadataVersion> allVersionsInBetween = new ArrayList<>();
    boolean enabled = isMetadataVersioningEnabled();

    try {

      if (!enabled) {
        throw new BadRequestException("Metadata versioning is not enabled for this instance.");
      }

      Date startDate;

      if (versionName == null || versionName.isEmpty()) {
        MetadataVersion initialVersion = versionService.getInitialVersion();

        if (initialVersion == null) {
          return getMetadataVersionsAsNode(allVersionsInBetween);
        }

        startDate = initialVersion.getCreated();
      } else {
        startDate = versionService.getCreatedDate(versionName);
      }

      if (startDate == null) {
        throw new MetadataVersionException(
            "There is no such metadata version. The latest version is Version "
                + versionService.getCurrentVersion().getName());
      }

      Date endDate = new Date();
      allVersionsInBetween = versionService.getAllVersionsInBetween(startDate, endDate);

      if (allVersionsInBetween != null) {
        // now remove the baseline version details
        for (Iterator<MetadataVersion> iterator = allVersionsInBetween.iterator();
            iterator.hasNext(); ) {
          MetadataVersion m = iterator.next();

          if (m.getName().equals(versionName)) {
            iterator.remove();
            break;
          }
        }

        if (!allVersionsInBetween.isEmpty()) {
          return getMetadataVersionsAsNode(allVersionsInBetween);
        }
      }

    } catch (MetadataVersionServiceException ex) {
      throw new MetadataVersionException(ex.getMessage(), ex);
    }
    return null;
  }

  // Gets the list of all versions
  @GetMapping(value = "/versions", produces = ContextUtils.CONTENT_TYPE_JSON)
  public @ResponseBody RootNode getAllVersion()
      throws MetadataVersionException, BadRequestException {
    boolean enabled = isMetadataVersioningEnabled();

    try {
      if (!enabled) {
        throw new BadRequestException("Metadata versioning is not enabled for this instance.");
      }

      List<MetadataVersion> allVersions = versionService.getAllVersions();
      return getMetadataVersionsAsNode(allVersions);

    } catch (MetadataVersionServiceException ex) {
      throw new MetadataVersionException(
          "Exception occurred while getting all metadata versions. " + ex.getMessage());
    }
  }

  // Creates version in versioning table, exports the metadata and saves the
  // snapshot in datastore
  @RequiresAuthority(anyOf = F_METADATA_MANAGE)
  @PostMapping(value = "/version/create", produces = ContextUtils.CONTENT_TYPE_JSON)
  public @ResponseBody MetadataVersion createSystemVersion(
      @RequestParam(value = "type") VersionType versionType)
      throws MetadataVersionException, BadRequestException {
    MetadataVersion versionToReturn = null;
    boolean enabled = isMetadataVersioningEnabled();

    try {
      if (!enabled) {
        throw new BadRequestException("Metadata versioning is not enabled for this instance.");
      }

      synchronized (versionService) {
        versionService.saveVersion(versionType);
        versionToReturn = versionService.getCurrentVersion();
        return versionToReturn;
      }

    } catch (MetadataVersionServiceException ex) {
      throw new MetadataVersionException("Unable to create version in system. " + ex.getMessage());
    }
  }

  // endpoint to download metadata
  @RequiresAuthority(anyOf = F_METADATA_MANAGE)
  @GetMapping(value = "/version/{versionName}/data", produces = APPLICATION_JSON_VALUE)
  public void downloadVersion(
      @PathVariable("versionName") String versionName, HttpServletResponse response)
      throws MetadataVersionException, BadRequestException, IOException {
    requireVersioningEnabledAndSnapshotExists(versionName);

    try {
      response.setContentType(APPLICATION_JSON_VALUE);
      // Snapshots are downloaded once per remote sync client and never re-requested.
      // Tell intermediaries not to spend cache space on a body nobody will ask for again.
      response.setHeader(ContextUtils.HEADER_CACHE_CONTROL, "no-store");
      versionService.streamVersionData(versionName, response.getOutputStream());
    } catch (MetadataVersionServiceException ex) {
      throw new MetadataVersionException(
          "Unable to download version from system: " + versionName + ex.getMessage());
    }
  }

  // endpoint to download metadata in gzip format
  @RequiresAuthority(anyOf = F_METADATA_MANAGE)
  @GetMapping(value = "/version/{versionName}/data.gz", produces = "*/*")
  public void downloadGZipVersion(
      @PathVariable("versionName") String versionName, HttpServletResponse response)
      throws MetadataVersionException, IOException, BadRequestException {
    requireVersioningEnabledAndSnapshotExists(versionName);

    try {
      contextUtils.configureResponse(
          response,
          ContextUtils.CONTENT_TYPE_GZIP,
          CacheStrategy.NO_CACHE,
          "metadata.json.gz",
          true);
      response.addHeader(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary");
      // CacheStrategy.NO_CACHE maps to Cache-Control: no-cache (revalidate, may store).
      // Override with no-store: snapshots are fetched once per remote, so caching wastes space.
      response.setHeader(ContextUtils.HEADER_CACHE_CONTROL, "no-store");

      try (GZIPOutputStream gos = new GZIPOutputStream(response.getOutputStream())) {
        versionService.streamVersionData(versionName, gos);
      }
    } catch (MetadataVersionServiceException ex) {
      throw new MetadataVersionException(
          "Unable to download version from system: " + versionName + ex.getMessage());
    }
  }

  /**
   * Pre-flight check before opening any response output stream. Once {@code GZIPOutputStream} is
   * constructed it writes the gzip magic header to the response, committing it — at which point a
   * thrown error can no longer reach the client (Spring's exception handler can't rewrite a
   * committed response). This method must run before any byte is written.
   */
  private void requireVersioningEnabledAndSnapshotExists(String versionName)
      throws BadRequestException, MetadataVersionException {
    if (!isMetadataVersioningEnabled()) {
      throw new BadRequestException("Metadata versioning is not enabled for this instance.");
    }
    if (!versionService.snapshotExists(versionName)) {
      throw new MetadataVersionException(
          "No metadata version snapshot found for the given version " + versionName);
    }
  }

  // ----------------------------------------------------------------------------------------
  // Private Methods
  // ----------------------------------------------------------------------------------------

  private boolean isMetadataVersioningEnabled() {
    return settingsProvider.getCurrentSettings().getVersionEnabled();
  }

  private RootNode getMetadataVersionsAsNode(List<MetadataVersion> versions) {
    RootNode rootNode = NodeUtils.createRootNode("metadataversions");
    CollectionNode collectionNode = new CollectionNode("metadataversions", true);
    rootNode.addChild(collectionNode);

    for (MetadataVersion version : versions) {
      ComplexNode complexNode = new ComplexNode("");
      complexNode.addChild(new SimpleNode("name", version.getName()));
      complexNode.addChild(new SimpleNode("type", version.getType()));
      complexNode.addChild(new SimpleNode("created", version.getCreated()));
      complexNode.addChild(new SimpleNode("id", version.getUid()));
      complexNode.addChild(new SimpleNode("importdate", version.getImportDate()));
      complexNode.addChild(new SimpleNode("hashCode", version.getHashCode()));

      collectionNode.addChild(complexNode);
    }

    return rootNode;
  }
}
