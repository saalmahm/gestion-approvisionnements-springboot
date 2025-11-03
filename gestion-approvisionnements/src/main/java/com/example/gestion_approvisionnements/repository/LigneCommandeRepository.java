package com.example.gestion_approvisionnements.repository;

import com.example.gestion_approvisionnements.entity.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {

    List<LigneCommande> findByCommandeFournisseurId(Long commandeId);
}