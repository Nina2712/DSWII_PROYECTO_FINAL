import { api, medicosEnCache } from '../api.js';
import { esc, badge, ESTADOS, etiquetaCita, iniciales, avatar, icono, puede, numero } from '../util.js';

const TAM = 10;

export async function pintarCitas(cont, ctx, estadoPrevio) {
  const { rol } = ctx;
  const f = estadoPrevio || { estado: '', medicoId: '', desde: '', hasta: '', orden: 'desc', page: 0 };
  let medicos = [];
  if (rol !== 'MEDICO') { try { medicos = await medicosEnCache(); } catch { /* el filtro es opcional */ } }

  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Citas</h1>
        <p class="pagina__sub" id="conteo">${rol === 'MEDICO' ? 'Tu agenda' : rol === 'PACIENTE' ? 'Tu historial de citas' : 'Todas las citas de la clínica'}</p></div>
      ${puede.reservar(rol) ? `<button class="btn btn--primario" data-nueva>${icono.mas}<span>Nueva cita</span></button>` : ''}
    </header>
    <section class="tarjeta">
      <form class="filtros" id="filtros">
        <label class="filtro"><span>Estado</span>
          <select class="campo" name="estado"><option value="">Todos</option>
            ${Object.entries(ESTADOS).map(([k, v]) => `<option value="${k}" ${f.estado === k ? 'selected' : ''}>${v.texto}</option>`).join('')}
          </select></label>
        ${rol !== 'MEDICO' ? `
        <label class="filtro"><span>Médico</span>
          <select class="campo" name="medicoId"><option value="">Todos</option>
            ${medicos.map(m => `<option value="${m.id}" ${String(f.medicoId) === String(m.id) ? 'selected' : ''}>${esc(m.nombre)}</option>`).join('')}
          </select></label>` : ''}
        <label class="filtro"><span>Desde</span><input class="campo" type="date" name="desde" value="${esc(f.desde)}"></label>
        <label class="filtro"><span>Hasta</span><input class="campo" type="date" name="hasta" value="${esc(f.hasta)}"></label>
        <label class="filtro"><span>Orden</span>
          <select class="campo" name="orden">
            <option value="desc" ${f.orden === 'desc' ? 'selected' : ''}>Más recientes primero</option>
            <option value="asc" ${f.orden === 'asc' ? 'selected' : ''}>Más antiguas primero</option>
          </select></label>
        <button type="button" class="btn btn--texto" id="limpiar">Limpiar</button>
      </form>
      <div id="tabla" class="tabla-contenedor"></div>
      <nav class="paginacion" id="paginacion" aria-label="Paginación"></nav>
    </section>`;

  cont.querySelector('[data-nueva]')?.addEventListener('click', ctx.nuevaCita);
  const form = cont.querySelector('#filtros');
  const leer = () => {
    const d = new FormData(form);
    f.estado = d.get('estado') || ''; f.medicoId = d.get('medicoId') || '';
    f.desde = d.get('desde') || ''; f.hasta = d.get('hasta') || ''; f.orden = d.get('orden') || 'desc';
  };
  form.addEventListener('change', () => { leer(); f.page = 0; cargar(); });
  cont.querySelector('#limpiar').addEventListener('click', () => {
    Object.assign(f, { estado: '', medicoId: '', desde: '', hasta: '', orden: 'desc', page: 0 });
    pintarCitas(cont, ctx, f);
  });

  const tabla = cont.querySelector('#tabla');
  const pag = cont.querySelector('#paginacion');

  async function cargar() {
    tabla.innerHTML = '<div class="esqueleto-filas">' + '<span></span>'.repeat(6) + '</div>';
    pag.innerHTML = '';
    let r;
    try {
      r = await api.citas({ estado: f.estado, medicoId: f.medicoId, desde: f.desde, hasta: f.hasta, orden: f.orden, page: f.page, size: TAM });
    } catch (e) {
      tabla.innerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      tabla.querySelector('[data-reintentar]').addEventListener('click', cargar);
      return;
    }
    if (f.page > 0 && r.content.length === 0) { f.page = Math.max(0, r.totalPages - 1); return cargar(); }

    cont.querySelector('#conteo').textContent = `${numero(r.totalElements)} ${r.totalElements === 1 ? 'cita' : 'citas'}${f.estado || f.medicoId || f.desde || f.hasta ? ' con los filtros aplicados' : ''}`;
    if (!r.content.length) {
      tabla.innerHTML = `<div class="vacio">${icono.vacio}<p><b>No hay citas para mostrar</b></p><p class="tenue">Cambia los filtros${puede.reservar(rol) ? ' o reserva una nueva cita' : ''}.</p></div>`;
      return;
    }
    tabla.innerHTML = `
      <table class="tabla">
        <thead><tr><th>Fecha y hora</th>${rol !== 'PACIENTE' ? '<th>Paciente</th>' : ''}${rol !== 'MEDICO' ? '<th>Médico</th>' : ''}<th>Sede</th><th>Estado</th><th>Motivo</th></tr></thead>
        <tbody>${r.content.map(c => `
          <tr tabindex="0" data-id="${c.id}">
            <td class="nowrap"><b>${esc(etiquetaCita(c.fechaHora))}</b></td>
            ${rol !== 'PACIENTE' ? `<td><span class="celda-persona"><span class="avatar">${esc(iniciales(c.pacienteNombre))}</span><span>${esc(c.pacienteNombre)}<small>DNI ${esc(c.pacienteDni)}</small></span></span></td>` : ''}
            ${rol !== 'MEDICO' ? `<td><span class="celda-persona">${avatar(c.medicoNombre, medicos.find(m => m.id === c.medicoId)?.foto, 'avatar--medico')}<span>${esc(c.medicoNombre)}<small>${esc(c.especialidad)}</small></span></span></td>` : ''}
            <td>${esc(c.sede)}</td>
            <td>${badge(c.estado)}</td>
            <td class="motivo">${c.motivo ? esc(c.motivo) : '<span class="tenue">—</span>'}</td>
          </tr>`).join('')}</tbody>
      </table>`;
    const abrir = tr => { const c = r.content.find(x => String(x.id) === tr.dataset.id); if (c) ctx.abrirCita(c); };
    tabla.querySelectorAll('tbody tr').forEach(tr => {
      tr.addEventListener('click', () => abrir(tr));
      tr.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); abrir(tr); } });
    });

    const desde = r.page * r.size + 1, hasta = r.page * r.size + r.content.length;
    pag.innerHTML = `
      <span class="tenue">Mostrando ${desde}–${hasta} de ${numero(r.totalElements)}</span>
      <div class="paginacion__botones">
        <button class="btn btn--suave btn--chico" data-p="-1" ${r.page === 0 ? 'disabled' : ''}>${icono.izq}Anterior</button>
        <span class="paginacion__pag">Página ${r.page + 1} de ${Math.max(r.totalPages, 1)}</span>
        <button class="btn btn--suave btn--chico" data-p="1" ${r.page + 1 >= r.totalPages ? 'disabled' : ''}>Siguiente${icono.der}</button>
      </div>`;
    pag.querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => { f.page += Number(b.dataset.p); cargar(); }));
  }

  await cargar();
  return { recargar: cargar };
}
