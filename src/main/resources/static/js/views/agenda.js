import { todasLasCitas, medicosEnCache } from '../api.js';
import {
  esc, partes, sumarDias, inicioSemana, diaSemana, hoyLima, ahoraLima, fechaCorta, mesAnio, DIAS_CORTOS, icono,
  puede, ymd
} from '../util.js';

const ALTO_HORA = 60; // px por hora en la vista semanal

const corto = nombre => String(nombre).split(/\s+/).slice(0, 2).join(' ');

function textoCita(c, rol) {
  if (rol === 'MEDICO') return { titulo: corto(c.pacienteNombre), sub: c.motivo || '' };
  if (rol === 'PACIENTE') return { titulo: corto(c.medicoNombre), sub: c.especialidad };
  return { titulo: corto(c.pacienteNombre), sub: corto(c.medicoNombre) };
}

/** Reparte en columnas las citas que se solapan para que no se tapen entre sí. */
function distribuir(citas) {
  const ordenadas = [...citas].sort((a, b) => partes(a.fechaHora).min - partes(b.fechaHora).min);
  const resultado = [];
  let grupo = [], finGrupo = -1;
  const cerrar = () => {
    const columnas = [];
    grupo.forEach(it => {
      let col = columnas.findIndex(fin => fin <= it.ini);
      if (col === -1) { col = columnas.length; columnas.push(0); }
      columnas[col] = it.fin; it.col = col;
    });
    grupo.forEach(it => { it.cols = columnas.length; resultado.push(it); });
    grupo = [];
  };
  ordenadas.forEach(c => {
    const ini = partes(c.fechaHora).min, fin = ini + (c.duracionMin || 30);
    if (grupo.length && ini >= finGrupo) { cerrar(); finGrupo = -1; }
    grupo.push({ c, ini, fin });
    finGrupo = Math.max(finGrupo, fin);
  });
  if (grupo.length) cerrar();
  return resultado;
}

