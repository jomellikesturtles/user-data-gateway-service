package com.mdb.user_data_gateway_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "mdb_profile", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "name" }) })
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Profile {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "bio")
    private String bio;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "is_main")
    private Boolean isMain;
}
