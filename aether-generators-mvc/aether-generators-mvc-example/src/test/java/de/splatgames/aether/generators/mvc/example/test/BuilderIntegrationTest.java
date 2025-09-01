package de.splatgames.aether.generators.mvc.example.test;

import de.splatgames.aether.generators.mvc.adapter.spring.SpringPersistAdapter;
import de.splatgames.aether.generators.mvc.example.domain.Role;
import de.splatgames.aether.generators.mvc.example.domain.User;
import de.splatgames.aether.generators.mvc.example.domain.UserBuilder;
import de.splatgames.aether.generators.mvc.example.repository.RoleRepository;
import de.splatgames.aether.generators.mvc.example.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EntityScan(basePackageClasses = {User.class, Role.class})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RoleRepository.class})
@Transactional
class BuilderIntegrationTest {

    @Autowired ApplicationContext ctx;
    @Autowired
    RoleRepository roleRepo;
    @Autowired
    UserRepository userRepo;

    @Test
    @DisplayName("asIdOnly(to-one): writes and persists user")
    void asIdOnly_roleId_writes_and_persists_user() {
        Role r = new Role();
        r.setName("ADMIN");
        r = this.roleRepo.saveAndFlush(r);
        Long roleId = r.getId();
        assertThat(roleId).isNotNull();

        SpringPersistAdapter adapter = new SpringPersistAdapter(this.ctx);

        User u = new UserBuilder(adapter)
                .persistent()
                .withUsername("jdoe")
                .withRoleId(roleId)
                .create();

        assertThat(u.getId()).isNotNull();
        User reloaded = this.userRepo.findById(u.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isNotNull();
        assertThat(reloaded.getRole().getId()).isEqualTo(roleId);
    }

    @Test
    @DisplayName("to-many (supervisors): References user first and then links")
    void supervisors_link_when_related_is_already_persisted() {
        SpringPersistAdapter adapter = new SpringPersistAdapter(this.ctx);

        User b = new UserBuilder(adapter)
                .persistent()
                .withUsername("b")
                .create();
        assertThat(b.getId()).isNotNull();

        User a = new UserBuilder(adapter)
                .persistent()
                .withUsername("a")
                .withSupervisors(List.of(b))
                .create();

        assertThat(a.getId()).isNotNull();
        User aLoaded = this.userRepo.findById(a.getId()).orElseThrow();
        assertThat(aLoaded.getSupervisors())
                .isNotNull()
                .anySatisfy(sup -> assertThat(sup.getId()).isEqualTo(b.getId()));
    }
}
