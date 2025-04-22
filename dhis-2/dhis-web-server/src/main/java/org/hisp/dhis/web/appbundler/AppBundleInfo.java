/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.web.appbundler;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class representing information about the bundled apps. This is serialized to JSON and stored in
 * the apps-bundle.json file.
 */
public class AppBundleInfo {
  @JsonProperty private String buildDate;

  @JsonProperty private List<AppInfo> apps = new ArrayList<>();

  public AppBundleInfo() {
    this.buildDate = new Date().toString();
  }

  public String getBuildDate() {
    return buildDate;
  }

  public void setBuildDate(String buildDate) {
    this.buildDate = buildDate;
  }

  public List<AppInfo> getApps() {
    return apps;
  }

  public void setApps(List<AppInfo> apps) {
    this.apps = apps;
  }

  public void addApp(AppInfo app) {
    this.apps.add(app);
  }

  /** Class representing information about a single bundled app. */
  public static class AppInfo {
    @JsonProperty private String name;

    @JsonProperty private String url;

    @JsonProperty private String branch;

    @JsonProperty private String etag;

    @JsonProperty private String downloadDate;

    public AppInfo() {
      this.downloadDate = new Date().toString();
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getBranch() {
      return branch;
    }

    public void setBranch(String branch) {
      this.branch = branch;
    }

    public String getEtag() {
      return etag;
    }

    public void setEtag(String etag) {
      this.etag = etag;
    }

    public String getDownloadDate() {
      return downloadDate;
    }

    public void setDownloadDate(String downloadDate) {
      this.downloadDate = downloadDate;
    }
  }
}
