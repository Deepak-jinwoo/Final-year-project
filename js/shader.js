// ============================================
// AquaNexus — WebGL Water Shader Background
// ============================================

const ShaderBG = (() => {
  function init() {
    const container = document.getElementById('shader-bg');
    if (!container) return;

    const canvas = document.createElement('canvas');
    canvas.id = 'shader-canvas';
    container.appendChild(canvas);

    function syncSize() {
      const w = canvas.clientWidth || 1280;
      const h = canvas.clientHeight || 720;
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
    }

    if (typeof ResizeObserver !== 'undefined') {
      new ResizeObserver(syncSize).observe(canvas);
    }
    syncSize();

    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    if (!gl) return;

    const vertexShader = `
      attribute vec2 a_position;
      varying vec2 v_texCoord;
      void main() {
        v_texCoord = a_position * 0.5 + 0.5;
        gl_Position = vec4(a_position, 0.0, 1.0);
      }
    `;

    const fragmentShader = `
      precision highp float;
      uniform float u_time;
      uniform vec2 u_resolution;
      uniform vec2 u_mouse;

      void main() {
        vec2 uv = gl_FragCoord.xy / u_resolution.xy;
        float time = u_time * 0.5;

        // Create multiple sine waves for water movement
        float wave1 = sin(uv.x * 6.0 + time) * 0.05;
        float wave2 = sin(uv.x * 3.0 - time * 0.8) * 0.03;
        float wave3 = cos(uv.x * 10.0 + time * 1.2) * 0.02;

        float combinedWaves = 0.5 + wave1 + wave2 + wave3;

        // Base colors from the design system (Emerald and Deep Charcoal)
        vec3 charcoal = vec3(0.047, 0.075, 0.133);  // #0c1322
        vec3 emerald  = vec3(0.063, 0.725, 0.506);   // #10b981
        vec3 teal     = vec3(0.078, 0.722, 0.651);    // #14b8a6

        // Mouse influence
        vec2 mouseNorm = u_mouse / u_resolution;
        float mouseDist = distance(uv, mouseNorm);
        float mouseEffect = smoothstep(0.4, 0.0, mouseDist) * 0.03;
        combinedWaves += mouseEffect;

        // Smooth transition at the wave surface
        float dist = combinedWaves - uv.y;
        float edge = smoothstep(0.0, 0.02, dist);

        // Liquid shading
        vec3 waterColor = mix(emerald, teal, uv.y * 2.0);
        waterColor += vec3(0.1) * sin(uv.x * 20.0 + time * 2.0);

        vec3 finalColor = mix(charcoal, waterColor, edge);

        // Add subtle top edge highlights
        float highlight = smoothstep(0.0, 0.005, dist) - smoothstep(0.005, 0.01, dist);
        finalColor += highlight * vec3(1.0, 1.0, 1.0) * 0.3;

        gl_FragColor = vec4(finalColor, 1.0);
      }
    `;

    function createShader(type, src) {
      const s = gl.createShader(type);
      gl.shaderSource(s, src);
      gl.compileShader(s);
      return s;
    }

    const prog = gl.createProgram();
    gl.attachShader(prog, createShader(gl.VERTEX_SHADER, vertexShader));
    gl.attachShader(prog, createShader(gl.FRAGMENT_SHADER, fragmentShader));
    gl.linkProgram(prog);
    gl.useProgram(prog);

    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);

    const pos = gl.getAttribLocation(prog, 'a_position');
    gl.enableVertexAttribArray(pos);
    gl.vertexAttribPointer(pos, 2, gl.FLOAT, false, 0, 0);

    const uTime = gl.getUniformLocation(prog, 'u_time');
    const uRes = gl.getUniformLocation(prog, 'u_resolution');
    const uMouse = gl.getUniformLocation(prog, 'u_mouse');

    let mouse = { x: canvas.width / 2, y: canvas.height / 2 };
    window.addEventListener('mousemove', (event) => {
      const rect = canvas.getBoundingClientRect();
      if (rect.width && rect.height) {
        const nx = (event.clientX - rect.left) / rect.width;
        const ny = 1.0 - (event.clientY - rect.top) / rect.height;
        mouse.x = nx * canvas.width;
        mouse.y = ny * canvas.height;
      }
    });

    function render(t) {
      if (typeof ResizeObserver === 'undefined') syncSize();
      gl.viewport(0, 0, canvas.width, canvas.height);
      if (uTime) gl.uniform1f(uTime, t * 0.001);
      if (uRes) gl.uniform2f(uRes, canvas.width, canvas.height);
      if (uMouse) gl.uniform2f(uMouse, mouse.x, mouse.y);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
      requestAnimationFrame(render);
    }
    render(0);
  }

  return { init };
})();
