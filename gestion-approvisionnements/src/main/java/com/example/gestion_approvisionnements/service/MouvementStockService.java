package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.entity.MouvementStock;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.enums.TypeMouvement;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.MouvementStockMapper;
import com.example.gestion_approvisionnements.repository.CommandeFournisseurRepository;
import com.example.gestion_approvisionnements.repository.MouvementStockRepository;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.math.RoundingMode;

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
        // Vérification du produit
        Produit produit = produitRepository.findById(mouvementDTO.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + mouvementDTO.getProduitId()));

        // Vérification de la commande fournisseur si fournie
        CommandeFournisseur commande = null;
        if (mouvementDTO.getCommandeFournisseurId() != null) {
            commande = commandeFournisseurRepository.findById(mouvementDTO.getCommandeFournisseurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Commande fournisseur introuvable avec l'id " + mouvementDTO.getCommandeFournisseurId()));
        }

        // Création de l'entité à partir du DTO
        MouvementStock mouvement = mouvementStockMapper.toEntity(mouvementDTO);
        mouvement.setProduit(produit);
        mouvement.setCommandeFournisseur(commande);
        mouvement.setDateMouvement(
                mouvementDTO.getDateMouvement() != null ? mouvementDTO.getDateMouvement() : LocalDateTime.now()
        );

        // Gestion du type de mouvement
        TypeMouvement type = mouvement.getTypeMouvement();
        int quantite = mouvement.getQuantite();

        switch (type) {
            case ENTREE:
                BigDecimal prixUnitaire = mouvement.getPrixUnitaire() != null
                        ? mouvement.getPrixUnitaire()
                        : produit.getPrixUnitaire();
                recalculerCump(produit, quantite, prixUnitaire);
                break;

            case SORTIE:
                int stockApresSortie = produit.getStockActuel() - quantite;
                if (stockApresSortie < 0) {
                    throw new BusinessException("Stock insuffisant pour effectuer cette sortie");
                }
                produit.setStockActuel(stockApresSortie);
                break;

            case AJUSTEMENT:
                // Pour un ajustement, on définit directement la nouvelle valeur du stock
                if (quantite < 0) {
                    throw new BusinessException("La quantité d'ajustement ne peut pas être négative");
                }
                produit.setStockActuel(quantite);
                break;
        }

        // Mise à jour du stock après mouvement
        mouvement.setStockApresMouvement(produit.getStockActuel());

        // Sauvegarde des modifications
        produitRepository.save(produit);
        MouvementStock saved = mouvementStockRepository.save(mouvement);

        return mouvementStockMapper.toDTO(saved);
    }

    private int calculerNouveauStock(int stockActuel, TypeMouvement type, int quantite) {
        return switch (type) {
            case ENTREE -> stockActuel + quantite;
            case SORTIE -> stockActuel - quantite;
            default -> throw new IllegalStateException("Type de mouvement non géré: " + type);
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

    private void recalculerCump(Produit produit, int quantiteEntree, BigDecimal prixUnitaire) {
        int ancienStock = produit.getStockActuel();
        BigDecimal ancienCump = produit.getCoutMoyenPondere();
        if (ancienCump == null) {
            ancienCump = BigDecimal.ZERO;
        }

        BigDecimal valeurAncienStock = ancienCump.multiply(BigDecimal.valueOf(ancienStock));
        BigDecimal valeurNouvelleEntree = prixUnitaire.multiply(BigDecimal.valueOf(quantiteEntree));
        int nouveauStock = ancienStock + quantiteEntree;

        if (nouveauStock <= 0) {
            produit.setCoutMoyenPondere(BigDecimal.ZERO);
            produit.setStockActuel(0);
            return;
        }

        BigDecimal nouveauCump = valeurAncienStock
                .add(valeurNouvelleEntree)
                .divide(BigDecimal.valueOf(nouveauStock), 2, RoundingMode.HALF_UP);

        produit.setCoutMoyenPondere(nouveauCump);
        produit.setStockActuel(nouveauStock);
    }
}