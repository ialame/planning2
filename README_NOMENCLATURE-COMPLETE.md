# Nomenclature Complète - Liste Exhaustive des Fichiers

## 🎯 Convention de Nommage Cohérente

Puisque l'entité s'appelle `Team` (pour éviter le mot-clé SQL `GROUP`), **TOUS** les fichiers associés doivent utiliser `Team`.

---

## 📋 Liste Complète des Fichiers

### ✅ Fichiers CORRECTS (Nomenclature Team)

| Fichier | Chemin | Statut |
|---------|--------|--------|
| **Team.java** | `entity/Team.java` | ✅ Entité principale |
| **TeamDto.java** | `dto/TeamDto.java` | ✅ Data Transfer Objects |
| **TeamRepository.java** | `repository/TeamRepository.java` | ✅ Repository |
| **TeamService.java** | `service/TeamService.java` | ✅ Service métier |
| **TeamMapperService.java** | `service/TeamMapperService.java` | ✅ Mapper DTO ↔ Entity |
| **TeamController.java** | `controller/TeamController.java` | ✅ Contrôleur simple |
| **EnhancedTeamController.java** | `controller/EnhancedTeamController.java` | ✅ Contrôleur avancé |

---

### ❌ Fichiers INCORRECTS (À Renommer ou Supprimer)

| Fichier Incorrect | → | Fichier Correct |
|-------------------|---|-----------------|
| **GroupDto.java** | → | **TeamDto.java** |
| **GroupRepository.java** | → | **TeamRepository.java** |
| **GroupService.java** | → | **TeamService.java** |
| **GroupMapperService.java** | → | **TeamMapperService.java** |
| **GroupController.java** | → | **TeamController.java** |
| **EnhancedGroupController.java** | → | **EnhancedTeamController.java** |

---

## 🔍 Comment Vérifier Votre Projet

### Commande de Vérification

```bash
# Chercher TOUS les fichiers avec "Group" dans le nom
find src/main/java -name "*Group*.java" -type f

# Résultat attendu : 
# Aucun fichier (ou seulement *.old)

# Si des fichiers Group* sont trouvés (sans .old), ils doivent être renommés !
```

---

## 🔄 Actions à Effectuer

### 1. Vérifier l'Existence des Fichiers Group

```bash
# Lister tous les fichiers Group
echo "=== Fichiers DTO ==="
ls -la src/main/java/com/pcagrade/order/dto/ | grep -i group

echo "=== Fichiers Service ==="
ls -la src/main/java/com/pcagrade/order/service/ | grep -i group

echo "=== Fichiers Controller ==="
ls -la src/main/java/com/pcagrade/order/controller/ | grep -i group

echo "=== Fichiers Repository ==="
ls -la src/main/java/com/pcagrade/order/repository/ | grep -i group
```

### 2. Archiver les Anciens Fichiers

```bash
# Archiver tous les fichiers Group*.java
find src/main/java -name "Group*.java" -type f -exec mv {} {}.old \;

# OU les supprimer directement si vous êtes sûr
# find src/main/java -name "Group*.java" -type f -delete
```

### 3. Créer les Nouveaux Fichiers Team

Copier depuis les artifacts :
- ✅ `TeamDto.java`
- ✅ `TeamService.java`
- ✅ `TeamMapperService.java`
- ✅ `EnhancedTeamController.java` (si besoin)

---

## 📊 Matrice de Correspondance Complète

### Backend

| Type | ❌ Ancien (Group) | ✅ Nouveau (Team) |
|------|------------------|-------------------|
| **Entité** | `Group.java` | `Team.java` |
| **DTO** | `GroupDto` | `TeamDto` |
| **Repository** | `GroupRepository` | `TeamRepository` |
| **Service Métier** | `GroupService` | `TeamService` |
| **Service Mapper** | `GroupMapperService` | `TeamMapperService` |
| **Controller** | `GroupController` | `TeamController` |
| **Controller Avancé** | `EnhancedGroupController` | `EnhancedTeamController` |

