package org.hisp.dhis.web.appbundler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class representing information about the bundled apps.
 * This is serialized to JSON and stored in the apps-bundle.json file.
 */
public class AppBundleInfo {
    @JsonProperty
    private String buildDate;

    @JsonProperty
    private List<AppInfo> apps = new ArrayList<>();

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

    /**
     * Class representing information about a single bundled app.
     */
    public static class AppInfo {
        @JsonProperty
        private String name;

        @JsonProperty
        private String url;

        @JsonProperty
        private String branch;

        @JsonProperty
        private String etag;

        @JsonProperty
        private String downloadDate;

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