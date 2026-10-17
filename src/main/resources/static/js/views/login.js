import { api, sesion } from '../api.js';
import { esc, icono } from '../util.js';

const DEMO = [
  { rol: 'Administrador', email: 'admin@medicita.pe' },
  { rol: 'Recepción', email: 'recepcion@medicita.pe' },
  { rol: 'Médico', email: 'dr.vargas@medicita.pe' },
  { rol: 'Paciente', email: 'ana.torres@correo.com' }
];
const CLAVE_DEMO = 'Medicita2026*';

export function pintarLogin(app, alEntrar, alternarTema, alRegistrarse) {
  app.innerHTML = `
    <main class="login">
      <section class="login__marca" aria-hidden="true">
        <div class="login__marca-interior">
          <img src="/img/logo-blanco.svg" alt="" class="login__logo">
          <h1>Reserva y gestión de citas médicas, en un solo lugar.</h1>
          
        </div>
      </section>
      <section class="login__panel">
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

  const error = $('#error');
  $('form').addEventListener('submit', async ev => {
    ev.preventDefault();
    error.hidden = true;
    const email = $('#email').value.trim();
    const password = $('#password').value;
    if (!email || !password) { error.textContent = 'Escribe tu correo y tu contraseña.'; error.hidden = false; return; }
    const boton = $('#entrar');
    boton.disabled = true; boton.classList.add('cargando'); boton.textContent = 'Entrando…';
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
    } finally { clearTimeout(aviso); $('#espera') && ($('#espera').hidden = true); }
  });
  $('#email').focus();
}
