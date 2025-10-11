import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { RefreshCw, CheckCircle, AlertCircle, Clock, Package, Scan, FileCheck, Award } from 'lucide-react';

interface SyncStats {
total_cards: number;
incomplete_cards: number;
needs_grading: number;
needs_certification: number;
needs_scanning: number;
needs_packaging: number;
completion_percentage: number;
}

interface SyncResponse {
success: boolean;
total_cards?: number;
synced_count?: number;
error_count?: number;
duration_ms?: number;
message: string;
stats?: SyncStats;
error?: string;
}

interface HealthStatus {
symfony_api: string;
status: string;
error?: string;
}

const CardSyncDashboard: React.FC = () => {
const [stats, setStats] = useState<SyncStats | null>(null);
const [health, setHealth] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [syncResult, setSyncResult] = useState<SyncResponse | null>(null);
    const [lastSyncTime, setLastSyncTime] = useState<Date | null>(null);

      const API_BASE = 'http://localhost:8080/api/sync';

      // Load initial data
      useEffect(() => {
      checkHealth();
      loadStats();
      }, []);

      const checkHealth = async () => {
      try {
      const response = await fetch(`${API_BASE}/health`);
      const data = await response.json();
      setHealth(data);
      } catch (error) {
      console.error('Health check failed:', error);
      setHealth({
      symfony_api: 'disconnected',
      status: 'unhealthy',
      error: 'Failed to connect'
      });
      }
      };

      const loadStats = async () => {
      try {
      const response = await fetch(`${API_BASE}/stats`);
      const data = await response.json();
      if (data.card_stats) {
      setStats(data.card_stats);
      }
      } catch (error) {
      console.error('Failed to load stats:', error);
      }
      };

      const syncCards = async (limit?: number) => {
      setLoading(true);
      setSyncResult(null);

      try {
      const url = limit
      ? `${API_BASE}/cards?limit=${limit}`
      : `${API_BASE}/cards`;

      const response = await fetch(url, { method: 'POST' });
      const data: SyncResponse = await response.json();

      setSyncResult(data);
      setLastSyncTime(new Date());

      if (data.stats) {
      setStats(data.stats);
      } else {
      // Reload stats if not included
      await loadStats();
      }
      } catch (error) {
      console.error('Sync failed:', error);
      setSyncResult({
      success: false,
      message: 'Sync failed',
      error: String(error)
      });
      } finally {
      setLoading(false);
      }
      };

      const syncAll = async () => {
      setLoading(true);
      setSyncResult(null);

      try {
      const response = await fetch(`${API_BASE}/all`, { method: 'POST' });
      const data = await response.json();

      setSyncResult({
      success: data.success,
      message: data.message,
      synced_count: data.cards?.synced_count,
      total_cards: data.cards?.total_cards
      });

      setLastSyncTime(new Date());
      await loadStats();
      } catch (error) {
      console.error('Full sync failed:', error);
      setSyncResult({
      success: false,
      message: 'Full sync failed',
      error: String(error)
      });
      } finally {
      setLoading(false);
      }
      };

      const formatDuration = (ms: number) => {
      if (ms < 1000) return `${ms}ms`;
      if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
      return `${(ms / 60000).toFixed(1)}min`;
      };

      const getHealthColor = () => {
      if (!health) return 'bg-gray-500';
      return health.status === 'healthy' ? 'bg-green-500' : 'bg-red-500';
      };

      return (
      <div className="p-6 space-y-6 bg-gray-50 min-h-screen">
        {/* Header */}
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Card Synchronization</h1>
            <p className="text-gray-600 mt-1">Sync Pokemon cards from Symfony API</p>
          </div>

          {/* Health Indicator */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <div className={`w-3 h-3 rounded-full ${getHealthColor()}`}></div>
              <span className="text-sm font-medium">
              {health?.symfony_api === 'connected' ? 'API Connected' : 'API Disconnected'}
            </span>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={checkHealth}
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {/* Health Alert */}
        {health?.status === 'unhealthy' && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Cannot connect to Symfony API. Please check if the API is running at the configured URL.
          </AlertDescription>
        </Alert>
        )}

        {/* Sync Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Synchronization Actions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => syncCards()}
              disabled={loading || health?.status !== 'healthy'}
              className="flex items-center gap-2"
              >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              Sync All Cards
              </Button>

              <Button
                onClick={() => syncCards(100)}
              disabled={loading || health?.status !== 'healthy'}
              variant="outline"
              >
              Sync 100 Cards (Test)
              </Button>

              <Button
                onClick={syncAll}
                disabled={loading || health?.status !== 'healthy'}
              variant="secondary"
              >
              Full Sync (Orders + Cards)
              </Button>

              <Button
                onClick={loadStats}
                disabled={loading}
                variant="ghost"
              >
                Refresh Stats
              </Button>
            </div>

            {lastSyncTime && (
            <p className="text-sm text-gray-600">
              Last sync: {lastSyncTime.toLocaleString()}
            </p>
            )}
          </CardContent>
        </Card>

        {/* Sync Result */}
        {syncResult && (
        <Alert variant={syncResult.success ? 'default' : 'destructive'}>
        {syncResult.success ? (
        <CheckCircle className="h-4 w-4 text-green-600" />
        ) : (
        <AlertCircle className="h-4 w-4" />
        )}
        <AlertDescription>
          <div className="font-medium">{syncResult.message}</div>
          {syncResult.success && syncResult.synced_count !== undefined && (
          <div className="text-sm mt-1">
            Synced {syncResult.synced_count} / {syncResult.total_cards} cards
            {syncResult.duration_ms && ` in ${formatDuration(syncResult.duration_ms)}`}
            {syncResult.error_count !== undefined && syncResult.error_count > 0 && (
            <span className="text-orange-600 ml-2">
                    ({syncResult.error_count} errors)
                  </span>
            )}
          </div>
          )}
          {syncResult.error && (
          <div className="text-sm mt-1 text-red-600">{syncResult.error}</div>
          )}
        </AlertDescription>
        </Alert>
        )}

        {/* Statistics Overview */}
        {stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total Cards */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Total Cards</p>
                  <p className="text-3xl font-bold text-gray-900">{stats.total_cards}</p>
                </div>
                <Package className="w-8 h-8 text-blue-500" />
              </div>
            </CardContent>
          </Card>

          {/* Completion Rate */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Completion Rate</p>
                  <p className="text-3xl font-bold text-green-600">
                    {stats.completion_percentage.toFixed(1)}%
                  </p>
                </div>
                <CheckCircle className="w-8 h-8 text-green-500" />
              </div>
              <div className="mt-2">
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-green-500 h-2 rounded-full transition-all duration-500"
                    style={{ width: `${stats.completion_percentage}%` }}
                  ></div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Incomplete Cards */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Incomplete</p>
                  <p className="text-3xl font-bold text-orange-600">{stats.incomplete_cards}</p>
                </div>
                <Clock className="w-8 h-8 text-orange-500" />
              </div>
            </CardContent>
          </Card>

          {/* Completed Cards */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Completed</p>
                  <p className="text-3xl font-bold text-green-600">
                    {stats.total_cards - stats.incomplete_cards}
                  </p>
                </div>
                <CheckCircle className="w-8 h-8 text-green-500" />
              </div>
            </CardContent>
          </Card>
        </div>
        )}

        {/* Task Breakdown */}
        {stats && (
        <Card>
          <CardHeader>
            <CardTitle>Tasks Pending by Type</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {/* Grading */}
              <div className="flex items-center gap-3 p-4 bg-blue-50 rounded-lg">
                <Award className="w-6 h-6 text-blue-600" />
                <div>
                  <p className="text-sm font-medium text-gray-600">Grading</p>
                  <p className="text-2xl font-bold text-blue-900">{stats.needs_grading}</p>
                  <p className="text-xs text-gray-500">
                    ~{(stats.needs_grading * 3).toFixed(0)} minutes
                  </p>
                </div>
              </div>

              {/* Certification */}
              <div className="flex items-center gap-3 p-4 bg-purple-50 rounded-lg">
                <FileCheck className="w-6 h-6 text-purple-600" />
                <div>
                  <p className="text-sm font-medium text-gray-600">Certification</p>
                  <p className="text-2xl font-bold text-purple-900">{stats.needs_certification}</p>
                  <p className="text-xs text-gray-500">
                    ~{(stats.needs_certification * 3).toFixed(0)} minutes
                  </p>
                </div>
              </div>

              {/* Scanning */}
              <div className="flex items-center gap-3 p-4 bg-green-50 rounded-lg">
                <Scan className="w-6 h-6 text-green-600" />
                <div>
                  <p className="text-sm font-medium text-gray-600">Scanning</p>
                  <p className="text-2xl font-bold text-green-900">{stats.needs_scanning}</p>
                  <p className="text-xs text-gray-500">
                    ~{(stats.needs_scanning * 3).toFixed(0)} minutes
                  </p>
                </div>
              </div>

              {/* Packaging */}
              <div className="flex items-center gap-3 p-4 bg-orange-50 rounded-lg">
                <Package className="w-6 h-6 text-orange-600" />
                <div>
                  <p className="text-sm font-medium text-gray-600">Packaging</p>
                  <p className="text-2xl font-bold text-orange-900">{stats.needs_packaging}</p>
                  <p className="text-xs text-gray-500">
                    ~{(stats.needs_packaging * 3).toFixed(0)} minutes
                  </p>
                </div>
              </div>
            </div>

            {/* Total Estimated Time */}
            <div className="mt-6 p-4 bg-gray-100 rounded-lg">
              <div className="flex justify-between items-center">
                <span className="font-medium text-gray-700">Total Estimated Processing Time:</span>
                <span className="text-xl font-bold text-gray-900">
                  {((stats.needs_grading + stats.needs_certification +
                     stats.needs_scanning + stats.needs_packaging) * 3).toFixed(0)} minutes
                  {' '}
                  <span className="text-sm text-gray-600">
                    (~{(((stats.needs_grading + stats.needs_certification +
                          stats.needs_scanning + stats.needs_packaging) * 3) / 60).toFixed(1)} hours)
                  </span>
                </span>
              </div>
            </div>
          </CardContent>
        </Card>
        )}

        {/* Instructions */}
        <Card>
          <CardHeader>
            <CardTitle>Quick Guide</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-start gap-3">
              <Badge variant="outline" className="mt-1">1</Badge>
              <div>
                <p className="font-medium">Check API Health</p>
                <p className="text-sm text-gray-600">
                  Ensure the Symfony API is connected (green indicator above)
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Badge variant="outline" className="mt-1">2</Badge>
              <div>
                <p className="font-medium">Test with 100 Cards</p>
                <p className="text-sm text-gray-600">
                  Start with a small batch to verify everything works correctly
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Badge variant="outline" className="mt-1">3</Badge>
              <div>
                <p className="font-medium">Sync All Cards</p>
                <p className="text-sm text-gray-600">
                  Once tested, sync all cards from the Symfony database
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Badge variant="outline" className="mt-1">4</Badge>
              <div>
                <p className="font-medium">Monitor Progress</p>
                <p className="text-sm text-gray-600">
                  Watch the statistics update to see processing status and completion rate
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
      );
      };

      export default CardSyncDashboard;
