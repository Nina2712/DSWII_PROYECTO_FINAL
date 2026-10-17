import { api, medicosEnCache } from '../api.js';
import { esc, badge, numero, saludo, fechaLarga, partes, fechaCorta, iniciales, avatar, icono, puede } from '../util.js';
import { dona, barrasPorDia, barrasHorizontales, datosEstado } from '../charts.js';

const TEXTO_ALCANCE = {
  ADMIN: 'Vista global de la clínica',
  RECEPCIONISTA: 'Vista global de la clínica',
  MEDICO: 'Tu agenda',
  PACIENTE: 'Tus citas'
};

function kpi({ titulo, valor, detalle, clase }) {
  return `
    <article class="kpi kpi--${clase}">
      <p class="kpi__titulo">${esc(titulo)}</p>
      <p class="kpi__valor">${valor}</p>
      <p class="kpi__detalle">${esc(detalle)}</p>
    </article>`;
}

function listaProximas(citas, rol, medicos = []) {
  if (!citas.length) {
    return `<div class="vacio vacio--chico">${icono.vacio}<p>No hay citas próximas.</p></div>`;
  }
  return `<ul class="proximas">${citas.map(c => {
    const p = partes(c.fechaHora);
    const quien = rol === 'MEDICO' ? c.pacienteNombre : rol === 'PACIENTE' ? c.medicoNombre : `${c.pacienteNombre}`;
    const sub = rol === 'MEDICO' ? (c.motivo || 'Sin motivo') : rol === 'PACIENTE' ? `${c.especialidad} · ${c.sede}` : `${c.medicoNombre} · ${c.especialidad}`;
    return `
      <li><button class="proxima" data-cita="${c.id}">
        <span class="proxima__fecha"><b>${esc(p.hora)}</b><small>${esc(fechaCorta(p.fecha))}</small></span>
        ${rol === 'PACIENTE' ? avatar(quien, medicos.find(m => m.id === c.medicoId)?.foto, 'avatar--medico') : avatar(quien, null)}
        <span class="proxima__texto"><b>${esc(quien)}</b><small>${esc(sub)}</small></span>
        ${badge(c.estado)}
      </button></li>`;
  }).join('')}</ul>`;
}

export async function pintarResumen(cont, ctx) {
  const { usuario, rol } = ctx;
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">${esc(saludo())}, ${esc(usuario.nombres.split(' ')[0])}</h1>
        <p class="pagina__sub">${esc(TEXTO_ALCANCE[rol])} · últimos 30 y próximos 30 días</p></div>
      ${puede.reservar(rol) ? `<button class="btn btn--primario" data-nueva>${icono.mas}<span>Nueva cita</span></button>` : ''}
    </header>
    <div class="kpis">${'<div class="kpi kpi--carga"><span class="esqueleto"></span></div>'.repeat(4)}</div>
    <div class="rejilla"><div class="tarjeta esqueleto-bloque"></div><div class="tarjeta esqueleto-bloque"></div></div>`;
  cont.querySelector('[data-nueva]')?.addEventListener('click', ctx.nuevaCita);

  let r;
  try { r = await api.resumen(); }
  catch (e) {
    cont.querySelector('.kpis').outerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
    cont.querySelector('[data-reintentar]')?.addEventListener('click', () => pintarResumen(cont, ctx));
    return;
  }

  // el paciente ve la foto del médico con quien tiene cita
  let medicos = [];
  if (rol === 'PACIENTE') { try { medicos = await medicosEnCache(); } catch { /* sin fotos, quedan las iniciales */ } }

  const k = r.kpis;
  const tasa = k.tasaAsistencia == null ? '—' : `${String(k.tasaAsistencia).replace('.', ',')}%`;
  const kpis = [
    { titulo: rol === 'PACIENTE' ? 'Citas hoy' : 'Citas de hoy', valor: numero(k.hoy), detalle: fechaLarga(r.hoy), clase: 'hoy' },
    { titulo: 'Pendientes', valor: numero(k.pendientes), detalle: rol === 'PACIENTE' ? 'Esperan confirmación' : 'Por confirmar', clase: 'pendiente' },
    { titulo: 'Confirmadas', valor: numero(k.confirmadas), detalle: 'Listas para atender', clase: 'confirmada' },
    { titulo: 'Asistencia', valor: tasa, detalle: `${numero(k.atendidas)} atendidas · ${numero(k.noAsistio)} inasistencias`, clase: 'atendida' }
  ];

  const veTodo = puede.verTodo(rol);
  cont.querySelector('.kpis').innerHTML = kpis.map(kpi).join('');
  cont.querySelector('.rejilla').outerHTML = `
    <div class="rejilla">
      <section class="tarjeta tarjeta--ancha">
        <header class="tarjeta__cab"><h2>Citas por día</h2><span class="tenue">Hoy resaltado · 14 días</span></header>
        ${barrasPorDia(r.porDia, r.hoy)}
      </section>
      <section class="tarjeta">
        <header class="tarjeta__cab"><h2>Citas por estado</h2><span class="tenue">${numero(k.total)} en el período</span></header>
        ${dona(datosEstado(r.porEstado), 'Citas por estado')}
      </section>
      <section class="tarjeta">
        <header class="tarjeta__cab"><h2>Próximas citas</h2></header>
        ${listaProximas(r.proximas, rol, medicos)}
      </section>
      <section class="tarjeta">
        <header class="tarjeta__cab"><h2>Por especialidad</h2><span class="tenue">Sin canceladas</span></header>
        ${barrasHorizontales(r.porEspecialidad)}
      </section>
      ${veTodo ? `
      <section class="tarjeta">
        <header class="tarjeta__cab"><h2>Carga por médico</h2><span class="tenue">Sin canceladas</span></header>
        ${barrasHorizontales(r.porMedico)}
      </section>` : ''}
    </div>`;

  cont.querySelectorAll('[data-cita]').forEach(b => b.addEventListener('click', async () => {
    const cita = r.proximas.find(c => String(c.id) === b.dataset.cita);
    if (cita) ctx.abrirCita(cita);
  }));
}
