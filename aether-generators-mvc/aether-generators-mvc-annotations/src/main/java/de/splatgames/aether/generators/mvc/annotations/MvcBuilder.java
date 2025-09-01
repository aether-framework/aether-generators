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

package de.splatgames.aether.generators.mvc.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Enables generation of a fluent MVC test-data builder for the annotated type.
 * <p>
 * The processor generates a {@code <SimpleName>Builder} class in the same package (unless overridden)
 * that supports {@code transient()} vs. {@code persistent()} modes, {@code create()} and
 * {@code createMany(n)}, and fluent {@code withX(...)} setters for selected fields.
 * </p>
 *
 * <h2>Key features</h2>
 * <ul>
 *   <li>Builder class name can be customized via {@link #builderName()}.</li>
 *   <li>Builder default mode is {@link Mode#TRANSIENT} (in-memory objects) unless changed via
 *       {@link #defaultMode()}.</li>
 *   <li>Field selection defaults to {@link FieldPolicy#ALL}. Use {@link FieldPolicy#EXPLICIT} with
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcField @MvcField} to opt-in fields.</li>
 *   <li>Relations can be auto-handled in persistent mode via {@link #autoRelations()}.</li>
 *   <li>Defaults can be supplied via {@link #defaultProvider()} or a class annotated with
 *       {@link MvcDefaults} in the same module.</li>
 * </ul>
 *
 * <h2>Generated class</h2>
 * <ul>
 *   <li>Class: {@code <SimpleName>Builder} or {@link #builderName()} if provided.</li>
 *   <li>Location: same package by default; override with {@link #generatedPackage()}.</li>
 *   <li>API: {@code transient()}, {@code persistent()}, {@code create()}, {@code createMany(int)}, {@code withX(...)}.</li>
 * </ul>
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li>{@link Mode#TRANSIENT}: only builds objects in memory (no DB interaction).</li>
 *   <li>{@link Mode#PERSISTENT}: calls a persistence adapter (e.g., Spring adapter) to {@code save(...)} entities
 *       and ensure related entities exist before saving the root object.</li>
 * </ul>
 *
 * <h2>Defaults provider</h2>
 * <p>
 * A defaults provider is a simple class with zero-arg methods to supply fallback values, e.g.:
 * {@code String roleName() { return "USER"; }}. Configure it via {@link #defaultProvider()} or annotate a class with
 * {@link MvcDefaults}. The processor will attempt to instantiate it reflectively (no DI required).
 * </p>
 *
 * <h2>Superclasses</h2>
 * <p>
 * By default, fields in superclasses are considered as well. Toggle via {@link #includeSuper()}.
 * </p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@Documented
@Target(TYPE)
@Retention(SOURCE)
public @interface MvcBuilder {

    /**
     * Optional explicit name of the generated builder class (without {@code .java}).
     * <p>Default is {@code <SimpleName>Builder}, e.g. {@code RoleBuilder}.</p>
     */
    String builderName() default "";

    /**
     * Default mode for the generated builder instance.
     */
    Mode defaultMode() default Mode.TRANSIENT;

    /**
     * Field selection strategy.
     * <ul>
     *   <li>{@link FieldPolicy#ALL}: all non-static, non-transient fields are considered unless
     *       {@link MvcField#ignore()} or {@link MvcIgnore} is set.</li>
     *   <li>{@link FieldPolicy#EXPLICIT}: only fields annotated with {@link MvcField} are considered.</li>
     * </ul>
     */
    FieldPolicy fieldPolicy() default FieldPolicy.ALL;

    /**
     * Whether the processor should attempt to handle relations automatically in persistent mode
     * (e.g. create/save a {@code @ManyToOne} target if it lacks an identifier).
     */
    boolean autoRelations() default true;

    /**
     * Optional defaults provider class; if {@link Void} is used, the processor may fall back to a
     * class annotated with {@link MvcDefaults} or to internal fallbacks.
     */
    Class<?> defaultProvider() default Void.class;

    /**
     * Whether fields from superclasses should be considered.
     */
    boolean includeSuper() default true;

    /**
     * Optional override for the package of the generated builder.
     * <p>Empty = same package as the annotated type.</p>
     */
    String generatedPackage() default "";

    /**
     * Builder mode used for the default behavior of the generated builder.
     */
    enum Mode {TRANSIENT, PERSISTENT}

    /**
     * Strategy controlling which fields become part of the builder API.
     */
    enum FieldPolicy {ALL, EXPLICIT}
}
