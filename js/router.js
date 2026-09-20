// ============================================
// AquaNexus — Hash-Based SPA Router
// ============================================

const Router = (() => {
  const routes = {
    '/login': 'pages/login.html',
    '/register': 'pages/register.html',
    '/dashboard': 'pages/dashboard.html',
    '/ai-assistant': 'pages/ai-assistant.html',
    '/cto': 'pages/cto.html',
    '/consumption': 'pages/consumption.html',
    '/reports': 'pages/reports.html',
    '/alerts': 'pages/alerts.html',
    '/settings': 'pages/settings.html',
  };

  const defaultRoute = '/login';

  // Cache fetched pages
  const cache = {};

  function getRoute() {
    const hash = window.location.hash.replace('#', '') || defaultRoute;
    return routes[hash] ? hash : defaultRoute;
  }

  async function loadPage(route) {
    const file = routes[route];
    if (!file) return;

    const app = document.getElementById('app');
    if (!app) return;

    // Route Protection
    const isAuthRoute = route === '/login' || route === '/register';
    
    if (window.Auth) {
      const isAuthenticated = window.Auth.isAuthenticated();
      
      // Redirect unauthenticated users trying to access protected routes
      if (!isAuthenticated && !isAuthRoute) {
        navigate('/login');
        return;
      }
      
      // Redirect authenticated users away from auth routes
      if (isAuthenticated && isAuthRoute) {
        navigate('/dashboard');
        return;
      }
    }

    try {
      // Always fetch fresh page fragment
      const fetchUrl = './' + file + '?v=' + Date.now();
      const res = await fetch(fetchUrl);
      if (!res.ok) throw new Error(`Failed to load ${file}`);
      const pageHtml = await res.text();

      // Fade out effect
      app.style.opacity = '0';
      app.style.transform = 'translateY(12px)';

      await new Promise(r => setTimeout(r, 150));

      // Inject content
      app.innerHTML = pageHtml;

      // Execute inline scripts
      const scripts = app.querySelectorAll('script');
      scripts.forEach(oldScript => {
        const newScript = document.createElement('script');
        if (oldScript.src) {
          newScript.src = oldScript.src;
        } else {
          newScript.textContent = oldScript.textContent;
        }
        oldScript.parentNode.replaceChild(newScript, oldScript);
      });

      // Fade in
      app.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
      app.style.opacity = '1';
      app.style.transform = 'translateY(0)';

      // Toggle login/register mode for layout
      const isAuthLayout = route === '/login' || route === '/register';
      document.body.classList.toggle('login-mode', isAuthLayout);

      // Update navigation active states
      updateNav(route);

      // Fire page-specific initializers
      if (window.AquaNexus && window.AquaNexus.onPageLoad) {
        window.AquaNexus.onPageLoad(route);
      }

    } catch (err) {
      console.error('Router error:', err);
      app.innerHTML = `
        <div class="flex items-center justify-center min-h-[60vh]">
          <div class="glass-card rounded-xl p-8 text-center max-w-md">
            <span class="material-symbols-outlined text-error text-5xl mb-4">error</span>
            <h2 class="text-xl font-bold text-white mb-2">Page Not Found</h2>
            <p class="text-on-surface-variant mb-6">Could not load the requested page.</p>
            <a href="#/dashboard" class="bg-primary text-on-primary px-6 py-2 rounded-xl font-bold">
              Return to Dashboard
            </a>
          </div>
        </div>`;
    }
  }

  function updateNav(route) {
    // Sidebar nav links
    document.querySelectorAll('.nav-link').forEach(link => {
      const href = link.getAttribute('data-route');
      link.classList.toggle('active', href === route);
      if (href === route) {
        link.classList.add('bg-primary/10', 'text-primary', 'border-l-4', 'border-primary');
        link.classList.remove('text-on-surface-variant');
      } else {
        link.classList.remove('bg-primary/10', 'text-primary', 'border-l-4', 'border-primary');
        link.classList.add('text-on-surface-variant');
      }
    });

    // Bottom nav links
    document.querySelectorAll('.bottom-nav-link').forEach(link => {
      const href = link.getAttribute('data-route');
      const isActive = href === route;
      link.classList.toggle('active', isActive);

      if (isActive) {
        link.classList.add('text-primary', 'font-bold');
        link.classList.remove('text-on-surface-variant/70');
      } else {
        link.classList.remove('text-primary', 'font-bold');
        link.classList.add('text-on-surface-variant/70');
      }
    });
  }

  function navigate(route) {
    window.location.hash = route;
  }

  function init() {
    window.addEventListener('hashchange', () => {
      loadPage(getRoute());
    });

    // Handle link clicks with data-route
    document.addEventListener('click', (e) => {
      const link = e.target.closest('[data-route]');
      if (link) {
        e.preventDefault();
        navigate(link.getAttribute('data-route'));
      }
    });

    // Load initial page
    loadPage(getRoute());
  }

  return { init, navigate, getRoute };
})();
