# Migration Group → Team - Renommage Cohérent

## 🎯 Objectif : Cohérence Complète du Code

Vous avez raison ! Puisque nous avons renommé `Group` en `Team` pour éviter le mot-clé SQL réservé, **TOUT doit être cohérent**.

---

## 📋 Table de Correspondance

| ❌ Ancien (Incohérent) | ✅ Nouveau (Cohérent) |
|------------------------|----------------------|
| `Group` (entité) | `Team` |
| `GroupDto` | `TeamDto` |
| `GroupMapperService` | `TeamMapperService` |
| `GroupRepository` | `TeamRepository` |
| `EnhancedGroupController` | `EnhancedTeamController` |
| `/api/groups` | `/api/teams` |

---

## 🔧 Migration en 3 Étapes

### Étape 1 : Remplacer les Fichiers

#### 1.1 Supprimer les Anciens (si existent)

```bash
# Sauvegarder d'abord (optionnel)
mv src/main/java/com/pcagrade/order/dto/GroupDto.java \
   src/main/java/com/pcagrade/order/dto/GroupDto.java.old

mv src/main/java/com/pcagrade/order/service/GroupMapperService.java \
   src/main/java/com/pcagrade/order/service/GroupMapperService.java.old

mv src/main/java/com/pcagrade/order/controller/EnhancedGroupController.java \
   src/main/java/com/pcagrade/order/controller/EnhancedGroupController.java.old
```

#### 1.2 Créer les Nouveaux

**Copier depuis les artifacts :**
- `TeamDto.java` → `src/main/java/com/pcagrade/order/dto/TeamDto.java`
- `TeamMapperService.java` → `src/main/java/com/pcagrade/order/service/TeamMapperService.java`
- `EnhancedTeamController.java` → `src/main/java/com/pcagrade/order/controller/EnhancedTeamController.java`

---

### Étape 2 : Rechercher et Remplacer dans Tout le Projet

```bash
# Chercher toutes les références à GroupDto
grep -r "GroupDto" src/main/java/

# Chercher toutes les références à GroupMapperService
grep -r "GroupMapperService" src/main/java/

# Chercher toutes les références à EnhancedGroupController
grep -r "EnhancedGroupController" src/main/java/
```

#### 2.1 Remplacements Automatiques

**Script de remplacement :**

```bash
#!/bin/bash

echo "🔄 Migration Group → Team..."

# Remplacer GroupDto par TeamDto
find src/main/java -type f -name "*.java" -exec sed -i '' 's/GroupDto/TeamDto/g' {} +

# Remplacer GroupMapperService par TeamMapperService
find src/main/java -type f -name "*.java" -exec sed -i '' 's/GroupMapperService/TeamMapperService/g' {} +

# Remplacer EnhancedGroupController par EnhancedTeamController
find src/main/java -type f -name "*.java" -exec sed -i '' 's/EnhancedGroupController/EnhancedTeamController/g' {} +

echo "✅ Remplacement terminé!"
echo "🔍 Vérification..."
grep -r "Group" src/main/java/ --include="*.java" | grep -v "// Group" | grep -v "Team"
```

**Sauvegarder et exécuter :**

```bash
chmod +x migrate_group_to_team.sh
./migrate_group_to_team.sh
```

---

### Étape 3 : Vérifications Manuelles

#### 3.1 Vérifier les Imports

```bash
# Chercher les imports GroupDto
grep -r "import.*GroupDto" src/main/java/

# Devrait retourner: aucun résultat
# Si résultats trouvés, les corriger manuellement
```

#### 3.2 Vérifier les Commentaires

```bash
# Chercher "Group" dans les commentaires
grep -r "Group" src/main/java/ --include="*.java"

# Remplacer manuellement dans les commentaires si nécessaire
```

#### 3.3 Vérifier les URLs

```bash
# Chercher /api/groups
grep -r "/api/groups" src/main/java/

# Devrait retourner: aucun résultat
# Si trouvé, remplacer par /api/teams
```

---

## 🧪 Tests de Validation

### Test 1 : Compilation

```bash
mvn clean compile
```

**Attendu** : BUILD SUCCESS

### Test 2 : Recherche de "Group" Résiduel

```bash
# Chercher "Group" sauf dans les cas légitimes
grep -r "Group" src/main/java/ --include="*.java" \
  | grep -v "Team" \
  | grep -v "// group" \
  | grep -v "groupBy"

# Devrait retourner: peu ou pas de résultats
```

### Test 3 : API REST

```bash
# Démarrer l'application
mvn spring-boot:run

# Dans un autre terminal, tester
curl http://localhost:8080/api/teams
# Devrait retourner: JSON (même vide)

curl http://localhost:8080/api/groups
# Devrait retourner: 404 Not Found (si l'ancien endpoint n'existe plus)
```

---

## 📊 Fichiers à Vérifier Manuellement

Après le script automatique, vérifier ces fichiers :

### 1. Contrôleurs

```bash
# Lister tous les contrôleurs
ls -la src/main/java/com/pcagrade/order/controller/

# Vérifier qu'il n'y a plus de *GroupController.java
```

### 2. Services

