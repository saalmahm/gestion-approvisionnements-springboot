package com.example.gestion_approvisionnements.controller;

import com.example.gestion_approvisionnements.dto.FournisseurDTO;
import com.example.gestion_approvisionnements.service.FournisseurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
public class FournisseurController {

    private final FournisseurService fournisseurService;

    @GetMapping
    public Page<FournisseurDTO> getFournisseurs(Pageable pageable) {
        return fournisseurService.getAllFournisseurs(pageable);
    }

    @GetMapping("/{id}")
    public FournisseurDTO getFournisseur(@PathVariable Long id) {
        return fournisseurService.getFournisseurById(id);
    }

    @GetMapping("/ice/{ice}")
    public FournisseurDTO getFournisseurByIce(@PathVariable String ice) {
        return fournisseurService.getFournisseurByIce(ice);
    }

    @PostMapping
    public ResponseEntity<FournisseurDTO> createFournisseur(@Valid @RequestBody FournisseurDTO fournisseurDTO) {
        FournisseurDTO created = fournisseurService.createFournisseur(fournisseurDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public FournisseurDTO updateFournisseur(@PathVariable Long id,
                                            @Valid @RequestBody FournisseurDTO fournisseurDTO) {
        return fournisseurService.updateFournisseur(id, fournisseurDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFournisseur(@PathVariable Long id) {
        fournisseurService.deleteFournisseur(id);
    }
}