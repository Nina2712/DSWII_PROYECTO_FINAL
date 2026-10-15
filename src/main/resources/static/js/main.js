// Punto de entrada del dashboard: sesión, navegación y tema.
import { api, sesion, alExpirarSesion, medicosEnCache } from './api.js';
import { esc, ROLES, iniciales, icono, puede, toast } from './util.js';
import { pintarLogin } from './views/login.js';
import { pintarRegistro } from './views/registro.js';
import { pintarPacientes } from './views/pacientes.js';
import { pintarMedicos } from './views/medicos.js';
import { pintarEspecialidades } from './views/especialidades.js';
import { pintarSedes } from './views/sedes.js';
import { pintarUsuarios } from './views/usuarios.js';
import { pintarResumen } from './views/resumen.js';
import { pintarAgenda } from './views/agenda.js';
import { pintarCitas } from './views/citas.js';
import { abrirDetalle, abrirReserva } from './dialogos.js';

const app = document.getElementById('app');
const TODAS = {
  resumen: { texto: 'Resumen', icono: icono.resumen, pintar: pintarResumen },
  agenda: { texto: 'Agenda', icono: icono.agenda, pintar: pintarAgenda },
  citas: { texto: 'Citas', icono: icono.citas, pintar: pintarCitas },
  pacientes: { texto: 'Pacientes', icono: icono.pacientes, pintar: pintarPacientes, permiso: puede.verPacientes },
  medicos: { texto: 'Médicos', icono: icono.medico, pintar: pintarMedicos, permiso: puede.administrar },
  especialidades: { texto: 'Especialidades', icono: icono.especialidad, pintar: pintarEspecialidades, permiso: puede.administrar },
  sedes: { texto: 'Sedes', icono: icono.sede, pintar: pintarSedes, permiso: puede.administrar },
  usuarios: { texto: 'Usuarios', icono: icono.pacientes, pintar: pintarUsuarios, permiso: puede.administrar }
};
/** Secciones que puede ver el rol (el servidor vuelve a validar cada llamada). */
let RUTAS = TODAS;
const rutasDe = rol => Object.fromEntries(Object.entries(TODAS).filter(([, r]) => !r.permiso || r.permiso(rol)));

let vistaActual = null;   // { recargar() } de la vista abierta
let ctx = null;

