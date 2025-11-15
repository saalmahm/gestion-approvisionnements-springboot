package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.CommandeFournisseurDTO;
import com.example.gestion_approvisionnements.dto.LigneCommandeDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import com.example.gestion_approvisionnements.entity.LigneCommande;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.enums.StatutCommande;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.CommandeFournisseurMapper;
import com.example.gestion_approvisionnements.mapper.LigneCommandeMapper;
import com.example.gestion_approvisionnements.repository.CommandeFournisseurRepository;
import com.example.gestion_approvisionnements.repository.FournisseurRepository;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.enums.TypeMouvement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommandeFournisseurService {

    private final CommandeFournisseurRepository commandeFournisseurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final CommandeFournisseurMapper commandeFournisseurMapper;
    private final LigneCommandeMapper ligneCommandeMapper;
    private final MouvementStockService mouvementStockService;

    @Transactional(readOnly = true)
    public Page<CommandeFournisseurDTO> getAllCommandes(Pageable pageable) {
        return commandeFournisseurRepository.findAll(pageable)
                .map(commandeFournisseurMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public CommandeFournisseurDTO getCommandeById(Long id) {
        CommandeFournisseur commande = commandeFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande fournisseur introuvable avec l'id " + id));
        return commandeFournisseurMapper.toDTO(commande);
    }

    public CommandeFournisseurDTO createCommande(CommandeFournisseurDTO commandeDTO) {
        Fournisseur fournisseur = fournisseurRepository.findById(commandeDTO.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec l'id " + commandeDTO.getFournisseurId()));

        CommandeFournisseur commande = commandeFournisseurMapper.toEntity(commandeDTO);
        commande.setFournisseur(fournisseur);

        List<LigneCommande> lignes = buildLignesCommande(commandeDTO.getLignesCommande(), commande);
        commande.setLignesCommande(lignes);

        commande.setMontantTotal(calculerMontantTotal(lignes));

        CommandeFournisseur saved = commandeFournisseurRepository.save(commande);
        return commandeFournisseurMapper.toDTO(saved);
    }

    public CommandeFournisseurDTO updateStatut(Long commandeId, StatutCommande nouveauStatut) {
        CommandeFournisseur commande = commandeFournisseurRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande fournisseur introuvable avec l'id " + commandeId));

        // ancien statut
        StatutCommande ancienStatut = commande.getStatut();
        commande.setStatut(nouveauStatut);
        CommandeFournisseur updated = commandeFournisseurRepository.save(commande);

        // Si la commande vient de passer à LIVREE: les mouvements de SORTIE
        if (!StatutCommande.LIVREE.equals(ancienStatut) && StatutCommande.LIVREE.equals(nouveauStatut)) {
            if (commande.getLignesCommande() != null) {
                commande.getLignesCommande().forEach(ligne -> {
                    MouvementStockDTO mouvement = new MouvementStockDTO();
                    mouvement.setProduitId(ligne.getProduit().getId());
                    mouvement.setCommandeFournisseurId(commande.getId());
                    //  SORTIE car on vend au fournisseur
                    mouvement.setTypeMouvement(TypeMouvement.SORTIE);
                    mouvement.setQuantite(ligne.getQuantite());

                    mouvement.setPrixUnitaire(ligne.getPrixUnitaire());
                    // mouvement.setReference("CMD-VENTE-" + commande.getId());

                    mouvementStockService.enregistrerMouvement(mouvement);
                });
            }
        }

        return commandeFournisseurMapper.toDTO(updated);
    }

    public void deleteCommande(Long id) {
        if (!commandeFournisseurRepository.existsById(id)) {
            throw new ResourceNotFoundException("Commande fournisseur introuvable avec l'id " + id);
        }
        commandeFournisseurRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CommandeFournisseurDTO> getCommandesParStatut(StatutCommande statut) {
        return commandeFournisseurMapper.toDTOList(
                commandeFournisseurRepository.findByStatut(statut)
        );
    }

    @Transactional(readOnly = true)
    public List<CommandeFournisseurDTO> getCommandesParFournisseur(Long fournisseurId) {
        return commandeFournisseurMapper.toDTOList(
                commandeFournisseurRepository.findByFournisseurId(fournisseurId)
        );
    }

    @Transactional(readOnly = true)
    public List<CommandeFournisseurDTO> getCommandesParPeriode(LocalDate debut, LocalDate fin) {
        return commandeFournisseurMapper.toDTOList(
                commandeFournisseurRepository.findByDateCommandeBetween(debut, fin)
        );
    }

    private List<LigneCommande> buildLignesCommande(List<LigneCommandeDTO> lignesDTO, CommandeFournisseur commande) {
        if (lignesDTO == null || lignesDTO.isEmpty()) {
            throw new BusinessException("La commande doit contenir au moins une ligne");
        }

        List<LigneCommande> lignes = new ArrayList<>();
        for (LigneCommandeDTO ligneDTO : lignesDTO) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id " + ligneDTO.getProduitId()));

            LigneCommande ligne = ligneCommandeMapper.toEntity(ligneDTO);
            ligne.setCommandeFournisseur(commande);
            ligne.setProduit(produit);

            BigDecimal prixUnitaire = ligneDTO.getPrixUnitaire() != null
                    ? ligneDTO.getPrixUnitaire()
                    : produit.getPrixUnitaire();
            ligne.setPrixUnitaire(prixUnitaire);

            ligne.calculerSousTotal();  
            lignes.add(ligne);
        }
        return lignes;
    }

    private BigDecimal calculerMontantTotal(List<LigneCommande> lignes) {
        return lignes.stream()
                .map(LigneCommande::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}