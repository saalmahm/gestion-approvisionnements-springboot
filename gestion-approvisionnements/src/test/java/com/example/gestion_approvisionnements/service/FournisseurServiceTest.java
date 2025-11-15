package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.FournisseurDTO;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.FournisseurMapper;
import com.example.gestion_approvisionnements.repository.FournisseurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FournisseurServiceTest {

    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private FournisseurMapper fournisseurMapper;

    @InjectMocks
    private FournisseurService fournisseurService;

    private FournisseurDTO baseDto;

    @BeforeEach
    void setUp() {
        baseDto = buildDto(null, "ICE123");
    }

    @Test
    void getAllFournisseurs_whenPageableProvided_shouldReturnMappedDtos() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 5);
        Fournisseur entity = buildEntity(1L, "ICE111");
        Page<Fournisseur> page = new PageImpl<>(List.of(entity));
        FournisseurDTO dto = buildDto(1L, "ICE111");

        when(fournisseurRepository.findAll(pageable)).thenReturn(page);
        when(fournisseurMapper.toDTO(entity)).thenReturn(dto);

        // Act
        Page<FournisseurDTO> result = fournisseurService.getAllFournisseurs(pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(dto);
        verify(fournisseurRepository).findAll(pageable);
        verify(fournisseurMapper).toDTO(entity);
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void getFournisseurById_whenFound_shouldReturnDto() {
        // Arrange
        Fournisseur entity = buildEntity(5L, "ICE555");
        FournisseurDTO dto = buildDto(5L, "ICE555");
        when(fournisseurRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(fournisseurMapper.toDTO(entity)).thenReturn(dto);

        // Act
        FournisseurDTO result = fournisseurService.getFournisseurById(5L);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(fournisseurRepository).findById(5L);
        verify(fournisseurMapper).toDTO(entity);
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void getFournisseurById_whenMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(fournisseurRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fournisseurService.getFournisseurById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(fournisseurRepository).findById(99L);
        verifyNoMoreInteractions(fournisseurRepository);
        verifyNoInteractions(fournisseurMapper);
    }

    @Test
    void createFournisseur_whenIceAvailable_shouldPersistAndReturnDto() {
        // Arrange
        Fournisseur entity = new Fournisseur();
        Fournisseur saved = buildEntity(10L, "ICE123");
        FournisseurDTO expected = buildDto(10L, "ICE123");

        when(fournisseurRepository.existsByIce("ICE123")).thenReturn(false);
        when(fournisseurMapper.toEntity(baseDto)).thenReturn(entity);
        when(fournisseurRepository.save(entity)).thenReturn(saved);
        when(fournisseurMapper.toDTO(saved)).thenReturn(expected);

        // Act
        FournisseurDTO result = fournisseurService.createFournisseur(baseDto);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(fournisseurRepository).existsByIce("ICE123");
        verify(fournisseurMapper).toEntity(baseDto);
        verify(fournisseurRepository).save(entity);
        verify(fournisseurMapper).toDTO(saved);
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void createFournisseur_whenIceAlreadyUsed_shouldThrowBusinessException() {
        // Arrange
        when(fournisseurRepository.existsByIce("ICE123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fournisseurService.createFournisseur(baseDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ICE123");
        verify(fournisseurRepository).existsByIce("ICE123");
        verifyNoMoreInteractions(fournisseurRepository);
        verifyNoInteractions(fournisseurMapper);
    }

    @Test
    void createFournisseur_whenIceBlank_shouldSkipAvailabilityCheck() {
        // Arrange
        FournisseurDTO dtoSansIce = buildDto(null, " ");
        Fournisseur entity = buildEntity(null, " ");
        Fournisseur saved = buildEntity(3L, " ");
        FournisseurDTO expected = buildDto(3L, " ");

        when(fournisseurMapper.toEntity(dtoSansIce)).thenReturn(entity);
        when(fournisseurRepository.save(entity)).thenReturn(saved);
        when(fournisseurMapper.toDTO(saved)).thenReturn(expected);

        // Act
        FournisseurDTO result = fournisseurService.createFournisseur(dtoSansIce);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(fournisseurMapper).toEntity(dtoSansIce);
        verify(fournisseurRepository).save(entity);
        verify(fournisseurMapper).toDTO(saved);
        verify(fournisseurRepository, never()).existsByIce(anyString());
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void updateFournisseur_whenIceAvailable_shouldPersistAndReturnDto() {
        // Arrange
        Fournisseur existing = buildEntity(7L, "ICE777");
        FournisseurDTO updateDto = buildDto(7L, "ICE888");
        FournisseurDTO expected = buildDto(7L, "ICE888");

        when(fournisseurRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(fournisseurRepository.existsByIce("ICE888")).thenReturn(false);
        when(fournisseurRepository.save(existing)).thenReturn(existing);
        when(fournisseurMapper.toDTO(existing)).thenReturn(expected);

        // Act
        FournisseurDTO result = fournisseurService.updateFournisseur(7L, updateDto);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(fournisseurRepository).findById(7L);
        verify(fournisseurRepository).existsByIce("ICE888");
        verify(fournisseurMapper).updateEntityFromDTO(updateDto, existing);
        verify(fournisseurRepository).save(existing);
        verify(fournisseurMapper).toDTO(existing);
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void updateFournisseur_whenIceBelongsToSameEntity_shouldSucceed() {
        // Arrange
        Fournisseur existing = buildEntity(4L, "ICE444");
        FournisseurDTO updateDto = buildDto(4L, "ICE444");
        FournisseurDTO expected = buildDto(4L, "ICE444");

        when(fournisseurRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(fournisseurRepository.existsByIce("ICE444")).thenReturn(true);
        when(fournisseurRepository.findByIce("ICE444")).thenReturn(Optional.of(existing));
        when(fournisseurRepository.save(existing)).thenReturn(existing);
        when(fournisseurMapper.toDTO(existing)).thenReturn(expected);

        // Act
        FournisseurDTO result = fournisseurService.updateFournisseur(4L, updateDto);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(fournisseurRepository).findById(4L);
        verify(fournisseurRepository).existsByIce("ICE444");
        verify(fournisseurRepository).findByIce("ICE444");
        verify(fournisseurMapper).updateEntityFromDTO(updateDto, existing);
        verify(fournisseurRepository).save(existing);
        verify(fournisseurMapper).toDTO(existing);
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void updateFournisseur_whenEntityMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(fournisseurRepository.findById(12L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fournisseurService.updateFournisseur(12L, baseDto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(fournisseurRepository).findById(12L);
        verifyNoMoreInteractions(fournisseurRepository);
        verifyNoInteractions(fournisseurMapper);
    }

    @Test
    void updateFournisseur_whenIceBelongsToAnotherEntity_shouldThrowBusinessException() {
        // Arrange
        Fournisseur existing = buildEntity(6L, "ICE600");
        Fournisseur other = buildEntity(99L, "ICE123");

        when(fournisseurRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(fournisseurRepository.existsByIce("ICE123")).thenReturn(true);
        when(fournisseurRepository.findByIce("ICE123")).thenReturn(Optional.of(other));

        // Act & Assert
        assertThatThrownBy(() -> fournisseurService.updateFournisseur(6L, baseDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ICE123");
        verify(fournisseurRepository).findById(6L);
        verify(fournisseurRepository).existsByIce("ICE123");
        verify(fournisseurRepository).findByIce("ICE123");
        verifyNoMoreInteractions(fournisseurRepository);
        verifyNoInteractions(fournisseurMapper);
    }

    @Test
    void deleteFournisseur_whenExists_shouldDelete() {
        // Arrange
        when(fournisseurRepository.existsById(3L)).thenReturn(true);

        // Act
        fournisseurService.deleteFournisseur(3L);

        // Assert
        verify(fournisseurRepository).existsById(3L);
        verify(fournisseurRepository).deleteById(3L);
        verifyNoMoreInteractions(fournisseurRepository);
    }

    @Test
    void deleteFournisseur_whenMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(fournisseurRepository.existsById(3L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> fournisseurService.deleteFournisseur(3L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(fournisseurRepository).existsById(3L);
        verifyNoMoreInteractions(fournisseurRepository);
    }

    @Test
    void getFournisseurByIce_whenFound_shouldReturnDto() {
        // Arrange
        Fournisseur entity = buildEntity(2L, "ICE222");
        FournisseurDTO dto = buildDto(2L, "ICE222");
        when(fournisseurRepository.findByIce("ICE222")).thenReturn(Optional.of(entity));
        when(fournisseurMapper.toDTO(entity)).thenReturn(dto);

        // Act
        FournisseurDTO result = fournisseurService.getFournisseurByIce("ICE222");

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(fournisseurRepository).findByIce("ICE222");
        verify(fournisseurMapper).toDTO(entity);
        verifyNoMoreInteractions(fournisseurRepository, fournisseurMapper);
    }

    @Test
    void getFournisseurByIce_whenMissing_shouldThrowResourceNotFound() {
        // Arrange
        when(fournisseurRepository.findByIce("ICE999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fournisseurService.getFournisseurByIce("ICE999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ICE999");
        verify(fournisseurRepository).findByIce("ICE999");
        verifyNoMoreInteractions(fournisseurRepository);
        verifyNoInteractions(fournisseurMapper);
    }

    private Fournisseur buildEntity(Long id, String ice) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(id);
        fournisseur.setSociete("Fournisseur " + (id != null ? id : "N/A")); // ← CORRIGÉ : setSociete()
        fournisseur.setAdresse("Adresse " + (id != null ? id : "N/A"));
        fournisseur.setContact("Contact");
        fournisseur.setEmail("contact@example.com");
        fournisseur.setTelephone("0612345678");
        fournisseur.setVille("Ville");
        fournisseur.setIce(ice);
        return fournisseur;
    }

    private FournisseurDTO buildDto(Long id, String ice) {
        return new FournisseurDTO(
                id,
                "Fournisseur " + (id != null ? id : "N/A"), // "societe" dans le DTO
                "Adresse " + (id != null ? id : "N/A"),
                "Contact",
                "contact@example.com",
                "0612345678",
                "Ville",
                ice
        );
    }
}