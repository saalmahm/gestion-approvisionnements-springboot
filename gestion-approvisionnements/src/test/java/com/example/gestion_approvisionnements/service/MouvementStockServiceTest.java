package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.entity.CommandeFournisseur;
import com.example.gestion_approvisionnements.entity.MouvementStock;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.enums.TypeMouvement;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.MouvementStockMapper;
import com.example.gestion_approvisionnements.repository.CommandeFournisseurRepository;
import com.example.gestion_approvisionnements.repository.MouvementStockRepository;
import com.example.gestion_approvisionnements.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MouvementStockServiceTest {

    @Mock
    private MouvementStockRepository mouvementStockRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private CommandeFournisseurRepository commandeFournisseurRepository;
    @Mock
    private MouvementStockMapper mouvementStockMapper;

    @InjectMocks
    private MouvementStockService mouvementStockService;

    private Produit produit;
    private MouvementStockDTO entreeDto;

    @BeforeEach
    void setUp() {
        produit = buildProduit(1L, 100, BigDecimal.valueOf(2.00));
        entreeDto = buildMouvementDTO(TypeMouvement.ENTREE, 50, BigDecimal.valueOf(3.00), null);
    }

    @Test
    void getMouvementsParProduit_shouldReturnMapperResult() {
        // Arrange
        List<MouvementStock> entities = List.of(buildMouvement(TypeMouvement.ENTREE, 10, BigDecimal.ONE));
        List<MouvementStockDTO> expected = List.of(buildResultDto(5L, 110));
        when(mouvementStockRepository.findByProduitIdOrderByDateMouvementAsc(1L)).thenReturn(entities);
        when(mouvementStockMapper.toDTOList(entities)).thenReturn(expected);

        // Act
        List<MouvementStockDTO> result = mouvementStockService.getMouvementsParProduit(1L);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(mouvementStockRepository).findByProduitIdOrderByDateMouvementAsc(1L);
        verify(mouvementStockMapper).toDTOList(entities);
        verifyNoMoreInteractions(mouvementStockRepository, mouvementStockMapper);
        verifyNoInteractions(produitRepository, commandeFournisseurRepository);
    }

    @Test
    void getMouvementsParType_shouldReturnMapperResult() {
        // Arrange
        List<MouvementStock> entities = List.of(buildMouvement(TypeMouvement.SORTIE, 5, BigDecimal.ONE));
        List<MouvementStockDTO> expected = List.of(buildResultDto(6L, 95));
        when(mouvementStockRepository.findByTypeMouvement(TypeMouvement.SORTIE)).thenReturn(entities);
        when(mouvementStockMapper.toDTOList(entities)).thenReturn(expected);

        // Act
        List<MouvementStockDTO> result = mouvementStockService.getMouvementsParType(TypeMouvement.SORTIE);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(mouvementStockRepository).findByTypeMouvement(TypeMouvement.SORTIE);
        verify(mouvementStockMapper).toDTOList(entities);
        verifyNoMoreInteractions(mouvementStockRepository, mouvementStockMapper);
    }

    @Test
    void enregistrerMouvement_entreeAvecPrix_shouldUpdateStockAndCump() {
        // Arrange
        MouvementStockDTO dto = entreeDto;
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(mouvementStockMapper.toEntity(dto)).thenAnswer(inv -> {
            MouvementStock mouvement = new MouvementStock();
            mouvement.setQuantite(dto.getQuantite());
            mouvement.setTypeMouvement(dto.getTypeMouvement());
            mouvement.setPrixUnitaire(dto.getPrixUnitaire());
            return mouvement;
        });
        when(produitRepository.save(produit)).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(mouvementStockMapper.toDTO(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock saved = inv.getArgument(0);
            MouvementStockDTO result = new MouvementStockDTO();
            result.setId(saved.getId());
            result.setStockApresMouvement(saved.getStockApresMouvement());
            return result;
        });

        // Act
        MouvementStockDTO result = mouvementStockService.enregistrerMouvement(dto);

        // Assert
        assertThat(produit.getStockActuel()).isEqualTo(150);
        assertThat(produit.getCoutMoyenPondere()).isEqualByComparingTo("2.33");
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStockApresMouvement()).isEqualTo(150);
        verify(produitRepository).findById(1L);
        verify(mouvementStockMapper).toEntity(dto);
        verify(commandeFournisseurRepository, never()).findById(anyLong());
        verify(produitRepository).save(produit);
        verify(mouvementStockRepository).save(any(MouvementStock.class));
        verify(mouvementStockMapper).toDTO(any(MouvementStock.class));
        verifyNoMoreInteractions(produitRepository, mouvementStockRepository, mouvementStockMapper);
    }

    @Test
    void enregistrerMouvement_entreeSansPrix_shouldUtiliserPrixProduit() {
        // Arrange
        MouvementStockDTO dto = buildMouvementDTO(TypeMouvement.ENTREE, 10, null, null);
        produit.setStockActuel(0);
        produit.setCoutMoyenPondere(BigDecimal.valueOf(4.00));
        produit.setPrixUnitaire(BigDecimal.valueOf(5.00));

        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(mouvementStockMapper.toEntity(dto)).thenAnswer(inv -> {
            MouvementStock mouvement = new MouvementStock();
            mouvement.setQuantite(dto.getQuantite());
            mouvement.setTypeMouvement(dto.getTypeMouvement());
            mouvement.setPrixUnitaire(dto.getPrixUnitaire());
            return mouvement;
        });
        when(produitRepository.save(produit)).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        when(mouvementStockMapper.toDTO(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock saved = inv.getArgument(0);
            MouvementStockDTO result = new MouvementStockDTO();
            result.setId(saved.getId());
            result.setStockApresMouvement(saved.getStockApresMouvement());
            return result;
        });

        // Act
        MouvementStockDTO result = mouvementStockService.enregistrerMouvement(dto);

        // Assert
        assertThat(produit.getStockActuel()).isEqualTo(10);
        assertThat(produit.getCoutMoyenPondere()).isEqualByComparingTo("5.00");
        assertThat(result.getStockApresMouvement()).isEqualTo(10);
        verify(produitRepository).findById(1L);
        verify(mouvementStockMapper).toEntity(dto);
        verify(produitRepository).save(produit);
        verify(mouvementStockRepository).save(any(MouvementStock.class));
        verify(mouvementStockMapper).toDTO(any(MouvementStock.class));
    }

    @Test
    void enregistrerMouvement_sortieMenantStockNegatif_shouldThrowBusinessException() {
        // Arrange
        MouvementStockDTO dto = buildMouvementDTO(TypeMouvement.SORTIE, 200, null, null);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(mouvementStockMapper.toEntity(dto)).thenAnswer(inv -> {
            MouvementStock mouvement = new MouvementStock();
            mouvement.setQuantite(dto.getQuantite());
            mouvement.setTypeMouvement(dto.getTypeMouvement());
            mouvement.setPrixUnitaire(dto.getPrixUnitaire());
            return mouvement;
        });

        // Act & Assert
        assertThatThrownBy(() -> mouvementStockService.enregistrerMouvement(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ne peut pas devenir négatif");
        verify(produitRepository).findById(1L);
        verify(mouvementStockMapper).toEntity(dto);
        verifyNoMoreInteractions(produitRepository, mouvementStockMapper);
        verifyNoInteractions(mouvementStockRepository, commandeFournisseurRepository);
    }

    @Test
    void enregistrerMouvement_ajustementDevraitFixerStockSansChangerCump() {
        // Arrange
        MouvementStockDTO dto = buildMouvementDTO(TypeMouvement.AJUSTEMENT, 42, null, null);
        BigDecimal cumpInitial = produit.getCoutMoyenPondere();
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(mouvementStockMapper.toEntity(dto)).thenAnswer(inv -> {
            MouvementStock mouvement = new MouvementStock();
            mouvement.setQuantite(dto.getQuantite());
            mouvement.setTypeMouvement(dto.getTypeMouvement());
            return mouvement;
        });
        when(produitRepository.save(produit)).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock saved = inv.getArgument(0);
            saved.setId(12L);
            return saved;
        });
        when(mouvementStockMapper.toDTO(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock saved = inv.getArgument(0);
            MouvementStockDTO result = new MouvementStockDTO();
            result.setId(saved.getId());
            result.setStockApresMouvement(saved.getStockApresMouvement());
            return result;
        });

        // Act
        MouvementStockDTO result = mouvementStockService.enregistrerMouvement(dto);

        // Assert
        assertThat(produit.getStockActuel()).isEqualTo(42);
        assertThat(produit.getCoutMoyenPondere()).isEqualByComparingTo(cumpInitial);
        assertThat(result.getStockApresMouvement()).isEqualTo(42);
        verify(produitRepository).findById(1L);
        verify(mouvementStockMapper).toEntity(dto);
        verify(produitRepository).save(produit);
        verify(mouvementStockRepository).save(any(MouvementStock.class));
        verify(mouvementStockMapper).toDTO(any(MouvementStock.class));
    }

    @Test
    void enregistrerMouvement_quandProduitInexistant_shouldThrowResourceNotFound() {
        // Arrange
        when(produitRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> mouvementStockService.enregistrerMouvement(entreeDto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(produitRepository).findById(1L);
        verifyNoMoreInteractions(produitRepository);
        verifyNoInteractions(mouvementStockMapper, mouvementStockRepository, commandeFournisseurRepository);
    }

    @Test
    void enregistrerMouvement_quandCommandeRenseigneeMaisInexistante_shouldThrowResourceNotFound() {
        // Arrange
        MouvementStockDTO dto = buildMouvementDTO(TypeMouvement.ENTREE, 5, BigDecimal.ONE, 9L);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeFournisseurRepository.findById(9L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> mouvementStockService.enregistrerMouvement(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Commande fournisseur introuvable");

        verify(produitRepository).findById(1L);
        verify(commandeFournisseurRepository).findById(9L);
        verifyNoInteractions(mouvementStockMapper);
        verifyNoMoreInteractions(produitRepository, commandeFournisseurRepository);
        verifyNoInteractions(mouvementStockRepository);
    }
    private Produit buildProduit(Long id, int stock, BigDecimal cump) {
        Produit p = new Produit();
        p.setId(id);
        p.setStockActuel(stock);
        p.setCoutMoyenPondere(cump);
        p.setPrixUnitaire(cump);
        return p;
    }

    private MouvementStockDTO buildMouvementDTO(TypeMouvement type, int quantite, BigDecimal prix, Long commandeId) {
        MouvementStockDTO dto = new MouvementStockDTO();
        dto.setProduitId(1L);
        dto.setQuantite(quantite);
        dto.setTypeMouvement(type);
        dto.setPrixUnitaire(prix);
        dto.setCommandeFournisseurId(commandeId);
        dto.setDateMouvement(LocalDateTime.now());
        return dto;
    }

    private MouvementStock buildMouvement(TypeMouvement type, int quantite, BigDecimal prix) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setTypeMouvement(type);
        mouvement.setQuantite(quantite);
        mouvement.setPrixUnitaire(prix);
        return mouvement;
    }

    private MouvementStockDTO buildResultDto(Long id, int stock) {
        MouvementStockDTO dto = new MouvementStockDTO();
        dto.setId(id);
        dto.setStockApresMouvement(stock);
        return dto;
    }
}