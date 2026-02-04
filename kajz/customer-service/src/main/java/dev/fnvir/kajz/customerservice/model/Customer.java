package dev.fnvir.kajz.customerservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer entity representing a customer in the platform.
 */
@Entity
@Table(name = "customers")
@Getter @Setter
public class Customer {
    
    /** Internal ID for primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The userId of the customer.
     */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID customerId; // userId in keycloak
    
    /**
     * Gender of the customer.
     */
    private String gender;
    
    /**
     * Phone number of the customer.
     */
    private String phoneNumber;
    
    /**
     * The profile picture of the customer, referenced by fileId in storage-service.
     */
    private String profilePicture; // fileId in storage-service
    
    /**
     * The list of saved addresses of the customer.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Address> addresses = new ArrayList<>();
    
    /** Creation timestamp */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @ColumnDefault("current_timestamp")
    private Instant createdAt;
    
    /** Last update timestamp */
    @UpdateTimestamp
    @ColumnDefault("current_timestamp")
    private Instant updatedAt;
    
    public void setAddresses(List<Address> addresses) {
        if (addresses != null) {
            this.addresses = new ArrayList<>(addresses.size());
            for (var addr : addresses) {
                addAddress(addr);
            }
        }
    }
    
    public void addAddress(Address address) {
        if (address != null) {
            address.setCustomer(this);
            addresses.add(address);
        }
    }
    
    public void removeAddress(Address address) {
        addresses.removeIf(e -> e.addressEquals(address));
    }
    
}
