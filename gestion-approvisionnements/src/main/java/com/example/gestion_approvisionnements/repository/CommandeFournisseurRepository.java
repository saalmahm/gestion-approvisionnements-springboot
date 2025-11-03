package com.example.gestion_approvisionnements.repository;

import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CommandeFournisseurRepository extends JpaRepository<CommandeFournisseur, Long> {

    List<CommandeFournisseur> findByStatut(StatutCommande statut);

    List<CommandeFournisseur> findByFournisseurId(Long fournisseurId);

    List<CommandeFournisseur> findByDateCommandeBetween(LocalDate debut, LocalDate fin);
}