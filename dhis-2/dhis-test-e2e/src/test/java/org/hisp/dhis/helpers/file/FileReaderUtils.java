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
package org.hisp.dhis.helpers.file;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.utils.DataGenerator;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class FileReaderUtils {
  private Logger logger = LogManager.getLogger(FileReaderUtils.class.getName());

  public FileReader read(File file) throws Exception {

    if (file.getName().endsWith(".csv")) {
      return new CsvFileReader(file);
    }

    if (file.getName().endsWith(".json")) {
      return new JsonFileReader(file);
    }

    if (file.getName().endsWith("xml")) {
      return new XmlFileReader(file);
    }

    logger.warn(
        "Tried to read file "
            + file.getName()
            + ", but there is no reader implemented for this file type. ");

    return null;
  }

  /**
   * Reads json file and generates data where needed. Json file should indicate what data should be
   * generated. Format: %string%, %id%
   *
   * @param file
   * @return
   * @throws IOException
   */
  public JsonObject readJsonAndGenerateData(File file) throws Exception {
    return read(file)
        .replace(
            e -> {
              String json = e.toString();

              json = json.replaceAll("%id%", new IdGenerator().generateUniqueId());
              json = json.replaceAll("%string%", DataGenerator.randomString());

              return new JsonParser().parse(json);
            })
        .get(JsonObject.class);
  }
}
