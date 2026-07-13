package com.mdb.user_data_gateway_service.entity.interaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "list", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_id", "name" }),
    @UniqueConstraint(columnNames = { "profile_id", "name" })
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "img_url")
    private String imageUrl;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @UpdateTimestamp
    @Column(name = "upd8_date")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "cr8_date")
    private LocalDateTime createdAt;
}
