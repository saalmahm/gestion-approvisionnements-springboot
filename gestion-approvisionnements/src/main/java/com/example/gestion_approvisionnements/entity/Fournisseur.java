package com.example.gestion_approvisionnements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fournisseur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String societe;

    @Column(length = 255)
    private String adresse;

    @Column(length = 100)
    private String contact;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Column(length = 100)
    private String ville;

    @Column(unique = true, length = 50)
    private String ice;

    @OneToMany(mappedBy = "fournisseur", cascade = CascadeType.ALL)
    private List<CommandeFournisseur> commandes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}