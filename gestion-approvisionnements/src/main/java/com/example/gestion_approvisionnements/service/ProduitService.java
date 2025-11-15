package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.ProduitDTO;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.ProduitMapper;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.enums.TypeMouvement;
import com.example.gestion_approvisionnements.service.MouvementStockService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;
    private final MouvementStockService mouvementStockService;

    @Transactional(readOnly = true)
    public Page<ProduitDTO> getAllProduits(Pageable pageable) {
        return produitRepository.findAll(pageable)
                .map(produitMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public ProduitDTO getProduitById(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + id));
        return produitMapper.toDTO(produit);
    }

    public ProduitDTO createProduit(ProduitDTO produitDTO) {
    validateProduit(produitDTO);

    Produit produit = produitMapper.toEntity(produitDTO);

    if (produit.getCoutMoyenPondere() == null && produit.getPrixUnitaire() != null) {
        produit.setCoutMoyenPondere(produit.getPrixUnitaire());
    }

    Produit saved = produitRepository.save(produit);
    return produitMapper.toDTO(saved);
}

    public ProduitDTO updateProduit(Long id, ProduitDTO produitDTO) {
        Produit existing = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + id));

        validateProduit(produitDTO);

        produitMapper.updateEntityFromDTO(produitDTO, existing);
        Produit updated = produitRepository.save(existing);
        return produitMapper.toDTO(updated);
    }

    public void deleteProduit(Long id) {
        if (!produitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produit introuvable avec l'id " + id);
        }
        produitRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsParCategorie(String categorie) {
        return produitMapper.toDTOList(produitRepository.findByCategorieIgnoreCase(categorie));
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsStockFaible(Integer seuil) {
        return produitMapper.toDTOList(produitRepository.findByStockActuelLessThanEqual(seuil));
    }

    public void ajusterStock(Long produitId, int variation, BigDecimal nouveauPrixUnitaire) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + produitId));

        int nouveauStock = produit.getStockActuel();
        
        // Création du mouvement
        MouvementStockDTO mouvement = new MouvementStockDTO();
        mouvement.setProduitId(produitId);
        
        if (nouveauPrixUnitaire != null && nouveauPrixUnitaire.compareTo(BigDecimal.ZERO) > 0) {
            // Cas 1: Nouvel arrivage avec nouveau prix
            mouvement.setTypeMouvement(TypeMouvement.ENTREE);
            mouvement.setQuantite(variation);
            mouvement.setPrixUnitaire(nouveauPrixUnitaire);
            produit.setPrixUnitaire(nouveauPrixUnitaire);
        } else {
            // Cas 2: Ajustement simple de stock
            mouvement.setTypeMouvement(TypeMouvement.AJUSTEMENT);
            nouveauStock += variation;  // On ajoute la variation ici
            if (nouveauStock < 0) {
                throw new BusinessException("Le stock ne peut pas devenir négatif");
            }
            mouvement.setQuantite(nouveauStock);
            mouvement.setPrixUnitaire(produit.getPrixUnitaire());
        }

        // Mise à jour du stock
        produit.setStockActuel(nouveauStock);
        produitRepository.save(produit);

        // Enregistrement du mouvement
        mouvementStockService.enregistrerMouvement(mouvement);
    }

    public void mettreAJourCoutMoyen(Long produitId, BigDecimal nouveauCump) {
        if (nouveauCump == null || nouveauCump.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le coût moyen pondéré doit être positif");
        }
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + produitId));

        produit.setCoutMoyenPondere(nouveauCump);
        produitRepository.save(produit);
    }

    private void validateProduit(ProduitDTO produitDTO) {
        if (produitDTO.getPrixUnitaire() != null && produitDTO.getPrixUnitaire().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le prix unitaire doit être positif");
        }
        if (produitDTO.getStockActuel() != null && produitDTO.getStockActuel() < 0) {
            throw new BusinessException("Le stock actuel ne peut pas être négatif");
        }
        if (produitDTO.getCoutMoyenPondere() != null && produitDTO.getCoutMoyenPondere().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le coût moyen pondéré doit être positif");
        }
    }
}