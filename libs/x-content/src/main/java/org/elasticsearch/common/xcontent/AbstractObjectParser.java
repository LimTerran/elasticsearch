/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.xcontent;

import org.elasticsearch.common.CheckedFunction;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ObjectParser.NamedObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Superclass for {@link ObjectParser} and {@link ConstructingObjectParser}. Defines most of the "declare" methods so they can be shared.
 */
public abstract class AbstractObjectParser<Value, Context> {

    /**
     * Declare some field. Usually it is easier to use {@link #declareString(BiConsumer, ParseField)} or
     * {@link #declareObject(BiConsumer, ContextParser, ParseField)} rather than call this directly.
     */
    public abstract <T> void declareField(BiConsumer<Value, T> consumer, ContextParser<Context, T> parser, ParseField parseField,
            ValueType type);

    /**
     * Declares a single named object.
     *
     * <pre>
     * <code>
     * {
     *   "object_name": {
     *     "instance_name": { "field1": "value1", ... }
     *     }
     *   }
     * }
     * </code>
     * </pre>
     *
     * @param consumer
     *            sets the value once it has been parsed
     * @param namedObjectParser
     *            parses the named object
     * @param parseField
     *            the field to parse
     */
    public abstract <T> void declareNamedObject(BiConsumer<Value, T> consumer, NamedObjectParser<T, Context> namedObjectParser,
                                                 ParseField parseField);


    /**
     * Declares named objects in the style of aggregations. These are named
     * inside and object like this:
     *
     * <pre>
     * <code>
     * {
     *   "aggregations": {
     *     "name_1": { "aggregation_type": {} },
     *     "name_2": { "aggregation_type": {} },
     *     "name_3": { "aggregation_type": {} }
     *     }
     *   }
     * }
     * </code>
     * </pre>
     *
     * Unlike the other version of this method, "ordered" mode (arrays of
     * objects) is not supported.
     *
     * See NamedObjectHolder in ObjectParserTests for examples of how to invoke
     * this.
     *
     * @param consumer
     *            sets the values once they have been parsed
     * @param namedObjectParser
     *            parses each named object
     * @param parseField
     *            the field to parse
     */
    public abstract <T> void declareNamedObjects(BiConsumer<Value, List<T>> consumer, NamedObjectParser<T, Context> namedObjectParser,
            ParseField parseField);

    /**
     * Declares named objects in the style of highlighting's field element.
     * These are usually named inside and object like this:
     *
     * <pre>
     * <code>
     * {
     *   "highlight": {
     *     "fields": {        &lt;------ this one
     *       "title": {},
     *       "body": {},
     *       "category": {}
     *     }
     *   }
     * }
     * </code>
     * </pre>
     *
     * but, when order is important, some may be written this way:
     *
     * <pre>
     * <code>
     * {
     *   "highlight": {
     *     "fields": [        &lt;------ this one
     *       {"title": {}},
     *       {"body": {}},
     *       {"category": {}}
     *     ]
     *   }
     * }
     * </code>
     * </pre>
     *
     * This is because json doesn't enforce ordering. Elasticsearch reads it in
     * the order sent but tools that generate json are free to put object
     * members in an unordered Map, jumbling them. Thus, if you care about order
     * you can send the object in the second way.
     *
     * See NamedObjectHolder in ObjectParserTests for examples of how to invoke
     * this.
     *
     * @param consumer
     *            sets the values once they have been parsed
     * @param namedObjectParser
     *            parses each named object
     * @param orderedModeCallback
     *            called when the named object is parsed using the "ordered"
     *            mode (the array of objects)
     * @param parseField
     *            the field to parse
     */
    public abstract <T> void declareNamedObjects(BiConsumer<Value, List<T>> consumer, NamedObjectParser<T, Context> namedObjectParser,
            Consumer<Value> orderedModeCallback, ParseField parseField);

    public abstract String getName();

    public <T> void declareField(BiConsumer<Value, T> consumer, CheckedFunction<XContentParser, T, IOException> parser,
            ParseField parseField, ValueType type) {
        if (parser == null) {
            throw new IllegalArgumentException("[parser] is required");
        }
        declareField(consumer, (p, c) -> parser.apply(p), parseField, type);
    }

