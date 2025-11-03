package com.example.gestion_approvisionnements.repository;

import com.example.gestion_approvisionnements.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByIce(String ice);

    boolean existsByIce(String ice);
}