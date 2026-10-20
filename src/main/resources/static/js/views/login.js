import { api, sesion } from '../api.js';
import { esc, icono } from '../util.js';

// Frases que rotan junto a las fotos del lado izquierdo (/img/fotos) y color de acento del resplandor.
// Para añadir fotos: copia el archivo a /img/fotos y agrégalo a FOTOS (la frase n usa la foto n).
const FOTOS = ['teleconsulta-1.jpg', 'teleconsulta-2.jpg', 'teleconsulta-3.jpg'];
const FRASES = [
  { titulo: 'Cada paciente merece tiempo y atención.', detalle: 'Reserva con el médico que prefieres y llega a una consulta ya preparada.', tono: '#14b8a6' },
  { titulo: 'Una clínica ordenada empieza en recepción.', detalle: 'Agenda sin cruces y confirmación de citas en segundos, sede por sede.', tono: '#38bdf8' },
  { titulo: 'Cuidar también es prevenir.', detalle: 'Alergias, seguro y contacto de emergencia a la vista del médico al atender.', tono: '#34d399' },
  { titulo: 'Tu historia clínica, completa y contigo.', detalle: 'Cada médico que te atiende suma al historial y tú lo consultas cuando quieras.', tono: '#a78bfa' }
];

const DEMO = [
  { rol: 'Administrador', email: 'admin@medicita.pe' },
  { rol: 'Recepción', email: 'recepcion@medicita.pe' },
  { rol: 'Médico', email: 'dr.vargas@medicita.pe' },
  { rol: 'Paciente', email: 'ana.torres@correo.com' }
];
const CLAVE_DEMO = 'Medicita2026*';

