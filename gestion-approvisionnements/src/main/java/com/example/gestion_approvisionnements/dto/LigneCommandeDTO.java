package com.example.gestion_approvisionnements.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommandeDTO {

    private Long id;

    @NotNull(message = "Le produit est obligatoire")
    private Long produitId;

    private String produitNom;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    @NotNull(message = "Le prix unitaire est obligatoire")
    private BigDecimal prixUnitaire;

    private BigDecimal sousTotal;
}