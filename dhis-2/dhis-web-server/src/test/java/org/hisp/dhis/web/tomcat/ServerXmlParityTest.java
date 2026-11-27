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
package org.hisp.dhis.web.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Embedded Tomcat keeps the settings of the server.xml of the standalone Tomcat images. */
class ServerXmlParityTest {
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  @ParameterizedTest
  @CsvSource({
    ",,,''",
    ",,/dhis2,/dhis2",
    ",/env,/dhis2,/env",
    "/property,/env,/dhis2,/property",
    "'',,/dhis2,''",
    ",,/,''",
    "/,,,''"
  })
  void resolvesContextPathWithTomcatPropertyAsFallback(
      String property, String environment, String tomcatProperty, String expected) {
    assertEquals(expected, Main.contextPath(property, environment, tomcatProperty));
  }

  @Test
  void configuresConnectorLikeServerXml() {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");

    Main.configureConnector(connector);

    assertEquals("\\ { } | [ ]", connector.getProperty("relaxedQueryChars"));
    assertEquals(Main.CONNECTION_TIMEOUT_MILLIS, connector.getProperty("connectionTimeout"));
  }

  @Test
  void leavesSessionCookieDefaultsWithoutTomcatProperties() {
    StandardContext context = new StandardContext();

    Main.configureSessionCookie(context, null, "");

    assertNull(context.getSessionCookiePath());
    assertNull(context.getCookieProcessor());
  }

  @ParameterizedTest
  @CsvSource({"Lax,LAX", "strict,STRICT", "None,NONE", "unset,UNSET"})
  void appliesSessionCookieTomcatProperties(String sameSite, SameSiteCookies expected) {
    StandardContext context = new StandardContext();

    Main.configureSessionCookie(context, "/dhis2", sameSite);

    assertEquals("/dhis2", context.getSessionCookiePath());
    assertEquals(
        expected, ((Rfc6265CookieProcessor) context.getCookieProcessor()).getSameSiteCookies());
  }

  @Test
  void rejectsUnsupportedSameSiteValue() {
    Context context = new StandardContext();

    assertThrows(
        IllegalArgumentException.class,
        () -> Main.configureSessionCookie(context, null, "sometimes"));
  }

  @ParameterizedTest
  @CsvSource({
    ",,false",
    ",true,true",
    "false,true,false",
    "true,,true",
    "TRUE,,true",
    "yes,,false"
  })
  void resolvesAccessLogWithPropertyPrecedence(
      String property, String environment, boolean expected) {
    assertEquals(expected, Main.useAccessLog(property, environment));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void addsAccessLogValveOnlyWhenEnabled(boolean enabled, @TempDir Path baseDir) {
    Tomcat tomcat = Main.createTomcat(baseDir.toString(), 8080, false, null);
    Host host = tomcat.getHost();

    Main.configureAccessLog(host, enabled);

    long valves =
        Arrays.stream(host.getPipeline().getValves())
            .filter(StdoutAccessLogValve.class::isInstance)
            .count();
    assertEquals(enabled ? 1 : 0, valves);
  }

  @Test
  void writesAccessLogInCommonFormat(@TempDir Path baseDir) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    // Tomcat writes the access log after it has sent the response, so wait for the line.
    CountDownLatch logged = new CountDownLatch(1);
    PrintStream out =
        new PrintStream(output, true, StandardCharsets.UTF_8) {
          @Override
          public void println(Object line) {
            super.println(line);
            logged.countDown();
          }
        };
    Tomcat tomcat = Main.createTomcat(baseDir.toString(), 0, false, null);
    tomcat.getConnector();
    tomcat.getHost().getPipeline().addValve(new StdoutAccessLogValve(out));
    Context context = tomcat.addContext("", baseDir.toString());
    Tomcat.addServlet(context, "ok", new OkServlet());
    context.addServletMappingDecoded("/", "ok");
    tomcat.start();
    try {
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(
                      "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/hello?x=1"))
              .build();
      HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      assertTrue(logged.await(5, TimeUnit.SECONDS), "no access log line written");
      String log = output.toString(StandardCharsets.UTF_8);
      assertTrue(log.startsWith("127.0.0.1 - - ["), log);
      assertTrue(log.contains("] \"GET /hello?x=1 HTTP/1.1\" 200 2"), log);
    } finally {
      tomcat.stop();
      tomcat.destroy();
    }
  }

  @Test
  void listsOnlySetTomcatScriptVariables() {
    assertEquals(
        List.of("CATALINA_OPTS"),
        Main.ignoredTomcatScriptVariables(
            Map.of("CATALINA_OPTS", "-Dcontext.path=/dhis2", "JAVA_OPTS", " ", "PATH", "/bin")));
    assertEquals(
        List.of("CATALINA_OPTS", "JAVA_OPTS"),
        Main.ignoredTomcatScriptVariables(
            Map.of("CATALINA_OPTS", "-Xmx2g", "JAVA_OPTS", "-Xms1g")));
    assertTrue(Main.ignoredTomcatScriptVariables(Map.of()).isEmpty());
  }

  private static class OkServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      response.getWriter().write("ok");
    }
  }
}
