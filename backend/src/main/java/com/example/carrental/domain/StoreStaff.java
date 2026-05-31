package com.example.carrental.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "store_staff",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_staff_user_store", columnNames = {"user_id", "store_id"}),
        indexes = {
                @Index(name = "idx_store_staff_user", columnList = "user_id"),
                @Index(name = "idx_store_staff_store", columnList = "store_id")
        }
)
public class StoreStaff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_staff_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }
}