    public <T> void declareObject(BiConsumer<Value, T> consumer, ContextParser<Context, T> objectParser, ParseField field) {
        declareField(consumer, (p, c) -> objectParser.parse(p, c), field, ValueType.OBJECT);
    }

    /**
     * Declare an object field that parses explicit {@code null}s in the json to a default value.
     */
    public <T> void declareObjectOrNull(BiConsumer<Value, T> consumer, ContextParser<Context, T> objectParser, T nullValue,
            ParseField field) {
        declareField(consumer, (p, c) -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : objectParser.parse(p, c),
                field, ValueType.OBJECT_OR_NULL);
    }

    public void declareFloat(BiConsumer<Value, Float> consumer, ParseField field) {
        // Using a method reference here angers some compilers
        declareField(consumer, p -> p.floatValue(), field, ValueType.FLOAT);
    }

    public void declareDouble(BiConsumer<Value, Double> consumer, ParseField field) {
        // Using a method reference here angers some compilers
        declareField(consumer, p -> p.doubleValue(), field, ValueType.DOUBLE);
    }

    /**
     * Declare a double field that parses explicit {@code null}s in the json to a default value.
     */
    public void declareDoubleOrNull(BiConsumer<Value, Double> consumer, double nullValue, ParseField field) {
        declareField(consumer, p -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : p.doubleValue(),
                field, ValueType.DOUBLE_OR_NULL);
    }

    public void declareLong(BiConsumer<Value, Long> consumer, ParseField field) {
        // Using a method reference here angers some compilers
        declareField(consumer, p -> p.longValue(), field, ValueType.LONG);
    }

    public void declareInt(BiConsumer<Value, Integer> consumer, ParseField field) {
        // Using a method reference here angers some compilers
        declareField(consumer, p -> p.intValue(), field, ValueType.INT);
    }

    /**
     * Declare a double field that parses explicit {@code null}s in the json to a default value.
     */
    public void declareIntOrNull(BiConsumer<Value, Integer> consumer, int nullValue, ParseField field) {
        declareField(consumer, p -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : p.intValue(),
                field, ValueType.INT_OR_NULL);
    }


    public void declareString(BiConsumer<Value, String> consumer, ParseField field) {
        declareField(consumer, XContentParser::text, field, ValueType.STRING);
    }

    public void declareStringOrNull(BiConsumer<Value, String> consumer, ParseField field) {
        declareField(consumer, (p) -> p.currentToken() == XContentParser.Token.VALUE_NULL ? null : p.text(), field,
                ValueType.STRING_OR_NULL);
    }

    public void declareBoolean(BiConsumer<Value, Boolean> consumer, ParseField field) {
        declareField(consumer, XContentParser::booleanValue, field, ValueType.BOOLEAN);
    }

    public <T> void declareObjectArray(BiConsumer<Value, List<T>> consumer, ContextParser<Context, T> objectParser, ParseField field) {
        declareFieldArray(consumer, (p, c) -> objectParser.parse(p, c), field, ValueType.OBJECT_ARRAY);
    }

