package com.example.gestion_approvisionnements.controller;

import com.example.gestion_approvisionnements.dto.MouvementStockDTO;
import com.example.gestion_approvisionnements.enums.TypeMouvement;
import com.example.gestion_approvisionnements.service.MouvementStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mouvements")
@RequiredArgsConstructor
public class MouvementStockController {

    private final MouvementStockService mouvementStockService;

    @PostMapping
    public MouvementStockDTO createMouvement(@RequestBody MouvementStockDTO dto) {
        return mouvementStockService.enregistrerMouvement(dto);
    }

    @GetMapping("/produit/{produitId}")
    public List<MouvementStockDTO> getByProduit(@PathVariable Long produitId) {
        return mouvementStockService.getMouvementsParProduit(produitId);
    }

    @GetMapping("/type/{type}")
    public List<MouvementStockDTO> getByType(@PathVariable TypeMouvement type) {
        return mouvementStockService.getMouvementsParType(type);
    }
}