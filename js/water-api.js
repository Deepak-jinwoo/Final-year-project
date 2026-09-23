// ============================================
// AquaNexus — Water API Client
// Dynamic host detection for local and cloud (Render) environments.
// ============================================

window.AQUANEXUS_API_BASE = window.AQUANEXUS_API_BASE || (
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080'
    : 'https://final-year-project-as9l.onrender.com'
);

window.WaterAPI = (() => {
  const HOST = window.AQUANEXUS_API_BASE;
  const API_BASE = `${HOST}/api/water`;

  async function apiPost(endpoint, body) {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    return res.json();
  }

  async function apiGet(endpoint) {
    const res = await fetch(`${API_BASE}${endpoint}`);
    return res.json();
  }

  // Submit a new daily water record
  async function submitRecord(data) {
    return apiPost('/records', data);
  }

  // Get records with optional filters
  async function getRecords(startDate, endDate, department) {
    let url = '/records?';
    if (startDate) url += `startDate=${startDate}&`;
    if (endDate) url += `endDate=${endDate}&`;
    if (department) url += `department=${department}&`;
    return apiGet(url);
  }

  // Get dynamic dashboard summary
  async function getDashboardSummary(days = 30) {
    return apiGet(`/dashboard-summary?days=${days}`);
  }

  // Get active alerts
  async function getAlerts() {
    return apiGet('/alerts');
  }

  // Get structured report
  async function getReport(startDate, endDate) {
    return apiGet(`/reports?startDate=${startDate}&endDate=${endDate}`);
  }

  // Delete a water record by ID
  async function deleteRecord(id) {
    const res = await fetch(`${API_BASE}/records/${id}`, { method: 'DELETE' });
    return res.json();
  }

  // Get water reuse recommendations
  async function getRecommendations() {
    return apiGet('/recommendations');
  }

  // Domain-specific AI Assistant Chat
  async function sendAIChat(prompt) {
    const res = await fetch(`${HOST}/api/ai/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt })
    });
    return res.json();
  }

  // Get Monthly Reports History
  async function getReportHistory() {
    return apiGet('/reports/history');
  }

  // Generate Monthly PDF Report
  async function generateMonthlyReport(startDate, endDate, periodName) {
    return apiPost('/reports/generate', { startDate, endDate, periodName });
  }

  // Get saved company profile for auto-fill
  async function getCompanyProfile() {
    return apiGet('/company-profile');
  }

  // Get Daily Report PDF URL
  function getDailyReportPdfUrl(recordId) {
    return `${API_BASE}/reports/daily/${recordId}/pdf`;
  }

  // Get Daily Report Preview
  async function getDailyReportPreview(recordId) {
    const res = await fetch(`${API_BASE}/reports/daily/${recordId}/preview`);
    return res.json();
  }

  // ===== ALERTS & NOTIFICATIONS =====
  async function getFilteredAlerts(filters = {}) {
    let query = '?';
    if (filters.severity) query += `severity=${encodeURIComponent(filters.severity)}&`;
    if (filters.status) query += `status=${encodeURIComponent(filters.status)}&`;
    if (filters.department) query += `department=${encodeURIComponent(filters.department)}&`;
    if (filters.type) query += `type=${encodeURIComponent(filters.type)}&`;
    if (filters.startDate) query += `startDate=${encodeURIComponent(filters.startDate)}&`;
    if (filters.endDate) query += `endDate=${encodeURIComponent(filters.endDate)}&`;
    const res = await fetch(`${HOST}/api/alerts${query}`);
    return res.json();
  }

  async function getAlertById(id) {
    const res = await fetch(`${HOST}/api/alerts/${id}`);
    return res.json();
  }

  async function getAlertsSummary() {
    const res = await fetch(`${HOST}/api/alerts/summary`);
    return res.json();
  }

  async function acknowledgeAlert(id) {
    const res = await fetch(`${HOST}/api/alerts/${id}/acknowledge`, { method: 'PATCH' });
    return res.json();
  }

  async function resolveAlert(id, resolvedBy, resolutionNotes) {
    const res = await fetch(`${HOST}/api/alerts/${id}/resolve`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ resolvedBy, resolutionNotes })
    });
    return res.json();
  }

  async function resendAlertNotification(id) {
    const res = await fetch(`${HOST}/api/alerts/${id}/resend-notification`, { method: 'POST' });
    return res.json();
  }

  async function getNotificationSettings(industryId = 1) {
    const res = await fetch(`${HOST}/api/notification-settings?industryId=${industryId}`);
    return res.json();
  }

  async function updateNotificationSettings(settings) {
    const res = await fetch(`${HOST}/api/notification-settings`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings)
    });
    return res.json();
  }

  async function getNotificationLogs(alertId) {
    let url = `${HOST}/api/notification-logs`;
    if (alertId) url += `?alertId=${alertId}`;
    const res = await fetch(url);
    return res.json();
  }

  async function sendAIChat(prompt) {
    const token = localStorage.getItem('aquanexus_jwt');
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${HOST}/api/ai/chat`, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ prompt })
    });
    return res.json();
  }

  return {
    submitRecord,
    getRecords,
    getDashboardSummary,
    getAlerts,
    getFilteredAlerts,
    getAlertById,
    getAlertsSummary,
    acknowledgeAlert,
    resolveAlert,
    resendAlertNotification,
    getNotificationSettings,
    updateNotificationSettings,
    getNotificationLogs,
    getReport,
    getCSVExportUrl: () => `${API_BASE}/reports/export/csv`,
    deleteRecord,
    getRecommendations,
    sendAIChat,
    getReportHistory,
    generateMonthlyReport,
    getReportPdfUrl: (id) => `${API_BASE}/reports/${id}/pdf`,
    getCompanyProfile,
    getDailyReportPdfUrl,
    getDailyReportPreview
  };
})();
