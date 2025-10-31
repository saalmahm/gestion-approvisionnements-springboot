package com.example.gestion_approvisionnements.mapper;

import com.example.gestion_approvisionnements.dto.FournisseurDTO;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FournisseurMapper {

    FournisseurDTO toDTO(Fournisseur fournisseur);

    Fournisseur toEntity(FournisseurDTO dto);

    List<FournisseurDTO> toDTOList(List<Fournisseur> fournisseurs);

    void updateEntityFromDTO(FournisseurDTO dto, @MappingTarget Fournisseur fournisseur);
}