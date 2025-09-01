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

/**
 * Annotation-based configuration for an MVC builder code generator.
 *
 * <h2>Overview</h2>
 * This package defines annotations that drive generation of fluent test-data builders
 * for domain models. The processor creates builder classes (e.g. {@code RoleBuilder})
 * that support fluent setters, {@code create()} / {@code createMany(int)}, and runtime
 * mode switches (e.g. {@code transientMode()} vs. {@code persistent()}).
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder @MvcBuilder} –
 *       place on a domain type to trigger builder generation. Configure builder name,
 *       default mode, field selection policy, defaults provider, superclass inclusion,
 *       and the target package for the generated class.</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcDefaults @MvcDefaults} –
 *       marks a class whose zero-arg methods provide default values for unset fields.</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcField @MvcField} –
 *       field-level controls (exclude from API, rename fluent setter via {@code alias},
 *       or expose relations as identifiers via {@code asIdOnly}).</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcIgnore @MvcIgnore} –
 *       shorthand for excluding a field; equivalent to {@code @MvcField(ignore = true)}.</li>
 * </ul>
 *
 * <h2>Generated Builder API</h2>
 * Generated builders typically expose:
 * <ul>
 *   <li>Mode switches: {@code transientMode()} and {@code persistent()}.</li>
 *   <li>Creation: {@code create()} and {@code createMany(int)}.</li>
 *   <li>Fluent setters: {@code withX(...)} for selected fields.</li>
 * </ul>
 *
 * <h2>Configuration Highlights</h2>
 * <ul>
 *   <li><strong>Naming:</strong> default builder name is {@code &lt;SimpleName&gt;Builder};
 *       override via {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#builderName()}.</li>
 *   <li><strong>Default mode:</strong> {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder.Mode#TRANSIENT}
 *       unless changed via {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#defaultMode()}.</li>
 *   <li><strong>Field selection:</strong> default
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder.FieldPolicy#ALL};
 *       use {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder.FieldPolicy#EXPLICIT}
 *       together with {@link de.splatgames.aether.generators.mvc.annotations.MvcField @MvcField} to opt in fields.</li>
 *   <li><strong>Defaults:</strong> reference a provider via
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#defaultProvider()}
 *       or annotate a class with {@link de.splatgames.aether.generators.mvc.annotations.MvcDefaults}.</li>
 *   <li><strong>Inheritance:</strong> include superclass fields via
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#includeSuper()}.</li>
 *   <li><strong>Package:</strong> generated class lives next to the annotated type by default; override via
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#generatedPackage()}.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <blockquote><pre>
 * &#64;MvcBuilder
 * public class Role {
 *   String name;
 *   boolean active;
 * }
 *
 * // Generated: RoleBuilder (shape depends on the code generator)
 * Role a = new RoleBuilder()
 *            .transientMode()
 *            .withName("ADMIN")
 *            .withActive(true)
 *            .create();
 *
 * Role b = new RoleBuilder()
 *            .persistent()
 *            .withName("USER")
 *            .create();
 * </pre></blockquote>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>All annotations in this package use {@link java.lang.annotation.RetentionPolicy#SOURCE RetentionPolicy.SOURCE} retention.</li>
 *   <li>These annotations control code generation only; persistence integration and runtime behavior
 *       are handled by the generated code and/or your own runtime modules.</li>
 * </ul>
 *
 * @since 1.0.0
 */
package de.splatgames.aether.generators.mvc.annotations;
