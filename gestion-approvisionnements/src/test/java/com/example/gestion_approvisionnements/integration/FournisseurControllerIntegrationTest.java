package com.example.gestion_approvisionnements.integration;

import com.example.gestion_approvisionnements.dto.FournisseurDTO;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import com.example.gestion_approvisionnements.repository.FournisseurRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FournisseurControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fournisseurRepository.deleteAll();
    }

    private Fournisseur buildFournisseur(String societe, String ice) {
        Fournisseur f = new Fournisseur();
        f.setSociete(societe);
        f.setIce(ice);
        f.setAdresse("Adresse");
        f.setContact("Contact");
        f.setEmail("test@example.com");
        f.setTelephone("0600000000");
        f.setVille("Ville");
        return f;
    }

    private FournisseurDTO buildFournisseurDTO(String societe, String ice) {
        FournisseurDTO dto = new FournisseurDTO();
        dto.setSociete(societe);
        dto.setIce(ice);
        dto.setAdresse("Adresse");
        dto.setContact("Contact");
        dto.setEmail("test@example.com");
        dto.setTelephone("0600000000");
        dto.setVille("Ville");
        return dto;
    }

    @Test
    void createFournisseur_shouldPersistAndReturnCreated() throws Exception {
        FournisseurDTO dto = buildFournisseurDTO("Fournisseur A", "ICE123");

        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.societe").value("Fournisseur A"));

        assertThat(fournisseurRepository.count()).isEqualTo(1);
        Fournisseur saved = fournisseurRepository.findAll().get(0);
        assertThat(saved.getIce()).isEqualTo("ICE123");
    }

    @Test
    void getFournisseurs_shouldReturnPage() throws Exception {
        fournisseurRepository.save(buildFournisseur("F1", "ICE1"));
        fournisseurRepository.save(buildFournisseur("F2", "ICE2"));

        mockMvc.perform(get("/api/fournisseurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getFournisseurById_whenExists_shouldReturnDto() throws Exception {
        Fournisseur saved = fournisseurRepository.save(buildFournisseur("F1", "ICE1"));

        mockMvc.perform(get("/api/fournisseurs/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societe").value("F1"));
    }

    @Test
    void getFournisseurById_whenMissing_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/fournisseurs/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fournisseur introuvable avec l'id 999"));
    }

    @Test
    void getFournisseurByIce_whenExists_shouldReturnDto() throws Exception {
        Fournisseur saved = fournisseurRepository.save(buildFournisseur("F1", "ICE1"));

        mockMvc.perform(get("/api/fournisseurs/ice/{ice}", "ICE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societe").value("F1"));
    }

    @Test
    void getFournisseurByIce_whenMissing_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/fournisseurs/ice/{ice}", "ICE999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fournisseur introuvable avec l'ICE ICE999"));
    }

    @Test
    void updateFournisseur_shouldUpdateFields() throws Exception {
        Fournisseur saved = fournisseurRepository.save(buildFournisseur("Ancienne", "ICE1"));

        FournisseurDTO dto = buildFournisseurDTO("Nouvelle", "ICE1");
        dto.setId(saved.getId());

        mockMvc.perform(put("/api/fournisseurs/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societe").value("Nouvelle"));

        Fournisseur reloaded = fournisseurRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getSociete()).isEqualTo("Nouvelle");
    }

    @Test
    void deleteFournisseur_shouldDelete() throws Exception {
        Fournisseur saved = fournisseurRepository.save(buildFournisseur("A supprimer", "ICEDEL"));

        mockMvc.perform(delete("/api/fournisseurs/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(fournisseurRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    void createFournisseur_withDuplicateIce_shouldReturn409() throws Exception {
        fournisseurRepository.save(buildFournisseur("Existant", "ICE_DUP"));

        FournisseurDTO dto = buildFournisseurDTO("Nouveau", "ICE_DUP");

        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un fournisseur existe déjà avec l'ICE ICE_DUP"));
    }

    @Test
    void createFournisseur_whenInvalidPayload_shouldReturn400() throws Exception {
        FournisseurDTO invalid = new FournisseurDTO();
        invalid.setSociete(""); // @NotBlank violée

        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }
}