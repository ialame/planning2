# Vue 3 Frontend Setup Guide

Guide d'installation et de configuration du frontend Vue 3 avec TypeScript pour le système de planification Pokemon Card.

## 📋 Prérequis

- Node.js 16+ et npm/yarn/pnpm
- Backend Spring Boot en cours d'exécution sur `http://localhost:8080`

## 🚀 Installation Rapide

### 1. Créer un nouveau projet Vue 3

```bash
# Avec npm
npm create vue@latest

# Suivre les prompts:
# ✅ Project name: pokemon-card-planning-frontend
# ✅ Add TypeScript? Yes
# ✅ Add JSX Support? No
# ❌ Add Vue Router? (Optional)
# ❌ Add Pinia? (Optional)
# ❌ Add Vitest? (Optional)
# ❌ Add Playwright? (Optional)
# ✅ Add ESLint? Yes
# ✅ Add Prettier? Yes

cd pokemon-card-planning-frontend
npm install
```

### 2. Installer les dépendances nécessaires

```bash
# Lucide Icons pour Vue 3
npm install lucide-vue-next

# TailwindCSS
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

### 3. Configurer TailwindCSS

**tailwind.config.js**:
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

**src/assets/main.css**:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### 4. Structure du projet

Créer la structure suivante dans `src/`:

```
src/
├── assets/
│   └── main.css
├── components/
│   └── PlanningDashboard.vue
├── types/
│   └── index.ts
├── services/
│   └── api.service.ts
├── App.vue
└── main.ts
```

### 5. Copier les fichiers

**src/components/PlanningDashboard.vue**:
- Copier le contenu de l'artifact "PlanningDashboard.vue"

**src/types/index.ts**:
- Copier le contenu de l'artifact "types.ts"

**src/services/api.service.ts**:
- Copier le contenu de l'artifact "api.service.ts"

### 6. Mettre à jour App.vue

```vue
<template>
  <PlanningDashboard />
</template>

<script setup lang="ts">
import PlanningDashboard from './components/PlanningDashboard.vue';
</script>
```

### 7. Mettre à jour main.ts

```typescript
import { createApp } from 'vue'
import App from './App.vue'
import './assets/main.css'

createApp(App).mount('#app')
```

### 8. Configuration de l'environnement

Créer un fichier `.env` à la racine:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 9. Lancer l'application

```bash
npm run dev
```

Accéder à: `http://localhost:5173`

## 📦 Structure Complète du Projet

```
pokemon-card-planning-frontend/
├── node_modules/
├── public/
├── src/
│   ├── assets/
│   │   └── main.css
│   ├── components/
│   │   └── PlanningDashboard.vue
│   ├── services/
│   │   └── api.service.ts
│   ├── types/
│   │   └── index.ts
│   ├── App.vue
│   └── main.ts
├── .env
├── .gitignore
├── index.html
├── package.json
├── postcss.config.js
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

## 🎨 Personnalisation

### Changer l'URL de l'API

Modifier `.env`:
```env
VITE_API_BASE_URL=https://votre-api.com/api
```

### Ajouter de nouvelles fonctionnalités

1. **Nouveau composant**:
```vue
<!-- src/components/NewComponent.vue -->
<template>
  <div>{{ message }}</div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const message = ref('Hello World');
</script>
```

2. **Nouveau service API**:
```typescript
// Dans api.service.ts
export const customApi = {
  async getCustomData(): Promise<ApiResponse<any>> {
    return fetchApi<any>('/custom-endpoint');
  }
};
```

## 🔧 Configuration TypeScript

Le projet utilise TypeScript avec des types stricts. Configuration dans `tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  }
}
```

## 🎯 Fonctionnalités du Dashboard

### Vue d'ensemble (Overview)
- Affichage de la charge de travail de chaque employé
- Barre de progression avec pourcentage d'utilisation
- Statistiques en temps réel

### Affectations (Assignments)
- Tableau complet de toutes les affectations
- Filtrage et tri
- Statuts colorés

### Employés (Employees)
- Liste des employés avec leurs rôles
- Capacité et charge de travail
- Taux d'utilisation

### Commandes (Orders)
- Commandes en attente triées par priorité
- Date limite de livraison
- Nombre de cartes par commande

## 🧪 Tests

Pour ajouter des tests:

```bash
npm install -D vitest @vue/test-utils
```

## 📱 Build pour Production

```bash
npm run build
```

Les fichiers seront générés dans `dist/`

## 🔍 Débogage

### Erreur CORS

Si vous rencontrez des erreurs CORS, ajouter dans `application.properties`:

```properties
spring.web.cors.allowed-origins=http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
```

### Backend non accessible

Vérifier que:
1. Le backend Spring Boot est en cours d'exécution
2. L'URL dans `.env` est correcte
3. Aucun firewall ne bloque le port 8080

### Types TypeScript

Si TypeScript se plaint des types, exécuter:

```bash
npm install --save-dev @types/node
```

## 📚 Ressources

- [Vue 3 Documentation](https://vuejs.org/)
- [TypeScript Vue Plugin](https://github.com/vuejs/language-tools)
- [Vite Documentation](https://vitejs.dev/)
- [TailwindCSS Documentation](https://tailwindcss.com/)
- [Lucide Icons](https://lucide.dev/)

## 🆘 Support

Pour toute question ou problème, vérifier:
1. Les logs du navigateur (F12)
2. Les logs du backend Spring Boot
3. La configuration de l'environnement

## 🚀 Prochaines Étapes

1. ✅ Installation complète
2. ✅ Configuration de base
3. 🔄 Ajouter Vue Router pour la navigation
4. 🔄 Ajouter Pinia pour la gestion d'état
5. 🔄 Implémenter l'authentification
6. 🔄 Ajouter des tests unitaires
7. 🔄 Optimiser les performances

---

**Bon développement ! 🎉**