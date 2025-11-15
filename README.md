<div align="center">

# Gestion des Approvisionnements

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

**API REST pour gérer fournisseurs, produits, commandes et stocks en temps réel**

[Installation](#installation-rapide) • [API](#endpoints) • [Architecture](#architecture) • [Tests](#stratégie-de-tests)

</div>

---

## 📖 Description

API REST complète développée avec **Spring Boot** pour la gestion du cycle de vie des commandes fournisseurs :

- gestion des fournisseurs (création, modification, suppression, recherche par ICE) ;
- catalogue produits (catégories, alertes stock faible, CUMP) ;
- commandes fournisseurs multi-lignes (statuts, filtres avancés) ;
- mouvements de stock et valorisation automatique.

Architecture : **Controller → Service → Repository**, exposition uniquement via **DTOs**.

---

## 🛠️ Stack technique

| Tech           | Usage                                |
|----------------|--------------------------------------|
| Java 17        | Langage                              |
| Spring Boot    | Web, JPA, Validation                 |
| Spring Data JPA| Accès base de données                |
| MapStruct      | Mapping DTO ↔ Entity                 |
| PostgreSQL     | Base de données (prod)               |
| H2             | Base en mémoire pour les tests       |
| Liquibase      | Migrations SQL                       |
| Maven          | Build / dépendances                  |
| JUnit 5        | Tests unitaires                      |
| Mockito        | Mocks / doubles de test              |
| JaCoCo         | Couverture de code                   |

---

## 🏗️ Architecture

```text
com.example.gestion_approvisionnements
├── controller/      # API REST (endpoints)
├── service/         # Logique métier
├── repository/      # Spring Data JPA
├── dto/             # Data Transfer Objects
├── mapper/          # Interfaces MapStruct
├── entity/          # Entités JPA
├── enums/           # Énumérations (StatutCommande, TypeMouvement...)
└── exception/       # Exceptions métiers + GlobalExceptionHandler
```

**Modèle de données (extraits) :**

- `fournisseur` : informations fournisseur (nom, ICE, contact, email…)
- `produit` : catalogue (référence, prix, stock, CUMP)
- `commande_fournisseur` : en-têtes commandes
- `ligne_commande` : lignes de commande
- `mouvement_stock` : mouvements (ENTREE, SORTIE, AJUSTEMENT)

---

## ⚡ Installation rapide

### 1️⃣ Cloner le projet

```bash
git clone https://github.com/.../gestion-approvisionnements-springboot.git
cd gestion-approvisionnements-springboot/gestion-approvisionnements
```

### 2️⃣ Créer la base PostgreSQL

```sql
CREATE DATABASE gestion_approvisionnements;
```

### 3️⃣ Configurer `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_approvisionnements
spring.datasource.username=postgres
spring.datasource.password=VOTRE_MOT_DE_PASSE

spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

### 4️⃣ Build + run

```bash
mvn clean install
mvn spring-boot:run
```

L'API est accessible sur : **http://localhost:8080**

---

## 📡 Endpoints

### 👥 Fournisseurs `/api/fournisseurs`

| Méthode | Endpoint     | Description              |
|---------|--------------|--------------------------|
| GET     | `/`          | Liste paginée            |
| GET     | `/{id}`      | Détail fournisseur       |
| GET     | `/ice/{ice}` | Recherche par ICE        |
| POST    | `/`          | Créer                    |
| PUT     | `/{id}`      | Modifier                 |
| DELETE  | `/{id}`      | Supprimer                |

**Exemple création :**

```json
{
  "societe": "Fournisseur SA",
  "ice": "001234567890001",
  "adresse": "123 Rue...",
  "telephone": "0522123456",
  "email": "contact@fournisseur.ma",
  "ville": "Casablanca"
}
```

### 📦 Produits `/api/produits`

| Méthode | Endpoint                          | Description                    |
|---------|-----------------------------------|--------------------------------|
| GET     | `/`                               | Liste paginée                  |
| GET     | `/{id}`                           | Détail produit                 |
| GET     | `/categorie/{categorie}`          | Filtrer par catégorie          |
| GET     | `/stock-faible?seuil=10`          | Produits avec stock ≤ seuil    |
| POST    | `/`                               | Créer                          |
| PUT     | `/{id}`                           | Modifier                       |
| PATCH   | `/{id}/stock?variation=5`         | Ajuster stock (+/-)            |
| PATCH   | `/{id}/cump?valeur=12.5`          | Mettre à jour le CUMP          |
| DELETE  | `/{id}`                           | Supprimer                      |

### 🛒 Commandes fournisseurs `/api/commandes`

| Méthode | Endpoint                                              | Description                |
|---------|-------------------------------------------------------|----------------------------|
| GET     | `/`                                                   | Liste paginée              |
| GET     | `/{id}`                                               | Détail commande            |
| POST    | `/`                                                   | Créer (avec lignes)        |
| PATCH   | `/{id}/statut?statut=VALIDEE`                         | Changer statut             |
| DELETE  | `/{id}`                                               | Supprimer                  |
| GET     | `/statut/{statut}`                                    | Filtrer par statut         |
| GET     | `/fournisseur/{fournisseurId}`                        | Filtrer par fournisseur    |
| GET     | `/periode?debut=2024-01-01&fin=2024-12-31`            | Filtrer par période        |

**Statuts disponibles :** `EN_ATTENTE`, `VALIDEE`, `LIVREE`, `ANNULEE`.

### 📊 Mouvements de stock `/api/mouvements`

| Méthode | Endpoint              | Description                  |
|---------|-----------------------|------------------------------|
| GET     | `/produit/{produitId}`| Historique d'un produit      |
| GET     | `/type/{type}`        | Filtrer par type             |
| POST    | `/`                   | Enregistrer un mouvement     |

**Types :** `ENTREE`, `SORTIE`, `AJUSTEMENT`.

---

## 🧮 CUMP (Coût Unitaire Moyen Pondéré)

Lors d'un mouvement d'entrée, le CUMP est recalculé automatiquement :

```
Nouveau CUMP = (Ancien stock × Ancien CUMP + Quantité entrée × Prix d'achat) / (Ancien stock + Quantité entrée)
```

L'objectif est de fournir une valorisation fiable du stock.

---

## ✅ Stratégie de tests

Ce projet inclut une campagne de tests unitaires et d'intégration pour répondre au cahier des charges pédagogique (JUnit 5, Mockito, H2, JaCoCo).

### Tests unitaires (JUnit 5 + Mockito)

#### **ProduitServiceTest**
- création / mise à jour produit ;
- ajustement de stock (cas OK + stock négatif → exception) ;
- mise à jour du CUMP ;
- filtres par catégorie / stock faible ;
- gestion des `ResourceNotFoundException`.

#### **FournisseurServiceTest**
- CRUD fournisseur ;
- validation de l'unicité de l'ICE ;
- ICE dupliqué → `BusinessException` ;
- `ResourceNotFoundException` sur ID inexistant.

#### **CommandeFournisseurServiceTest**
- création de commande avec lignes ;
- calcul du montant total ;
- commande sans lignes → `BusinessException` ;
- fournisseur introuvable ;
- produit introuvable ;
- changement de statut.

#### **MouvementStockServiceTest**
- enregistrement de mouvements (ENTREE / SORTIE / AJUSTEMENT) ;
- impact sur le stock produit ;
- recalcul CUMP lorsque nécessaire.

> Les repositories ne sont pas testés isolément, mais via les services et tests d'intégration, conformément aux consignes.

### Tests d'intégration (Spring Boot Test + MockMvc + H2)

#### **AbstractIntegrationTest**
- configuration commune (Spring Boot + MockMvc + H2 en mémoire).

#### **ProduitControllerIntegrationTest**
- toutes les routes `/api/produits` (CRUD, filtres, validations, erreurs).

#### **FournisseurControllerIntegrationTest**
- toutes les routes `/api/fournisseurs` (CRUD, recherche ICE, erreurs 404/409, validations).

#### **CommandeFournisseurControllerIntegrationTest**
- routes `/api/commandes` (CRUD, filtres statut/fournisseur/période, erreurs de fournisseur/produit introuvable).

---

## 🧪 Exécution des tests

### Tous les tests (unitaires + intégration)

```bash
mvn clean test
```

### Tests unitaires par classe

```bash
mvn test -Dtest=ProduitServiceTest
mvn test -Dtest=FournisseurServiceTest
mvn test -Dtest=CommandeFournisseurServiceTest
mvn test -Dtest=MouvementStockServiceTest
```

### Tests d'intégration par contrôleur

```bash
mvn test -Dtest=ProduitControllerIntegrationTest
mvn test -Dtest=FournisseurControllerIntegrationTest
mvn test -Dtest=CommandeFournisseurControllerIntegrationTest
```

---

## 📈 Couverture de code (JaCoCo)

Le plugin JaCoCo est configuré dans le `pom.xml` pour générer un rapport après `verify` :

```bash
mvn clean test
mvn jacoco:report
```

**Rapport HTML disponible ici :**

```
target/site/jacoco/index.html
```

Le rapport détaille :

- couverture ligne / instruction ;
- couverture méthodes / classes ;
- couverture de branches (chemins alternatifs, exceptions).

Les services et contrôleurs critiques bénéficient d'un bon niveau de couverture, y compris sur les cas d'erreurs (404, 400, 409).

---

## 👤 Auteur

**Développé par :** Salma Hamdi  
