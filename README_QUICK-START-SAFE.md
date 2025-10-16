# 🚀 Démarrage Rapide Sécurisé (15 minutes)

## 🎯 Objectif : Ajouter le Code Sans Rien Casser

Cette procédure ajoute tous les fichiers nécessaires **sans modifier votre code existant**.

---

## ✅ Phase 1 : Backend (5 minutes)

### Étape 1.1 : Ajouter la Dépendance

**Fichier** : `pom.xml`  
**Action** : Ajouter dans la section `<dependencies>`

```xml
<!-- ULID Generator -->
<dependency>
    <groupId>com.github.f4b6a3</groupId>
    <artifactId>ulid-creator</artifactId>
    <version>5.2.3</version>
</dependency>
```

**Commande** :
```bash
mvn clean install
```

---

### Étape 1.2 : Créer UlidGenerator

**Nouveau Fichier** : `src/main/java/com/pcagrade/order/util/UlidGenerator.java`

```bash
mkdir -p src/main/java/com/pcagrade/order/util
```

**Copier le contenu de l'artifact "UlidGenerator.java"**

**Vérification** :
```bash
# Le fichier doit exister
ls src/main/java/com/pcagrade/order/util/UlidGenerator.java
```

---

### Étape 1.3 : Créer AbstractUlidEntity (Optionnel)

**Nouveau Fichier** : `src/main/java/com/pcagrade/order/entity/AbstractUlidEntity.java`

**Copier le contenu de l'artifact "AbstractUlidEntity.java"**

⚠️ **IMPORTANT** : Si vous avez DÉJÀ une classe de base (AbstractEntity), **NE PAS** la remplacer !  
→ Gardez les deux classes séparées pour l'instant.

---

### Étape 1.4 : Compiler et Vérifier

```bash
mvn clean compile
```

**Attendu** : ✅ BUILD SUCCESS  
**Si erreur** : Vérifier le package et les imports

---

## ✅ Phase 2 : Frontend (10 minutes)

### Étape 2.1 : Installer Lucide Icons

```bash
cd frontend  # ou votre dossier frontend
npm install lucide-vue-next
```

---

### Étape 2.2 : Créer les Dossiers

```bash
mkdir -p src/types
mkdir -p src/services
mkdir -p src/composables
```

---

### Étape 2.3 : Copier les Types

**Nouveau Fichier** : `src/types/planning.ts`

**Copier le contenu de l'artifact "types.ts"**

---

### Étape 2.4 : Copier le Service API

**Nouveau Fichier** : `src/services/planning.service.ts`

**Copier le contenu de l'artifact "api.service.ts"**

---

### Étape 2.5 : Copier les Composables

**Nouveau Fichier** : `src/composables/usePlanning.ts`

**Copier le contenu de l'artifact "usePlanning.ts"**

---

### Étape 2.6 : Créer le Fichier .env (Si Pas Existant)

**Nouveau Fichier** : `.env`

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

### Étape 2.7 : Compiler et Vérifier

```bash
npm run build
```

**Attendu** : ✅ Build successful  
**Si erreur TypeScript** : Vérifier les imports

---

## 🧪 Phase 3 : Test (5 minutes)

### Test Backend

```bash
# Démarrer le backend
mvn spring-boot:run

# Dans un autre terminal, tester l'API
curl http://localhost:8080/api/teams
```

**Attendu** : Réponse JSON (même vide)

---

### Test Frontend

```bash
# Démarrer le frontend
npm run dev

# Ouvrir le navigateur
http://localhost:5173
```

**Attendu** : Application démarre normalement

---

### Test d'Intégration Simple

**Créer un composant de test** : `src/components/TestPlanning.vue`

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { planningApi } from '@/services/planning.service';

const status = ref('Chargement...');

onMounted(async () => {
  try {
    const result = await planningApi.getPendingOrders();
    if (result.success) {
      status.value = `✅ API OK - ${result.data?.length || 0} commandes`;
    } else {
      status.value = '⚠️ API erreur : ' + result.error;
    }
  } catch (error) {
    status.value = '❌ Erreur connexion';
  }
});
</script>

<template>
  <div class="test-planning">
    <h2>Test Planning API</h2>
    <p>{{ status }}</p>
  </div>
</template>
```

**Intégrer dans votre App.vue existant** :

```vue
<script setup>
import TestPlanning from './components/TestPlanning.vue';
</script>

<template>
  <div>
    <!-- Votre contenu existant -->
    
    <!-- Ajouter le test (à retirer après) -->
    <TestPlanning />
  </div>
</template>
```

**Attendu** : Message "✅ API OK - X commandes"

---

## 📋 Checklist de Validation

### Backend ✅
- [ ] Dépendance ULID ajoutée au pom.xml
- [ ] UlidGenerator.java créé
- [ ] AbstractUlidEntity.java créé (optionnel)
- [ ] `mvn clean compile` : SUCCESS
- [ ] `mvn spring-boot:run` : démarre sans erreur
- [ ] API accessible : `curl http://localhost:8080/api/teams`