// ------------------------------------------------------------------------ tema
function aplicarTema(tema) {
  document.documentElement.dataset.theme = tema;
  try { localStorage.setItem('medicita.tema', tema); } catch { /* modo privado */ }
  const btn = document.getElementById('tema');
  if (btn) { btn.innerHTML = tema === 'dark' ? icono.sol : icono.luna; btn.title = tema === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'; }
}
function alternarTema() {
  aplicarTema(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
}

// --------------------------------------------------------------------- sesiones
function cerrarSesion(mensaje) {
  sesion.clear();
  vistaActual = null; ctx = null;
  if (mensaje) toast(mensaje, 'error');
  mostrarLogin();
}
alExpirarSesion(() => cerrarSesion('Tu sesión expiró. Inicia sesión de nuevo.'));

function mostrarLogin() {
  history.replaceState(null, '', location.pathname);
  pintarLogin(app, mostrarApp, alternarTema, mostrarRegistro);
  aplicarTema(document.documentElement.dataset.theme);
}

function mostrarRegistro() {
  history.replaceState(null, '', location.pathname);
  pintarRegistro(app, mostrarApp, alternarTema, mostrarLogin);
  aplicarTema(document.documentElement.dataset.theme);
  window.scrollTo(0, 0);
}

// ------------------------------------------------------------------------ shell
function mostrarApp() {
  const usuario = sesion.get();
  if (!usuario || (usuario.expira && usuario.expira < Date.now())) { cerrarSesion(); return; }
  const rol = usuario.rol;
  RUTAS = rutasDe(rol);
  const nombre = `${usuario.nombres} ${usuario.apellidos}`;
  ctx = {
    usuario, rol,
    recargar: () => vistaActual?.recargar ? vistaActual.recargar() : navegar(),
    abrirCita: cita => abrirDetalle(cita, ctx),
    nuevaCita: () => abrirReserva({ ctx })
  };

  app.innerHTML = `
    <div class="shell">
      <aside class="lateral" id="lateral">
        <div class="lateral__marca"><img src="/img/logo.svg" class="logo-claro" alt="MediCita"><img src="/img/logo-oscuro.svg" class="logo-oscuro" alt="MediCita"></div>
        <nav class="lateral__nav" aria-label="Principal">
          ${Object.entries(RUTAS).map(([k, r]) => `<a href="#/${k}" class="nav" data-ruta="${k}">${r.icono}<span>${r.texto}</span></a>`).join('')}
        </nav>
        ${puede.reservar(rol) ? `<button class="btn btn--primario btn--ancho lateral__nueva" id="nueva">${icono.mas}<span>Nueva cita</span></button>` : ''}
        <div class="lateral__pie">
          <div class="usuario"><span class="avatar avatar--m">${esc(iniciales(nombre))}</span>
            <div><b>${esc(nombre)}</b><small>${esc(ROLES[rol] || rol)}</small></div></div>
          <button class="btn btn--suave btn--ancho btn--chico" id="salir">${icono.salir}<span>Cerrar sesión</span></button>
        </div>
      </aside>
      <div class="velo" id="velo"></div>
      <div class="principal">
        <header class="topbar">
          <button class="icono-btn topbar__menu" id="menu" aria-label="Abrir menú">${icono.menu}</button>
          <span class="topbar__rol"><i class="punto punto--atendida"></i>${esc(ROLES[rol] || rol)}</span>
          <div class="topbar__acciones">
            <button class="icono-btn" id="tema" aria-label="Cambiar entre modo claro y oscuro"></button>
          </div>
        </header>
        <main class="pagina" id="vista" tabindex="-1"></main>
      </div>
    </div>`;

  const lateral = document.getElementById('lateral');
  const abrirMenu = abierto => { lateral.classList.toggle('abierto', abierto); document.getElementById('velo').classList.toggle('visible', abierto); };
  document.getElementById('menu').addEventListener('click', () => abrirMenu(true));
  document.getElementById('velo').addEventListener('click', () => abrirMenu(false));
  lateral.addEventListener('click', e => { if (e.target.closest('.nav')) abrirMenu(false); });
  document.getElementById('tema').addEventListener('click', alternarTema);
  document.getElementById('salir').addEventListener('click', () => cerrarSesion());
  document.getElementById('nueva')?.addEventListener('click', () => { abrirMenu(false); ctx.nuevaCita(); });
  aplicarTema(document.documentElement.dataset.theme);

  if (!location.hash.startsWith('#/')) location.hash = '#/resumen';
  navegar();
  // Precarga el catálogo para que el diálogo de reserva abra al instante
  medicosEnCache().catch(() => {});
}

let numeroNavegacion = 0;
async function navegar() {
  if (!ctx) return;
  const clave = location.hash.replace('#/', '').split('?')[0];
  const ruta = RUTAS[clave] ? clave : 'resumen';
  document.querySelectorAll('.nav').forEach(a => a.classList.toggle('activa', a.dataset.ruta === ruta));
  document.title = `${RUTAS[ruta].texto} · MediCita`;
  const cont = document.getElementById('vista');
  if (!cont) return;
  const mia = ++numeroNavegacion;
  vistaActual = null;
  const vista = await RUTAS[ruta].pintar(cont, ctx);
  if (mia === numeroNavegacion) vistaActual = vista || { recargar: () => RUTAS[ruta].pintar(cont, ctx) };
  cont.scrollTop = 0;
}

window.addEventListener('hashchange', navegar);
// Si el usuario ya tenía sesión en esta pestaña, entra directo
if (sesion.get()) mostrarApp(); else mostrarLogin();
