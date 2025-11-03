package com.example.gestion_approvisionnements.repository;

import com.example.gestion_approvisionnements.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByCategorieIgnoreCase(String categorie);

    List<Produit> findByStockActuelLessThanEqual(Integer seuil);
}