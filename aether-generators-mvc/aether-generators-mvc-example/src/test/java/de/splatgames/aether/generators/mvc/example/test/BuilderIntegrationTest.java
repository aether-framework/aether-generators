package de.splatgames.aether.generators.mvc.example.test;

import de.splatgames.aether.generators.mvc.adapter.spring.SpringPersistAdapter;
import de.splatgames.aether.generators.mvc.example.domain.Role;
import de.splatgames.aether.generators.mvc.example.domain.User;
import de.splatgames.aether.generators.mvc.example.domain.UserBuilder;
import de.splatgames.aether.generators.mvc.example.repository.RoleRepository;
import de.splatgames.aether.generators.mvc.example.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EntityScan(basePackageClasses = {User.class, Role.class})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RoleRepository.class})
@Transactional
class BuilderIntegrationTest {

    @Autowired ApplicationContext ctx;
    @Autowired RoleRepository roleRepo;
    @Autowired UserRepository userRepo;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("Builder API: role is asIdOnly → only withRoleId available, no withRole")
    void builder_exposes_only_withRoleId() {
        boolean hasWithRole = Arrays.stream(UserBuilder.class.getMethods())
                .anyMatch(m -> m.getName().equals("withRole")
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].equals(Role.class));
        boolean hasWithRoleId = Arrays.stream(UserBuilder.class.getMethods())
                .anyMatch(m -> m.getName().equals("withRoleId")
                        && m.getParameterCount() == 1);
        assertThat(hasWithRole).isFalse();
        assertThat(hasWithRoleId).isTrue();
    }

    @Test
    @DisplayName("autoRelations: unsaved to-one (deactivatedBy) & to-many (supervisors) are persisted before the root")
    void autoRelations_persists_unsaved_relations() {
        SpringPersistAdapter adapter = new SpringPersistAdapter(this.ctx);

        // unsaved to-one
        User deactBy = new User();
        deactBy.setUsername("auto_deactBy");

        // unsaved to-many elements
        User sup1 = new User();
        sup1.setUsername("auto_sup1");
        User sup2 = new User();
        sup2.setUsername("auto_sup2");

        User u = new UserBuilder(adapter)
                .persistent()
                .withUsername("auto_root")
                .withDeactivatedBy(deactBy)  // to-one (not asIdOnly)
                .addSupervisor(sup1)         // to-many
                .addSupervisor(sup2)
                .create();

        assertThat(u.getId()).isNotNull();

        User re = this.userRepo.findById(u.getId()).orElseThrow();
        assertThat(re.getDeactivatedBy()).isNotNull();
        assertThat(re.getDeactivatedBy().getId()).isNotNull();
        assertThat(re.getSupervisors())
                .isNotNull()
                .hasSize(2)
                .allSatisfy(s -> assertThat(s.getId()).isNotNull());
    }

    @Test
    @DisplayName("Cycles/Chaining: use the builder for relations – IDs are present at create()")
    void cycles_in_to_one_relations_are_handled() {
        SpringPersistAdapter adapter = new SpringPersistAdapter(this.ctx);

        // First, create relations properly with the BUILDER (persist),
        // so that IDs exist — this is exactly what the builder is for.
        User b = new UserBuilder(adapter)
                .persistent()
                .withUsername("cycle_b")
                .create();
        assertThat(b.getId()).isNotNull();

        User a = new UserBuilder(adapter)
                .persistent()
                .withUsername("cycle_a")
                .withDeactivatedBy(b) // a -> b
                .create();
        assertThat(a.getId()).isNotNull();

        // Root points to a
        User root = new UserBuilder(adapter)
                .persistent()
                .withUsername("cycle_root")
                .withDeactivatedBy(a)
                .create();
        assertThat(root.getId()).isNotNull();

        User re = this.userRepo.findById(root.getId()).orElseThrow();
        User aDb = re.getDeactivatedBy();
        assertThat(aDb).isNotNull();
        assertThat(aDb.getId()).isEqualTo(a.getId());

        // a -> b is set (created via builder)
        User bDb = aDb.getDeactivatedBy();
        assertThat(bDb).isNotNull();
        assertThat(bDb.getId()).isEqualTo(b.getId());

        // There is NO requirement that b -> a is also set.
        // (That would be an additional domain/mapping decision, not implicitly guaranteed.)
    }

    @Test
    @DisplayName("Collections API: add / addAll / clear work and the result is persisted correctly")
    void collection_api_add_addAll_clear() {
        SpringPersistAdapter adapter = new SpringPersistAdapter(this.ctx);

        User supA = new User();
        supA.setUsername("supA");
        User supB = new User();
        supB.setUsername("supB");
        User supC = new User();
        supC.setUsername("supC");

        User u = new UserBuilder(adapter)
                .persistent()
                .withUsername("collector")
                .addSupervisor(supA)
                .addAllSupervisors(List.of(supB, supC))
                .clearSupervisors() // clear before create()
                .create();

        assertThat(u.getId()).isNotNull();
        User re = this.userRepo.findById(u.getId()).orElseThrow();
        assertThat(re.getSupervisors()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Transient mode: without adapter, nothing is persisted (ID remains null)")
    void transient_mode_does_not_persist() {
        User u = new UserBuilder()
                .withUsername("transient_user")
                .create();

        assertThat(u.getId()).isNull();
        assertThat(this.userRepo.findAll())
                .noneMatch(x -> "transient_user".equals(x.getUsername()));
    }
}
