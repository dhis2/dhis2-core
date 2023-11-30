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
package org.hisp.dhis.dataintegrity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.jsonschema.JsonSchemaValidator;
import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;

/**
 * Reads {@link DataIntegrityCheck}s from YAML files.
 *
 * @author Jan Bernitt
 */
@Slf4j
class DataIntegrityYamlReader {
  private DataIntegrityYamlReader() {
    throw new UnsupportedOperationException("util");
  }

  static class ListYamlFile {
    @JsonProperty List<String> checks;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class CheckYamlFile {
    @JsonProperty String name;

    @JsonProperty String description;

    @JsonProperty String section;

    @JsonProperty("section_order")
    int sectionOrder;

    @JsonProperty("summary_sql")
    String summarySql;

    @JsonProperty("details_sql")
    String detailsSql;

    @JsonProperty("details_id_type")
    String detailsIdType;

    @JsonProperty("is_slow")
    boolean isSlow;

    @JsonProperty String introduction;

    @JsonProperty String recommendation;

    @JsonProperty DataIntegritySeverity severity;
  }

  public static void readDataIntegrityYaml(
      DefaultDataIntegrityService.DataIntegrityRecord dataIntegrityRecord) {
    ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    ListYamlFile file;

    AbstractFileResolvingResource resource =
        getResourceFromType(
            dataIntegrityRecord.resourceLocation(), dataIntegrityRecord.yamlFileChecks());

    if (resource == null) {
      log.warn(
          "Failed to get resource from location `{}` and with file`{}`",
          dataIntegrityRecord.resourceLocation(),
          dataIntegrityRecord.yamlFileChecks());
      return;
    }

    try (InputStream is = resource.getInputStream()) {
      file = yaml.readValue(is, ListYamlFile.class);
    } catch (Exception ex) {
      log.warn(
          "Failed to load data integrity check from YAML. Error message `{}`", ex.getMessage());
      return;
    }

    file.checks.forEach(
        dataIntegrityCheckFile -> processFile(dataIntegrityCheckFile, yaml, dataIntegrityRecord));
  }

  /**
   * This method processes a {@link DataIntegrityCheck} file by:
   *
   * <ol>
   *   <li>resolving the file path
   *   <li>converting file to {@link JsonNode}
   *   <li>validate JsonNode against {@link JsonSchema}
   *   <li>Then either
   *       <ol>
   *         <li>add {@link DataIntegrityCheck} to map if no validation errors or
   *         <li>log a warning if any validation errors and do not add {@link DataIntegrityCheck} to
   *             map
   *       </ol>
   * </ol>
   *
   * @param dataIntegrityCheckFile the yaml file version of a {@link DataIntegrityCheck}
   * @param yaml object mapper
   * @param dataIntegrityRecord record used for storing {@link DataIntegrityCheck} details used for
   *     processing
   */
  private static void processFile(
      String dataIntegrityCheckFile,
      ObjectMapper yaml,
      DefaultDataIntegrityService.DataIntegrityRecord dataIntegrityRecord) {

    try {
      String path =
          Path.of(dataIntegrityRecord.yamlFileChecks())
              .resolve("..")
              .resolve(dataIntegrityRecord.checksDir())
              .resolve(dataIntegrityCheckFile)
              .toString();

      AbstractFileResolvingResource resource =
          getResourceFromType(dataIntegrityRecord.resourceLocation(), path);

      if (resource == null) {
        log.warn(
            "Failed to get resource from location `{}` and with file`{}`",
            dataIntegrityRecord.resourceLocation(),
            path);
        return;
      }

      try (InputStream is = resource.getInputStream()) {

        JsonNode jsonNode = yaml.readValue(is, JsonNode.class);
        Set<ValidationMessage> validationMessages =
            JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);

        if (validationMessages.isEmpty()) {
          CheckYamlFile yamlFile = yaml.convertValue(jsonNode, CheckYamlFile.class);
          acceptDataIntegrityCheck(dataIntegrityRecord, yamlFile);
        } else {
          log.warn(
              "JsonSchema validation errors found for Data Integrity Check `{}`. Errors: {}",
              dataIntegrityCheckFile,
              validationMessages);
        }
      }
    } catch (Exception ex) {
      log.error(
          "Failed to load data integrity check `{}` with error message {}",
          dataIntegrityCheckFile,
          ex.getMessage());
    }
  }

  private static void acceptDataIntegrityCheck(
      DefaultDataIntegrityService.DataIntegrityRecord dataIntegrityRecord, CheckYamlFile yamlFile) {
    String name = yamlFile.name.trim();
    dataIntegrityRecord
        .adder()
        .accept(
            DataIntegrityCheck.builder()
                .name(name)
                .displayName(
                    dataIntegrityRecord.info().apply(name + ".name", name.replace('_', ' ')))
                .description(
                    dataIntegrityRecord
                        .info()
                        .apply(name + ".description", trim(yamlFile.description)))
                .introduction(
                    dataIntegrityRecord
                        .info()
                        .apply(name + ".introduction", trim(yamlFile.introduction)))
                .recommendation(
                    dataIntegrityRecord
                        .info()
                        .apply(name + ".recommendation", trim(yamlFile.recommendation)))
                .issuesIdType(trim(yamlFile.detailsIdType))
                .section(trim(yamlFile.section))
                .sectionOrder(yamlFile.sectionOrder)
                .severity(yamlFile.severity)
                .isSlow(yamlFile.isSlow)
                .runSummaryCheck(
                    dataIntegrityRecord.sqlToSummary().apply(sanitiseSQL(yamlFile.summarySql)))
                .runDetailsCheck(
                    dataIntegrityRecord.sqlToDetails().apply(sanitiseSQL(yamlFile.detailsSql)))
                .build());
  }

  private static AbstractFileResolvingResource getResourceFromType(
      ResourceLocation resourceLocation, String filePath) {
    AbstractFileResolvingResource resource = null;
    try {
      resource =
          resourceLocation == ResourceLocation.CLASS_PATH
              ? new ClassPathResource(filePath)
              : new UrlResource("file://" + filePath);
    } catch (Exception ex) {
      log.warn(
          "Failed to load data integrity checks from YAML file path `{}`. Error message: {}",
          filePath,
          ex.getMessage());
    }
    return resource;
  }

  private static String trim(String str) {
    return str == null ? null : str.trim();
  }

  /**
   * The purpose of this method is to strip some details from the SQL queries that are present for
   * their 2nd use case scenario but are not needed here and might confuse the database (even if
   * this is just in unit tests).
   */
  private static String sanitiseSQL(String sql) {
    return trim(
        sql.replaceAll("select '[^']+' as [^,]+,", "select ")
            .replaceAll("'[^']+' as description", "")
            .replace("::varchar", "")
            .replace("|| '%'", ""));
  }

  enum ResourceLocation {
    CLASS_PATH,
    FILE_SYSTEM
  }
}
