package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.CommandeFournisseurDTO;
import com.example.gestion_approvisionnements.dto.LigneCommandeDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import com.example.gestion_approvisionnements.entity.LigneCommande;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.enums.StatutCommande;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.CommandeFournisseurMapper;
import com.example.gestion_approvisionnements.mapper.LigneCommandeMapper;
import com.example.gestion_approvisionnements.repository.CommandeFournisseurRepository;
import com.example.gestion_approvisionnements.repository.FournisseurRepository;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandeFournisseurServiceTest {

    @Mock
    private CommandeFournisseurRepository commandeFournisseurRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private CommandeFournisseurMapper commandeFournisseurMapper;
    @Mock
    private LigneCommandeMapper ligneCommandeMapper;

    @InjectMocks
    private CommandeFournisseurService commandeFournisseurService;

    private CommandeFournisseurDTO baseCommandeDTO;
    private Fournisseur fournisseur;
    private Produit produit1, produit2;

    @BeforeEach
    void setUp() {
        // Setup Fournisseur
        fournisseur = new Fournisseur();
        fournisseur.setId(1L);
        fournisseur.setSociete("Fournisseur Test");

        // Setup Produits
        produit1 = new Produit();
        produit1.setId(1L);
        produit1.setNom("Produit 1");
        produit1.setPrixUnitaire(BigDecimal.valueOf(10.0));

        produit2 = new Produit();
        produit2.setId(2L);
        produit2.setNom("Produit 2");
        produit2.setPrixUnitaire(BigDecimal.valueOf(15.0));

        // Setup Lignes Commande DTO
        LigneCommandeDTO ligne1 = new LigneCommandeDTO(null, 1L, "Produit 1", 2, BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0));
        LigneCommandeDTO ligne2 = new LigneCommandeDTO(null, 2L, "Produit 2", 3, BigDecimal.valueOf(15.0), BigDecimal.valueOf(45.0));

        // Setup Commande DTO
        baseCommandeDTO = new CommandeFournisseurDTO(
                null,
                LocalDate.now(),
                StatutCommande.EN_ATTENTE,
                BigDecimal.valueOf(65.0),
                1L,
                "Fournisseur Test",
                List.of(ligne1, ligne2)
        );
    }

    @Test
    void getAllCommandes_whenPageableProvided_shouldReturnMappedDtos() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        CommandeFournisseur entity = buildCommandeEntity(1L);
        Page<CommandeFournisseur> page = new PageImpl<>(List.of(entity));
        CommandeFournisseurDTO expectedDto = buildCommandeDTO(1L);

        when(commandeFournisseurRepository.findAll(pageable)).thenReturn(page);
        when(commandeFournisseurMapper.toDTO(entity)).thenReturn(expectedDto);

        // Act
        Page<CommandeFournisseurDTO> result = commandeFournisseurService.getAllCommandes(pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(expectedDto);
        verify(commandeFournisseurRepository).findAll(pageable);
        verify(commandeFournisseurMapper).toDTO(entity);
        verifyNoMoreInteractions(commandeFournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void getCommandeById_whenFound_shouldReturnDto() {
        // Arrange
        CommandeFournisseur entity = buildCommandeEntity(5L);
        CommandeFournisseurDTO expectedDto = buildCommandeDTO(5L);

        when(commandeFournisseurRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(commandeFournisseurMapper.toDTO(entity)).thenReturn(expectedDto);

        // Act
        CommandeFournisseurDTO result = commandeFournisseurService.getCommandeById(5L);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        verify(commandeFournisseurRepository).findById(5L);
        verify(commandeFournisseurMapper).toDTO(entity);
        verifyNoMoreInteractions(commandeFournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void getCommandeById_whenMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(commandeFournisseurRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> commandeFournisseurService.getCommandeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(commandeFournisseurRepository).findById(99L);
        verifyNoMoreInteractions(commandeFournisseurRepository);
        verifyNoInteractions(commandeFournisseurMapper);
    }

    @Test
    void createCommande_whenValidData_shouldPersistAndReturnDto() {
        // Arrange
        CommandeFournisseur entity = new CommandeFournisseur();
        CommandeFournisseur savedEntity = buildCommandeEntity(10L);
        CommandeFournisseurDTO expectedDto = buildCommandeDTO(10L);

        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));
        when(commandeFournisseurMapper.toEntity(baseCommandeDTO)).thenReturn(entity);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit1));
        when(produitRepository.findById(2L)).thenReturn(Optional.of(produit2));
        when(ligneCommandeMapper.toEntity(any(LigneCommandeDTO.class))).thenAnswer(inv -> {
            LigneCommandeDTO dto = inv.getArgument(0);
            LigneCommande ligne = new LigneCommande();
            ligne.setQuantite(dto.getQuantite());
            ligne.setPrixUnitaire(dto.getPrixUnitaire());
            return ligne;
        });
        when(commandeFournisseurRepository.save(entity)).thenReturn(savedEntity);
        when(commandeFournisseurMapper.toDTO(savedEntity)).thenReturn(expectedDto);

        // Act
        CommandeFournisseurDTO result = commandeFournisseurService.createCommande(baseCommandeDTO);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        verify(fournisseurRepository).findById(1L);
        verify(commandeFournisseurMapper).toEntity(baseCommandeDTO);
        verify(produitRepository).findById(1L);
        verify(produitRepository).findById(2L);
        verify(ligneCommandeMapper, times(2)).toEntity(any(LigneCommandeDTO.class));
        verify(commandeFournisseurRepository).save(entity);
        verify(commandeFournisseurMapper).toDTO(savedEntity);
        verifyNoMoreInteractions(fournisseurRepository, commandeFournisseurMapper, produitRepository,
                ligneCommandeMapper, commandeFournisseurRepository);
    }

    @Test
    void createCommande_whenFournisseurMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(fournisseurRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> commandeFournisseurService.createCommande(baseCommandeDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fournisseur");
        verify(fournisseurRepository).findById(1L);
        verifyNoMoreInteractions(fournisseurRepository);
        verifyNoInteractions(commandeFournisseurMapper, produitRepository, ligneCommandeMapper, commandeFournisseurRepository);
    }

    @Test
    void createCommande_whenLignesNull_shouldThrowBusinessException() {
        // Arrange
        CommandeFournisseurDTO dtoSansLignes = new CommandeFournisseurDTO(
                null, LocalDate.now(), StatutCommande.EN_ATTENTE, BigDecimal.ZERO, 1L, "Fournisseur", null
        );

        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));
        when(commandeFournisseurMapper.toEntity(dtoSansLignes)).thenReturn(new CommandeFournisseur());

        // Act & Assert
        assertThatThrownBy(() -> commandeFournisseurService.createCommande(dtoSansLignes))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("au moins une ligne");
        verify(fournisseurRepository).findById(1L);
        verify(commandeFournisseurMapper).toEntity(dtoSansLignes);
        verifyNoMoreInteractions(fournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void createCommande_whenLignesVides_shouldThrowBusinessException() {
        // Arrange
        CommandeFournisseurDTO dtoLignesVides = new CommandeFournisseurDTO(
                null, LocalDate.now(), StatutCommande.EN_ATTENTE, BigDecimal.ZERO, 1L, "Fournisseur", List.of()
        );

        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));
        when(commandeFournisseurMapper.toEntity(dtoLignesVides)).thenReturn(new CommandeFournisseur());

        // Act & Assert
        assertThatThrownBy(() -> commandeFournisseurService.createCommande(dtoLignesVides))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("au moins une ligne");
        verify(fournisseurRepository).findById(1L);
        verify(commandeFournisseurMapper).toEntity(dtoLignesVides);
        verifyNoMoreInteractions(fournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void createCommande_whenProduitMissing_shouldThrowResourceNotFound() {
        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));
        when(commandeFournisseurMapper.toEntity(baseCommandeDTO)).thenReturn(new CommandeFournisseur());

        when(produitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandeFournisseurService.createCommande(baseCommandeDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Produit");

        verify(fournisseurRepository).findById(1L);
        verify(commandeFournisseurMapper).toEntity(baseCommandeDTO);
        verify(produitRepository).findById(1L);
        verifyNoMoreInteractions(fournisseurRepository, commandeFournisseurMapper, produitRepository);
        verifyNoInteractions(ligneCommandeMapper);
    }

    @Test
    void updateStatut_whenCommandeExists_shouldUpdateAndReturnDto() {
        // Arrange
        CommandeFournisseur commande = buildCommandeEntity(7L);
        CommandeFournisseurDTO expectedDto = buildCommandeDTO(7L);
        expectedDto.setStatut(StatutCommande.VALIDEE);

        when(commandeFournisseurRepository.findById(7L)).thenReturn(Optional.of(commande));
        when(commandeFournisseurRepository.save(commande)).thenReturn(commande);
        when(commandeFournisseurMapper.toDTO(commande)).thenReturn(expectedDto);

        // Act
        CommandeFournisseurDTO result = commandeFournisseurService.updateStatut(7L, StatutCommande.VALIDEE);

        // Assert
        assertThat(result.getStatut()).isEqualTo(StatutCommande.VALIDEE);
        verify(commandeFournisseurRepository).findById(7L);
        verify(commandeFournisseurRepository).save(commande);
        verify(commandeFournisseurMapper).toDTO(commande);
        verifyNoMoreInteractions(commandeFournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void updateStatut_whenCommandeMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(commandeFournisseurRepository.findById(50L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> commandeFournisseurService.updateStatut(50L, StatutCommande.LIVREE))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(commandeFournisseurRepository).findById(50L);
        verifyNoMoreInteractions(commandeFournisseurRepository);
        verifyNoInteractions(commandeFournisseurMapper);
    }

    @Test
    void deleteCommande_whenExists_shouldDelete() {
        // Arrange
        when(commandeFournisseurRepository.existsById(3L)).thenReturn(true);

        // Act
        commandeFournisseurService.deleteCommande(3L);

        // Assert
        verify(commandeFournisseurRepository).existsById(3L);
        verify(commandeFournisseurRepository).deleteById(3L);
        verifyNoMoreInteractions(commandeFournisseurRepository);
    }

    @Test
    void deleteCommande_whenMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(commandeFournisseurRepository.existsById(3L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> commandeFournisseurService.deleteCommande(3L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(commandeFournisseurRepository).existsById(3L);
        verifyNoMoreInteractions(commandeFournisseurRepository);
    }

    @Test
    void getCommandesParStatut_shouldReturnMappedDtos() {
        // Arrange
        List<CommandeFournisseur> entities = List.of(buildCommandeEntity(1L), buildCommandeEntity(2L));
        List<CommandeFournisseurDTO> expectedDtos = List.of(buildCommandeDTO(1L), buildCommandeDTO(2L));

        when(commandeFournisseurRepository.findByStatut(StatutCommande.EN_ATTENTE)).thenReturn(entities);
        when(commandeFournisseurMapper.toDTOList(entities)).thenReturn(expectedDtos);

        // Act
        List<CommandeFournisseurDTO> result = commandeFournisseurService.getCommandesParStatut(StatutCommande.EN_ATTENTE);

        // Assert
        assertThat(result).isEqualTo(expectedDtos);
        verify(commandeFournisseurRepository).findByStatut(StatutCommande.EN_ATTENTE);
        verify(commandeFournisseurMapper).toDTOList(entities);
        verifyNoMoreInteractions(commandeFournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void getCommandesParFournisseur_shouldReturnMappedDtos() {
        // Arrange
        List<CommandeFournisseur> entities = List.of(buildCommandeEntity(1L));
        List<CommandeFournisseurDTO> expectedDtos = List.of(buildCommandeDTO(1L));

        when(commandeFournisseurRepository.findByFournisseurId(1L)).thenReturn(entities);
        when(commandeFournisseurMapper.toDTOList(entities)).thenReturn(expectedDtos);

        // Act
        List<CommandeFournisseurDTO> result = commandeFournisseurService.getCommandesParFournisseur(1L);

        // Assert
        assertThat(result).isEqualTo(expectedDtos);
        verify(commandeFournisseurRepository).findByFournisseurId(1L);
        verify(commandeFournisseurMapper).toDTOList(entities);
        verifyNoMoreInteractions(commandeFournisseurRepository, commandeFournisseurMapper);
    }

    @Test
    void getCommandesParPeriode_shouldReturnMappedDtos() {
        // Arrange
        LocalDate debut = LocalDate.now().minusDays(30);
        LocalDate fin = LocalDate.now();
        List<CommandeFournisseur> entities = List.of(buildCommandeEntity(1L));
        List<CommandeFournisseurDTO> expectedDtos = List.of(buildCommandeDTO(1L));

        when(commandeFournisseurRepository.findByDateCommandeBetween(debut, fin)).thenReturn(entities);
        when(commandeFournisseurMapper.toDTOList(entities)).thenReturn(expectedDtos);

        // Act
        List<CommandeFournisseurDTO> result = commandeFournisseurService.getCommandesParPeriode(debut, fin);

        // Assert
        assertThat(result).isEqualTo(expectedDtos);
        verify(commandeFournisseurRepository).findByDateCommandeBetween(debut, fin);
        verify(commandeFournisseurMapper).toDTOList(entities);
        verifyNoMoreInteractions(commandeFournisseurRepository, commandeFournisseurMapper);
    }

    private CommandeFournisseur buildCommandeEntity(Long id) {
        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setId(id);
        commande.setDateCommande(LocalDate.now());
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setMontantTotal(BigDecimal.valueOf(65.0));
        commande.setFournisseur(fournisseur);
        return commande;
    }

    private CommandeFournisseurDTO buildCommandeDTO(Long id) {
        return new CommandeFournisseurDTO(
                id,
                LocalDate.now(),
                StatutCommande.EN_ATTENTE,
                BigDecimal.valueOf(65.0),
                1L,
                "Fournisseur Test",
                List.of()
        );
    }
}