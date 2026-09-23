// =========================================================
// AquaNexus — Centralized Real-Time State & Data Layer
// Single source of truth for all pages: Dashboard, Consumption,
// Alerts, Reports, AI Insights, and Top Navigation Bell Badge.
// Compatible with both Localhost and Render Cloud Deployments.
// =========================================================

window.AQUANEXUS_API_BASE = window.AQUANEXUS_API_BASE || (
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080'
    : 'https://final-year-project-as9l.onrender.com'
);

window.AppState = (() => {
  const HOST = window.AQUANEXUS_API_BASE;
  const listeners = {};

  let state = {
    records: [],
    summary: null,
    alerts: [],
    alertsSummary: { totalOpen: 0, openCritical: 0, openWarning: 0, totalAlerts: 0, resolvedCount: 0 },
    recommendations: null,
    activeTimeframe: 30,
    isLoading: false,
    error: null
  };

  function subscribe(event, callback) {
    if (!listeners[event]) listeners[event] = [];
    listeners[event].push(callback);
    return () => {
      listeners[event] = listeners[event].filter(cb => cb !== callback);
    };
  }

  function emit(event, data) {
    if (listeners[event]) {
      listeners[event].forEach(cb => {
        try { cb(data, state); } catch (err) { console.error(`Error in subscriber for ${event}:`, err); }
      });
    }
  }

  // Clear legacy mock/seeded keys from localStorage
  function cleanLegacyStorage() {
    try {
      localStorage.removeItem('aquanexus_local_records');
      localStorage.removeItem('aquanexus_mock_initialized');
    } catch (e) {}
  }

  // Fetch Dashboard Summary
  async function fetchSummary(days = state.activeTimeframe) {
    state.activeTimeframe = days;
    try {
      const res = await fetch(`${HOST}/api/water/dashboard-summary?days=${days}`);
      if (res.ok) {
        state.summary = await res.json();
        emit('summaryUpdated', state.summary);
        return state.summary;
      }
    } catch (err) {
      console.warn('Backend unavailable during fetchSummary:', err);
    }
    return state.summary;
  }

  // Fetch Records
  async function fetchRecords(department = '') {
    try {
      const query = department && department !== 'ALL' ? `?department=${encodeURIComponent(department)}` : '';
      const res = await fetch(`${HOST}/api/water/records${query}`);
      if (res.ok) {
        state.records = await res.json();
        emit('recordsUpdated', state.records);
        return state.records;
      }
    } catch (err) {
      console.warn('Backend unavailable during fetchRecords:', err);
    }
    return state.records;
  }

  // Fetch Alerts Summary & Top Badge Count
  async function fetchAlertsSummary() {
    try {
      const res = await fetch(`${HOST}/api/alerts/summary`);
      if (res.ok) {
        state.alertsSummary = await res.json();
        emit('alertsSummaryUpdated', state.alertsSummary);
        updateTopBellBadge(state.alertsSummary.totalOpen || 0);
        return state.alertsSummary;
      }
    } catch (err) {
      console.warn('Backend unavailable during fetchAlertsSummary:', err);
    }
    return state.alertsSummary;
  }

  // Fetch Active Alerts List
  async function fetchAlerts(filters = {}) {
    try {
      let query = '?';
      if (filters.severity && filters.severity !== 'ALL') query += `severity=${encodeURIComponent(filters.severity)}&`;
      if (filters.status && filters.status !== 'ALL') query += `status=${encodeURIComponent(filters.status)}&`;
      if (filters.department && filters.department !== 'ALL') query += `department=${encodeURIComponent(filters.department)}&`;
      if (filters.type && filters.type !== 'ALL') query += `type=${encodeURIComponent(filters.type)}&`;

      const res = await fetch(`${HOST}/api/alerts${query}`);
      if (res.ok) {
        state.alerts = await res.json();
        emit('alertsUpdated', state.alerts);
        return state.alerts;
      }
    } catch (err) {
      console.warn('Backend unavailable during fetchAlerts:', err);
    }
    return state.alerts;
  }

  // Fetch Reuse Recommendations
  async function fetchRecommendations() {
    try {
      const res = await fetch(`${HOST}/api/water/recommendations`);
      if (res.ok) {
        state.recommendations = await res.json();
        emit('recommendationsUpdated', state.recommendations);
        return state.recommendations;
      }
    } catch (err) {
      console.warn('Backend unavailable during fetchRecommendations:', err);
    }
    return state.recommendations;
  }

  // Update Top Navigation Bell Badge
  function updateTopBellBadge(count) {
    const badge = document.getElementById('top-notification-badge');
    if (badge) {
      badge.innerText = count;
      if (count > 0) {
        badge.classList.remove('hidden');
      } else {
        badge.classList.add('hidden');
      }
    }
  }

  // Submit New Daily Water Entry (Immediately recalculates all views)
  async function submitDailyEntry(payload) {
    state.isLoading = true;
    emit('loading', true);

    try {
      const res = await fetch(`${HOST}/api/water/records`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.message || 'Failed to save water record.');
      }

      // Immediately refresh all dependent state asynchronously
      await refreshAll();

      state.isLoading = false;
      emit('loading', false);
      emit('recordSaved', data.record || data);
      return { success: true, record: data.record || data };
    } catch (err) {
      state.isLoading = false;
      emit('loading', false);
      emit('error', err.message);
      throw err;
    }
  }

  // Delete Record
  async function deleteDailyEntry(id) {
    try {
      const res = await fetch(`${HOST}/api/water/records/${id}`, { method: 'DELETE' });
      const data = await res.json();
      await refreshAll();
      return data;
    } catch (err) {
      console.error('Failed to delete record:', err);
      throw err;
    }
  }

  // Full Refresh (Synchronizes all pages simultaneously)
  async function refreshAll() {
    await Promise.allSettled([
      fetchSummary(state.activeTimeframe),
      fetchRecords(),
      fetchAlertsSummary(),
      fetchAlerts(),
      fetchRecommendations()
    ]);
    emit('stateRefreshed', state);
  }

  // Reset all database records for clean testing
  async function resetAllData() {
    try {
      cleanLegacyStorage();
      await fetch(`${HOST}/api/water/reset-all-data`, { method: 'POST' });
      await refreshAll();
      return { success: true };
    } catch (err) {
      console.error('Reset all data error:', err);
      throw err;
    }
  }

  // Initialize
  function init() {
    cleanLegacyStorage();
    refreshAll();
  }

  return {
    getState: () => ({ ...state }),
    subscribe,
    fetchSummary,
    fetchRecords,
    fetchAlertsSummary,
    fetchAlerts,
    fetchRecommendations,
    submitDailyEntry,
    deleteDailyEntry,
    refreshAll,
    resetAllData,
    cleanLegacyStorage,
    init
  };
})();
