package com.example.gestion_approvisionnements.dto;

import com.example.gestion_approvisionnements.enums.TypeMouvement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementStockDTO {

    private Long id;
    private LocalDateTime dateMouvement;
    private Integer quantite;
    private TypeMouvement typeMouvement;
    private BigDecimal prixUnitaire;
    private Integer stockApresMouvement;
    private Long produitId;
    private String produitNom;
    private Long commandeFournisseurId;
}