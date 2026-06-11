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
package org.hisp.dhis.gist;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.schema.annotation.Gist;

/**
 * Parsing and working with the URL-parameter {@code fields} as used in Gist and metadata API.
 *
 * @author Jan Bernitt
 * @param fields
 */
public record Fields(List<Field> fields) implements Iterable<Fields.Field> {

  public static final Fields DEFAULT = new Fields(List.of());

  @Nonnull
  public static Fields of(String fields) {
    if (fields == null || fields.isEmpty()) return DEFAULT;
    List<Field> res = new ArrayList<>();
    for (Field.Expression e : Field.Expression.split(Text.of(fields))) e.addFields(res, "", "");
    return new Fields(List.copyOf(res));
  }

  public List<String> names() {
    return fields.stream().map(Fields.Field::name).toList();
  }

  public Fields add(Field f) {
    if (fields.isEmpty()) return new Fields(List.of(f));
    List<Field> concat = new ArrayList<>(fields.size() + 1);
    concat.addAll(fields);
    concat.add(f);
    return new Fields(List.copyOf(concat));
  }

  public int size() {
    return fields.size();
  }

  @Nonnull
  @Override
  public Iterator<Field> iterator() {
    return fields.iterator();
  }

  /**
   * @param propertyPath the path as found in the schema model
   * @param propertyName the name (also nested) as it should be rendered in the response (if
   *     different to the {@link #propertyPath()}) or empty if same as {@link #propertyPath()}
   * @param transformation
   * @param transformationArgument
   * @param attribute when true, the property is an attribute UID
   */
  public record Field(
      @JsonProperty String propertyPath,
      @JsonProperty String propertyName,
      @JsonProperty Gist.Transform transformation,
      @JsonProperty String transformationArgument,
      @JsonProperty boolean attribute) {

    public static final String REFS_PATH = "__refs__";
    public static final String ALL_PATH = "*";
    public static final Field ALL = new Field(ALL_PATH, Gist.Transform.NONE);

    public Field(String propertyPath) {
      this(propertyPath, Gist.Transform.AUTO);
    }

    public Field(String propertyPath, Gist.Transform transformation) {
      this(propertyPath, transformation, null);
    }

    public Field(
        String propertyPath, Gist.Transform transformation, String transformationArgument) {
      this(propertyPath, "", transformation, transformationArgument, false);
    }

    static Field of(String field) {
      String[] parts = field.split("(?:::|~|@)(?![^\\[\\]]*])");
      if (parts.length == 1) {
        return new Field(field, Gist.Transform.AUTO);
      }
      Gist.Transform transform = Gist.Transform.AUTO;
      String alias = "";
      String arg = null;
      for (int i = 1; i < parts.length; i++) {
        String part = parts[i];
        if (part.startsWith("rename")) {
          alias = parseArgument(part);
        } else {
          transform = Gist.Transform.parse(part);
          if (part.indexOf('(') >= 0) {
            arg = parseArgument(part);
          }
        }
      }
      return new Field(parts[0], alias, transform, arg, false);
    }

    @JsonProperty
    public String name() {
      return propertyName.isEmpty() ? propertyPath : propertyName;
    }

    public Field withTransformation(Gist.Transform transform) {
      return withTransformation(transform, null);
    }

    public Field withTransformation(Gist.Transform transform, String argument) {
      return new Field(propertyPath, propertyName, transform, argument, attribute);
    }

    public Field withPropertyPath(String path) {
      return new Field(path, propertyName, transformation, transformationArgument, attribute);
    }

    public Field withPropertyName(String name) {
      return new Field(propertyPath, name, transformation, transformationArgument, attribute);
    }

    public Field asAttribute() {
      return new Field(propertyPath, propertyName, transformation, transformationArgument, true);
    }

    public boolean isMultiPluck() {
      return transformation == Gist.Transform.PLUCK
          && transformationArgument != null
          && transformationArgument.contains(",");
    }

    private static String parseArgument(String part) {
      return part.substring(part.indexOf('(') + 1, part.lastIndexOf(')'));
    }

    private record Expression(Text field, List<Expression> children) {

      void addFields(List<Field> res, String parentPath, String parentName) {
        Field f = of(field.toString());
        String path = f.propertyPath();
        if (!parentPath.isEmpty()) f = f.withPropertyPath(chain(parentPath, path));
        if (!parentPath.equals(parentName) && !parentName.isEmpty())
          f =
              f.withPropertyName(
                  chain(parentName, !f.propertyName().isEmpty() ? f.propertyName() : path));
        if (children.isEmpty()) {
          res.add(f);
        } else for (Expression e : children) e.addFields(res, f.propertyPath(), f.name());
      }

      static String chain(String parent, String child) {
        return parent.isEmpty() ? child : parent + "." + child;
      }

      static List<Expression> split(Text fields) {
        int s = 0;
        int len = fields.length();
        List<Expression> res = new ArrayList<>();
        while (s < len) {
          int nextComma = fields.indexOf(',', s);
          int nextSquare = fields.indexOf('[', s);
          int nextRound = fields.indexOf('(', s);
          if (nextRound >= 0 && nextRound < nextComma)
            nextComma = fields.indexOf(',', fields.indexOf(')', s));
          if (nextComma > 0 && (nextSquare < 0 || nextComma < nextSquare)) {
            // until comma; (no [...] but maybe transform)
            res.add(new Expression(fields.subSequence(s, nextComma), List.of()));
            s = nextComma + 1;
          } else if (nextSquare > 0) {
            // until [ with children
            Text field = fields.subSequence(s, nextSquare);
            s = skipToClosingBracket(fields, nextSquare + 1);
            res.add(new Expression(field, split(fields.subSequence(nextSquare + 1, s))));
            s++; // now skip the ]
            if (s < len && fields.charAt(s) == ',') s++; // skip , after [...]
          } else {
            // until end of input...
            res.add(new Expression(fields.subSequence(s, len), List.of()));
            s = len;
          }
        }
        return res;
      }

      private static int skipToClosingBracket(Text fields, int from) {
        int open = 0;
        int i = from;
        int len = fields.length();
        while (i < len) {
          char c = fields.charAt(i++);
          if (c == ']') {
            if (open == 0) return i - 1;
            open--;
          } else if (c == '[') open++;
        }
        return len; // treat end as ]
      }
    }
  }
}
