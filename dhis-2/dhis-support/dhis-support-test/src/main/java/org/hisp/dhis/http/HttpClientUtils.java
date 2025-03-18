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
package org.hisp.dhis.http;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.http.HttpClientAdapter.RequestComponent;

/**
 * Helpers needed when testing with {@link HttpClientAdapter} and {@code
 * org.springframework.test.web.servlet.MockMvc}.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpClientUtils {

  static String substitutePlaceholders(String url, Object... args) {
    if (args.length == 0) return url;

    Object[] urlArgs =
        Stream.of(args)
            .filter(arg -> !(arg instanceof RequestComponent) && !(arg instanceof Path))
            .map(arg -> arg == null ? "" : arg)
            .toArray();
    return String.format(url.replaceAll("\\{[a-zA-Z]+}", "%s"), urlArgs);
  }

  static RequestComponent[] requestComponentsIn(Object... args) {
    return Stream.of(args)
        .map(
            arg ->
                arg instanceof Path path
                    ? new HttpClientAdapter.Body(fileContent(path.toString()))
                    : arg)
        .filter(RequestComponent.class::isInstance)
        .toArray(RequestComponent[]::new);
  }

  @SuppressWarnings("unchecked")
  static <T extends RequestComponent> T getComponent(Class<T> type, RequestComponent[] components) {
    return (T)
        Stream.of(components)
            .filter(Objects::nonNull)
            .filter(c -> c.getClass() == type)
            .findFirst()
            .orElse(null);
  }

  static String fileContent(String filename) {
    try {
      return Files.readString(
          Path.of(
              Objects.requireNonNull(HttpClientUtils.class.getClassLoader().getResource(filename))
                  .toURI()),
          StandardCharsets.UTF_8);
    } catch (IOException | URISyntaxException e) {
      fail(e);
      return null;
    }
  }
}
