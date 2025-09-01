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

package de.splatgames.aether.generators.mvc.adapter.spring;

import de.splatgames.aether.generators.mvc.runtime.PersistAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * A Spring-backed {@link PersistAdapter} that resolves Spring Data repositories by domain type
 * and falls back to a {@link EntityManager} if no repository is available.
 *
 * <p><strong>Transactions:</strong> This adapter is stateless and does <em>not</em> establish transactional
 * boundaries. Transaction management is expected to be handled by the caller (e.g., service layer or
 * tests with {@code @DataJpaTest}).</p>
 *
 * <p><strong>Resolution order:</strong></p>
 * <ol>
 *   <li>If a Spring Data {@link CrudRepository} exists for the entity type, its operations are used.</li>
 *   <li>Otherwise, if an {@link EntityManager} is available, it is used as a fallback. The adapter
 *       chooses between {@code persist} (no ID) and {@code merge} (ID present) based on the detected
 *       identifier value.</li>
 * </ol>
 *
 * <p><strong>ID detection:</strong> The primary key is detected via reflection. Fields annotated with
 * {@link jakarta.persistence.Id} are preferred; if none is found, a field named {@code id} is used as a
 * conventional fallback. For legacy models, {@code javax.persistence.Id} is supported if present.</p>
 *
 * <p><strong>Example</strong></p>
 * <blockquote><pre>
 * SpringPersistAdapter adapter = new SpringPersistAdapter(ctx);
 *
 * // Save (uses repository if present; otherwise EM persist/merge based on ID)
 * User saved = adapter.save(new User(null, "Alice"));
 *
 * // Find by ID (repository if present; otherwise EM.find)
 * User byId = adapter.findById(User.class, saved.getId());
 * </pre></blockquote>
 *
 * @author Erik Pförtner
 * @since Spring Adapter 1.0.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class SpringPersistAdapter implements PersistAdapter {
    private final ApplicationContext ctx;
    private final Repositories repositories;
    private final EntityManager emOrNull;

    /**
     * Creates a new adapter backed by the given Spring {@link ApplicationContext}.
     * Attempts to obtain an {@link EntityManager} from the context; if none is present,
     * only repository resolution will be used.
     *
     * @param ctx the Spring {@link ApplicationContext}; must not be {@code null}
     */
    public SpringPersistAdapter(@NotNull final ApplicationContext ctx) {
        this.ctx = ctx;
        this.repositories = new Repositories(ctx);
        this.emOrNull = getEntityManagerOrNull(ctx);
    }

    /**
     * Tries to get an {@link EntityManager} from the {@link ApplicationContext}.
     *
     * @param ctx the application context; must not be {@code null}
     * @return the {@link EntityManager} if available; otherwise {@code null}
     */
    @Nullable
    private static EntityManager getEntityManagerOrNull(@NotNull final ApplicationContext ctx) {
        try {
            return ctx.getBean(EntityManager.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }
    }

    /**
     * Reads the identifier value of the given entity via reflection.
     * Prefers a field annotated with {@link jakarta.persistence.Id} (or legacy {@code javax.persistence.Id});
     * falls back to a field named {@code id}.
     *
     * @param entity the entity instance; must not be {@code null}
     * @return the identifier value or {@code null} if no ID field is found or the value is {@code null}
     * @throws RuntimeException if reflective access fails
     */
    @Nullable
    private static Object readIdValue(@NotNull final Object entity) {
        Field idField = findIdField(entity.getClass());
        if (idField == null) return null;
        try {
            idField.setAccessible(true);
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Locates the ID field on the given class or its superclasses.
     * Checks for {@link jakarta.persistence.Id} or legacy {@code javax.persistence.Id} first;
     * if none is present, falls back to a field named {@code id}.
     *
     * @param type the entity class; must not be {@code null}
     * @return the ID field if resolved; otherwise {@code null}
     */
    @Nullable
    private static Field findIdField(Class<?> type) {
        Class<?> t = type;
        while (t != null && t != Object.class) {
            for (Field f : t.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class) || hasLegacyId(f)) {
                    return f;
                }
                if (f.getName().equals("id")) {
                    return f;
                }
            }
            t = t.getSuperclass();
        }
        return null;
    }

    /**
     * Checks whether the given field is annotated with legacy {@code javax.persistence.Id}.
     *
     * @param f the field to inspect; must not be {@code null}
     * @return {@code true} if legacy {@code javax.persistence.Id} is present; otherwise {@code false}
     */
    private static boolean hasLegacyId(@NotNull final Field f) {
        try {
            return f.isAnnotationPresent((Class) Class.forName("javax.persistence.Id"));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Saves (creates or updates) the given entity.
     * <ul>
     *   <li>If a {@link CrudRepository} is available for the entity type, {@code repo.save(entity)} is used.</li>
     *   <li>Otherwise, if an {@link EntityManager} is available:
     *     <ul>
     *       <li>{@code persist(entity)} is used when the ID value is {@code null}.</li>
     *       <li>{@code merge(entity)} is used when the ID value is non-{@code null}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param entity the entity to save; must not be {@code null}
     * @param <T>    entity type
     * @return the saved (possibly merged) entity; never {@code null}
     * @throws IllegalStateException if neither a repository nor an {@link EntityManager} is available
     */
    @NotNull
    @Override
    public <T> T save(@NotNull final T entity) {
        Class<?> domainType = entity.getClass();
        Optional<Object> repoOpt = this.repositories.getRepositoryFor(domainType);
        if (repoOpt.isPresent()) {
            CrudRepository repo = (CrudRepository) repoOpt.get();
            return (T) repo.save(entity);
        }
        if (this.emOrNull != null) {
            Object idVal = readIdValue(entity);
            if (idVal == null) {
                this.emOrNull.persist(entity);
                return entity;
            } else {
                return this.emOrNull.merge(entity);
            }
        }
        throw new IllegalStateException("No repository or EntityManager available for " + domainType.getName());
    }

    /**
     * Finds an entity by its type and identifier.
     * <ul>
     *   <li>If a repository is available, {@code findById} is used.</li>
     *   <li>Otherwise, if an {@link EntityManager} is available, {@link EntityManager#find(Class, Object)} is used.</li>
     * </ul>
     *
     * @param entityType the entity class; must not be {@code null}
     * @param id         the identifier value; must not be {@code null}
     * @param <T>        entity type
     * @return the entity instance, or {@code null} if not found
     * @throws UnsupportedOperationException if neither a repository nor an {@link EntityManager} is available
     */
    @Nullable
    @Override
    public <T> T findById(@NotNull final Class<T> entityType, @NotNull final Object id) {
        Optional<Object> repoOpt = this.repositories.getRepositoryFor(entityType);
        if (repoOpt.isPresent()) {
            CrudRepository repo = (CrudRepository) repoOpt.get();
            Optional<T> res = repo.findById(id);
            return res.orElse(null);
        }
        if (this.emOrNull != null) {
            return this.emOrNull.find(entityType, id);
        }
        throw new UnsupportedOperationException("findById not supported for " + entityType.getName());
    }

    /**
     * Resolves a Spring bean from the underlying {@link ApplicationContext}.
     *
     * @param type the bean type; must not be {@code null}
     * @param <R>  generic return type
     * @return the resolved bean; never {@code null}
     * @throws UnsupportedOperationException if no bean of the given type is available
     */
    @NotNull
    @Override
    public <R> R require(@NotNull final Class<R> type) {
        try {
            return this.ctx.getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            throw new UnsupportedOperationException("No bean of type " + type.getName() + " available", e);
        }
    }
}
