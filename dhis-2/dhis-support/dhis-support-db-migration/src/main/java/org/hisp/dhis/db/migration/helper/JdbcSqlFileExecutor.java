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
package org.hisp.dhis.db.migration.helper;

import java.io.*;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.FlywayException;

/**
 * Reads sql commands from an external/internal file (provided as buffered reader) and executes them
 * onto the given jdbc connection. This is used to create the base schema (if db is empy) by reading
 * from the sql script packaged as classpath resource.
 *
 * @author Ameen Mohamed
 */
@Slf4j
public class JdbcSqlFileExecutor {
  private static final String DEFAULT_DELIMITER = ";";

  /**
   * regex to detect delimiter. ignores spaces, allows delimiter in comment, allows an equals-sign
   */
  public static final Pattern delimP =
      Pattern.compile(
          "^\\s*(--)?\\s*delimiter\\s*=?\\s*([^\\s]+)+\\s*.*$", Pattern.CASE_INSENSITIVE);

  private final Connection connection;

  private final boolean stopOnError;

  private final boolean autoCommit;

  private String delimiter = DEFAULT_DELIMITER;

  private boolean fullLineDelimiter = false;

  /** Default constructor */
  public JdbcSqlFileExecutor(Connection connection, boolean autoCommit, boolean stopOnError) {
    this.connection = connection;
    this.autoCommit = autoCommit;
    this.stopOnError = stopOnError;
  }

  public void setDelimiter(String delimiter, boolean fullLineDelimiter) {
    this.delimiter = delimiter;
    this.fullLineDelimiter = fullLineDelimiter;
  }

  /**
   * Runs an SQL script (read in using the Reader parameter)
   *
   * @param reader - the source of the script
   */
  public void runScript(Reader reader) throws IOException, SQLException {
    try {
      boolean originalAutoCommit = connection.getAutoCommit();
      try {
        if (originalAutoCommit != this.autoCommit) {
          connection.setAutoCommit(this.autoCommit);
        }
        runScript(connection, reader);
      } finally {
        connection.setAutoCommit(originalAutoCommit);
      }
    } catch (IOException | SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new FlywayException("Error running script.  Cause: " + e, e);
    }
  }

  /**
   * Runs an SQL script (read in using the Reader parameter) using the connection passed in
   *
   * @param conn - the connection to use for the script
   * @param reader - the source of the script
   * @throws SQLException if any SQL errors occur
   * @throws IOException if there is an error reading from the Reader
   */
  private void runScript(Connection conn, Reader reader) throws IOException, SQLException {
    StringBuilder command = null;
    try {
      LineNumberReader lineReader = new LineNumberReader(reader);
      String line;
      while ((line = lineReader.readLine()) != null) {
        if (command == null) {
          command = new StringBuilder();
        }
        String trimmedLine = line.trim();
        final Matcher delimMatch = delimP.matcher(trimmedLine);
        if (trimmedLine.length() < 1 || trimmedLine.startsWith("//")) {
          // Do nothing
        } else if (delimMatch.matches()) {
          setDelimiter(delimMatch.group(2), false);
        } else if (trimmedLine.startsWith("--")) {
          log.debug(trimmedLine);
        } else if (trimmedLine.length() < 1 || trimmedLine.startsWith("--")) {
          // Do nothing
        } else if (!fullLineDelimiter && trimmedLine.endsWith(this.delimiter)
            || fullLineDelimiter && trimmedLine.equals(this.delimiter)) {
          command.append(line.substring(0, line.lastIndexOf(this.delimiter)));
          command.append(" ");
          execCommand(conn, command, lineReader);
          command = null;
        } else {
          command.append(line);
          command.append("\n");
        }
      }
      if (command != null) {
        this.execCommand(conn, command, lineReader);
      }
      if (!autoCommit) {
        conn.commit();
      }
    } catch (IOException e) {
      throw new IOException(String.format("Error executing '%s': %s", command, e.getMessage()), e);
    } finally {
      conn.rollback();
    }
  }

  private void execCommand(Connection conn, StringBuilder command, LineNumberReader lineReader)
      throws SQLException {
    Statement statement = conn.createStatement();

    log.debug(command.toString());

    boolean hasResults = false;
    try {
      hasResults = statement.execute(command.toString());
    } catch (SQLException e) {
      final String errText =
          String.format(
              "Error executing '%s' (line %d): %s",
              command, lineReader.getLineNumber(), e.getMessage());
      log.error(errText);
      if (stopOnError) {
        throw new SQLException(errText, e);
      }
    }

    if (autoCommit && !conn.getAutoCommit()) {
      conn.commit();
    }

    ResultSet rs = statement.getResultSet();
    if (hasResults && rs != null) {
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();
      for (int i = 1; i <= cols; i++) {
        String name = md.getColumnLabel(i);
        log.debug(name + "\t");
      }
      log.debug("");
      while (rs.next()) {
        for (int i = 1; i <= cols; i++) {
          String value = rs.getString(i);
          log.debug(value + "\t");
        }
        log.debug("");
      }
    }

    try {
      statement.close();
    } catch (Exception e) {
      // Ignore to workaround a bug in Jakarta DBCP
    }
  }
}
