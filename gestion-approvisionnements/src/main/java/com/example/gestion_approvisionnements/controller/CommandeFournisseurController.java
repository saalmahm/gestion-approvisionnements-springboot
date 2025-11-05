package com.example.gestion_approvisionnements.controller;

import com.example.gestion_approvisionnements.dto.CommandeFournisseurDTO;
import com.example.gestion_approvisionnements.enums.StatutCommande;
import com.example.gestion_approvisionnements.service.CommandeFournisseurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class CommandeFournisseurController {

    private final CommandeFournisseurService commandeFournisseurService;

    @GetMapping
    public Page<CommandeFournisseurDTO> getCommandes(Pageable pageable) {
        return commandeFournisseurService.getAllCommandes(pageable);
    }

    @GetMapping("/{id}")
    public CommandeFournisseurDTO getCommande(@PathVariable Long id) {
        return commandeFournisseurService.getCommandeById(id);
    }

    @PostMapping
    public ResponseEntity<CommandeFournisseurDTO> createCommande(@Valid @RequestBody CommandeFournisseurDTO commandeDTO) {
        CommandeFournisseurDTO created = commandeFournisseurService.createCommande(commandeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/statut")
    public CommandeFournisseurDTO updateStatut(@PathVariable Long id,
                                               @RequestParam StatutCommande statut) {
        return commandeFournisseurService.updateStatut(id, statut);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommande(@PathVariable Long id) {
        commandeFournisseurService.deleteCommande(id);
    }

    @GetMapping("/statut/{statut}")
    public List<CommandeFournisseurDTO> getCommandesParStatut(@PathVariable StatutCommande statut) {
        return commandeFournisseurService.getCommandesParStatut(statut);
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public List<CommandeFournisseurDTO> getCommandesParFournisseur(@PathVariable Long fournisseurId) {
        return commandeFournisseurService.getCommandesParFournisseur(fournisseurId);
    }

    @GetMapping("/periode")
    public List<CommandeFournisseurDTO> getCommandesParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return commandeFournisseurService.getCommandesParPeriode(debut, fin);
    }
}