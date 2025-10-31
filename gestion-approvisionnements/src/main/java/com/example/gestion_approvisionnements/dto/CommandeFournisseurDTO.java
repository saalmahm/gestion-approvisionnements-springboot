package com.example.gestion_approvisionnements.dto;

import com.example.gestion_approvisionnements.enums.StatutCommande;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeFournisseurDTO {

    private Long id;

    @NotNull(message = "La date de commande est obligatoire")
    private LocalDate dateCommande;

    private StatutCommande statut;

    private BigDecimal montantTotal;

    @NotNull(message = "Le fournisseur est obligatoire")
    private Long fournisseurId;

    private String fournisseurSociete;

    @NotEmpty(message = "La commande doit contenir au moins une ligne")
    private List<LigneCommandeDTO> lignesCommande;
}