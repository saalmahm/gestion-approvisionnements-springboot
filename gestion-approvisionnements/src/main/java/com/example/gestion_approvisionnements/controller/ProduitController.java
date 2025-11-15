package com.example.gestion_approvisionnements.controller;

import com.example.gestion_approvisionnements.dto.ProduitDTO;
import com.example.gestion_approvisionnements.service.ProduitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
public class ProduitController {

    private final ProduitService produitService;

    @GetMapping
    public Page<ProduitDTO> getProduits(Pageable pageable) {
        return produitService.getAllProduits(pageable);
    }

    @GetMapping("/{id}")
    public ProduitDTO getProduit(@PathVariable Long id) {
        return produitService.getProduitById(id);
    }

    @GetMapping("/categorie/{categorie}")
    public List<ProduitDTO> getProduitsParCategorie(@PathVariable String categorie) {
        return produitService.getProduitsParCategorie(categorie);
    }

    @GetMapping("/stock-faible")
    public List<ProduitDTO> getProduitsStockFaible(@RequestParam(defaultValue = "10") Integer seuil) {
        return produitService.getProduitsStockFaible(seuil);
    }

    @PostMapping
    public ResponseEntity<ProduitDTO> createProduit(@Valid @RequestBody ProduitDTO produitDTO) {
        ProduitDTO created = produitService.createProduit(produitDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ProduitDTO updateProduit(@PathVariable Long id, @Valid @RequestBody ProduitDTO produitDTO) {
        return produitService.updateProduit(id, produitDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
    }

    @PatchMapping("/{id}/stock")
    public void ajusterStock(
        @PathVariable Long id,
        @RequestParam int variation,
        @RequestParam(required = false) BigDecimal prixUnitaire
    ) {
        produitService.ajusterStock(id, variation, prixUnitaire);
    }

    @PatchMapping("/{id}/cump")
    public void mettreAJourCump(@PathVariable Long id,
                                @RequestParam BigDecimal valeur) {
        produitService.mettreAJourCoutMoyen(id, valeur);
    }
}