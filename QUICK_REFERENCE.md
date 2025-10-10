# ⚡ Quick Reference - Commandes Essentielles

## 🚀 Démarrage Rapide

```bash
# 1. Démarrer Symfony
cd /path/to/symfony
symfony server:start

# 2. Démarrer Spring Boot  
cd /path/to/planning
mvn spring-boot:run

# 3. Synchroniser tout
curl -X POST http://localhost:8080/api/sync/all
```

---

## 📡 API Endpoints

### Symfony Export API

```bash
# Health check
curl http://localhost:8000/api/planning/export/health

# Get orders (format minimal)
curl http://localhost:8000/api/planning/export/orders

# Get cards (format minimal)
curl http://localhost:8000/api/planning/export/cards

# Get cards for specific order
curl http://localhost:8000/api/planning/export/order/123/cards
```

### Spring Boot Sync API

```bash
# Health check (teste la connexion Symfony)
curl http://localhost:8080/api/sync/health

# Sync orders only
curl -X POST http://localhost:8080/api/sync/orders

# Sync cards only
curl -X POST http://localhost:8080/api/sync/cards

# Sync everything (orders + cards)
curl -X POST http://localhost:8080/api/sync/all

# Sync cards for specific order
curl -X POST http://localhost:8080/api/sync/order/123/cards
```

---

## 🔍 Vérification Base de Données

```bash
# Connexion MySQL
mysql -u ia -p dev-planning

# Voir les tables
SHOW TABLES;

# Compter les orders
SELECT COUNT(*) FROM `order`;

# Voir les orders récents
SELECT id, order_number, delivery_date, total_cards, status 
FROM `order` 
ORDER BY delivery_date ASC 
LIMIT 10;

# Compter les cards
SELECT COUNT(*) FROM card;

# Voir les cards avec leur statut
SELECT c.id, c.card_name, c.processing_status,
       c.grading_completed, c.certification_completed,
       c.scanning_completed, c.packaging_completed
FROM card c
LIMIT 10;

# Statistiques par order
SELECT o.order_number, o.total_cards,
       COUNT(c.id) as cards_count,
       SUM(c.grading_completed) as graded,
       SUM(c.certification_completed) as certified,
       SUM(c.scanning_completed) as scanned
FROM `order` o
LEFT JOIN card c ON c.order_id = o.id
GROUP BY o.id
ORDER BY o.delivery_date ASC;
```

---

## 🐛 Debugging

### Voir les logs Spring Boot

```bash
# En temps réel
tail -f target/logs/spring.log

# Chercher les erreurs
grep "ERROR" target/logs/spring.log

# Chercher les syncs
grep "sync" target/logs/spring.log
```

### Tester la connexion Symfony

```bash
# Test simple
curl -I http://localhost:8000/api/planning/export/health

# Avec timeout
curl --max-time 5 http://localhost:8000/api/planning/export/health

# Verbose pour debug
curl -v http://localhost:8000/api/planning/export/health
```

### Vérifier les ports

```bash
# Voir qui écoute sur le port 8000 (Symfony)
lsof -i :8000

# Voir qui écoute sur le port 8080 (Spring Boot)
lsof -i :8080

# Voir qui écoute sur le port 3306 (MySQL)
lsof -i :3306
```

---

## 🔧 Maintenance

### Resynchroniser tout

```bash
# 1. Vider les tables (ATTENTION: efface les données!)
mysql -u ia -p dev-planning << EOF
DELETE FROM card;
DELETE FROM \`order\`;
EOF

# 2. Resynchroniser
curl -X POST http://localhost:8080/api/sync/all
```

### Nettoyer et recompiler

```bash
# Spring Boot
mvn clean install
mvn spring-boot:run

# Symfony (si nécessaire)
php bin/console cache:clear
symfony server:start
```

### Recréer la base de données

```bash
mysql -u ia -p << EOF
DROP DATABASE \`dev-planning\`;
CREATE DATABASE \`dev-planning\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EOF

# Puis redémarrer Spring Boot (Hibernate recrée les tables)
mvn spring-boot:run
```

---

## 📊 Monitoring

### Statistiques de sync

