package com.example.gestion_approvisionnements.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurDTO {

    private Long id;

    @NotBlank(message = "La société est obligatoire")
    @Size(max = 200, message = "La société ne peut pas dépasser 200 caractères")
    private String societe;

    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String adresse;

    @Size(max = 100, message = "Le contact ne peut pas dépasser 100 caractères")
    private String contact;

    @Email(message = "L'email doit être valide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;

    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String telephone;

    @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
    private String ville;

    @Size(max = 50, message = "L'ICE ne peut pas dépasser 50 caractères")
    private String ice;
}