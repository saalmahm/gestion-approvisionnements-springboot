package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.entity.MouvementStock;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.enums.TypeMouvement;
import com.example.gestion_approvisionnements.mapper.MouvementStockMapper;
import com.example.gestion_approvisionnements.repository.CommandeFournisseurRepository;
import com.example.gestion_approvisionnements.repository.MouvementStockRepository;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MouvementStockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;
    private final CommandeFournisseurRepository commandeFournisseurRepository;
    private final MouvementStockMapper mouvementStockMapper;

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> getMouvementsParProduit(Long produitId) {
        return mouvementStockMapper.toDTOList(
                mouvementStockRepository.findByProduitIdOrderByDateMouvementAsc(produitId)
        );
    }

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> getMouvementsParType(TypeMouvement typeMouvement) {
        return mouvementStockMapper.toDTOList(
                mouvementStockRepository.findByTypeMouvement(typeMouvement)
        );
    }

    public MouvementStockDTO enregistrerMouvement(MouvementStockDTO mouvementDTO) {
        Produit produit = produitRepository.findById(mouvementDTO.getProduitId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable avec l'id " + mouvementDTO.getProduitId()));

        CommandeFournisseur commande = null;
        if (mouvementDTO.getCommandeFournisseurId() != null) {
            commande = commandeFournisseurRepository.findById(mouvementDTO.getCommandeFournisseurId())
                    .orElseThrow(() -> new EntityNotFoundException("Commande fournisseur introuvable avec l'id " + mouvementDTO.getCommandeFournisseurId()));
        }

        MouvementStock mouvement = mouvementStockMapper.toEntity(mouvementDTO);
        mouvement.setProduit(produit);
        mouvement.setCommandeFournisseur(commande);
        mouvement.setDateMouvement(mouvementDTO.getDateMouvement() != null
                ? mouvementDTO.getDateMouvement()
                : LocalDateTime.now());

        int nouveauStock = calculerNouveauStock(produit.getStockActuel(), mouvement.getTypeMouvement(), mouvement.getQuantite());
        if (nouveauStock < 0) {
            throw new IllegalStateException("Le stock ne peut pas devenir négatif");
        }

        produit.setStockActuel(nouveauStock);

        // Mettre à jour le CUMP en cas d’entrée (placeholder pour logique FIFO/CUMP)
        if (TypeMouvement.ENTREE.equals(mouvement.getTypeMouvement())) {
            BigDecimal prixUnitaire = mouvement.getPrixUnitaire() != null
                    ? mouvement.getPrixUnitaire()
                    : produit.getPrixUnitaire();
            mettreAJourCoutMoyen(produit, mouvement.getQuantite(), prixUnitaire);
        }

        mouvement.setStockApresMouvement(nouveauStock);

        produitRepository.save(produit);
        MouvementStock saved = mouvementStockRepository.save(mouvement);

        return mouvementStockMapper.toDTO(saved);
    }

    private int calculerNouveauStock(int stockActuel, TypeMouvement type, int quantite) {
        return switch (type) {
            case ENTREE -> stockActuel + quantite;
            case SORTIE -> stockActuel - quantite;
            case AJUSTEMENT -> quantite;
        };
    }

    private void mettreAJourCoutMoyen(Produit produit, int quantiteEntree, BigDecimal prixEntree) {
        BigDecimal stockActuel = BigDecimal.valueOf(produit.getStockActuel());
        BigDecimal quantiteEntreeBig = BigDecimal.valueOf(quantiteEntree);

        if (stockActuel.subtract(quantiteEntreeBig).compareTo(BigDecimal.ZERO) <= 0) {
            produit.setCoutMoyenPondere(prixEntree);
            return;
        }

        BigDecimal stockAvantEntree = stockActuel.subtract(quantiteEntreeBig);
        BigDecimal valeurAvant = produit.getCoutMoyenPondere() != null
                ? produit.getCoutMoyenPondere().multiply(stockAvantEntree)
                : BigDecimal.ZERO;

        BigDecimal valeurEntree = prixEntree.multiply(quantiteEntreeBig);
        BigDecimal nouvelleValeurTotale = valeurAvant.add(valeurEntree);
        BigDecimal nouvelleQuantiteTotale = stockAvantEntree.add(quantiteEntreeBig);

        BigDecimal nouveauCump = nouvelleQuantiteTotale.compareTo(BigDecimal.ZERO) > 0
                ? nouvelleValeurTotale.divide(nouvelleQuantiteTotale, 2, BigDecimal.ROUND_HALF_UP)
                : prixEntree;

        produit.setCoutMoyenPondere(nouveauCump);
    }
}