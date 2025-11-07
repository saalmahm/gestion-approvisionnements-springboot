package com.example.gestion_approvisionnements.mapper;

import com.example.gestion_approvisionnements.dto.LigneCommandeDTO;
import com.example.gestion_approvisionnements.entity.LigneCommande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LigneCommandeMapper {

    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.nom", target = "produitNom")
    LigneCommandeDTO toDTO(LigneCommande ligneCommande);

    @Mapping(target = "commandeFournisseur", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneCommande toEntity(LigneCommandeDTO dto);

    List<LigneCommandeDTO> toDTOList(List<LigneCommande> lignes);

    List<LigneCommande> toEntityList(List<LigneCommandeDTO> lignesDTO);
}