export function pintarLogin(app, alEntrar, alternarTema, alRegistrarse) {
  app.innerHTML = `
    <main class="login login--vivo">
      <section class="login__marca" id="marca" aria-label="Presentación de MediCita">
        <div class="fotos" id="fotos" aria-hidden="true">
          ${FOTOS.map((f, i) => `<img class="foto${i === 0 ? ' activa' : ''}" src="/img/fotos/${f}" alt="" ${i ? 'loading="lazy"' : ''}>`).join('')}
        </div>
        <div class="login__velo" aria-hidden="true"></div>
        <div class="login__luz" aria-hidden="true"></div>
        <div class="login__marca-interior">
          <img src="/img/logo-blanco.svg" alt="" class="login__logo">
          <h1>Reserva y gestión de citas médicas, en un solo lugar.</h1>
          <div class="escena" id="escena">
            <div class="escena__texto" id="escena-texto" aria-live="off">
              <b id="escena-titulo">${esc(FRASES[0].titulo)}</b>
              <span id="escena-detalle">${esc(FRASES[0].detalle)}</span>
            </div>
            <div class="puntos" aria-hidden="true">${FRASES.map((_, i) => `<i${i === 0 ? ' class="on"' : ''}></i>`).join('')}</div>
          </div>
        </div>
        <svg class="escena__ecg" viewBox="0 0 600 80" preserveAspectRatio="none" focusable="false" aria-hidden="true">
          <path class="ecg-base" d="M0 42 H110 L128 42 L140 16 L156 66 L170 28 L180 42 H330 L348 42 L360 10 L378 70 L392 30 L402 42 H600" />
          <path class="ecg-pulso" d="M0 42 H110 L128 42 L140 16 L156 66 L170 28 L180 42 H330 L348 42 L360 10 L378 70 L392 30 L402 42 H600" />
        </svg>
      </section>
      <section class="login__panel" id="panel">
        <div class="login__fondo" aria-hidden="true">
          ${FOTOS.map(f => `<img src="/img/fotos/${f}" alt="">`).join('')}
        </div>
        <button class="icono-btn login__tema" id="tema" aria-label="Cambiar entre modo claro y oscuro">${icono.luna}</button>
        <form class="login__form" novalidate>
          <span class="login__logo--movil"><img src="/img/logo.svg" class="logo-claro" alt="MediCita"><img src="/img/logo-oscuro.svg" class="logo-oscuro" alt="MediCita"></span>
          <h2>Inicia sesión</h2>
          <p class="tenue">Ingresa con tu correo y contraseña.</p>
          <div class="grupo"><label class="campo__etiqueta" for="email">Correo electrónico</label>
            <input id="email" class="campo" type="email" autocomplete="username" required placeholder="nombre@correo.com"></div>
          <div class="grupo"><label class="campo__etiqueta" for="password">Contraseña</label>
            <div class="campo__clave"><input id="password" class="campo" type="password" autocomplete="current-password" required>
              <button type="button" class="btn btn--texto" id="ver" aria-pressed="false">Mostrar</button></div></div>
          <p id="error" class="alerta alerta--error" role="alert" hidden></p>
          <p id="espera" class="alerta alerta--info" hidden>El servidor gratuito puede tardar hasta un minuto en despertar. Gracias por esperar…</p>
          <button class="btn btn--primario btn--ancho" id="entrar" type="submit">Entrar</button>
          <p class="login__alt">¿Eres paciente y aún no tienes cuenta? <button type="button" class="btn btn--texto" id="registrarse">Crear cuenta</button></p>
          <details class="demo">
            <summary>Cuentas de demostración</summary>
            <p class="tenue">Toca una para rellenar el formulario. Contraseña: <code>${esc(CLAVE_DEMO)}</code></p>
            <div class="demo__lista">${DEMO.map(d => `<button type="button" class="chip" data-email="${esc(d.email)}">${esc(d.rol)}</button>`).join('')}</div>
          </details>
        </form>
      </section>
    </main>`;

  const $ = s => app.querySelector(s);
  $('#tema').addEventListener('click', alternarTema);
  $('#registrarse').addEventListener('click', alRegistrarse);
  $('#ver').addEventListener('click', e => {
    const campo = $('#password');
    const visible = campo.type === 'text';
    campo.type = visible ? 'password' : 'text';
    e.currentTarget.textContent = visible ? 'Mostrar' : 'Ocultar';
    e.currentTarget.setAttribute('aria-pressed', String(!visible));
  });
  app.querySelectorAll('[data-email]').forEach(b => b.addEventListener('click', () => {
    $('#email').value = b.dataset.email;
    $('#password').value = CLAVE_DEMO;
    $('#entrar').focus();
  }));

  // Fotos: el puntero las mueve con paralaje y una luz suave lo sigue.
  const marca = $('#marca'), reducido = matchMedia('(prefers-reduced-motion: reduce)').matches;
  marca.addEventListener('pointermove', ev => {
    const r = marca.getBoundingClientRect();
    const x = (ev.clientX - r.left) / r.width, y = (ev.clientY - r.top) / r.height;
    marca.style.setProperty('--mx', `${(x * 100).toFixed(1)}%`);
    marca.style.setProperty('--my', `${(y * 100).toFixed(1)}%`);
    if (!reducido) { marca.style.setProperty('--px', ((x - .5) * 2).toFixed(3)); marca.style.setProperty('--py', ((y - .5) * 2).toFixed(3)); }
  });
  marca.addEventListener('pointerleave', () => { marca.style.setProperty('--px', '0'); marca.style.setProperty('--py', '0'); });

  // Frases: cambian solas cada 7 s (salvo que se pida reducir el movimiento) y mueven el color del resplandor.
  let actual = 0;
  const pintarFrase = n => {
    actual = n % FRASES.length;
    $('.login').style.setProperty('--acento', FRASES[actual].tono);
    app.querySelectorAll('.foto').forEach((f, i) => f.classList.toggle('activa', i === actual % FOTOS.length));
    app.querySelectorAll('.puntos i').forEach((p, i) => p.classList.toggle('on', i === actual));
    const texto = $('#escena-texto');
    texto.classList.add('escena__texto--cambia');
    setTimeout(() => {
      if (!texto.isConnected) return;
      $('#escena-titulo').textContent = FRASES[actual].titulo;
      $('#escena-detalle').textContent = FRASES[actual].detalle;
      texto.classList.remove('escena__texto--cambia');
    }, 260);
  };
  $('.login').style.setProperty('--acento', FRASES[0].tono);
  if (!reducido) {
    const reloj = setInterval(() => { if (!$('#escena')) { clearInterval(reloj); return; } pintarFrase(actual + 1); }, 7000);
  }

  // La imagen difuminada del lado del formulario se desplaza suavemente con el puntero.
  const panel = $('#panel');
  if (!reducido) {
    panel.addEventListener('pointermove', ev => {
      const r = panel.getBoundingClientRect();
      panel.style.setProperty('--bx', (((ev.clientX - r.left) / r.width - .5) * 2).toFixed(3));
      panel.style.setProperty('--by', (((ev.clientY - r.top) / r.height - .5) * 2).toFixed(3));
    });
  }

  const error = $('#error');
  $('form').addEventListener('submit', async ev => {
    ev.preventDefault();
    error.hidden = true;
    const email = $('#email').value.trim();
    const password = $('#password').value;
    if (!email || !password) { error.textContent = 'Escribe tu correo y tu contraseña.'; error.hidden = false; return; }
    const boton = $('#entrar');
    boton.disabled = true; boton.classList.add('cargando'); boton.textContent = 'Entrando…';
    $('.login').classList.add('entrando');
    const aviso = setTimeout(() => { $('#espera').hidden = false; }, 4000);
    try {
      const r = await api.login(email, password);
      sesion.set({
        token: r.token, email: r.email, nombres: r.nombres, apellidos: r.apellidos, rol: r.rol,
        expira: Date.now() + r.expiraEnMinutos * 60000
      });
      alEntrar();
    } catch (e) {
      error.textContent = e.message; error.hidden = false;
      boton.disabled = false; boton.classList.remove('cargando'); boton.textContent = 'Entrar';
      $('.login').classList.remove('entrando');
    } finally { clearTimeout(aviso); $('#espera') && ($('#espera').hidden = true); }
  });
  $('#email').focus();
}