```bash
# Lister tous les services
ls -la src/main/java/com/pcagrade/order/service/

# Vérifier qu'il n'y a plus de *GroupService.java ou GroupMapper*.java
```

### 3. DTOs

```bash
# Lister tous les DTOs
ls -la src/main/java/com/pcagrade/order/dto/

# Vérifier qu'il n'y a plus de GroupDto.java
```

---

## 🔄 Migration des Tests (Si Applicable)

Si vous avez des tests unitaires :

```bash
# Remplacer dans les tests aussi
find src/test/java -type f -name "*.java" -exec sed -i '' 's/GroupDto/TeamDto/g' {} +
find src/test/java -type f -name "*.java" -exec sed -i '' 's/GroupMapperService/TeamMapperService/g' {} +
find src/test/java -type f -name "*.java" -exec sed -i '' 's/GroupController/GroupController/g' {} +
```

---

## 📝 Checklist Finale

Après migration complète :

- [ ] Aucun fichier nommé `*Group*.java` (sauf si légitime)
- [ ] `TeamDto.java` existe
- [ ] `TeamMapperService.java` existe
- [ ] `EnhancedTeamController.java` existe
- [ ] Aucun import `GroupDto` dans le code
- [ ] Aucun import `GroupMapperService` dans le code
- [ ] `mvn clean compile` : BUILD SUCCESS
- [ ] `curl http://localhost:8080/api/teams` : fonctionne
- [ ] Documentation mise à jour (README, etc.)
- [ ] Tests unitaires passent (si applicable)

---

## 🎯 Exemple de Fichier AVANT/APRÈS

### AVANT (Incohérent)

```java
// EnhancedGroupController.java
@RestController
@RequestMapping("/api/groups")
public class EnhancedGroupController {
    
    private final GroupRepository groupRepository;
    private final GroupMapperService groupMapperService;
    
    @GetMapping
    public ResponseEntity<List<GroupDto.Response>> getAllGroups() {
        // ...
    }
}
```

### APRÈS (Cohérent) ✅

```java
// EnhancedTeamController.java
@RestController
@RequestMapping("/api/teams")
public class EnhancedTeamController {
    
    private final TeamRepository teamRepository;
    private final TeamMapperService teamMapperService;
    
    @GetMapping
    public ResponseEntity<List<TeamDto.Response>> getAllTeams() {
        // ...
    }
}
```

---

## 🚨 Points d'Attention

### Ne PAS Renommer

Ces éléments peuvent contenir "group" mais sont OK :

- `GROUP BY` dans les requêtes SQL (commentaires)
- `.groupBy()` dans les streams Java
- Variables locales nommées `group` (si dans un contexte différent)

### À Renommer

Tout ce qui fait référence à l'ancienne entité "Group" :

- ✅ Noms de classes
- ✅ Noms de variables de classe
- ✅ Noms de méthodes
- ✅ URLs API (`/api/groups` → `/api/teams`)
- ✅ Commentaires de documentation
- ✅ Messages de log

---

## 📱 Impact Frontend (Si Applicable)

Si votre frontend utilise ces endpoints :

### TypeScript/JavaScript

```typescript
// AVANT
const response = await fetch('/api/groups');
const groups: GroupDto[] = await response.json();

// APRÈS
const response = await fetch('/api/teams');
const teams: TeamDto[] = await response.json();
```

### Types TypeScript

```typescript
// Renommer aussi dans le frontend
export interface GroupDto {  // ❌
export interface TeamDto {   // ✅
```

---

## 🎉 Bénéfices de cette Migration

1. ✅ **Cohérence** : Tout utilise "Team"
2. ✅ **Clarté** : Pas de confusion Group vs Team
3. ✅ **Maintenance** : Plus facile à comprendre
4. ✅ **Standards** : Suit la convention établie dès le début
5. ✅ **SQL Safe** : Évite le mot-clé réservé

---

## 🆘 En Cas de Problème

### Erreur : "Cannot find symbol: TeamDto"

**Solution** : Vérifier que `TeamDto.java` existe et que le package est correct

```bash
ls -la src/main/java/com/pcagrade/order/dto/TeamDto.java
grep "package" src/main/java/com/pcagrade/order/dto/TeamDto.java
```

### Erreur : "Duplicate class TeamDto"

**Solution** : Vous avez à la fois `GroupDto.java` et `TeamDto.java`

```bash
# Supprimer l'ancien
rm src/main/java/com/pcagrade/order/dto/GroupDto.java
```

### Tests Échouent

**Solution** : Mettre à jour les tests aussi (voir section tests ci-dessus)

---

## ✅ Résumé

**Ce que vous devez faire :**

1. ✅ Copier les 3 nouveaux fichiers (TeamDto, TeamMapperService, EnhancedTeamController)
2. ✅ Supprimer ou renommer les anciens (GroupDto, GroupMapperService, EnhancedGroupController)
3. ✅ Exécuter le script de remplacement automatique
4. ✅ Compiler et tester
5. ✅ Vérifier manuellement les cas limites

**Temps estimé** : 15-30 minutes  
**Risque** : Faible (changement de noms seulement)  
**Bénéfice** : Code cohérent et maintenable

---

**Maintenant votre code est 100% cohérent avec la convention Team ! 🎯**