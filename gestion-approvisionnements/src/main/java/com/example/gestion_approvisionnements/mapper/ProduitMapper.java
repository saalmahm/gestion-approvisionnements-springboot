package com.example.gestion_approvisionnements.mapper;

import com.example.gestion_approvisionnements.dto.ProduitDTO;
import com.example.gestion_approvisionnements.entity.Produit;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProduitMapper {

    ProduitDTO toDTO(Produit produit);

    Produit toEntity(ProduitDTO dto);

    List<ProduitDTO> toDTOList(List<Produit> produits);

    void updateEntityFromDTO(ProduitDTO dto, @MappingTarget Produit produit);
}