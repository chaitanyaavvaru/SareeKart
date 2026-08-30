package com.sareekart.entity;

import com.sareekart.entity.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AddressType type = AddressType.SHIPPING;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Size(max = 255)
    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Size(max = 255)
    @Column(name = "line2", length = 255)
    private String line2;

    @NotBlank
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @NotBlank
    @Size(max = 10)
    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "country", nullable = false, length = 100)
    private String country = "India";

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(line1);
        if (line2 != null && !line2.isBlank()) {
            sb.append(", ").append(line2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" - ").append(pincode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}