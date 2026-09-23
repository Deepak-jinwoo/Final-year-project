// ============================================
// AquaNexus — Authentication Module
// Supports Java Spring Boot Backend + LocalStorage Fallback
// ============================================

window.AQUANEXUS_API_BASE = window.AQUANEXUS_API_BASE || (
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080'
    : 'https://final-year-project-as9l.onrender.com'
);

window.Auth = (() => {
  const API_BASE = `${window.AQUANEXUS_API_BASE}/api/auth`;
  const TOKEN_KEY = 'aquanexus_jwt';
  const SESSION_KEY = 'aquanexus_session';
  const USERS_KEY = 'aquanexus_users';

  // --- Local Storage Helpers ---
  function getUsers() {
    return JSON.parse(localStorage.getItem(USERS_KEY) || '[]');
  }

  function saveUsers(users) {
    localStorage.setItem(USERS_KEY, JSON.stringify(users));
  }

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  function getSession() {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  function setSession(userData) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(userData));
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(SESSION_KEY);
  }

  function isAuthenticated() {
    return !!getSession();
  }

  // --- API Helpers ---
  async function apiPost(endpoint, body) {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Request failed');
    }
    return data;
  }

  async function apiGet(endpoint) {
    const token = getToken();
    const res = await fetch(`${API_BASE}${endpoint}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      }
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Request failed');
    }
    return data;
  }

  // --- Register ---
  async function register(userData) {
    const { fullName, username, email, password, confirmPassword } = userData;

    if (!fullName || !username || !email || !password || !confirmPassword) {
      return { success: false, message: 'All fields are required.' };
    }
    if (password !== confirmPassword) {
      return { success: false, message: 'Passwords do not match.' };
    }
    const strongPwd = /^(?=.*[A-Z])(?=.*\d).{8,}$/;
    if (!strongPwd.test(password)) {
      return { success: false, message: 'Password must be at least 8 chars, 1 uppercase and 1 number.' };
    }

    try {
      const data = await apiPost('/register', { fullName, username, email, password, confirmPassword });
      setToken(data.token);
      setSession(data);
      updateSidebarProfile();
      return { success: true, message: 'Registration successful!' };
    } catch (err) {
      if (err.message.includes('Failed to fetch') || err.name === 'TypeError') {
        // Fallback to LocalStorage
        const users = getUsers();
        if (users.find(u => u.email.toLowerCase() === email.toLowerCase())) {
          return { success: false, message: 'Email is already registered.' };
        }
        const newUser = { id: Date.now().toString(), fullName, username, email: email.toLowerCase(), role: 'User' };
        users.push(newUser);
        saveUsers(users);
        setToken('mock_jwt_token_' + Date.now());
        setSession(newUser);
        updateSidebarProfile();
        return { success: true, message: 'Registration successful! (Offline Mode)' };
      }
      return { success: false, message: err.message };
    }
  }

  // --- Login ---
  async function login(email, password) {
    if (!email || !password) {
      return { success: false, message: 'Email and password are required.' };
    }

    try {
      const data = await apiPost('/login', { email, password });
      setToken(data.token);
      setSession(data);
      updateSidebarProfile();
      return { success: true, message: 'Login successful.' };
    } catch (err) {
      if (err.message.includes('Failed to fetch') || err.name === 'TypeError') {
        // Fallback to LocalStorage
        const users = getUsers();
        const user = users.find(u => u.email.toLowerCase() === email.toLowerCase());
        const sessionUser = user || {
          id: 'local_' + Date.now(),
          fullName: email.split('@')[0],
          username: email.split('@')[0],
          email: email,
          role: 'User'
        };
        setToken('mock_jwt_token_' + Date.now());
        setSession(sessionUser);
        updateSidebarProfile();
        return { success: true, message: 'Login successful! (Offline Mode)' };
      }
      return { success: false, message: err.message };
    }
  }

  // --- Logout ---
  function logout() {
    clearSession();
    updateSidebarProfile();
    if (window.Router) {
      window.Router.navigate('/login');
    } else {
      window.location.hash = '/login';
    }
  }

  // --- Restore Session ---
  async function restoreSession() {
    const session = getSession();
    if (session) {
      updateSidebarProfile();
    }
    const token = getToken();
    if (token && !token.startsWith('mock_jwt_token_')) {
      try {
        const data = await apiGet('/me');
        setSession(data);
        updateSidebarProfile();
      } catch (err) {
        // Ignored in fallback mode
      }
    }
  }

  // --- Google OAuth ---
  function initGoogleSignIn() {
    if (typeof google === 'undefined' || !google.accounts) {
      alert('Google Identity Services failed to load. Please check your internet connection.');
      return;
    }

    const GOOGLE_CLIENT_ID = '125421011304-r2aim0eo5sr6q8ioliegtnh9340jvi98.apps.googleusercontent.com';

    const client = google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: 'https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email',
      callback: async (tokenResponse) => {
        if (tokenResponse && tokenResponse.access_token) {
          try {
            // Try backend first
            const data = await apiPost('/google', { accessToken: tokenResponse.access_token });
            setToken(data.token);
            setSession(data);
            handleSuccessfulAuth(data.fullName);
          } catch (err) {
            // Fallback: Fetch directly from Google API if backend is down
            try {
              const googleRes = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
                headers: { Authorization: `Bearer ${tokenResponse.access_token}` }
              });
              const googleUser = await googleRes.json();
              
              const sessionData = {
                id: 'google_' + googleUser.sub,
                fullName: googleUser.name,
                username: googleUser.email.split('@')[0],
                email: googleUser.email,
                photoUrl: googleUser.picture || '',
                role: 'Google User',
                isOAuth: true
              };

              setToken('mock_google_token_' + Date.now());
              setSession(sessionData);
              handleSuccessfulAuth(sessionData.fullName);
            } catch (fallbackErr) {
              showAuthError('Google Sign-In failed: ' + fallbackErr.message);
            }
          }
        }
      },
    });

    client.requestAccessToken();
  }

  function handleSuccessfulAuth(name) {
    updateSidebarProfile();
    const alertBox = document.getElementById('login-alert');
    if (alertBox) {
      alertBox.className = 'mb-4 p-3 rounded-xl text-sm border relative z-10 border-success/30 bg-success/10 text-success';
      alertBox.innerText = `Welcome, ${name || 'User'}! Redirecting...`;
      alertBox.classList.remove('hidden');
    }
    setTimeout(() => {
      if (window.Router) window.Router.navigate('/dashboard');
      else window.location.hash = '/dashboard';
    }, 500);
  }

  // --- Sidebar Profile Updater ---
  function updateSidebarProfile() {
    const avatarEl = document.getElementById('sidebar-avatar');
    const nameEl = document.getElementById('sidebar-name');
    const roleEl = document.getElementById('sidebar-role');

    if (!avatarEl || !nameEl || !roleEl) return;

    const session = getSession();

    if (session) {
      if (session.photoUrl) {
        avatarEl.src = session.photoUrl;
      } else {
        const initials = encodeURIComponent(session.fullName || session.username || 'U');
        avatarEl.src = `https://ui-avatars.com/api/?name=${initials}&background=10b981&color=fff&size=80&bold=true`;
      }
      avatarEl.alt = session.fullName || 'User';
      nameEl.innerText = session.fullName || session.username || 'User';
      roleEl.innerText = session.role || session.email || 'Authenticated User';
    } else {
      avatarEl.src = `https://ui-avatars.com/api/?name=NA&background=2e3545&color=94a3b8&size=80`;
      avatarEl.alt = 'Not Signed In';
      nameEl.innerText = 'Not Signed In';
      roleEl.innerText = 'Please Sign In';
    }
  }

  // --- UI Helpers ---
  function togglePasswordVisibility(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    if (!input || !icon) return;
    if (input.type === 'password') {
      input.type = 'text';
      icon.innerText = 'visibility_off';
    } else {
      input.type = 'password';
      icon.innerText = 'visibility';
    }
  }

  function showAuthError(message) {
    const alertBox = document.getElementById('login-alert');
    if (alertBox) {
      alertBox.className = 'mb-4 p-3 rounded-xl text-sm border relative z-10 border-error/30 bg-error/10 text-error';
      alertBox.innerText = message;
      alertBox.classList.remove('hidden');
    } else {
      alert(message);
    }
  }

  return {
    register,
    login,
    logout,
    isAuthenticated,
    getSession,
    restoreSession,
    togglePasswordVisibility,
    initGoogleSignIn,
    updateSidebarProfile
  };
})();
