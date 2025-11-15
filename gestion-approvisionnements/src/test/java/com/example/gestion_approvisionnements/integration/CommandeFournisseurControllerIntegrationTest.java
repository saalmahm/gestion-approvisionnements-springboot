package com.example.gestion_approvisionnements.integration;

import com.example.gestion_approvisionnements.dto.CommandeFournisseurDTO;
import com.example.gestion_approvisionnements.dto.LigneCommandeDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.enums.StatutCommande;
import com.example.gestion_approvisionnements.repository.CommandeFournisseurRepository;
import com.example.gestion_approvisionnements.repository.FournisseurRepository;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommandeFournisseurControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CommandeFournisseurRepository commandeFournisseurRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Fournisseur fournisseur;
    private Produit produit1;
    private Produit produit2;

    @BeforeEach
    void setUp() {
        commandeFournisseurRepository.deleteAll();
        fournisseurRepository.deleteAll();
        produitRepository.deleteAll();

        fournisseur = new Fournisseur();
        fournisseur.setSociete("Fournisseur IT");
        fournisseur.setIce("ICE_CMD");
        fournisseur = fournisseurRepository.save(fournisseur);

        produit1 = buildProduit("Prod 1", BigDecimal.valueOf(10));
        produit2 = buildProduit("Prod 2", BigDecimal.valueOf(20));
        produit1 = produitRepository.save(produit1);
        produit2 = produitRepository.save(produit2);
    }

    private Produit buildProduit(String nom, BigDecimal prix) {
        Produit p = new Produit();
        p.setNom(nom);
        p.setPrixUnitaire(prix);
        p.setCategorie("TEST");
        p.setStockActuel(0);
        p.setCoutMoyenPondere(prix);
        return p;
    }

    private CommandeFournisseurDTO buildCommandeDTO() {
        LigneCommandeDTO l1 = new LigneCommandeDTO(
                null,
                produit1.getId(),
                produit1.getNom(),
                2,
                produit1.getPrixUnitaire(),
                produit1.getPrixUnitaire().multiply(BigDecimal.valueOf(2))
        );
        LigneCommandeDTO l2 = new LigneCommandeDTO(
                null,
                produit2.getId(),
                produit2.getNom(),
                1,
                produit2.getPrixUnitaire(),
                produit2.getPrixUnitaire()
        );

        return new CommandeFournisseurDTO(
                null,
                LocalDate.now(),
                StatutCommande.EN_ATTENTE,
                null,
                fournisseur.getId(),
                fournisseur.getSociete(),
                List.of(l1, l2)
        );
    }

    @Test
    void createCommande_shouldPersistAndReturnCreated() throws Exception {
        CommandeFournisseurDTO dto = buildCommandeDTO();

        mockMvc.perform(post("/api/commandes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fournisseurId").value(fournisseur.getId()))
                .andExpect(jsonPath("$.lignesCommande.length()").value(2));

        assertThat(commandeFournisseurRepository.count()).isEqualTo(1);
    }

    @Test
    void getCommandes_shouldReturnPage() throws Exception {
        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setDateCommande(LocalDate.now());
        commande.setFournisseur(fournisseur);
        commandeFournisseurRepository.save(commande);

        mockMvc.perform(get("/api/commandes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getCommandeById_whenExists_shouldReturnDto() throws Exception {
        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setDateCommande(LocalDate.now());
        commande.setFournisseur(fournisseur);
        commande = commandeFournisseurRepository.save(commande);

        mockMvc.perform(get("/api/commandes/{id}", commande.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commande.getId()));
    }

    @Test
    void getCommandeById_whenMissing_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/commandes/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Commande fournisseur introuvable avec l'id 999"));
    }

    @Test
    void updateStatut_shouldUpdateStatus() throws Exception {
        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setDateCommande(LocalDate.now());
        commande.setFournisseur(fournisseur);
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande = commandeFournisseurRepository.save(commande);

        mockMvc.perform(patch("/api/commandes/{id}/statut", commande.getId())
                        .param("statut", "VALIDEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDEE"));

        CommandeFournisseur reloaded = commandeFournisseurRepository.findById(commande.getId()).orElseThrow();
        assertThat(reloaded.getStatut()).isEqualTo(StatutCommande.VALIDEE);
    }

    @Test
    void deleteCommande_shouldDelete() throws Exception {
        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setDateCommande(LocalDate.now());
        commande.setFournisseur(fournisseur);
        commande = commandeFournisseurRepository.save(commande);

        mockMvc.perform(delete("/api/commandes/{id}", commande.getId()))
                .andExpect(status().isNoContent());

        assertThat(commandeFournisseurRepository.existsById(commande.getId())).isFalse();
    }

    @Test
    void getCommandesParStatut_shouldFilter() throws Exception {
        CommandeFournisseur c1 = new CommandeFournisseur();
        c1.setDateCommande(LocalDate.now());
        c1.setFournisseur(fournisseur);
        c1.setStatut(StatutCommande.EN_ATTENTE);
        commandeFournisseurRepository.save(c1);

        CommandeFournisseur c2 = new CommandeFournisseur();
        c2.setDateCommande(LocalDate.now());
        c2.setFournisseur(fournisseur);
        c2.setStatut(StatutCommande.VALIDEE);
        commandeFournisseurRepository.save(c2);

        mockMvc.perform(get("/api/commandes/statut/{statut}", "VALIDEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCommandesParFournisseur_shouldFilter() throws Exception {
        CommandeFournisseur c1 = new CommandeFournisseur();
        c1.setDateCommande(LocalDate.now());
        c1.setFournisseur(fournisseur);
        commandeFournisseurRepository.save(c1);

        mockMvc.perform(get("/api/commandes/fournisseur/{fournisseurId}", fournisseur.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCommandesParPeriode_shouldFilterByDates() throws Exception {
        CommandeFournisseur c1 = new CommandeFournisseur();
        c1.setDateCommande(LocalDate.now().minusDays(5));
        c1.setFournisseur(fournisseur);
        commandeFournisseurRepository.save(c1);

        mockMvc.perform(get("/api/commandes/periode")
                        .param("debut", LocalDate.now().minusDays(10).toString())
                        .param("fin", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}