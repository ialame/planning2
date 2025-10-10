# 🚀 Installation Complète - Approche Minimale

## ✅ Checklist Rapide

- [ ] Symfony: Créer `PlanningExportController.php`
- [ ] Spring Boot: Créer `MinimalSyncService.java`
- [ ] Spring Boot: Créer `MinimalSyncController.java`
- [ ] Configuration: Vérifier `application-local.properties`
- [ ] Tests: Vérifier les endpoints

---

## 📁 Étape 1: Symfony (5 min)

### 1.1 Créer le Controller

```bash
# Dans votre projet Symfony
mkdir -p src/Controller/Api
```

Créez `src/Controller/Api/PlanningExportController.php` et copiez le contenu de l'artifact.

### 1.2 Tester Symfony

```bash
# Démarrer Symfony
cd /path/to/symfony/project
symfony server:start

# Tester le health check
curl http://localhost:8000/api/planning/export/health
```

**Résultat attendu :**
```json
{
  "status": "ok",
  "service": "Planning Export API",
  "version": "1.0.0"
}
```

### 1.3 Tester l'export des orders

```bash
curl http://localhost:8000/api/planning/export/orders | jq
```

**Résultat attendu :**
```json
{
  "success": true,
  "count": 5,
  "orders": [
    {
      "id": 123,
      "order_number": "CMD-2025-001",
      "delivery_date": "2025-06-15",
      "total_cards": 25,
      "status": "PENDING",
      "priority": 5
    }
  ]
}
```

✅ **Symfony est prêt !**

---

## 📁 Étape 2: Spring Boot (10 min)

### 2.1 Créer les fichiers

```bash
# Dans votre projet Spring Boot
cd /path/to/planning/project
```

**Créer deux fichiers :**

1. `src/main/java/com/pcagrade/order/service/MinimalSyncService.java`
2. `src/main/java/com/pcagrade/order/controller/MinimalSyncController.java`

Copiez le contenu des artifacts correspondants.

### 2.2 Vérifier la configuration

Éditez `src/main/resources/application-local.properties` :

```properties
# URL Symfony - VÉRIFIEZ CETTE URL!
symfony.api.base-url=http://localhost:8000

# Autres configurations (déjà présentes normalement)
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning
spring.jpa.hibernate.ddl-auto=update
```

### 2.3 Ajouter RestTemplate Bean (si nécessaire)

Si vous avez une erreur "Could not autowire RestTemplate", ajoutez dans votre classe de configuration :

