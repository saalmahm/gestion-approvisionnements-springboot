package com.example.gestion_approvisionnements.repository;

import com.example.gestion_approvisionnements.entity.MouvementStock;
import com.example.gestion_approvisionnements.enums.TypeMouvement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitIdOrderByDateMouvementAsc(Long produitId);

    List<MouvementStock> findByTypeMouvement(TypeMouvement type);

    List<MouvementStock> findByCommandeFournisseurId(Long commandeId);
}