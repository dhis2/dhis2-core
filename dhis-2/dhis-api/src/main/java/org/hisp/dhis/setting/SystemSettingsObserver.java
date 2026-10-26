/*
 * Copyright (c) 2004-2026, University of Oslo
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

import java.util.Set;

/**
 * Can be implemented by Spring beans to observe {@link SystemSettings} changes.
 *
 * <p>The method names are intentionally specific as we assume that they are implemented by other
 * services where their name has to transport the context of settings.
 *
 * @author Jan Bernitt
 * @since 2.44
 */
@FunctionalInterface
public interface SystemSettingsObserver {

  /**
   * @return the keys the listener wants to observe
   */
  Set<String> observedSystemSettingsKeys();

  /**
   * Observers are notified after the DB update has taken place. Exceptions thrown by observers are
   * logged but otherwise ignored.
   *
   * @param key the setting that changed
   * @param value the new value
   */
  default void onSystemSettingPut(String key, String value) {
    // override to handle
  }

  /**
   * Observers are notified after the DB update has taken place. Exceptions thrown by observers are
   * logged but otherwise ignored.
   *
   * @param key the setting that got cleared (null value)
   */
  default void onSystemSettingsDelete(String key) {
    // override to handle
  }
}
