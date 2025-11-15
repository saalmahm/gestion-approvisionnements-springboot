package com.example.gestion_approvisionnements.integration;

import com.example.gestion_approvisionnements.dto.ProduitDTO;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProduitControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        produitRepository.deleteAll();
    }

    @Test
    void createProduit_shouldPersistAndReturnCreated() throws Exception {
        ProduitDTO dto = new ProduitDTO(null, "Clavier mécanique", "Switch bleu",
                BigDecimal.valueOf(129.99), "Informatique", 25, BigDecimal.ZERO);

        mockMvc.perform(post("/api/produits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nom").value("Clavier mécanique"));

        assertThat(produitRepository.count()).isEqualTo(1);
        Produit saved = produitRepository.findAll().get(0);
        assertThat(saved.getNom()).isEqualTo("Clavier mécanique");
    }

    @Test
    void getProduits_shouldReturnPageContent() throws Exception {
        produitRepository.save(buildProduit("Laptop", "High end", BigDecimal.valueOf(1999.0)));
        produitRepository.save(buildProduit("Souris", "Ergonomique", BigDecimal.valueOf(59.0)));

        mockMvc.perform(get("/api/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getProduitById_whenExists_shouldReturnDto() throws Exception {
        Produit saved = produitRepository.save(buildProduit("Écran", "4K", BigDecimal.valueOf(499.0)));

        mockMvc.perform(get("/api/produits/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Écran"));
    }

    @Test
    void getProduitById_whenMissing_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/produits/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produit introuvable avec l'id 999"));
    }

    @Test
    void createProduit_whenInvalidPayload_shouldReturn400WithErrors() throws Exception {
        ProduitDTO invalid = new ProduitDTO(null, "", "Pas de nom",
                BigDecimal.ZERO, null, -5, BigDecimal.ZERO);

        mockMvc.perform(post("/api/produits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

        @Test
    void updateProduit_shouldUpdateFields() throws Exception {
        Produit saved = produitRepository.save(buildProduit("Ancien nom", "Desc", BigDecimal.valueOf(10)));

        ProduitDTO updateDto = new ProduitDTO(
                saved.getId(),
                "Nouveau nom",
                "Nouvelle description",
                BigDecimal.valueOf(20),
                "NouvelleCat",
                50,
                BigDecimal.valueOf(15)
        );

        mockMvc.perform(put("/api/produits/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Nouveau nom"))
                .andExpect(jsonPath("$.prixUnitaire").value(20.00));

        Produit reloaded = produitRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getNom()).isEqualTo("Nouveau nom");
        assertThat(reloaded.getPrixUnitaire()).isEqualByComparingTo("20.00");
        assertThat(reloaded.getStockActuel()).isEqualTo(50);
    }

    @Test
    void ajusterStock_shouldChangeStock() throws Exception {
        Produit saved = produitRepository.save(buildProduit("Prod stock", "Desc", BigDecimal.valueOf(10)));

        mockMvc.perform(patch("/api/produits/{id}/stock", saved.getId())
                        .param("variation", "5"))
                .andExpect(status().isOk());

        Produit reloaded = produitRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStockActuel()).isEqualTo(15); // 10 + 5
    }

    @Test
    void mettreAJourCump_shouldUpdateCump() throws Exception {
        Produit saved = produitRepository.save(buildProduit("Prod cump", "Desc", BigDecimal.valueOf(10)));

        mockMvc.perform(patch("/api/produits/{id}/cump", saved.getId())
                        .param("valeur", "42.50"))
                .andExpect(status().isOk());

        Produit reloaded = produitRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCoutMoyenPondere()).isEqualByComparingTo("42.50");
    }

    @Test
    void getProduitsParCategorie_shouldFilterByCategorie() throws Exception {
        produitRepository.save(buildProduit("Prod A", "Desc", BigDecimal.valueOf(10)));
        Produit autreCategorie = buildProduit("Prod B", "Desc", BigDecimal.valueOf(20));
        autreCategorie.setCategorie("AUTRE");
        produitRepository.save(autreCategorie);

        mockMvc.perform(get("/api/produits/categorie/{categorie}", "TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("Prod A"));
    }

    @Test
    void getProduitsStockFaible_shouldReturnUnderThreshold() throws Exception {
        Produit lowStock = buildProduit("Low", "Desc", BigDecimal.valueOf(10));
        lowStock.setStockActuel(3);
        produitRepository.save(lowStock);

        Produit highStock = buildProduit("High", "Desc", BigDecimal.valueOf(10));
        highStock.setStockActuel(20);
        produitRepository.save(highStock);

        mockMvc.perform(get("/api/produits/stock-faible").param("seuil", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("Low"));
    }

    @Test
    void deleteProduit_shouldRemoveEntity() throws Exception {
        Produit saved = produitRepository.save(buildProduit("Casque", "Sans fil", BigDecimal.valueOf(149.0)));

        mockMvc.perform(delete("/api/produits/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(produitRepository.existsById(saved.getId())).isFalse();
    }

    private Produit buildProduit(String nom, String description, BigDecimal prix) {
        Produit p = new Produit();
        p.setNom(nom);
        p.setDescription(description);
        p.setPrixUnitaire(prix);
        p.setCategorie("TEST");
        p.setStockActuel(10);
        p.setCoutMoyenPondere(prix);
        return p;
    }
}
