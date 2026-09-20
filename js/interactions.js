// ============================================
// AquaNexus — Micro-Interactions Library
// ============================================

window.AquaNexus = window.AquaNexus || {};

// Animated number counter
AquaNexus.animateCounter = (elementId, target, duration = 2000) => {
  const el = document.getElementById(elementId);
  if (!el) return;

  let startTime = null;
  const startVal = 0;

  function step(timestamp) {
    if (!startTime) startTime = timestamp;
    const progress = Math.min((timestamp - startTime) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3); // easeOutCubic
    el.innerText = Math.floor(eased * target).toLocaleString();
    if (progress < 1) {
      requestAnimationFrame(step);
    }
  }
  requestAnimationFrame(step);
};

// Glass card hover lift effect
AquaNexus.initCardHover = () => {
  const cards = document.querySelectorAll('.glass-card');
  cards.forEach(card => {
    card.addEventListener('mouseenter', () => {
      if (window.innerWidth > 768) {
        card.style.transform = 'translateY(-4px)';
        card.style.borderColor = 'rgba(255, 255, 255, 0.2)';
        card.style.boxShadow = '0 10px 40px rgba(16, 185, 129, 0.08)';
      }
    });
    card.addEventListener('mouseleave', () => {
      card.style.transform = 'translateY(0)';
      card.style.borderColor = 'rgba(255, 255, 255, 0.1)';
      card.style.boxShadow = '0 4px 30px rgba(0, 0, 0, 0.1)';
    });
  });
};

// Input focus icon color change
AquaNexus.initInputFocus = () => {
  document.querySelectorAll('.input-glass').forEach(input => {
    input.addEventListener('focus', () => {
      const icon = input.parentElement.querySelector('.material-symbols-outlined');
      if (icon) icon.style.color = '#4edea3';
    });
    input.addEventListener('blur', () => {
      const icon = input.parentElement.querySelector('.material-symbols-outlined');
      if (icon) icon.style.color = '';
    });
  });
};

// Login card 3D tilt mouse tracking
AquaNexus.initLoginTilt = () => {
  const card = document.querySelector('.login-card');
  if (!card) return;

  document.addEventListener('mousemove', (e) => {
    const x = (window.innerWidth / 2 - e.pageX) / 45;
    const y = (window.innerHeight / 2 - e.pageY) / 45;
    card.style.transform = `perspective(1000px) rotateY(${x}deg) rotateX(${-y}deg)`;
  });
};

// Search bar ring animation
AquaNexus.initSearchRing = () => {
  const searchInput = document.querySelector('#search-input');
  if (!searchInput) return;

  searchInput.addEventListener('focus', () => {
    searchInput.parentElement.classList.add('ring-2', 'ring-primary/20');
  });
  searchInput.addEventListener('blur', () => {
    searchInput.parentElement.classList.remove('ring-2', 'ring-primary/20');
  });
};

// Heatmap generator
AquaNexus.generateHeatmap = () => {
  const container = document.getElementById('heatmap-grid');
  if (!container) return;

  const colors = [
    'bg-surface-variant/20',
    'bg-primary/20',
    'bg-primary/40',
    'bg-primary/60',
    'bg-primary/80',
    'bg-primary'
  ];

  for (let i = 0; i < 168; i++) {
    const div = document.createElement('div');
    const intensity = Math.floor(Math.random() * colors.length);
    const pct = Math.floor(Math.random() * 100);
    div.className = `h-16 md:h-20 rounded-lg ${colors[intensity]} transition-all hover:ring-2 hover:ring-white/20 cursor-help relative group`;
    div.innerHTML = `<div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"><span class="text-[10px] font-mono-data text-white">${pct}%</span></div>`;
    container.appendChild(div);
  }
};

// Glass card mouse tracking (spotlight effect)
AquaNexus.initMouseTrack = () => {
  document.querySelectorAll('.glass-card').forEach(card => {
    card.addEventListener('mousemove', (e) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      card.style.setProperty('--mouse-x', `${x}px`);
      card.style.setProperty('--mouse-y', `${y}px`);
    });
  });
};

// Page-specific initialization
AquaNexus.onPageLoad = (route) => {
  // Short delay to ensure DOM is ready
  setTimeout(() => {
    AquaNexus.initCardHover();
    AquaNexus.initMouseTrack();

    switch (route) {
      case '/login':
        AquaNexus.initInputFocus();
        AquaNexus.initLoginTilt();
        break;

      case '/dashboard':
        AquaNexus.animateCounter('counter-consumed', 4821);
        AquaNexus.animateCounter('counter-reused', 3104);
        AquaNexus.animateCounter('counter-saved', 1717);
        AquaNexus.generateHeatmap();
        break;

      case '/cto':
        // Cards are self-contained
        break;

      case '/consumption':
        AquaNexus.initSearchRing();
        break;
    }
  }, 100);
};
