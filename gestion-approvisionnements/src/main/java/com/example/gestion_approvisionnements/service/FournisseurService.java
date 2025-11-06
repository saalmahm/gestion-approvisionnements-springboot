package com.example.gestion_approvisionnements.service;

import com.example.gestion_approvisionnements.dto.FournisseurDTO;
import com.example.gestion_approvisionnements.entity.Fournisseur;
import com.example.gestion_approvisionnements.exception.BusinessException;
import com.example.gestion_approvisionnements.exception.ResourceNotFoundException;
import com.example.gestion_approvisionnements.mapper.FournisseurMapper;
import com.example.gestion_approvisionnements.repository.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final FournisseurMapper fournisseurMapper;

    @Transactional(readOnly = true)
    public Page<FournisseurDTO> getAllFournisseurs(Pageable pageable) {
        return fournisseurRepository.findAll(pageable)
                .map(fournisseurMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public FournisseurDTO getFournisseurById(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec l'id " + id));
        return fournisseurMapper.toDTO(fournisseur);
    }

    public FournisseurDTO createFournisseur(FournisseurDTO fournisseurDTO) {
        validateIceAvailability(fournisseurDTO.getIce(), null);

        Fournisseur fournisseur = fournisseurMapper.toEntity(fournisseurDTO);
        Fournisseur saved = fournisseurRepository.save(fournisseur);
        return fournisseurMapper.toDTO(saved);
    }

    public FournisseurDTO updateFournisseur(Long id, FournisseurDTO fournisseurDTO) {
        Fournisseur existing = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec l'id " + id));

        validateIceAvailability(fournisseurDTO.getIce(), existing.getId());

        fournisseurMapper.updateEntityFromDTO(fournisseurDTO, existing);
        Fournisseur updated = fournisseurRepository.save(existing);
        return fournisseurMapper.toDTO(updated);
    }

    public void deleteFournisseur(Long id) {
        if (!fournisseurRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fournisseur introuvable avec l'id " + id);
        }
        fournisseurRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public FournisseurDTO getFournisseurByIce(String ice) {
        Fournisseur fournisseur = fournisseurRepository.findByIce(ice)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec l'ICE " + ice));
        return fournisseurMapper.toDTO(fournisseur);
    }

    private void validateIceAvailability(String ice, Long currentId) {
        if (ice == null || ice.isBlank()) {
            return;
        }
        boolean iceAlreadyExists = fournisseurRepository.existsByIce(ice);
        if (!iceAlreadyExists) {
            return;
        }
        if (currentId == null) {
            throw new BusinessException("Un fournisseur existe déjà avec l'ICE " + ice);
        }
        fournisseurRepository.findByIce(ice)
                .filter(f -> f.getId().equals(currentId))
                .orElseThrow(() -> new BusinessException("Un fournisseur existe déjà avec l'ICE " + ice));
    }
}