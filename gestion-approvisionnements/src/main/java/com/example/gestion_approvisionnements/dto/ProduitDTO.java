package com.example.gestion_approvisionnements.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDTO {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    private String nom;

    private String description;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix unitaire doit être supérieur à 0")
    private BigDecimal prixUnitaire;

    @Size(max = 100, message = "La catégorie ne peut pas dépasser 100 caractères")
    private String categorie;

    @Min(value = 0, message = "Le stock actuel ne peut pas être négatif")
    private Integer stockActuel;

    private BigDecimal coutMoyenPondere;
}