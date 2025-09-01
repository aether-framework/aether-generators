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

/**
 * Minimal persistence abstraction used by generated builders when operating in a persistent mode.
 *
 * <p>This interface is intentionally small to remain framework-agnostic. Implementations may be backed by
 * any persistence technology (e.g., JPA, MyBatis, custom repositories, or a test double).
 * The adapter is expected to return managed/attached entities where applicable.</p>
 *
 * <h2>Usage</h2>
 * <blockquote><pre>
 * // Example integration inside a builder's persistence step:
 * public final class RoleBuilder extends AbstractBuilder&lt;Role, RoleBuilder&gt; {
 *   private final PersistAdapter adapter;
 *
 *   public RoleBuilder(PersistAdapter adapter) {
 *     super(Mode.TRANSIENT);
 *     this.adapter = adapter;
 *   }
 *
 *   &#64;Override protected Role build() {
 *     Role r = new Role();
 *     r.setName(defaults.name());
 *     return r;
 *   }
 *
 *   &#64;Override protected Role persistIfNeeded(Role obj) {
 *     return adapter.save(obj); // return managed instance (e.g., with assigned ID)
 *   }
 * }
 * </pre></blockquote>
 *
 * <h2>Transactions</h2>
 * <p>This abstraction does not establish transactional boundaries. Callers are responsible for
 * transaction demarcation (e.g., via an outer service layer or test setup).</p>
 *
 * <h2>Thread-safety</h2>
 * <p>Implementations should document their own thread-safety characteristics. The interface
 * itself imposes no synchronization requirements.</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public interface PersistAdapter {

    /**
     * Persists the given entity and returns the managed instance.
     *
     * <p>The returned instance should reflect any persistence-side mutations (e.g., assigned identifiers)
     * and be considered the canonical reference going forward.</p>
     *
     * @param entity the entity to persist; must not be {@code null}
     * @param <T>    entity type
     * @return the managed (possibly the same, possibly a different) instance; never {@code null}
     * @throws RuntimeException if the underlying persistence layer reports a failure
     * @implSpec Implementations should either:
     * <ul>
     *   <li>attach the given instance and return it, or</li>
     *   <li>return a newly managed copy (e.g., via a merge operation).</li>
     * </ul>
     * The choice should be documented by the implementation.
     */
    @NotNull
    <T> T save(@NotNull final T entity);

    /**
     * Optionally looks up an entity by its type and identifier.
     *
     * <p>The default implementation signals that lookup is unsupported by throwing
     * {@link UnsupportedOperationException}. Implementations that support retrieval
     * should override this method.</p>
     *
     * @param entityType the entity class; must not be {@code null}
     * @param id         the identifier value; must not be {@code null}
     * @param <T>        entity type
     * @return the found instance, or {@code null} if no matching entity exists
     * @throws UnsupportedOperationException if lookup is not supported by this adapter
     * @throws RuntimeException              if the underlying persistence layer reports a failure
     * @apiNote Returning {@code null} (rather than {@code Optional}) keeps the surface area minimal
     * for generated code. Callers can adapt to {@code Optional} if desired.
     */
    @Nullable
    default <T> T findById(@NotNull final Class<T> entityType, @NotNull final Object id) {
        throw new UnsupportedOperationException("findById not supported");
    }

    /**
     * Resolves an arbitrary dependency known to the adapter (e.g., a repository or defaults provider).
     *
     * <p>The default implementation signals that dependency resolution is unsupported by throwing
     * {@link UnsupportedOperationException}. Implementations that offer a resolution mechanism should
     * override this method.</p>
     *
     * @param type the dependency type; must not be {@code null}
     * @param <R>  resolved type
     * @return an instance of {@code type}; never {@code null}
     * @throws UnsupportedOperationException if resolution is not supported by this adapter
     * @throws RuntimeException              if resolution fails at runtime
     * @implSpec Implementations that support resolution should define scoping rules
     * (e.g., per-invocation, per-thread, or application-scoped) and document them.
     */
    @Nullable
    default <R> R require(@NotNull final Class<R> type) {
        throw new UnsupportedOperationException("No resolver available for " + type);
    }
}
