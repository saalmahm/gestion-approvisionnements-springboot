package com.example.gestion_approvisionnements.mapper;

import com.example.gestion_approvisionnements.dto.CommandeFournisseurDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {LigneCommandeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommandeFournisseurMapper {

    @Mapping(source = "fournisseur.id", target = "fournisseurId")
    @Mapping(source = "fournisseur.societe", target = "fournisseurSociete")
    CommandeFournisseurDTO toDTO(CommandeFournisseur commande);

    @Mapping(target = "fournisseur", ignore = true)
    @Mapping(target = "mouvements", ignore = true)
    CommandeFournisseur toEntity(CommandeFournisseurDTO dto);

    List<CommandeFournisseurDTO> toDTOList(List<CommandeFournisseur> commandes);
}