### Frontend ✅
- [ ] lucide-vue-next installé
- [ ] `src/types/planning.ts` créé
- [ ] `src/services/planning.service.ts` créé
- [ ] `src/composables/usePlanning.ts` créé
- [ ] `.env` configuré avec API_BASE_URL
- [ ] `npm run build` : SUCCESS
- [ ] `npm run dev` : démarre sans erreur
- [ ] TestPlanning.vue affiche "✅ API OK"

### Validation Finale ✅
- [ ] Backend et Frontend démarrent ensemble
- [ ] Aucune erreur console
- [ ] Aucune erreur TypeScript
- [ ] Test API fonctionne
- [ ] Fonctionnalités existantes fonctionnent toujours

---

## 🎉 Félicitations !

Vous avez ajouté avec succès :
- ✅ Support ULID (backend)
- ✅ Services Planning (frontend)
- ✅ Composables réutilisables (frontend)
- ✅ Types TypeScript (frontend)

**Sans casser quoi que ce soit !** 🎯

---

## 🚀 Prochaines Étapes (Optionnelles)

### Option A : Ne Rien Faire de Plus

Gardez le code tel quel pour utilisation future.  
**Temps** : 0 minute  
**Avantage** : Zéro risque

---

### Option B : Créer un Composant Simple

**Créer** : `src/components/planning/WorkloadSummary.vue`

```vue
<script setup lang="ts">
import { usePlanning } from '@/composables/usePlanning';

const { workload, loading } = usePlanning();
</script>

<template>
  <div v-if="!loading" class="workload-summary">
    <h3>Employee Workload</h3>
    <ul>
      <li v-for="emp in workload" :key="emp.employeeId">
        {{ emp.employeeName }}: {{ emp.utilizationPercentage }}%
      </li>
    </ul>
  </div>
</template>
```

**Intégrer** dans une page existante :

```vue
<script setup>
import WorkloadSummary from '@/components/planning/WorkloadSummary.vue';
</script>

<template>
  <div>
    <!-- Votre contenu -->
    <WorkloadSummary />
  </div>
</template>
```

**Temps** : 10 minutes  
**Avantage** : Voir les données immédiatement

---

### Option C : Migrer une Table vers ULID

**Si vous voulez tester ULID :**

```java
// Dans Team.java, remplacer :
@Id
@GeneratedValue(generator = "uuid2")
@GenericGenerator(name = "uuid2", strategy = "uuid2")
private UUID id;

// Par :
@Id
@GeneratedValue(generator = "ulid-generator")
@GenericGenerator(
    name = "ulid-generator",
    strategy = "com.pcagrade.order.util.UlidGenerator"
)
private UUID id;
```

**Temps** : 5 minutes  
**Avantage** : Tester ULID en conditions réelles  
**Rollback** : Remettre "uuid2" si problème

---

## 🆘 En Cas de Problème

### Erreur : "Cannot find module 'lucide-vue-next'"

**Solution** :
```bash
npm install lucide-vue-next
```

---

### Erreur : "Cannot resolve '@/types/planning'"

**Solution** : Vérifier que le fichier existe
```bash
ls src/types/planning.ts
```

Si manquant, copier de nouveau.

---

### Erreur : "Class UlidGenerator not found"

**Solution** : Vérifier le package
```bash
# Vérifier que le fichier est au bon endroit
ls src/main/java/com/pcagrade/order/util/UlidGenerator.java

# Vérifier le package dans le fichier
grep "package" src/main/java/com/pcagrade/order/util/UlidGenerator.java
# Devrait afficher: package com.pcagrade.order.util;
```

---

### Backend démarre mais API ne répond pas

**Solution** : Vérifier le port et CORS

```properties
# application.properties
server.port=8080

# Ajouter si nécessaire pour le frontend
spring.web.cors.allowed-origins=http://localhost:5173
```

---

## 📞 Support

Si vous rencontrez un problème non listé :

1. Vérifier les logs backend : console Spring Boot
2. Vérifier les logs frontend : console navigateur (F12)
3. Vérifier que tous les fichiers sont aux bons emplacements
4. Relancer backend et frontend

---

## 🎯 Récapitulatif

**Ce que vous avez ajouté :**
- Code backend prêt pour ULID
- Services frontend pour planning
- Composables réutilisables
- Types TypeScript complets

**Ce que vous N'AVEZ PAS changé :**
- ❌ Aucune entité existante
- ❌ Aucun composant existant
- ❌ Aucune route existante
- ❌ Aucune donnée en base

**Résultat** : Architecture enrichie, zéro régression ! ✨

**Vous êtes prêt à utiliser ces nouveaux outils quand vous le souhaitez ! 🚀**