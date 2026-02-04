package dev.fnvir.kajz.customerservice.model;

import java.time.Instant;

import org.apache.commons.lang3.Strings;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.StringUtils;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a customer's saved addresses.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address {

    /**
     * The ID of the address.
     */
    @Id
    @Tsid
    private Long id;

    /**
     * The first line of the address.
     */
    @Column(nullable = false)
    private String addressLine1;

    /**
     * The second line of the address (optional).
     */
    private String addressLine2;

    /**
     * The city of the address.
     */
    @Column(nullable = false)
    private String city;

    /**
     * The state/province/district of the address.
     */
    @Column(nullable = false)
    private String state;

    /**
     * The postal code of the address.
     */
    @Column(nullable = false)
    private String postalCode;

    /**
     * The country of the address.
     */
    @Column(nullable = false)
    private String country;

    /**
     * Foreign key to the customer who owns this address.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Creation timestamp */
    @CreationTimestamp
    @Column(updatable = false)
    @ColumnDefault("current_timestamp")
    private Instant createdAt;

    /** Last update timestamp */
    @UpdateTimestamp
    @ColumnDefault("current_timestamp")
    private Instant updatedAt;
    
    @Transient
    public Long getCustomerInternalId() {
        return customer != null ? customer.getId() : null;
    }
    
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (StringUtils.hasText(addressLine2)) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(city)
          .append(", ").append(state)
          .append(" ").append(postalCode)
          .append(", ").append(country);
        return sb.toString();
    }

    public boolean addressEquals(Address other) {
        return Strings.CI.equals(addressLine1, other.addressLine1)
                && Strings.CI.equals(addressLine2, other.addressLine2)
                && Strings.CI.equals(city, other.city)
                && Strings.CI.equals(state, other.state)
                && Strings.CI.equals(postalCode, other.postalCode)
                && Strings.CI.equals(country, other.country);
    }
    
}