### API REST

| Type | ❌ Ancien | ✅ Nouveau |
|------|----------|-----------|
| **Base URL** | `/api/groups` | `/api/teams` |
| **Get All** | `GET /api/groups` | `GET /api/teams` |
| **Get One** | `GET /api/groups/{id}` | `GET /api/teams/{id}` |
| **Create** | `POST /api/groups` | `POST /api/teams` |
| **Update** | `PUT /api/groups/{id}` | `PUT /api/teams/{id}` |
| **Delete** | `DELETE /api/groups/{id}` | `DELETE /api/teams/{id}` |

### Frontend (TypeScript)

| Type | ❌ Ancien | ✅ Nouveau |
|------|----------|-----------|
| **Interface** | `GroupDto` | `TeamDto` |
| **Service** | `groupService` | `teamService` |
| **API Call** | `fetchGroups()` | `fetchTeams()` |
| **Variable** | `const groups =` | `const teams =` |
| **Props** | `group: Group` | `team: Team` |

---

## 🧪 Tests de Validation

### Test 1 : Aucun Fichier Group Actif

```bash
# Cette commande ne doit RIEN retourner
find src/main/java -name "*Group*.java" -type f ! -name "*.old" ! -name "*.bak"

# Si retourne des fichiers → À corriger !
```

### Test 2 : Tous les Fichiers Team Existent

```bash
# Vérifier que tous les fichiers Team existent
[ -f "src/main/java/com/pcagrade/order/entity/Team.java" ] && echo "✅ Team.java"
[ -f "src/main/java/com/pcagrade/order/dto/TeamDto.java" ] && echo "✅ TeamDto.java"
[ -f "src/main/java/com/pcagrade/order/repository/TeamRepository.java" ] && echo "✅ TeamRepository.java"
[ -f "src/main/java/com/pcagrade/order/service/TeamService.java" ] && echo "✅ TeamService.java"
[ -f "src/main/java/com/pcagrade/order/service/TeamMapperService.java" ] && echo "✅ TeamMapperService.java"
```

### Test 3 : Aucune Référence Group dans le Code

```bash
# Chercher "GroupDto", "GroupService", etc. dans le code actif
grep -r "GroupDto\|GroupService\|GroupMapper\|GroupController" src/main/java/ \
  --include="*.java" \
  --exclude="*.old" \
  --exclude="*.bak"

# Si retourne des résultats → À corriger !
```

### Test 4 : Compilation Réussie

```bash
mvn clean compile

# Attendu : BUILD SUCCESS
```

### Test 5 : API Accessible

```bash
# Démarrer l'app
mvn spring-boot:run &

# Attendre 10 secondes
sleep 10

# Tester l'endpoint
curl -f http://localhost:8080/api/teams

# Doit retourner : 200 OK (même si liste vide)

# Arrêter l'app
kill %1
```

---

## 📝 Checklist Complète

### Phase 1 : Audit
- [ ] Lister tous les fichiers `*Group*.java` dans le projet
- [ ] Identifier les fichiers à renommer
- [ ] Vérifier les références dans le code

### Phase 2 : Migration
- [ ] Créer `TeamDto.java`
- [ ] Créer `TeamService.java`
- [ ] Créer `TeamMapperService.java`
- [ ] Créer `EnhancedTeamController.java` (si nécessaire)
- [ ] Archiver les anciens fichiers `*Group*.java`

### Phase 3 : Nettoyage
- [ ] Remplacer toutes les références `GroupDto` → `TeamDto`
- [ ] Remplacer toutes les références `GroupService` → `TeamService`
- [ ] Remplacer toutes les références `GroupMapperService` → `TeamMapperService`
- [ ] Remplacer toutes les références `GroupController` → `TeamController`

