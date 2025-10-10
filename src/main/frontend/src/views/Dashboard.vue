<template>
  <div class="dashboard">
    <!-- En-tête principal -->
    <div class="hero">
      <h1>🎴 Planification Pokémon</h1>
      <p>Système de gestion des commandes de cartes Pokémon</p>
      <div class="timestamp">Dernière mise à jour: {{ currentTime }}</div>
    </div>

    <!-- Statut du système -->
    <div class="status-grid">
      <div class="status-card" :class="{ 'connected': backendConnected }">
        <h3>🔧 Backend</h3>
        <p class="status">{{ backendConnected ? '✅ Connecté' : '❌ Déconnecté' }}</p>
        <button @click="checkBackend" class="btn-refresh" :disabled="checkingBackend">
          {{ checkingBackend ? 'Test...' : 'Tester' }}
        </button>
      </div>

      <div class="status-card">
        <h3>📊 Statistiques</h3>
        <div class="stats" v-if="!loadingStats">
          <p>{{ stats.ordersCount || 0 }} Commandes</p>
          <p>{{ stats.employeesCount || 0 }} Employés</p>
          <p>{{ stats.planningCount || 0 }} Planifications</p>
        </div>
        <div v-else class="loading">Chargement...</div>
      </div>

      <div class="status-card">
        <h3>⚡ Actions Rapides</h3>
        <div class="actions">
          <button @click="generatePlanning" class="btn-primary" :disabled="generatingPlanning">
            {{ generatingPlanning ? 'Génération...' : 'Générer Planning' }}
          </button>
          <button @click="refreshStats" class="btn-secondary" :disabled="loadingStats">
            {{ loadingStats ? 'Actualisation...' : 'Actualiser' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Aperçu des données -->
    <div class="data-preview">
      <h2>📋 Aperçu des Données</h2>

      <!-- Employés -->
      <div class="preview-section">
        <h3>👥 Employés ({{ employees.length }})</h3>
        <div class="preview-list">
          <div v-for="employee in employees.slice(0, 3)" :key="employee.id" class="preview-item">
            <span class="item-name">{{ employee.fullName }}</span>
            <span class="item-detail">{{ employee.workHoursPerDay }}h/jour</span>
            <span :class="['item-status', employee.active ? 'active' : 'inactive']">
              {{ employee.active ? 'Actif' : 'Inactif' }}
            </span>
          </div>
          <div v-if="employees.length > 3" class="preview-more">
            +{{ employees.length - 3 }} autres employés
          </div>
        </div>
      </div>

      <!-- Commandes récentes -->
      <div class="preview-section">
        <h3>📦 Commandes Récentes ({{ orders.length }})</h3>
        <div class="preview-list">
          <div v-for="order in orders.slice(0, 5)" :key="order.id" class="preview-item">
            <span class="item-name">{{ order.orderNumber }}</span>
            <span class="item-detail">{{ order.cardCount }} cartes</span>
            <span :class="['item-priority', order.priority.toLowerCase()]">
              {{ order.priority }}
            </span>
          </div>
          <div v-if="orders.length > 5" class="preview-more">
            +{{ orders.length - 5 }} autres commandes
          </div>
        </div>
      </div>

      <!-- Statistiques par priorité -->
      <div class="preview-section" v-if="stats.ordersByPriority">
        <h3>🏆 Commandes par Priorité</h3>
        <div class="priority-stats">
          <div v-for="(count, priority) in stats.ordersByPriority" :key="priority"
               :class="['priority-item', priority.toLowerCase()]">
            <span class="priority-label">{{ priority }}</span>
            <span class="priority-count">{{ count }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <div class="navigation-grid">
      <h2>🚀 Navigation</h2>
      <div class="nav-cards">
        <router-link to="/orders" class="nav-card">
          <div class="card-icon">📦</div>
          <h3>Commandes</h3>
          <p>Gérer les commandes Pokémon</p>
          <div class="card-count">{{ orders.length }} commandes</div>
        </router-link>

        <router-link to="/employees" class="nav-card">
          <div class="card-icon">👥</div>
          <h3>Employés</h3>
          <p>Gérer l'équipe</p>
          <div class="card-count">{{ employees.length }} employés</div>
        </router-link>

        <router-link to="/planning" class="nav-card">
          <div class="card-icon">📅</div>
          <h3>Planning</h3>
          <p>Voir et modifier les plannings</p>
          <div class="card-count">{{ stats.planningCount || 0 }} planifications</div>
        </router-link>
        <router-link to="/sync" class="nav-card">
          <div class="card-icon">🔄</div>
          <h3>Data Sync</h3>
        </router-link>

      </div>
    </div>

    <!-- Messages système -->
    <div v-if="systemMessage" :class="['system-message', systemMessage.type]">
      <h4>{{ systemMessage.title }}</h4>
      <p>{{ systemMessage.message }}</p>
      <button @click="systemMessage = null" class="close-btn">×</button>
    </div>
  </div>
</template>

<script>
import ApiService from '../services/api.ts'

export default {
  name: 'Dashboard',

  data() {
    return {
      // État du système
      backendConnected: false,
      checkingBackend: false,
      loadingStats: false,
      generatingPlanning: false,

      // Données
      currentTime: new Date().toLocaleString('fr-FR'),
      stats: {},
      employees: [],
      orders: [],
      planning: [],

      // Messages
      systemMessage: null
    }
  },

  async mounted() {
    console.log('🚀 Dashboard mounted - Chargement des données...')

    // Démarrage automatique
    await this.checkBackend()
    await this.loadAllData()

    // Mise à jour de l'heure toutes les minutes
    this.timeInterval = setInterval(() => {
      this.currentTime = new Date().toLocaleString('fr-FR')
    }, 60000)
  },

  beforeUnmount() {
    if (this.timeInterval) {
      clearInterval(this.timeInterval)
    }
  },

  methods: {
    /**
     * 🏥 Vérifier la connexion backend
     */
    async checkBackend() {
      this.checkingBackend = true
      try {
        this.backendConnected = await ApiService.healthCheck()

        if (this.backendConnected) {
          this.showMessage('success', 'Connexion OK', 'Backend disponible')
        } else {
          this.showMessage('error', 'Connexion échouée', 'Vérifiez que le serveur Spring Boot tourne sur le port 8080')
        }
      } catch (error) {
        this.backendConnected = false
        this.showMessage('error', 'Erreur de connexion', error.message)
      } finally {
        this.checkingBackend = false
      }
    },

    /**
     * 📊 Charger toutes les données
     */
    async loadAllData() {
      this.loadingStats = true
      try {
        console.log('📊 Chargement des données complètes...')

        // Charger en parallèle
        const [employees, orders, stats] = await Promise.all([
          ApiService.getEmployees(),
          ApiService.getOrdersSinceJune2025(), // Commandes depuis juin 2025
          ApiService.getStats()
        ])

        this.employees = employees
        this.orders = orders
        this.stats = stats

        console.log('✅ Données chargées:', {
          employees: employees.length,
          orders: orders.length,
          stats
        })

        this.showMessage('success', 'Données chargées',
          `${employees.length} employés, ${orders.length} commandes`)

      } catch (error) {
        console.error('❌ Erreur chargement données:', error)
        this.showMessage('error', 'Erreur de chargement', error.message)
      } finally {
        this.loadingStats = false
      }
    },

    /**
     * 🔄 Actualiser les statistiques
     */
    async refreshStats() {
      await this.loadAllData()
    },

    /**
     * 🚀 Générer la planification
     */
    async generatePlanning() {
      this.generatingPlanning = true
      try {
        console.log('🚀 Génération de la planification...')

        const result = await ApiService.generatePlanning({
          startDate: '2025-06-01',
          timePerCard: 3,
          cleanFirst: false
        })

        console.log('✅ Planification générée:', result)

        if (result.success) {
          this.showMessage('success', 'Planification générée',
            result.message || 'Planification créée avec succès')

          // Recharger les données
          await this.loadAllData()
        } else {
          this.showMessage('error', 'Erreur de planification',
            result.message || 'Impossible de générer la planification')
        }

      } catch (error) {
        console.error('❌ Erreur génération:', error)
        this.showMessage('error', 'Erreur de génération', error.message)
      } finally {
        this.generatingPlanning = false
      }
    },

    /**
     * 💬 Afficher un message système
     */
    showMessage(type, title, message) {
      this.systemMessage = { type, title, message }

      // Auto-fermeture après 5 secondes pour les succès
      if (type === 'success') {
        setTimeout(() => {
          this.systemMessage = null
        }, 5000)
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.hero {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
}

.hero h1 {
  font-size: 2.5rem;
  margin: 0 0 10px 0;
}

.timestamp {
  opacity: 0.8;
  font-size: 0.9rem;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.status-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-left: 4px solid #ddd;
}

.status-card.connected {
  border-left-color: #28a745;
}

.status-card h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.status {
  font-weight: bold;
  margin-bottom: 10px;
}

.stats p, .actions {
  margin: 5px 0;
}

.btn-refresh, .btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-right: 10px;
}

.btn-refresh {
  background: #6c757d;
  color: white;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-secondary {
  background: #28a745;
  color: white;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.data-preview {
  margin-bottom: 40px;
}

.preview-section {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
}

.item-name {
  font-weight: bold;
}

.item-status.active {
  color: #28a745;
}

.item-status.inactive {
  color: #dc3545;
}

.item-priority.urgent {
  color: #dc3545;
  font-weight: bold;
}

.item-priority.high {
  color: #fd7e14;
  font-weight: bold;
}

.item-priority.medium {
  color: #ffc107;
  font-weight: bold;
}

.item-priority.low {
  color: #28a745;
}

.priority-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}

.priority-item {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  border-radius: 4px;
  font-weight: bold;
}

.priority-item.urgent {
  background: #f8d7da;
  color: #721c24;
}

.priority-item.high {
  background: #ffeaa7;
  color: #856404;
}

.priority-item.medium {
  background: #fff3cd;
  color: #856404;
}

.priority-item.low {
  background: #d4edda;
  color: #155724;
}

.navigation-grid {
  margin-bottom: 40px;
}

.nav-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.nav-card {
  background: white;
  padding: 30px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  text-align: center;
}

.nav-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.card-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.card-count {
  color: #6c757d;
  font-size: 0.9rem;
  margin-top: 10px;
}

.system-message {
  position: fixed;
  bottom: 20px;
  right: 20px;
  max-width: 400px;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  z-index: 1000;
}

.system-message.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.system-message.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: inherit;
}

.loading {
  color: #6c757d;
  font-style: italic;
}

.preview-more {
  color: #6c757d;
  font-style: italic;
  text-align: center;
  padding: 10px;
}
</style>
