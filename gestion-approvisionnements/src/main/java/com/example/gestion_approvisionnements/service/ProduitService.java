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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;

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

    public void ajusterStock(Long produitId, int variation) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + produitId));

        int nouveauStock = produit.getStockActuel() + variation;
        if (nouveauStock < 0) {
            throw new BusinessException("Le stock ne peut pas devenir négatif (variation: " + variation + ")");
        }

        produit.setStockActuel(nouveauStock);
        produitRepository.save(produit);
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