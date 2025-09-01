/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.generators.mvc.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import de.splatgames.aether.generators.mvc.processor.struct.FieldModel;
import de.splatgames.aether.generators.mvc.processor.utils.GenHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Utility for generating structured JavaDoc blocks for builder classes.
 *
 * <p>This class centralizes the creation of {@link CodeBlock} instances that are
 * attached to generated types and methods. By keeping JavaDoc generation consistent
 * and deterministic, the processor avoids duplicating string templates throughout
 * the code generator.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Create a class-level JavaDoc for a generated builder via {@link #classDoc(String, boolean, boolean, List)}.</li>
 *   <li>Create field-level method JavaDocs for fluent API methods
 *       ({@code withX}, {@code withXId}, {@code addX}, {@code addAllX}, {@code clearX}).</li>
 *   <li>Generate stable, environment-independent output (no timestamps or random content).</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>This class is package-private and not intended for external use.</li>
 *   <li>All methods return {@link CodeBlock} so that Javapoet can embed them directly
 *       when building {@link com.squareup.javapoet.TypeSpec} and {@link com.squareup.javapoet.MethodSpec}.</li>
 *   <li>Deterministic ordering is enforced (e.g. alphabetical by alias).</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
final class Javadocs {

    /**
     * Private constructor to prevent instantiation.
     */
    private Javadocs() {
        // utility class; not instantiable
    }

    /**
     * Builds the class-level JavaDoc for a generated builder type.
     *
     * <p>The output contains:</p>
     * <ul>
     *   <li>Header with the builder name.</li>
     *   <li>Feature overview (modes, defaults, setter vs. reflection, collections, auto-relations).</li>
     *   <li>Usage examples (transient and persistent mode).</li>
     *   <li>Fluent API overview for all fields.</li>
     *   <li>Notes about thread-safety and null-handling.</li>
     * </ul>
     *
     * @param entitySimple  simple name of the entity type (without package)
     * @param hasDefaults   whether a defaults provider is configured
     * @param autoRelations whether auto-relations should be handled
     * @param fields        list of field models that drive field-specific documentation
     * @return a {@link CodeBlock} representing the JavaDoc content
     */
    @NotNull
    static CodeBlock classDoc(@NotNull final String entitySimple,
                              final boolean hasDefaults,
                              final boolean autoRelations,
                              @NotNull final List<@NotNull FieldModel> fields) {
        // stable order
        var docs = fields.stream()
                .sorted(Comparator.comparing(FieldModel::alias, String.CASE_INSENSITIVE_ORDER))
                .map(f -> new DocField(
                        f.name(), f.alias(), f.type(), f.isBoolean(),
                        f.isCollection(), f.isList(), f.isSet(), f.elementType(),
                        f.asIdOnly(), f.relationKind().name()
                ))
                .toList();

        CodeBlock.Builder b = CodeBlock.builder();
        b.add("<h2>$LBuilder</h2>\n", entitySimple);
        b.add("<p>Auto-generated fluent builder for <code>$L</code>. ", entitySimple)
                .add("Produced by <em>Aether Generators MVC</em> and extending <code>AbstractBuilder</code>.")
                .add("</p>\n");

        // Features
        b.add("<h3>Features</h3>\n");
        b.add("<ul>\n");
        b.add("  <li><b>Modes:</b> <code>transientMode()</code> (build-only) and <code>persistent()</code> (persist via adapter).</li>\n");
        if (hasDefaults) {
            b.add("  <li><b>Defaults provider:</b> If a value is not set, uses <code>defaults.&lt;field&gt;()</code> or, for <em>asIdOnly</em>, <code>defaults.&lt;field&gt;Id()</code>.</li>\n");
        } else {
            b.add("  <li><b>Defaults provider:</b> Not configured.</li>\n");
        }
        b.add("  <li><b>Setter-first:</b> Uses entity setter if available; otherwise falls back to reflection.</li>\n");
        b.add("  <li><b>Collections:</b> <code>withX(...)</code>, <code>addX(...)</code>, <code>addAllX(...)</code>, <code>clearX()</code>; empty collections are created when needed (ArrayList/LinkedHashSet).</li>\n");
        b.add("  <li><b>Deterministic API:</b> Methods are generated in alphabetical order by alias.</li>\n");
        if (autoRelations) {
            b.add("  <li><b>Auto-relations (persistent mode):</b> Related objects are persisted before saving the root (depth limit, cycle guard).</li>\n");
        } else {
            b.add("  <li><b>Auto-relations:</b> Disabled.</li>\n");
        }
        b.add("</ul>\n");

        // Usage examples (stable, minimal)
        b.add("<h3>Examples</h3>\n");
        b.add("<pre>\n// transient\nvar obj = new $LBuilder()\n    .with...\n    .create();\n\n// persistent (Spring)\nvar adapter = new SpringPersistAdapter(ctx);\nvar obj2 = new $LBuilder(adapter, new Defaults())\n    .persistent()\n    .with...\n    .create();\n</pre>\n", entitySimple, entitySimple);

        // Field overview
        b.add("<h3>Fields &amp; Fluent API</h3>\n");
        b.add("<ul>\n");
        for (DocField f : docs) {
            if (f.asIdOnly() && "TO_ONE".equals(f.relationKind)) {
                b.add("  <li><code>with$LId($L)</code> — relation (<em>asIdOnly</em>), sets only the identifier; attempts adapter lookup, otherwise creates a lightweight reference with the id.</li>\n",
                        GenHelpers.capitalize(f.alias()), typeName(f.elementType() != null ? f.elementType() : f.type()));
            } else if (f.isCollection()) {
                b.add("  <li><code>with$L($L)</code>, <code>add$L($L)</code>, <code>addAll$L(Collection&lt;? extends $L&gt;)</code>, <code>clear$L()</code></li>\n",
                        GenHelpers.capitalize(f.alias()), typeName(f.type()),
                        GenHelpers.capitalize(GenHelpers.singularize(f.alias())), typeName(f.elementType()),
                        GenHelpers.capitalize(f.alias()), typeName(f.elementType()),
                        GenHelpers.capitalize(f.alias()));
            } else {
                b.add("  <li><code>with$L($L)</code></li>\n", GenHelpers.capitalize(f.alias()), typeName(f.type()));
            }
        }
        b.add("</ul>\n");

        // Notes
        b.add("<h3>Notes</h3>\n");
        b.add("<ul>\n");
        b.add("  <li>Thread-safety: Builders are <em>not</em> thread-safe.</li>\n");
        b.add("  <li>Null handling: Primitive fields receive default literals; collections are never stored as <code>null</code>.</li>\n");
        b.add("  <li>Determinism: These Javadocs avoid timestamps and environment-specific content.</li>\n");
        b.add("</ul>\n");

        // @since and @see (stable)
        b.add("@since 1.0.0\n");
        b.add("@see de.splatgames.aether.generators.mvc.runtime.AbstractBuilder\n");
        return b.build();
    }

    // ---------- CLASS JAVADOC ----------

    /**
     * Creates JavaDoc for a {@code withX(...)} setter method.
     *
     * @param f field metadata
     * @return JavaDoc block describing the {@code withX(...)} method
     */
    @NotNull
    static CodeBlock withDoc(@NotNull final FieldModel f) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.add("Sets the value of field <code>$L</code> (alias: <code>$L</code>).<br>\n", f.name(), f.alias());
        b.add("Overrides any value from a configured defaults provider.<br>\n");
        b.add("@param $L value to set\n", f.name());
        b.add("@return this builder\n");
        return b.build();
    }

    // ---------- METHOD JAVADOCS ----------

    /**
     * Creates JavaDoc for an {@code addX(...)} method on collection fields.
     *
     * @param f field metadata
     * @return JavaDoc block describing the {@code addX(...)} method
     */
    @NotNull
    static CodeBlock addDoc(@NotNull final FieldModel f) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.add("Adds a single element to collection field <code>$L</code> (alias: <code>$L</code>).<br>\n", f.name(), f.alias());
        b.add("Creates an empty collection on first use if necessary.<br>\n");
        b.add("@param $L element to add\n", GenHelpers.singularVar(f.name()));
        b.add("@return this builder\n");
        return b.build();
    }

    /**
     * Creates JavaDoc for an {@code addAllX(Collection)} method on collection fields.
     *
     * @param f field metadata
     * @return JavaDoc block describing the {@code addAllX(...)} method
     */
    @NotNull
    static CodeBlock addAllDoc(@NotNull final FieldModel f) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.add("Adds all elements to collection field <code>$L</code> (alias: <code>$L</code>).<br>\n", f.name(), f.alias());
        b.add("No-op for <code>null</code> or empty input. Creates an empty collection on first use if necessary.<br>\n");
        b.add("@param $L elements to add\n", f.name());
        b.add("@return this builder\n");
        return b.build();
    }

    /**
     * Creates JavaDoc for a {@code clearX()} method on collection fields.
     *
     * @param f field metadata
     * @return JavaDoc block describing the {@code clearX()} method
     */
    @NotNull
    static CodeBlock clearDoc(@NotNull final FieldModel f) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.add("Clears collection field <code>$L</code> (alias: <code>$L</code>).<br>\n", f.name(), f.alias());
        b.add("Creates an empty collection if it was <code>null</code> before.<br>\n");
        b.add("@return this builder\n");
        return b.build();
    }

    /**
     * Creates JavaDoc for a {@code withXId(...)} method
     * when a relation field is marked as {@code asIdOnly}.
     *
     * @param f field metadata
     * @return JavaDoc block describing the {@code withXId(...)} method
     */
    @NotNull
    static CodeBlock withIdDoc(@NotNull final FieldModel f) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.add("Sets only the identifier for relation field <code>$L</code> (alias: <code>$L</code>).<br>\n", f.name(), f.alias());
        b.add("In persistent mode, the adapter may resolve the entity via <code>findById</code>; otherwise a reference with just the id is created.<br>\n");
        b.add("@param $LId identifier value\n", f.name());
        b.add("@return this builder\n");
        return b.build();
    }


    /**
     * Returns a stable string representation of a type.
     * Defaults to {@code "Object"} if {@code null}.
     *
     * @param tn type to render (may be {@code null})
     * @return human-readable type name for JavaDoc
     */
    @NotNull
    private static String typeName(@Nullable final TypeName tn) {
        return tn == null ? "Object" : tn.toString();
    }

    /**
     * Internal immutable record used to carry field information for class-level docs.
     *
     * <p>Values are derived from {@link FieldModel} but simplified into
     * stable strings and booleans for documentation purposes.</p>
     *
     * @param name         the declared field name
     * @param alias        the API alias used for fluent method names
     * @param type         the declared field type
     * @param isBoolean    whether the field is a boolean
     * @param isCollection whether the field is a collection
     * @param isList       whether the field is a list
     * @param isSet        whether the field is a set
     * @param elementType  the collection element type
     * @param asIdOnly     whether the field is exposed as an identifier
     * @param relationKind the relation classification (NONE, TO_ONE, TO_MANY)
     */
    record DocField(
            String name,
            String alias,
            TypeName type,
            boolean isBoolean,
            boolean isCollection,
            boolean isList,
            boolean isSet,
            TypeName elementType,
            boolean asIdOnly,
            String relationKind // "NONE", "TO_ONE", "TO_MANY"
    ) {
    }
}