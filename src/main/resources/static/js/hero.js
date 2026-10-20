// Encabezado de bienvenida del resumen: foto grande, saludo, hora y fecha de Lima, clima y datos del día.
import { api } from './api.js';
import { esc, ROLES, ZONA, avatar, partes, fechaCorta, saludo } from './util.js';

const FRASES = {
  PACIENTE: ['Tu salud es lo primero: reserva con tiempo y llega tranquilo.', 'Cuidarte hoy es ahorrarte preocupaciones mañana.', 'Tu historial y tus citas, siempre a la mano.'],
  MEDICO: ['Cada consulta cuenta. Tu agenda de hoy ya está lista.', 'Con el historial a la vista, la atención empieza mejor.', 'Gracias por cuidar a quienes confían en ti.'],
  RECEPCIONISTA: ['Una agenda en orden hace una clínica tranquila.', 'Tú eres la primera sonrisa de cada paciente.', 'Confirma, reprograma y ordena: todo en un solo lugar.'],
  ADMIN: ['La clínica de un vistazo: citas, equipo y sedes.', 'Buenas decisiones empiezan con buenos datos.', 'Todo el equipo coordinado, sin papeles de por medio.']
};
const CLIMA = [[0, 'Despejado'], [3, 'Algo nublado'], [48, 'Neblina'], [57, 'Llovizna'], [67, 'Lluvia'], [99, 'Tormenta']];
const textoClima = c => (CLIMA.find(([max]) => c <= max) || CLIMA.at(-1))[1];

const pad = n => String(n).padStart(2, '0');
function reloj() {
  const d = new Date();
  const f = new Intl.DateTimeFormat('es-PE', { timeZone: ZONA, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).formatToParts(d);
  const v = t => f.find(x => x.type === t).value;
  const fecha = new Intl.DateTimeFormat('es-PE', { timeZone: ZONA, weekday: 'long', day: 'numeric', month: 'long' }).format(d);
  return { hm: `${pad(v('hour'))}:${v('minute')}`, s: v('second'), fecha: fecha.charAt(0).toUpperCase() + fecha.slice(1) };
}

/** Devuelve { actualizar(r) } para completar los datos cuando llega el resumen. */
export function pintarHero(cont, ctx) {
  const { usuario, rol } = ctx;
  const nombre = `${usuario.nombres} ${usuario.apellidos}`;
  const dia = Math.floor(Date.now() / 86400000);
  const frase = FRASES[rol][dia % FRASES[rol].length];
  const t = reloj();
  cont.innerHTML = `
    <section class="hero" aria-label="Bienvenida">
      <img class="hero__foto" src="/img/fotos/teleconsulta-1.jpg" alt="">
      <div class="hero__velo"></div>
      <div class="hero__perfil">
        <a class="hero__avatar" href="#/perfil" title="Mi perfil: cambiar foto y datos" id="hero-avatar">${avatar(nombre, null, 'avatar--xl')}<i class="hero__anillo"></i></a>
        <div class="hero__saludo">
          <p>${esc(saludo())},</p>
          <h1>${esc(usuario.nombres.split(' ')[0])}</h1>
          <p class="hero__rol">${esc(ROLES[rol] || rol)}</p>
        </div>
      </div>
      <div class="hero__centro">
        <p class="hero__frase">${esc(frase)}</p>
        <div class="hero__datos" id="hero-datos" aria-live="polite"><span class="hero__dato hero__dato--carga">Cargando tu día…</span></div>
      </div>
      <div class="hero__tiempo">
        <p class="hero__hora"><time id="hero-hora">${t.hm}</time><small id="hero-seg">:${t.s}</small></p>
        <p class="hero__fecha" id="hero-fecha">${esc(t.fecha)}</p>
        <p class="hero__clima" id="hero-clima" hidden></p>
      </div>
      <svg class="hero__ecg" viewBox="0 0 600 60" preserveAspectRatio="none" aria-hidden="true" focusable="false">
        <path class="ecg-base" d="M0 34 H110 L128 34 L140 10 L156 52 L170 22 L180 34 H330 L348 34 L360 6 L378 54 L392 24 L402 34 H600" />
        <path class="ecg-pulso" d="M0 34 H110 L128 34 L140 10 L156 52 L170 22 L180 34 H330 L348 34 L360 6 L378 54 L392 24 L402 34 H600" />
      </svg>
    </section>`;

  const $ = s => cont.querySelector(s);
  const tic = setInterval(() => {
    if (!$('#hero-hora')) { clearInterval(tic); return; }
    const a = reloj();
    $('#hero-hora').textContent = a.hm; $('#hero-seg').textContent = `:${a.s}`; $('#hero-fecha').textContent = a.fecha;
  }, 1000);

  // La foto real del usuario (si la tiene)
  const ponerFoto = (n, foto) => { const a = $('#hero-avatar'); if (a) a.innerHTML = `${avatar(n, foto, 'avatar--xl')}<i class="hero__anillo"></i>`; };
  api.perfil().then(p => ponerFoto(`${p.nombres} ${p.apellidos}`, p.foto)).catch(() => {});
  window.addEventListener('perfil-actualizado', e => ponerFoto(e.detail.nombre, e.detail.foto), { once: true });

  // Clima de Lima (Open-Meteo, sin clave). Si no responde, simplemente no se muestra.
  const ctl = new AbortController(); setTimeout(() => ctl.abort(), 4000);
  fetch('https://api.open-meteo.com/v1/forecast?latitude=-12.05&longitude=-77.04&current=temperature_2m,weather_code&timezone=America%2FLima', { signal: ctl.signal })
    .then(r => (r.ok ? r.json() : Promise.reject()))
    .then(j => { const e = $('#hero-clima'); if (e && j.current) { e.textContent = `Lima · ${Math.round(j.current.temperature_2m)} °C · ${textoClima(j.current.weather_code)}`; e.hidden = false; } })
    .catch(() => {});

  return {
    /** Completa los datos del día con la respuesta de /api/dashboard/resumen. */
    actualizar(r) {
      const cuerpo = $('#hero-datos'); if (!cuerpo) return;
      const k = r.kpis, sig = r.proximas?.[0];
      const dato = (valor, texto, clase = '') => `<span class="hero__dato ${clase}"><b>${valor}</b><small>${esc(texto)}</small></span>`;
      let proxima = dato('—', rol === 'PACIENTE' ? 'Sin citas próximas' : 'Sin citas próximas');
      if (sig) {
        const p = partes(sig.fechaHora);
        const con = rol === 'PACIENTE' ? sig.medicoNombre : sig.pacienteNombre;
        proxima = dato(p.hora, `Próxima · ${p.fecha === r.hoy ? 'hoy' : fechaCorta(p.fecha)} · ${con}`, 'hero__dato--ancho');
      }
      cuerpo.innerHTML = dato(k.hoy, k.hoy === 1 ? 'cita hoy' : 'citas hoy', 'hero__dato--hoy') + proxima +
        dato(k.pendientes, rol === 'PACIENTE' ? 'esperan confirmación' : 'por confirmar');
    }
  };
}
