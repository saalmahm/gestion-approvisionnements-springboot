package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.ProduitDTO;
import com.example.gestion_approvisionnements.entity.Produit;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.ProduitMapper;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private ProduitMapper produitMapper;

    @InjectMocks
    private ProduitService produitService;

    private ProduitDTO baseProduitDTO;

    @BeforeEach
    void setUp() {
        baseProduitDTO = new ProduitDTO(
                null,
                "Stylo",
                "Stylo bille bleu",
                BigDecimal.valueOf(4.50),
                "Papeterie",
                100,
                BigDecimal.ZERO
        );
    }

    @Test
    void getAllProduits_whenPageableProvided_shouldMapEntitiesToDtos() {
        // Arrange
        Produit produit1 = buildProduit(1L, 10, BigDecimal.valueOf(3.5));
        Produit produit2 = buildProduit(2L, 20, BigDecimal.valueOf(5.0));
        Page<Produit> page = new PageImpl<>(List.of(produit1, produit2));
        PageRequest pageable = PageRequest.of(0, 10);
        ProduitDTO dto1 = buildProduitDTO(1L);
        ProduitDTO dto2 = buildProduitDTO(2L);

        when(produitRepository.findAll(pageable)).thenReturn(page);
        when(produitMapper.toDTO(produit1)).thenReturn(dto1);
        when(produitMapper.toDTO(produit2)).thenReturn(dto2);

        // Act
        Page<ProduitDTO> result = produitService.getAllProduits(pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(dto1, dto2);
        verify(produitRepository).findAll(pageable);
        verify(produitMapper).toDTO(produit1);
        verify(produitMapper).toDTO(produit2);
        verifyNoMoreInteractions(produitRepository, produitMapper);
    }

    @Test
    void getProduitById_whenEntityExists_shouldReturnDto() {
        // Arrange
        Produit produit = buildProduit(5L, 50, BigDecimal.valueOf(8.5));
        ProduitDTO expected = buildProduitDTO(5L);
        when(produitRepository.findById(5L)).thenReturn(Optional.of(produit));
        when(produitMapper.toDTO(produit)).thenReturn(expected);

        // Act
        ProduitDTO result = produitService.getProduitById(5L);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(produitRepository).findById(5L);
        verify(produitMapper).toDTO(produit);
        verifyNoMoreInteractions(produitRepository, produitMapper);
    }

    @Test
    void getProduitById_whenEntityMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(produitRepository.findById(42L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produitService.getProduitById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
        verify(produitRepository).findById(42L);
        verifyNoMoreInteractions(produitRepository);
        verifyNoInteractions(produitMapper);
    }

    @Test
    void createProduit_whenValidDto_shouldPersistAndReturnDto() {
        // Arrange
        Produit entity = new Produit();
        Produit saved = new Produit();
        saved.setId(15L);
        ProduitDTO expected = buildProduitDTO(15L);

        when(produitMapper.toEntity(baseProduitDTO)).thenReturn(entity);
        when(produitRepository.save(entity)).thenReturn(saved);
        when(produitMapper.toDTO(saved)).thenReturn(expected);

        // Act
        ProduitDTO result = produitService.createProduit(baseProduitDTO);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(produitMapper).toEntity(baseProduitDTO);
        verify(produitRepository).save(entity);
        verify(produitMapper).toDTO(saved);
        verifyNoMoreInteractions(produitRepository, produitMapper);
    }

    @Test
    void createProduit_whenPrixUnitaireNegatif_shouldThrowBusinessException() {
        // Arrange
        ProduitDTO invalidDto = new ProduitDTO(null, "Nom", "Desc", BigDecimal.valueOf(-1), "Cat", 10, BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> produitService.createProduit(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prix unitaire");
        verifyNoInteractions(produitMapper, produitRepository);
    }

    @Test
    void createProduit_whenStockNegatif_shouldThrowBusinessException() {
        // Arrange
        ProduitDTO invalidDto = new ProduitDTO(null, "Nom", "Desc", BigDecimal.ONE, "Cat", -5, BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> produitService.createProduit(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("stock actuel");
        verifyNoInteractions(produitMapper, produitRepository);
    }

    @Test
    void createProduit_whenCoutMoyenNegatif_shouldThrowBusinessException() {
        // Arrange
        ProduitDTO invalidDto = new ProduitDTO(null, "Nom", "Desc", BigDecimal.ONE, "Cat", 1, BigDecimal.valueOf(-0.5));

        // Act & Assert
        assertThatThrownBy(() -> produitService.createProduit(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("coût moyen pondéré");
        verifyNoInteractions(produitMapper, produitRepository);
    }

    @Test
    void updateProduit_whenEntityExistsAndDtoValid_shouldUpdateAndReturnDto() {
        // Arrange
        Produit existing = buildProduit(7L, 40, BigDecimal.valueOf(12.5));
        ProduitDTO expected = buildProduitDTO(7L);

        when(produitRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(produitRepository.save(existing)).thenReturn(existing);
        when(produitMapper.toDTO(existing)).thenReturn(expected);

        // Act
        ProduitDTO result = produitService.updateProduit(7L, baseProduitDTO);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(produitRepository).findById(7L);
        verify(produitMapper).updateEntityFromDTO(baseProduitDTO, existing);
        verify(produitRepository).save(existing);
        verify(produitMapper).toDTO(existing);
        verifyNoMoreInteractions(produitRepository, produitMapper);
    }

    @Test
    void updateProduit_whenEntityMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produitService.updateProduit(99L, baseProduitDTO))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(produitRepository).findById(99L);
        verifyNoMoreInteractions(produitRepository);
        verifyNoInteractions(produitMapper);
    }

    @Test
    void updateProduit_whenDtoFailsValidation_shouldThrowBusinessException() {
        // Arrange
        Produit existing = buildProduit(3L, 30, BigDecimal.valueOf(9));
        ProduitDTO invalidDto = new ProduitDTO(3L, "Nom", "Desc", BigDecimal.valueOf(-2), "Cat", 5, BigDecimal.ZERO);
        when(produitRepository.findById(3L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> produitService.updateProduit(3L, invalidDto))
                .isInstanceOf(BusinessException.class);
        verify(produitRepository).findById(3L);
        verifyNoMoreInteractions(produitRepository);
        verifyNoInteractions(produitMapper);
    }

    @Test
    void deleteProduit_whenProduitExists_shouldDeleteById() {
        // Arrange
        when(produitRepository.existsById(11L)).thenReturn(true);

        // Act
        produitService.deleteProduit(11L);

        // Assert
        verify(produitRepository).existsById(11L);
        verify(produitRepository).deleteById(11L);
        verifyNoMoreInteractions(produitRepository);
    }

    @Test
    void deleteProduit_whenProduitMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(produitRepository.existsById(11L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> produitService.deleteProduit(11L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(produitRepository).existsById(11L);
        verifyNoMoreInteractions(produitRepository);
    }

    @Test
    void getProduitsParCategorie_shouldReturnMapperResult() {
        // Arrange
        List<Produit> entities = List.of(buildProduit(1L, 5, BigDecimal.ONE));
        List<ProduitDTO> expected = List.of(buildProduitDTO(1L));
        when(produitRepository.findByCategorieIgnoreCase("Bureautique")).thenReturn(entities);
        when(produitMapper.toDTOList(entities)).thenReturn(expected);

        // Act
        List<ProduitDTO> result = produitService.getProduitsParCategorie("Bureautique");

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(produitRepository).findByCategorieIgnoreCase("Bureautique");
        verify(produitMapper).toDTOList(entities);
        verifyNoMoreInteractions(produitRepository, produitMapper);
    }

    @Test
    void getProduitsStockFaible_shouldReturnMapperResult() {
        // Arrange
        List<Produit> entities = List.of(buildProduit(2L, 1, BigDecimal.ONE));
        List<ProduitDTO> expected = List.of(buildProduitDTO(2L));
        when(produitRepository.findByStockActuelLessThanEqual(3)).thenReturn(entities);
        when(produitMapper.toDTOList(entities)).thenReturn(expected);

        // Act
        List<ProduitDTO> result = produitService.getProduitsStockFaible(3);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(produitRepository).findByStockActuelLessThanEqual(3);
        verify(produitMapper).toDTOList(entities);
        verifyNoMoreInteractions(produitRepository, produitMapper);
    }

    @Test
    void ajusterStock_whenVariationKeepsStockPositive_shouldPersistUpdatedStock() {
        // Arrange
        Produit produit = buildProduit(4L, 10, BigDecimal.ONE);
        when(produitRepository.findById(4L)).thenReturn(Optional.of(produit));

        // Act
        produitService.ajusterStock(4L, 5);

        // Assert
        assertThat(produit.getStockActuel()).isEqualTo(15);
        verify(produitRepository).findById(4L);
        verify(produitRepository).save(produit);
        verifyNoMoreInteractions(produitRepository);
        verifyNoInteractions(produitMapper);
    }

    @Test
    void ajusterStock_whenProduitMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(produitRepository.findById(8L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produitService.ajusterStock(8L, 3))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(produitRepository).findById(8L);
        verifyNoMoreInteractions(produitRepository);
    }

    @Test
    void ajusterStock_whenResultingStockNegative_shouldThrowBusinessException() {
        // Arrange
        Produit produit = buildProduit(9L, 2, BigDecimal.ONE);
        when(produitRepository.findById(9L)).thenReturn(Optional.of(produit));

        // Act & Assert
        assertThatThrownBy(() -> produitService.ajusterStock(9L, -5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("variation");
        verify(produitRepository).findById(9L);
        verifyNoMoreInteractions(produitRepository);
    }

    @Test
    void mettreAJourCoutMoyen_whenValueValid_shouldUpdateProduit() {
        // Arrange
        Produit produit = buildProduit(12L, 100, BigDecimal.valueOf(6));
        when(produitRepository.findById(12L)).thenReturn(Optional.of(produit));

        // Act
        produitService.mettreAJourCoutMoyen(12L, BigDecimal.valueOf(7.5));

        // Assert
        assertThat(produit.getCoutMoyenPondere()).isEqualByComparingTo("7.5");
        verify(produitRepository).findById(12L);
        verify(produitRepository).save(produit);
        verifyNoMoreInteractions(produitRepository);
        verifyNoInteractions(produitMapper);
    }

    @Test
    void mettreAJourCoutMoyen_whenValueNull_shouldThrowBusinessException() {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> produitService.mettreAJourCoutMoyen(12L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("positif");
        verifyNoInteractions(produitRepository, produitMapper);
    }

    @Test
    void mettreAJourCoutMoyen_whenValueNegative_shouldThrowBusinessException() {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> produitService.mettreAJourCoutMoyen(12L, BigDecimal.valueOf(-1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("positif");
        verifyNoInteractions(produitRepository, produitMapper);
    }

    @Test
    void mettreAJourCoutMoyen_whenProduitMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(produitRepository.findById(13L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produitService.mettreAJourCoutMoyen(13L, BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(produitRepository).findById(13L);
        verifyNoMoreInteractions(produitRepository);
    }

    private Produit buildProduit(Long id, int stock, BigDecimal prixUnitaire) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setStockActuel(stock);
        produit.setPrixUnitaire(prixUnitaire);
        produit.setCoutMoyenPondere(prixUnitaire);
        return produit;
    }

    private ProduitDTO buildProduitDTO(Long id) {
        return new ProduitDTO(
                id,
                "Produit" + id,
                "Description" + id,
                BigDecimal.ONE,
                "Categorie",
                10,
                BigDecimal.ONE
        );
    }
}