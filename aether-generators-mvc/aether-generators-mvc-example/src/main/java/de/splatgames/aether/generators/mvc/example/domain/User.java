package de.splatgames.aether.generators.mvc.example.domain;

import de.splatgames.aether.generators.mvc.annotations.MvcBuilder;
import de.splatgames.aether.generators.mvc.annotations.MvcField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@MvcBuilder
@Entity
@Table(name = "app_user")
public class User {
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Column(nullable = false, unique = true)
    private String username;

    @Setter
    @Getter
    @MvcField(asIdOnly = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "role_id")
    private Role role;

    @Setter
    @Getter
    @MvcField(asIdOnly = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Getter
    @ManyToMany
    @JoinTable(
            name = "user_supervisors",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "supervisor_id")
    )
    private List<User> supervisors = new ArrayList<>();

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "deactivated_by")
    private User deactivatedBy;

    public User() {
    }

    public static void linkMutualSupervision(final User a, final User b) {
        if (a == null || b == null) return;
        if (!a.supervisors.contains(b)) a.supervisors.add(b);
        if (!b.supervisors.contains(a)) b.supervisors.add(a);
    }

    public void addSupervisor(final User u) {
        if (u == null) return;
        if (!this.supervisors.contains(u)) this.supervisors.add(u);
    }

    public void setSupervisors(final Collection<User> supervisors) {
        this.supervisors.clear();
        if (supervisors != null) this.supervisors.addAll(supervisors);
    }
}
