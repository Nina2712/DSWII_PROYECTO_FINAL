// Vista "Médicos" (ADMIN): listado con filtros, alta y edición.
import { api } from '../api.js';
import { esc, iniciales, avatar, icono, numero, DIAS_CORTOS } from '../util.js';
import { abrirFormularioMedico } from '../medico-form.js';

const ORDEN = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
const sinTildes = t => String(t).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/** "Lun · Mié · Vie 08:00–13:00" agrupando los días que comparten el mismo tramo. */
function resumenHorarios(horarios) {
  if (!horarios.length) return '<span class="tenue">Sin horarios</span>';
  const grupos = new Map();
  for (const h of horarios) {
    const clave = `${h.inicio}–${h.fin}`;
    if (!grupos.has(clave)) grupos.set(clave, []);
    grupos.get(clave).push(h.dia);
  }
  return [...grupos].map(([tramo, dias]) =>
    `<span class="horario-res"><b>${dias.sort((a, b) => ORDEN.indexOf(a) - ORDEN.indexOf(b)).map(d => DIAS_CORTOS[ORDEN.indexOf(d)]).join(' · ')}</b> ${esc(tramo)}</span>`).join('');
}

export async function pintarMedicos(cont, ctx) {
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Médicos</h1><p class="pagina__sub" id="conteo">Cargando…</p></div>
      <button class="btn btn--primario" data-nuevo>${icono.mas}<span>Nuevo médico</span></button>
    </header>
    <section class="tarjeta">
      <form class="filtros" onsubmit="return false">
        <label class="filtro filtro--ancho"><span>Buscar</span>
          <input class="campo" type="search" id="buscar" placeholder="Nombre, CMP o correo" autocomplete="off"></label>
        <label class="filtro"><span>Especialidad</span><select class="campo" id="f-esp"><option value="">Todas</option></select></label>
        <label class="filtro"><span>Estado</span>
          <select class="campo" id="f-estado"><option value="">Todos</option><option value="activos">Activos</option><option value="inactivos">Inactivos</option></select></label>
      </form>
      <div id="tabla" class="tabla-contenedor"><div class="esqueleto-filas">${'<span></span>'.repeat(5)}</div></div>
    </section>`;

  const tabla = cont.querySelector('#tabla');
  const conteo = cont.querySelector('#conteo');
  const $ = s => cont.querySelector(s);
  let medicos = [], especialidades = [], sedes = [];

  function dibujar() {
    const q = sinTildes($('#buscar').value.trim());
    const esp = $('#f-esp').value, estado = $('#f-estado').value;
    const lista = medicos.filter(m =>
      (!q || sinTildes(`${m.nombres} ${m.apellidos} ${m.cmp} ${m.email}`).includes(q)) &&
      (!esp || String(m.especialidadId) === esp) &&
      (!estado || (estado === 'activos') === m.activo));
    const filtrado = q || esp || estado;
    conteo.textContent = filtrado
      ? `${numero(lista.length)} de ${numero(medicos.length)} médicos`
      : `${numero(medicos.length)} ${medicos.length === 1 ? 'médico registrado' : 'médicos registrados'}`;
    if (!lista.length) {
      tabla.innerHTML = `<div class="vacio">${icono.vacio}<p><b>${medicos.length ? 'Ningún médico coincide' : 'Aún no hay médicos'}</b></p>
        <p class="tenue">${medicos.length ? 'Cambia la búsqueda o los filtros.' : 'Da de alta al primero con el botón de arriba.'}</p></div>`;
      return;
    }
    tabla.innerHTML = `
      <table class="tabla">
        <thead><tr><th>Médico</th><th>Especialidad</th><th>Sede</th><th>Horario de atención</th><th>Estado</th></tr></thead>
        <tbody>${lista.map(m => `
          <tr tabindex="0" data-id="${m.id}" aria-label="Editar a ${esc(m.nombres)} ${esc(m.apellidos)}">
            <td><span class="celda-persona">${avatar(`${m.nombres} ${m.apellidos}`, m.foto, 'avatar--medico')}
              <span><b>${esc(m.nombres)} ${esc(m.apellidos)}</b><small>${esc(m.cmp)} · ${esc(m.email)}</small></span></span></td>
            <td>${esc(m.especialidad)}</td>
            <td>${esc(m.sede)}</td>
            <td class="horarios-res">${resumenHorarios(m.horarios)}</td>
            <td>${m.activo ? '<span class="badge badge--atendida">Activo</span>' : '<span class="badge badge--inactivo">Inactivo</span>'}</td>
          </tr>`).join('')}</tbody>
      </table>`;
    const editar = tr => abrirFormularioMedico({ medico: medicos.find(m => String(m.id) === tr.dataset.id), especialidades, sedes, alGuardar: cargar });
    tabla.querySelectorAll('tbody tr').forEach(tr => {
      tr.addEventListener('click', () => editar(tr));
      tr.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); editar(tr); } });
    });
  }

  async function cargar() {
    try {
      [medicos, especialidades, sedes] = await Promise.all([api.medicosAdmin(), api.especialidades(), api.sedes()]);
      const actual = $('#f-esp').value;
      $('#f-esp').innerHTML = '<option value="">Todas</option>' +
        especialidades.map(e => `<option value="${e.id}" ${String(e.id) === actual ? 'selected' : ''}>${esc(e.nombre)}</option>`).join('');
      dibujar();
    } catch (e) {
      conteo.textContent = '';
      tabla.innerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      tabla.querySelector('[data-reintentar]').addEventListener('click', cargar);
    }
  }

  $('#buscar').addEventListener('input', dibujar);
  $('#f-esp').addEventListener('change', dibujar);
  $('#f-estado').addEventListener('change', dibujar);
  $('[data-nuevo]').addEventListener('click', () => {
    if (!especialidades.length && !sedes.length) return; // aún cargando o con error
    abrirFormularioMedico({ medico: null, especialidades, sedes, alGuardar: cargar });
  });
  await cargar();
  return { recargar: cargar };
}