export async function pintarAgenda(cont, ctx, previo) {
  const { rol } = ctx;
  const e = previo || { modo: 'semana', ancla: hoyLima(), medicoId: '' };
  let medicos = [];
  if (puede.verTodo(rol)) { try { medicos = await medicosEnCache(); } catch { /* filtro opcional */ } }

  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Agenda</h1><p class="pagina__sub" id="rango"></p></div>
      ${puede.reservar(rol) ? `<button class="btn btn--primario" data-nueva>${icono.mas}<span>Nueva cita</span></button>` : ''}
    </header>
    <section class="tarjeta tarjeta--agenda">
      <div class="agenda__barra">
        <div class="agenda__nav">
          <button class="icono-btn" data-nav="-1" aria-label="Anterior">${icono.izq}</button>
          <button class="btn btn--suave btn--chico" data-nav="0">Hoy</button>
          <button class="icono-btn" data-nav="1" aria-label="Siguiente">${icono.der}</button>
        </div>
        <div class="agenda__opciones">
          ${puede.verTodo(rol) ? `<select class="campo campo--chico" id="medico" aria-label="Filtrar por médico"><option value="">Todos los médicos</option>
            ${medicos.map(m => `<option value="${m.id}" ${String(e.medicoId) === String(m.id) ? 'selected' : ''}>${esc(m.nombre)}</option>`).join('')}</select>` : ''}
          <div class="segmentos" role="group" aria-label="Vista">
            <button class="segmento ${e.modo === 'semana' ? 'activo' : ''}" data-modo="semana">Semana</button>
            <button class="segmento ${e.modo === 'mes' ? 'activo' : ''}" data-modo="mes">Mes</button>
          </div>
        </div>
      </div>
      <div class="agenda__leyenda">
        <span><i class="punto punto--pendiente"></i>Pendiente</span><span><i class="punto punto--confirmada"></i>Confirmada</span>
        <span><i class="punto punto--atendida"></i>Atendida</span><span><i class="punto punto--noasistio"></i>No asistió</span>
      </div>
      <div id="agenda-cuerpo" class="agenda__cuerpo"></div>
    </section>`;

  cont.querySelector('[data-nueva]')?.addEventListener('click', ctx.nuevaCita);
  const cuerpo = cont.querySelector('#agenda-cuerpo');
  const rango = cont.querySelector('#rango');

  function limites() {
    if (e.modo === 'semana') { const d = inicioSemana(e.ancla); return { desde: d, hasta: sumarDias(d, 6) }; }
    const [y, m] = e.ancla.split('-').map(Number);
    const d = inicioSemana(ymd(y, m, 1));
    return { desde: d, hasta: sumarDias(d, 41) };
  }

  function titulo(lim) {
    if (e.modo === 'mes') return mesAnio(e.ancla);
    return `${fechaCorta(lim.desde)} – ${fechaCorta(lim.hasta)}`;
  }

  cont.querySelectorAll('[data-nav]').forEach(b => b.addEventListener('click', () => {
    const n = Number(b.dataset.nav);
    if (n === 0) e.ancla = hoyLima();
    else if (e.modo === 'semana') e.ancla = sumarDias(e.ancla, 7 * n);
    else { const [y, m] = e.ancla.split('-').map(Number); e.ancla = ymd(...(m + n < 1 ? [y - 1, 12] : m + n > 12 ? [y + 1, 1] : [y, m + n]), 1); }
    cargar();
  }));
  cont.querySelectorAll('[data-modo]').forEach(b => b.addEventListener('click', () => {
    e.modo = b.dataset.modo;
    cont.querySelectorAll('[data-modo]').forEach(x => x.classList.toggle('activo', x === b));
    cargar();
  }));
  cont.querySelector('#medico')?.addEventListener('change', ev => { e.medicoId = ev.target.value; cargar(); });

  let citasActuales = [];
  const abrir = id => { const c = citasActuales.find(x => String(x.id) === String(id)); if (c) ctx.abrirCita(c); };

  async function cargar() {
    const lim = limites();
    rango.textContent = titulo(lim);
    cuerpo.innerHTML = '<div class="esqueleto-bloque esqueleto-bloque--alto"></div>';
    try {
      citasActuales = (await todasLasCitas({ desde: lim.desde, hasta: lim.hasta, medicoId: e.medicoId }))
        .filter(c => c.estado !== 'CANCELADA'); // las canceladas dejan el horario libre
    } catch (err) {
      cuerpo.innerHTML = `<div class="alerta alerta--error">${esc(err.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      cuerpo.querySelector('[data-reintentar]').addEventListener('click', cargar);
      return;
    }
    if (e.modo === 'semana') semana(lim); else mes(lim);
  }

  function semana(lim) {
    const hoy = hoyLima();
    const dias = Array.from({ length: 7 }, (_, i) => sumarDias(lim.desde, i));
    const mins = citasActuales.map(c => partes(c.fechaHora).min);
    const ini = Math.min(8, ...mins.map(m => Math.floor(m / 60)));
    const fin = Math.min(24, Math.max(19, ...citasActuales.map(c => Math.ceil((partes(c.fechaHora).min + (c.duracionMin || 30)) / 60))));
    const horas = Array.from({ length: fin - ini }, (_, i) => ini + i);
    const ahora = ahoraLima();

    cuerpo.innerHTML = `
      <div class="semana" style="--alto-hora:${ALTO_HORA}px">
        <div class="semana__cab">
          <span class="semana__esq"></span>
          ${dias.map((d, i) => `<div class="semana__dia ${d === hoy ? 'semana__dia--hoy' : ''}">
            <small>${DIAS_CORTOS[i]}</small><b>${Number(d.slice(8))}</b></div>`).join('')}
        </div>
        <div class="semana__cuerpo" style="height:${horas.length * ALTO_HORA}px">
          <div class="semana__horas">${horas.map(h => `<span style="top:${(h - ini) * ALTO_HORA}px">${String(h).padStart(2, '0')}:00</span>`).join('')}</div>
          ${dias.map(d => {
            const delDia = citasActuales.filter(c => partes(c.fechaHora).fecha === d);
            const items = distribuir(delDia).map(({ c, ini: i0, col, cols }) => {
              const t = textoCita(c, rol);
              const top = (i0 - ini * 60) / 60 * ALTO_HORA;
              const alto = Math.max(((c.duracionMin || 30) / 60) * ALTO_HORA - 2, 24);
              return `<button class="evento evento--${c.estado.toLowerCase().replace('_', '')}" data-id="${c.id}"
                style="top:${top}px;height:${alto}px;left:calc(${(col / cols) * 100}% + 2px);width:calc(${100 / cols}% - 4px)"
                title="${esc(partes(c.fechaHora).hora)} · ${esc(c.pacienteNombre)} con ${esc(c.medicoNombre)}">
                <b>${esc(partes(c.fechaHora).hora)}</b> ${esc(t.titulo)}<small>${esc(t.sub)}</small></button>`;
            }).join('');
            const linea = d === hoy && ahora.min >= ini * 60 && ahora.min <= fin * 60
              ? `<span class="ahora" style="top:${(ahora.min - ini * 60) / 60 * ALTO_HORA}px"></span>` : '';
            return `<div class="semana__col ${d === hoy ? 'semana__col--hoy' : ''}">${items}${linea}</div>`;
          }).join('')}
        </div>
      </div>
      ${citasActuales.length ? '' : '<p class="agenda__vacia">No hay citas programadas en esta semana.</p>'}`;
    cuerpo.querySelectorAll('.evento').forEach(b => b.addEventListener('click', () => abrir(b.dataset.id)));
    const hasta = Math.max(ahora.min / 60 - 1, 0) * ALTO_HORA;
    const sc = cuerpo.querySelector('.semana'); if (sc && lim.desde <= hoy && hoy <= lim.hasta) sc.scrollTop = Math.max(0, hasta - ini * ALTO_HORA);
  }

  function mes(lim) {
    const hoy = hoyLima();
    const [, mesActual] = e.ancla.split('-').map(Number);
    const dias = Array.from({ length: 42 }, (_, i) => sumarDias(lim.desde, i));
    cuerpo.innerHTML = `
      <div class="mes">
        ${DIAS_CORTOS.map(d => `<div class="mes__cab">${d}</div>`).join('')}
        ${dias.map(d => {
          const delDia = citasActuales.filter(c => partes(c.fechaHora).fecha === d).sort((a, b) => a.fechaHora.localeCompare(b.fechaHora));
          const fuera = Number(d.slice(5, 7)) !== mesActual;
          return `<div class="mes__dia ${fuera ? 'mes__dia--fuera' : ''} ${d === hoy ? 'mes__dia--hoy' : ''}">
            <button class="mes__num" data-dia="${d}" aria-label="Ver semana del ${esc(fechaCorta(d))}">${Number(d.slice(8))}</button>
            ${delDia.slice(0, 3).map(c => `<button class="mes__cita mes__cita--${c.estado.toLowerCase().replace('_', '')}" data-id="${c.id}">
              <b>${esc(partes(c.fechaHora).hora)}</b> ${esc(corto(textoCita(c, rol).titulo))}</button>`).join('')}
            ${delDia.length > 3 ? `<button class="mes__mas" data-dia="${d}">+${delDia.length - 3} más</button>` : ''}
          </div>`;
        }).join('')}
      </div>`;
    cuerpo.querySelectorAll('.mes__cita').forEach(b => b.addEventListener('click', () => abrir(b.dataset.id)));
    cuerpo.querySelectorAll('[data-dia]').forEach(b => b.addEventListener('click', () => {
      e.modo = 'semana'; e.ancla = b.dataset.dia;
      cont.querySelectorAll('[data-modo]').forEach(x => x.classList.toggle('activo', x.dataset.modo === 'semana'));
      cargar();
    }));
  }

  await cargar();
  return { recargar: cargar };
}