    public void declareStringArray(BiConsumer<Value, List<String>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.text(), field, ValueType.STRING_ARRAY);
    }

    public void declareDoubleArray(BiConsumer<Value, List<Double>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.doubleValue(), field, ValueType.DOUBLE_ARRAY);
    }

    public void declareFloatArray(BiConsumer<Value, List<Float>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.floatValue(), field, ValueType.FLOAT_ARRAY);
    }

    public void declareLongArray(BiConsumer<Value, List<Long>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.longValue(), field, ValueType.LONG_ARRAY);
    }

    public void declareIntArray(BiConsumer<Value, List<Integer>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.intValue(), field, ValueType.INT_ARRAY);
    }

    /**
     * Declares a field that can contain an array of elements listed in the type ValueType enum
     */
    public <T> void declareFieldArray(BiConsumer<Value, List<T>> consumer, ContextParser<Context, T> itemParser,
                                      ParseField field, ValueType type) {
        declareField(consumer, (p, c) -> parseArray(p, () -> itemParser.parse(p, c)), field, type);
    }

    /**
     * Declares a set of fields that are required for parsing to succeed. Only one of the values
     * provided per String[] must be matched.
     *
     * E.g. <code>declareRequiredFieldSet("foo", "bar");</code> means at least one of "foo" or
     * "bar" fields must be present.  If neither of those fields are present, an exception will be thrown.
     *
     * Multiple required sets can be configured:
     *
     * <pre><code>
     *   parser.declareRequiredFieldSet("foo", "bar");
     *   parser.declareRequiredFieldSet("bizz", "buzz");
     * </code></pre>
     *
     * requires that one of "foo" or "bar" fields are present, and also that one of "bizz" or
     * "buzz" fields are present.
     *
     * In JSON, it means any of these combinations are acceptable:
     *
     * <ul>
     *   <li><code>{"foo":"...", "bizz": "..."}</code></li>
     *   <li><code>{"bar":"...", "bizz": "..."}</code></li>
     *   <li><code>{"foo":"...", "buzz": "..."}</code></li>
     *   <li><code>{"bar":"...", "buzz": "..."}</code></li>
     *   <li><code>{"foo":"...", "bar":"...", "bizz": "..."}</code></li>
     *   <li><code>{"foo":"...", "bar":"...", "buzz": "..."}</code></li>
     *   <li><code>{"foo":"...", "bizz":"...", "buzz": "..."}</code></li>
     *   <li><code>{"bar":"...", "bizz":"...", "buzz": "..."}</code></li>
     *   <li><code>{"foo":"...", "bar":"...", "bizz": "...", "buzz": "..."}</code></li>
     * </ul>
     *
     * The following would however be rejected:
     *
     * <table>
     *   <caption>failure cases</caption>
     *   <tr><th>Provided JSON</th><th>Reason for failure</th></tr>
     *   <tr><td><code>{"foo":"..."}</code></td><td>Missing "bizz" or "buzz" field</td></tr>
     *   <tr><td><code>{"bar":"..."}</code></td><td>Missing "bizz" or "buzz" field</td></tr>
     *   <tr><td><code>{"bizz": "..."}</code></td><td>Missing "foo" or "bar" field</td></tr>
     *   <tr><td><code>{"buzz": "..."}</code></td><td>Missing "foo" or "bar" field</td></tr>
     *   <tr><td><code>{"foo":"...", "bar": "..."}</code></td><td>Missing "bizz" or "buzz" field</td></tr>
     *   <tr><td><code>{"bizz":"...", "buzz": "..."}</code></td><td>Missing "foo" or "bar" field</td></tr>
     *   <tr><td><code>{"unrelated":"..."}</code></td>  <td>Missing "foo" or "bar" field, and missing "bizz" or "buzz" field</td></tr>
     * </table>
     *
     * @param requiredSet
     *          A set of required fields, where at least one of the fields in the array _must_ be present
     */
    public abstract void declareRequiredFieldSet(String... requiredSet);

    /**
     * Declares a set of fields of which at most one must appear for parsing to succeed
     *
     * E.g. <code>declareExclusiveFieldSet("foo", "bar");</code> means that only one of 'foo'
     * or 'bar' must be present, and if both appear then an exception will be thrown.  Note
     * that this does not make 'foo' or 'bar' required - see {@link #declareRequiredFieldSet(String...)}
     * for required fields.
     *
     * Multiple exclusive sets may be declared
     *
     * @param exclusiveSet a set of field names, at most one of which must appear
     */
    public abstract void declareExclusiveFieldSet(String... exclusiveSet);

    private interface IOSupplier<T> {
        T get() throws IOException;
    }

    private static <T> List<T> parseArray(XContentParser parser, IOSupplier<T> supplier) throws IOException {
        List<T> list = new ArrayList<>();
        if (parser.currentToken().isValue()
                || parser.currentToken() == XContentParser.Token.VALUE_NULL
                || parser.currentToken() == XContentParser.Token.START_OBJECT) {
            list.add(supplier.get()); // single value
        } else {
            while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                if (parser.currentToken().isValue()
                        || parser.currentToken() == XContentParser.Token.VALUE_NULL
                        || parser.currentToken() == XContentParser.Token.START_OBJECT) {
                    list.add(supplier.get());
                } else {
                    throw new IllegalStateException("expected value but got [" + parser.currentToken() + "]");
                }
            }
        }
        return list;
    }
}
