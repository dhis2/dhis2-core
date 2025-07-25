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
package org.hisp.dhis.appmanager;

import lombok.Getter;

@Getter
public enum AppStatus {
  OK("ok"),
  INVALID_BUNDLED_APP_OVERRIDE("invalid_bundled_app_override"),
  INVALID_CORE_APP("invalid_core_app"),
  NAMESPACE_TAKEN("namespace_defined_in_manifest_is_in_use"),
  NAMESPACE_INVALID("namespace_invalid"),
  INVALID_ZIP_FORMAT("zip_file_could_not_be_read"),
  MISSING_MANIFEST("missing_manifest"),
  INVALID_MANIFEST_JSON("invalid_json_in_app_manifest_file"),
  INSTALLATION_FAILED("app_could_not_be_installed_on_file_system"),
  NOT_FOUND("app_could_not_be_found"),
  MISSING_SYSTEM_BASE_URL("system_base_url_is_not_defined"),
  APPROVED("approved"),
  PENDING("pending"),
  NOT_APPROVED("not_approved"),
  FAILED_TO_WRITE_BUNDLED_APP_INFO("failed_to_write_bundled_app_info");

  private final String message;

  AppStatus(String message) {
    this.message = message;
  }

  public boolean ok() {
    return this == OK;
  }
}
