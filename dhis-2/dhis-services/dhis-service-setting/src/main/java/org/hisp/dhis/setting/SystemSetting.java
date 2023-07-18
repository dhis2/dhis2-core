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
package org.hisp.dhis.setting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;

/**
 * @author Stian Strandli
 */
@Slf4j
public class SystemSetting implements Serializable {
  private static final ObjectMapper objectMapper = JacksonObjectMapperConfig.staticJsonMapper();

  private long id;

  private String name;

  private String value;

  private transient Serializable displayValue;

  protected Map<String, String> translations = new HashMap<>();

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasValue() {
    return value != null;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Should be used only by Spring/Hibernate. */
  public void setValue(String value) {
    this.value = value;
  }

  /** Should be used only by Spring/Hibernate. */
  public String getValue() {
    return value;
  }

  public void setDisplayValue(Serializable displayValue) {
    this.displayValue = displayValue;
    try {
      this.value = objectMapper.writeValueAsString(displayValue);
    } catch (JsonProcessingException e) {
      log.error(String.format("An error occurred while serializing system setting: '%s'", name), e);
    }
  }

  public Serializable getDisplayValue() {
    if (displayValue == null) {
      displayValue = convertValueToSerializable();
    }

    return displayValue;
  }

  private Serializable convertValueToSerializable() {
    Serializable valueAsSerializable = null;
    if (hasValue()) {
      Optional<SettingKey> settingKey = SettingKey.getByName(name);

      try {
        if (settingKey.isPresent()) {
          Object valueAsObject = objectMapper.readValue(value, settingKey.get().getClazz());
          valueAsSerializable = (Serializable) valueAsObject;
        } else {
          valueAsSerializable = StringEscapeUtils.unescapeJava(value);
        }
      } catch (MismatchedInputException ex) {
        log.warn("Content could not be de-serialized by Jackson", ex);
        valueAsSerializable = StringEscapeUtils.unescapeJava(value);
      } catch (JsonProcessingException ex) {
        log.error(
            String.format("An error occurred while de-serializing system setting: '%s'", name), ex);
      }
    }

    return valueAsSerializable;
  }

  public Map<String, String> getTranslations() {
    return translations;
  }

  public void setTranslations(Map<String, String> translations) {
    if (translations != null) {
      this.translations = new HashMap<>(translations);
    } else {
      this.translations.clear();
    }
  }

  public Optional<String> getTranslation(String locale) {
    return Optional.ofNullable(translations.get(locale));
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (!(o instanceof SystemSetting)) {
      return false;
    }

    final SystemSetting other = (SystemSetting) o;

    return name.equals(other.getName());
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;

    result = result * prime + name.hashCode();

    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SystemSetting.class.getSimpleName() + "[", "]")
        .add("id=" + id)
        .add("name='" + name + "'")
        .add("value='" + value + "'")
        .add("displayValue=" + displayValue)
        .add("translations=" + translations)
        .toString();
  }
}