```bash
# Compter les orders par statut
mysql -u ia -p dev-planning << EOF
SELECT status, COUNT(*) as count 
FROM \`order\` 
GROUP BY status;
EOF

# Compter les cards par statut de traitement
mysql -u ia -p dev-planning << EOF
SELECT processing_status, COUNT(*) as count 
FROM card 
GROUP BY processing_status;
EOF

# Progression du traitement
mysql -u ia -p dev-planning << EOF
SELECT 
    COUNT(*) as total_cards,
    SUM(grading_completed) as graded,
    SUM(certification_completed) as certified,
    SUM(scanning_completed) as scanned,
    SUM(packaging_completed) as packaged,
    ROUND(SUM(grading_completed) * 100.0 / COUNT(*), 2) as percent_graded,
    ROUND(SUM(packaging_completed) * 100.0 / COUNT(*), 2) as percent_completed
FROM card;
EOF
```

---

## 🎯 Tests Rapides

### Test complet en une commande

```bash
# Health check + sync + vérification
(
  echo "1. Health check..."
  curl -s http://localhost:8080/api/sync/health | jq -r '.status'
  
  echo "2. Syncing orders..."
  curl -s -X POST http://localhost:8080/api/sync/orders | jq -r '.message'
  
  echo "3. Syncing cards..."
  curl -s -X POST http://localhost:8080/api/sync/cards | jq -r '.message'
  
  echo "4. Checking database..."
  mysql -u ia -p dev-planning -e "SELECT COUNT(*) as orders FROM \`order\`; SELECT COUNT(*) as cards FROM card;" -t
)
```

### Script de test automatisé

Créez `test-sync.sh` :

```bash
#!/bin/bash

echo "🧪 Testing Sync System..."

# Test Symfony
echo -n "Symfony API: "
if curl -sf http://localhost:8000/api/planning/export/health > /dev/null; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

# Test Spring Boot
echo -n "Spring Boot: "
if curl -sf http://localhost:8080/api/sync/health > /dev/null; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

# Sync orders
echo -n "Sync orders: "
RESULT=$(curl -sf -X POST http://localhost:8080/api/sync/orders | jq -r '.success')
if [ "$RESULT" = "true" ]; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

# Sync cards
echo -n "Sync cards: "
RESULT=$(curl -sf -X POST http://localhost:8080/api/sync/cards | jq -r '.success')
if [ "$RESULT" = "true" ]; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

echo ""
echo "✅ All tests passed!"
```

Utilisation :
```bash
chmod +x test-sync.sh
./test-sync.sh
```

---

## 💡 Tips

### Formatter le JSON avec jq

```bash
# Au lieu de:
curl http://localhost:8080/api/sync/health

# Utilisez:
curl http://localhost:8080/api/sync/health | jq

# Ou pour extraire un champ:
curl http://localhost:8080/api/sync/health | jq -r '.status'
```

### Surveiller les syncs en temps réel

```bash
# Dans un terminal, surveillez les logs
tail -f target/logs/spring.log | grep -E "(sync|ERROR)"

# Dans un autre terminal, lancez le sync
curl -X POST http://localhost:8080/api/sync/all
```

### Benchmark de performance

```bash
# Mesurer le temps de sync
time curl -X POST http://localhost:8080/api/sync/all

# Ou avec plus de détails
curl -w "\nTime: %{time_total}s\n" \
     -X POST http://localhost:8080/api/sync/all
```

---

## 🔑 Variables d'Environnement Utiles

```bash
# Pour Spring Boot
export SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/dev-planning
export SPRING_DATASOURCE_USERNAME=ia
export SPRING_DATASOURCE_PASSWORD=yourpassword
export SYMFONY_API_BASE_URL=http://localhost:8000

# Pour Symfony
export DATABASE_URL="mysql://ia:yourpassword@127.0.0.1:3306/symfony_db"
export APP_ENV=dev
```

---

## 📞 URLs Importantes

```
Symfony API:        http://localhost:8000
Planning API:       http://localhost:8080
Health Check:       http://localhost:8080/api/sync/health
Swagger (si activé): http://localhost:8080/swagger-ui.html
```

---

**Gardez cette référence à portée de main ! 🚀**