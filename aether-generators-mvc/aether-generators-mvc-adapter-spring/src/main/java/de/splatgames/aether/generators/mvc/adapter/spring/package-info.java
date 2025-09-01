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
 * Spring-based adapter implementations for the Aether MVC persistence layer.
 *
 * <h2>Overview</h2>
 * This package provides a {@link de.splatgames.aether.generators.mvc.adapter.spring.SpringPersistAdapter}
 * that integrates the generic {@link de.splatgames.aether.generators.mvc.runtime.PersistAdapter PersistAdapter}
 * abstraction with Spring Data and JPA.
 *
 * <p>It resolves persistence operations in the following order:</p>
 * <ol>
 *   <li>If a Spring Data {@code CrudRepository} is registered for the domain type,
 *       it will be used for persistence operations.</li>
 *   <li>If no repository is available, but a managed {@link jakarta.persistence.EntityManager EntityManager}
 *       is present, it will be used as a fallback (choosing between {@code persist} and
 *       {@code merge} depending on the presence of an identifier).</li>
 * </ol>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic resolution of Spring Data repositories via {@link org.springframework.data.repository.support.Repositories Repositories}.</li>
 *   <li>Optional fallback to {@link jakarta.persistence.EntityManager EntityManager} if no repository is available.</li>
 *   <li>Identifier detection via reflection ({@link jakarta.persistence.Id Id} or conventional {@code id} field;
 *       legacy {@code javax.persistence.Id} is also supported).</li>
 *   <li>Stateless, thread-safe implementation that does not establish transactional boundaries.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <blockquote><pre>
 * ApplicationContext ctx = ...;
 * SpringPersistAdapter adapter = new SpringPersistAdapter(ctx);
 *
 * // Save entity
 * User user = adapter.save(new User(null, "Alice"));
 *
 * // Find entity by ID
 * User found = adapter.findById(User.class, user.getId());
 *
 * // Access another Spring bean
 * MyService service = adapter.require(MyService.class);
 * </pre></blockquote>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Transaction management must be handled by the caller
 *       (e.g. service layer, {@code @Transactional} methods, or {@code @DataJpaTest}).</li>
 *   <li>The adapter itself does not define transactional scopes or manage flush behavior.</li>
 * </ul>
 *
 * @since Spring Adapter 1.0.0
 */
package de.splatgames.aether.generators.mvc.adapter.spring;
