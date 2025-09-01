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

package de.splatgames.aether.generators.mvc.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for generated or hand-written MVC test-data builders.
 * <p>
 * A builder created from this base can produce object graphs either
 * <em>transiently</em> (pure in-memory construction) or <em>persistently</em>
 * (construction followed by a persistence step). The current behavior is
 * controlled by the {@linkplain #mode(Mode) mode}.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Example shape of a generated builder:
 * public final class RoleBuilder extends AbstractBuilder<Role, RoleBuilder> {
 *   public RoleBuilder() { super(Mode.TRANSIENT); }
 *
 *   @Override protected Role build() {
 *     Role r = new Role();
 *     // set defaults & overrides...
 *     return r;
 *   }
 *
 *   @Override protected Role persistIfNeeded(Role obj) {
 *     // e.g., adapter.save(obj) and return the managed instance
 *     return obj;
 *   }
 * }
 *
 * // Transient object
 * Role transientRole = new RoleBuilder()
 *     .transientMode()
 *     .create();
 *
 * // Persistent object
 * Role persistedRole = new RoleBuilder()
 *     .persistent()
 *     .create();
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #create()} calls {@link #build()} to construct the object graph.</li>
 *   <li>If the current {@link #mode} is {@link Mode#PERSISTENT},
 *       {@link #persistIfNeeded(Object)} is invoked and its result is returned.</li>
 *   <li>Otherwise the transient instance from {@code build()} is returned.</li>
 * </ol>
 *
 * <h2>Thread-safety</h2>
 * <p>
 * Builder instances are <strong>not</strong> thread-safe. Do not share a single
 * builder instance across threads without external synchronization.
 * </p>
 *
 * <h2>Extensibility</h2>
 * <ul>
 *   <li>Subclasses must implement {@link #build()} and may override
 *       {@link #persistIfNeeded(Object)} to integrate a persistence adapter.</li>
 *   <li>The {@link #createMany(int)} helper repeatedly invokes {@link #create()}
 *       to produce independent instances.</li>
 * </ul>
 *
 * @param <T>    the type produced by this builder
 * @param <SELF> the concrete builder type (for fluent chaining)
 * @since 1.0.0
 */
public abstract class AbstractBuilder<T, SELF extends AbstractBuilder<T, SELF>> {

    /**
     * Current operation mode of this builder.
     * <p>
     * Use {@link #mode(Mode)}, {@link #transientMode()} or {@link #persistent()}
     * to change it fluently.
     * </p>
     */
    protected Mode mode;

    /**
     * Creates a new builder with the given default mode.
     *
     * @param defaultMode the initial mode; if {@code null}, {@link Mode#TRANSIENT} is used
     */
    protected AbstractBuilder(@Nullable final Mode defaultMode) {
        this.mode = defaultMode == null ? Mode.TRANSIENT : defaultMode;
    }

    /**
     * Returns {@code this} cast to the concrete builder type.
     * <p>
     * Used by the fluent API to preserve the self type.
     * </p>
     *
     * @return this builder as {@code SELF}
     */
    @SuppressWarnings("unchecked")
    protected final SELF self() {
        return (SELF) this;
    }

    /**
     * Sets the current mode.
     *
     * @param mode the new mode; if {@code null}, falls back to {@link Mode#TRANSIENT}
     * @return this builder for fluent chaining
     */
    @NotNull
    public final SELF mode(@Nullable final Mode mode) {
        this.mode = mode == null ? Mode.TRANSIENT : mode;
        return self();
    }

    /**
     * Switches the builder to {@link Mode#TRANSIENT}.
     *
     * @return this builder for fluent chaining
     */
    @NotNull
    public final SELF transientMode() {
        this.mode = Mode.TRANSIENT;
        return self();
    }

    /**
     * Switches the builder to {@link Mode#PERSISTENT}.
     *
     * @return this builder for fluent chaining
     */
    @NotNull
    public final SELF persistent() {
        this.mode = Mode.PERSISTENT;
        return self();
    }

    /**
     * Builds the object graph in memory, without any persistence side effects.
     * <p>
     * Implementations should apply defaults and any stored overrides to produce
     * a fully initialized instance ready for use or persistence.
     * </p>
     *
     * @return the constructed instance (never {@code null})
     */
    @NotNull
    protected abstract T build();

    /**
     * Persists the given object graph if required.
     * <p>
     * The default implementation is a no-op and simply returns {@code obj}.
     * Subclasses may override this method to integrate their persistence mechanism
     * (e.g. via a repository/adapter) and should return the resulting managed
     * instance (e.g. with identifiers assigned).
     * </p>
     *
     * @param obj the instance produced by {@link #build()}
     * @return the persisted (or otherwise post-processed) instance
     */
    @NotNull
    protected T persistIfNeeded(@NotNull final T obj) {
        return obj;
    }

    /**
     * Creates one instance according to the current {@link #mode}.
     * <p>
     * Equivalent to:
     * </p>
     * <pre>{@code
     * T t = build();
     * return (mode == Mode.PERSISTENT) ? persistIfNeeded(t) : t;
     * }</pre>
     *
     * @return a newly created instance
     */
    @NotNull
    public final T create() {
        T obj = build();
        return this.mode == Mode.PERSISTENT ? persistIfNeeded(obj) : obj;
    }

    /**
     * Creates {@code n} instances by repeatedly invoking {@link #create()}.
     * <p>
     * If {@code n} is less than or equal to zero, an empty list is returned.
     * Each element is created independently and may result in distinct
     * persisted entities when in {@link Mode#PERSISTENT}.
     * </p>
     *
     * @param n the number of instances to create
     * @return a list containing {@code n} instances (never {@code null})
     */
    @NotNull
    public final List<T> createMany(final int n) {
        final List<T> list = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) list.add(create());
        return list;
    }

    /**
     * Operation modes for the builder.
     */
    public enum Mode {
        /**
         * Build in memory only; {@link #persistIfNeeded(Object)} is not invoked.
         */
        TRANSIENT,
        /**
         * Build in memory and then execute {@link #persistIfNeeded(Object)} to
         * persist or otherwise finalize the object graph.
         */
        PERSISTENT
    }
}
