package com.example.gestion_approvisionnements.mapper;

import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.entity.MouvementStock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MouvementStockMapper {

    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.nom", target = "produitNom")
    @Mapping(source = "commandeFournisseur.id", target = "commandeFournisseurId")
    MouvementStockDTO toDTO(MouvementStock mouvement);

    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "commandeFournisseur", ignore = true)
    MouvementStock toEntity(MouvementStockDTO dto);

    List<MouvementStockDTO> toDTOList(List<MouvementStock> mouvements);
}