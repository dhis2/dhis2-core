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
package org.hisp.dhis.setting;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;

/**
 * @author Jan Bernitt (refactored version)
 */
public interface SystemSettingsService extends SystemSettingsProvider {

  /**
   * Called at the start of a new request to ensure fresh snapshot view of the current {@link
   * SystemSettings} is returned by {@link #getCurrentSettings()}
   */
  void clearCurrentSettings();

  /**
   * Saves a single setting with {@link Settings#valueOf(Serializable)} applied to the provided
   * value.
   *
   * <p>This method is for internal use only. Therefore, if the key does not exist no exception is
   * thrown but an error is logged.
   *
   * @param key of the setting to insert or update
   * @param value of the setting, null or empty to delete
   */
  void put(@Nonnull String key, @CheckForNull Serializable value);

  /**
   * Saves the given system setting key and value.
   *
   * @param settings the new values, null or empty values delete the setting
   * @throws NotFoundException if any of the provided keys is not a key contained in {@link
   *     SystemSettings#keysWithDefaults()}
   */
  void putAll(@Nonnull Map<String, String> settings) throws NotFoundException, BadRequestException;

  /**
   * Deletes the system setting with the given name.
   *
   * @param keys of the system setting to delete
   */
  void deleteAll(@Nonnull Set<String> keys);
}