### Phase 4 : Validation
- [ ] `mvn clean compile` : BUILD SUCCESS
- [ ] Aucun fichier `*Group*.java` actif
- [ ] Aucune référence `Group*` dans le code
- [ ] API `/api/teams` accessible
- [ ] Tests unitaires passent (si applicable)

### Phase 5 : Documentation
- [ ] Mettre à jour README.md
- [ ] Mettre à jour documentation API
- [ ] Mettre à jour commentaires de code
- [ ] Informer l'équipe

---

## 🚨 Erreurs Communes

### Erreur 1 : `cannot find symbol: GroupDto`

**Cause** : Fichier utilise encore `GroupDto` au lieu de `TeamDto`

**Solution** :
```bash
grep -r "GroupDto" src/main/java/ --include="*.java"
# Remplacer manuellement ou utiliser le script
```

### Erreur 2 : `cannot find symbol: GroupService`

**Cause** : Fichier utilise encore `GroupService` au lieu de `TeamService`

**Solution** :
```bash
grep -r "GroupService" src/main/java/ --include="*.java"
# Remplacer manuellement ou utiliser le script
```

### Erreur 3 : Deux versions coexistent

**Cause** : Vous avez à la fois `GroupDto.java` et `TeamDto.java`

**Solution** :
```bash
# Supprimer l'ancienne version
rm src/main/java/com/pcagrade/order/dto/GroupDto.java
# OU l'archiver
mv src/main/java/com/pcagrade/order/dto/GroupDto.java \
   src/main/java/com/pcagrade/order/dto/GroupDto.java.old
```

---

## 🎯 Résumé Visuel

```
AVANT (Incohérent)          APRÈS (Cohérent)
═══════════════════         ════════════════

Group.java         ────────► Team.java ✅
GroupDto.java      ────────► TeamDto.java ✅
GroupService.java  ────────► TeamService.java ✅
GroupMapper*.java  ────────► TeamMapperService.java ✅
GroupController*   ────────► TeamController* ✅

/api/groups        ────────► /api/teams ✅
```

---

## ✅ Validation Finale

Une fois TOUTES les étapes complétées :

```bash
# Script de validation complet
./validate-nomenclature.sh
```

**Contenu du script** :

```bash
#!/bin/bash

echo "🔍 Validation de la nomenclature..."

ERRORS=0

# 1. Vérifier fichiers Group actifs
if find src/main/java -name "*Group*.java" -type f ! -name "*.old" ! -name "*.bak" | grep -q .; then
    echo "❌ Fichiers Group* trouvés (à renommer)"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ Aucun fichier Group* actif"
fi

# 2. Vérifier références Group dans le code
if grep -r "GroupDto\|GroupService\|GroupMapper" src/main/java/ --include="*.java" --exclude="*.old" --exclude="*.bak" | grep -q .; then
    echo "❌ Références Group* trouvées dans le code"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ Aucune référence Group* dans le code"
fi

# 3. Vérifier compilation
if mvn clean compile -DskipTests -q; then
    echo "✅ Compilation réussie"
else
    echo "❌ Échec de compilation"
    ERRORS=$((ERRORS + 1))
fi

# Résultat
echo ""
if [ $ERRORS -eq 0 ]; then
    echo "🎉 Validation réussie ! Nomenclature 100% cohérente."
    exit 0
else
    echo "⚠️  $ERRORS erreur(s) trouvée(s)"
    exit 1
fi
```

---

## 🎓 Bénéfices de la Nomenclature Cohérente

1. ✅ **Clarté** : Tout le monde comprend immédiatement que c'est lié aux équipes
2. ✅ **Maintenabilité** : Facile à retrouver et modifier
3. ✅ **Cohérence** : Suit la convention dès l'entité
4. ✅ **Pas de confusion** : Aucun mélange Group/Team
5. ✅ **Standard SQL** : Évite le mot-clé réservé
6. ✅ **Documentation** : Auto-explicatif

---

**Maintenant vous avez la liste COMPLÈTE ! Plus aucun fichier Group ne doit rester. 🎯**