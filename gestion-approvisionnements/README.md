<div align="center">

#  Gestion des Approvisionnements
</div>
<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

**API REST complète pour gérer fournisseurs, produits, commandes et stocks en temps réel**

[Installation](#-installation-rapide) • [API Documentation](#-endpoints) • [Architecture](#-architecture)

</div>

---
## 📖 Description
API REST complète développée avec **Spring Boot** pour la gestion du cycle de vie des commandes fournisseurs, incluant la gestion des stocks, la valorisation CUMP et le suivi des mouvements.

## 🎯 Contexte
Application développée pour l'entreprise **Tricol**, spécialisée dans la conception de vêtements professionnels, afin de digitaliser la gestion des approvisionnements en matières premières.
---
## 📖 Vue d'ensemble

Système de gestion d'approvisionnements avec :
- ✅ Gestion fournisseurs (CRUD + recherche ICE)
- ✅ Catalogue produits (catégories, alertes stock faible, CUMP)
- ✅ Commandes fournisseur (multi-lignes, statuts, filtres avancés)
- ✅ Traçabilité stock (historique, ajustements, calculs automatiques)

**Architecture :** Controller → Service → Repository  
**Exposition :** DTOs uniquement (pas d'entités exposées)

---

## 🛠️ Stack

| Tech | Usage |
|------|-------|
| ☕ Java 17 | Langage |
| 🍃 Spring Boot | Framework (Web, JPA, Validation) |
| 🗺️ MapStruct | Mapping DTO ↔ Entity |
| 🐘 PostgreSQL | Base de données |
| 💧 Liquibase | Migrations DB |
| 🔨 Maven | Build |

---

## 🏗️ Architecture

```
com.example.gestion_approvisionnements/
├── 🎮 controller/      # API REST endpoints
├── ⚙️ service/         # Logique métier
├── 💾 repository/      # Accès BDD (Spring Data JPA)
├── 📦 dto/             # Data Transfer Objects
├── 🗺️ mapper/          # MapStruct interfaces
├── 🏛️ entity/          # Entités JPA
├── 🎨 enums/           # Énumérations (StatutCommande, TypeMouvement...)
└── ⚠️ exception/       # Gestion erreurs globale
```

**Tables créées par Liquibase :**
- `fournisseur` - Informations fournisseurs (nom, ICE, contact)
- `produit` - Catalogue produits (référence, prix, stock, CUMP)
- `commande_fournisseur` - En-têtes commandes
- `ligne_commande` - Détails lignes commandes
- `mouvement_stock` - Historique mouvements (entrées/sorties/ajustements)

---

## ⚡ Installation rapide

### 1️⃣ Cloner & accéder au projet
```bash
git clone https://github.com/.../gestion-approvisionnements-springboot.git
cd gestion-approvisionnements-springboot/gestion-approvisionnements
```

### 2️⃣ Créer la base PostgreSQL
```sql
CREATE DATABASE gestion_approvisionnements;
```

### 3️⃣ Vérifier `application.properties`
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_approvisionnements
spring.datasource.username=postgres
spring.datasource.password=votre password 
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

### 4️⃣ Lancer
```bash
mvn clean install
mvn spring-boot:run
```

✅ **API disponible :** http://localhost:8080

---

## 📡 Endpoints

### 👥 Fournisseurs `/api/fournisseurs`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/` | Liste paginée |
| `GET` | `/{id}` | Détail fournisseur |
| `GET` | `/ice/{ice}` | Recherche par numéro ICE |
| `POST` | `/` | Créer |
| `PUT` | `/{id}` | Modifier |
| `DELETE` | `/{id}` | Supprimer |

**Exemple création :**
```json
{
  "nom": "Fournisseur SA",
  "ice": "001234567890001",
  "adresse": "123 Rue...",
  "telephone": "0522123456",
  "email": "contact@fournisseur.ma"
}
```

---

### 📦 Produits `/api/produits`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/` | Liste paginée |
| `GET` | `/{id}` | Détail produit |
| `GET` | `/categorie/{categorie}` | Filtrer par catégorie |
| `GET` | `/stock-faible?seuil=10` | Alertes stock < seuil |
| `POST` | `/` | Créer |
| `PUT` | `/{id}` | Modifier |
| `PATCH` | `/{id}/stock?variation=5` | Ajuster stock (+5 ou -5) |
| `PATCH` | `/{id}/cump?valeur=12.5` | Mettre à jour CUMP |

**Exemple création :**
```json
{
  "reference": "PROD-001",
  "designation": "Écran 24 pouces",
  "categorie": "INFORMATIQUE",
  "prixUnitaire": 1200.00,
  "quantiteStock": 50,
  "seuilAlerte": 10
}
```

---

### 🛒 Commandes `/api/commandes`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/` | Liste paginée |
| `GET` | `/{id}` | Détail commande |
| `POST` | `/` | Créer (avec lignes) |
| `PATCH` | `/{id}/statut?statut=VALIDEE` | Changer statut |
| `DELETE` | `/{id}` | Supprimer |
| `GET` | `/statut/{statut}` | Filtrer par statut |
| `GET` | `/fournisseur/{fournisseurId}` | Par fournisseur |
| `GET` | `/periode?debut=2024-01-01&fin=2024-12-31` | Par période |

**Statuts disponibles :** `EN_ATTENTE`, `VALIDEE`, `LIVREE`, `ANNULEE`

**Exemple création :**
```json
{
  "fournisseurId": 1,
  "dateCommande": "2024-11-07",
  "lignes": [
    {
      "produitId": 5,
      "quantite": 20,
      "prixUnitaire": 1200.00
    },
    {
      "produitId": 8,
      "quantite": 10,
      "prixUnitaire": 350.00
    }
  ]
}
```

---

### 📊 Mouvements Stock `/api/mouvements`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/produit/{produitId}` | Historique par produit |
| `GET` | `/type/{type}` | Filtrer par type |
| `POST` | `/` | Enregistrer mouvement |

**Types de mouvement :** `ENTREE`, `SORTIE`, `AJUSTEMENT`

**Exemple enregistrement :**
```json
{
  "produitId": 5,
  "typeMouvement": "ENTREE",
  "quantite": 100,
  "prixUnitaire": 1150.00,
  "reference": "CMD-2024-001"
}
```

**Impact automatique :**
- ✅ Mise à jour stock produit
- ✅ Recalcul CUMP (Coût Unitaire Moyen Pondéré)
- ✅ Traçabilité complète

---

## 🔍 Fonctionnalités clés

### 💰 Calcul automatique CUMP
```
Nouveau CUMP = (Ancien stock × Ancien CUMP + Quantité entrée × Prix) 
                / (Ancien stock + Quantité entrée)
```

### 📊 Filtres avancés
- Commandes par période (date début/fin)
- Commandes par statut ou fournisseur
- Produits par catégorie
- Mouvements par type

### 🔒 Validation des données
Spring Validation sur tous les DTOs (contraintes métier respectées)

---

## 📝 Prérequis

- Java 17+
- Maven 3.8+
- PostgreSQL
- Git

---

## 👥 Auteur

- **Développée par** : Salma Hamdi

