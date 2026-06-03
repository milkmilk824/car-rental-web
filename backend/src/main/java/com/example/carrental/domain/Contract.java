package com.example.carrental.domain;

import com.example.carrental.common.Enums.ContractStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "contract", indexes = {
        @Index(name = "idx_contract_no", columnList = "contractNo", unique = true),
        @Index(name = "idx_contract_order", columnList = "order_id"),
        @Index(name = "idx_contract_user_create_time", columnList = "user_id,create_time")
})
public class Contract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String contractNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String contractUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus signStatus = ContractStatus.UNSIGNED;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public RentalOrder getRentalOrder() {
        return rentalOrder;
    }

    public void setRentalOrder(RentalOrder rentalOrder) {
        this.rentalOrder = rentalOrder;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getContractUrl() {
        return contractUrl;
    }

    public void setContractUrl(String contractUrl) {
        this.contractUrl = contractUrl;
    }

    public ContractStatus getSignStatus() {
        return signStatus;
    }

    public void setSignStatus(ContractStatus signStatus) {
        this.signStatus = signStatus;
    }
}