```java
// Dans PlanningApplication.java ou une classe @Configuration

@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

### 2.4 Compiler

```bash
mvn clean compile
```

**Résultat attendu :** ✅ BUILD SUCCESS

### 2.5 Démarrer Spring Boot

```bash
mvn spring-boot:run
```

**Vérifiez dans les logs :**
```
Started PlanningApplication in X seconds
```

✅ **Spring Boot is ready!**

---

## 📁 Étape 3: Tests Complets (5 min)

### Test 1: Health Check

```bash
# Test Spring Boot → Symfony connection
curl http://localhost:8080/api/sync/health | jq
```

**Attendu :**
```json
{
  "symfony_api": "connected",
  "symfony_url": "http://localhost:8000",
  "status": "healthy"
}
```

### Test 2: Sync Orders

```bash
# Synchroniser les commandes
curl -X POST http://localhost:8080/api/sync/orders | jq
```

**Attendu :**
```json
{
  "success": true,
  "total_orders": 5,
  "synced_count": 5,
  "duration_ms": 234,
  "message": "Synced 5/5 orders"
}
```

### Test 3: Vérifier en Base

```bash
# Vérifier que les orders sont en base
mysql -u ia -p dev-planning -e "SELECT id, order_number, delivery_date, total_cards FROM \`order\` LIMIT 5;"
```

**Attendu :**
```
+----+--------------+---------------+-------------+
| id | order_number | delivery_date | total_cards |
+----+--------------+---------------+-------------+
|  1 | CMD-2025-001 | 2025-06-15    |          25 |
|  2 | CMD-2025-002 | 2025-06-20    |          30 |
+----+--------------+---------------+-------------+
```

### Test 4: Sync Cards

```bash
# Synchroniser les cartes
curl -X POST http://localhost:8080/api/sync/cards | jq
```

**Attendu :**
```json
{
  "success": true,
  "total_cards": 125,
  "synced_count": 125,
  "duration_ms": 567,
  "message": "Synced 125/125 cards"
}
```

### Test 5: Sync Complet

```bash
# Synchroniser tout (orders + cards)
curl -X POST http://localhost:8080/api/sync/all | jq
```

**Attendu :**
```json
{
  "success": true,
  "orders": {
    "synced_count": 5
  },
  "cards": {
    "synced_count": 125
  },
  "duration_ms": 801,
  "message": "Complete sync successful"
}
```

---

## 🎯 Vérification Complète

### Checklist Finale

- [ ] ✅ Symfony API retourne les orders au format minimal
- [ ] ✅ Symfony API retourne les cards au format minimal
- [ ] ✅ Spring Boot compile sans erreur
- [ ] ✅ Spring Boot démarre sans erreur
- [ ] ✅ Health check fonctionne
- [ ] ✅ Sync orders fonctionne
- [ ] ✅ Sync cards fonctionne
- [ ] ✅ Données présentes en base MySQL

---

## 🐛 Troubleshooting

### Erreur: "Unknown host"

```
Error: Could not connect to Symfony API
```

**Solution :** Vérifiez l'URL dans `application-local.properties` :
```properties
symfony.api.base-url=http://localhost:8000
```

### Erreur: "404 Not Found"

```
Error: 404 on /api/planning/export/orders
```

**Solution :** Vérifiez que le controller Symfony est bien créé :
```bash
ls -la src/Controller/Api/PlanningExportController.php
```

### Erreur: "Order not found for card"

```
⚠️ Order 123 not found for card 456, skipping
```

**Solution :** Synchronisez les orders AVANT les cards :
```bash
curl -X POST http://localhost:8080/api/sync/orders
curl -X POST http://localhost:8080/api/sync/cards
```

### Erreur: "Cannot find symbol: method findBySymfonyOrderId"

Votre `OrderRepository` n'a pas cette méthode. Le service utilise la reflection pour contourner ça automatiquement.

Si vous voulez ajouter la méthode :
```java
// Dans OrderRepository.java
Optional<Order> findBySymfonyOrderId(Long symfonyOrderId);
```

### Erreur de compilation: "Cannot find RestTemplate"

Ajoutez le bean RestTemplate :
```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

---

## 📊 Logs à Surveiller

### Logs Normaux (Succès)

```
🔄 Starting orders sync: 5 orders
✅ Synced order: 1 (Symfony ID: 123)
✅ Synced order: 2 (Symfony ID: 124)
✅ Orders sync completed: 5/5 orders synced

🔄 Starting cards sync: 125 cards
✅ Saved batch: 50 cards
✅ Saved batch: 50 cards
✅ Saved final batch: 25 cards
✅ Cards sync completed: 125/125 cards synced
```

### Logs Problèmes

```
❌ Error syncing order 123: Order must have an id
⚠️ Card 456 has no order_id, skipping
⚠️ Order 789 not found for card 999, skipping
```

---

## 🎉 Résultat Final

Après ces étapes, vous devriez avoir :

1. ✅ **Symfony** qui exporte 8 champs par order
2. ✅ **Spring Boot** qui synchronise sans erreur
3. ✅ **Base de données** avec orders et cards
4. ✅ **0 erreur de compilation**
5. ✅ **Code simple** et maintenable

---

## 🚀 Prochaines Étapes

Maintenant que la synchronisation fonctionne, vous pouvez :

1. **Calculer le planning** basé sur les données synchronisées
2. **Assigner les tâches** aux employés (ROLE_GRADER, ROLE_CERTIFIER, ROLE_SCANNER)
3. **Afficher le planning** dans le frontend

---

## 📞 Support

Si vous rencontrez des problèmes :

1. Vérifiez les logs Spring Boot et Symfony
2. Testez chaque endpoint individuellement
3. Vérifiez la base de données
4. Relisez ce guide étape par étape

**Temps total estimé : 20-30 minutes** ⏱️

---

**Félicitations ! Vous avez implémenté l'approche minimale ! 🎊**