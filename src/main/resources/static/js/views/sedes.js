// Vista "Sedes" (ADMIN): sedes con los médicos y especialidades de cada una, y su administración.
import { api, olvidarMedicos } from '../api.js';
import { esc, icono, toast, numero, badge, etiquetaCita } from '../util.js';
import { crearDialogo, confirmar, conCarga } from '../dialogo-base.js';

export async function pintarSedes(cont, ctx) {
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Sedes</h1><p class="pagina__sub" id="conteo">Cargando…</p></div>
      <button class="btn btn--primario" data-nueva>${icono.mas}<span>Nueva sede</span></button>
    </header>
    <div id="contenido"><div class="esqueleto-bloque"></div></div>`;

  const contenido = cont.querySelector('#contenido');
  const conteo = cont.querySelector('#conteo');
  let sedes = [];

  function dibujar() {
    conteo.textContent = `${numero(sedes.length)} ${sedes.length === 1 ? 'sede' : 'sedes'}`;
    if (!sedes.length) {
      contenido.innerHTML = `<section class="tarjeta"><div class="vacio">${icono.vacio}<p><b>Aún no hay sedes</b></p><p class="tenue">Crea la primera para poder dar de alta médicos.</p></div></section>`;
      return;
    }
    contenido.innerHTML = `
      <div class="sedes">${sedes.map(s => `
        <article class="tarjeta sede" aria-labelledby="sede-${s.id}">
          <header class="sede__cab">
            <div><h2 id="sede-${s.id}">${esc(s.nombre)}</h2><p class="tenue">${esc(s.direccion)} · ${esc(s.distrito)}</p></div>
            <span class="sede__medicos"><b>${numero(s.medicos)}</b><small>${s.medicos === 1 ? 'médico' : 'médicos'}</small></span>
          </header>
          <div class="sede__cuerpo">
            ${s.especialidades.length
              ? `<ul class="sede__esp">${s.especialidades.map(e => `<li><span>${esc(e.nombre)}</span><b>${numero(e.medicos)}</b></li>`).join('')}</ul>`
              : '<p class="tenue">Sin médicos activos: aún no se atiende ninguna especialidad.</p>'}
          </div>
          <footer class="sede__pie">
            <button class="btn btn--primario btn--chico" data-panel="${s.id}">${icono.resumen}<span>Ver panel</span></button>
            <button class="btn btn--suave btn--chico" data-editar="${s.id}">${icono.editar}<span>Editar</span></button>
            <button class="btn btn--peligro-suave btn--chico" data-eliminar="${s.id}" ${s.enUso ? 'disabled' : ''}
              title="${s.enUso ? 'Tiene médicos asignados (activos o no): cámbialos de sede para poder eliminarla' : 'Eliminar sede'}">${icono.basura}<span>Eliminar</span></button>
          </footer>
        </article>`).join('')}</div>
      ${matriz()}`;
    contenido.querySelectorAll('[data-panel]').forEach(b => b.addEventListener('click', () => panel(sedes.find(s => String(s.id) === b.dataset.panel))));
    contenido.querySelectorAll('[data-editar]').forEach(b => b.addEventListener('click', () => formulario(sedes.find(s => String(s.id) === b.dataset.editar))));
    contenido.querySelectorAll('[data-eliminar]').forEach(b => b.addEventListener('click', () => eliminar(sedes.find(s => String(s.id) === b.dataset.eliminar), b)));
  }

  /** Especialidades en filas y sedes en columnas: cuántos médicos activos hay en cada cruce. */
  function matriz() {
    const nombres = new Map();
    sedes.forEach(s => s.especialidades.forEach(e => nombres.set(e.id, e.nombre)));
    if (!nombres.size) return '';
    const filas = [...nombres].sort((a, b) => a[1].localeCompare(b[1], 'es'));
    return `
      <section class="tarjeta" aria-labelledby="t-matriz">
        <div class="tarjeta__cab"><h2 id="t-matriz">Cobertura por sede</h2><span class="tenue">Médicos activos por especialidad</span></div>
        <div class="tabla-contenedor">
          <table class="tabla tabla--plana matriz">
            <thead><tr><th>Especialidad</th>${sedes.map(s => `<th class="num">${esc(s.nombre)}</th>`).join('')}</tr></thead>
            <tbody>${filas.map(([id, nombre]) => `
              <tr><td><b>${esc(nombre)}</b></td>${sedes.map(s => {
                const e = s.especialidades.find(x => x.id === id);
                return e ? `<td class="num"><span class="matriz__si">${numero(e.medicos)}</span></td>` : '<td class="num"><span class="matriz__no" aria-label="Sin médicos">—</span></td>';
              }).join('')}</tr>`).join('')}</tbody>
          </table>
        </div>
      </section>`;
  }

  async function cargar() {
    try { sedes = await api.sedesDetalle(); dibujar(); }
    catch (e) {
      conteo.textContent = '';
      contenido.innerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      contenido.querySelector('[data-reintentar]').addEventListener('click', cargar);
    }
  }

  /** Panel de una sede: carga de hoy, citas por atender, resultados del mes, equipo y próximas citas. */
  async function panel(sede) {
    const dlg = crearDialogo('modal modal--ancho');
    dlg.innerHTML = `<div class="modal__cuerpo"><div class="esqueleto-bloque"></div></div>`;
    dlg.showModal();
    try {
      const r = await api.sedeResumen(sede.id);
      const vistos = r.atendidas30d + r.noAsistio30d;
      const inasistencia = vistos ? Math.round(r.noAsistio30d * 100 / vistos) : null;
      dlg.innerHTML = `
        <div class="modal__cuerpo">
          <header class="modal__cabecera">
            <div><h2 class="modal__titulo">${esc(r.nombre)}</h2><p class="modal__sub">${esc(r.direccion)} · ${esc(r.distrito)}</p></div>
            <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
          </header>
          <dl class="kpis-sede">
            <div><dt>Citas hoy</dt><dd>${numero(r.citasHoy)}</dd></div>
            <div><dt>Por atender</dt><dd>${numero(r.porAtender)}</dd><small>pendientes y confirmadas</small></div>
            <div><dt>Atendidas</dt><dd>${numero(r.atendidas30d)}</dd><small>últimos 30 días</small></div>
            <div><dt>No asistieron</dt><dd>${inasistencia === null ? '—' : inasistencia + '%'}</dd><small>${numero(r.noAsistio30d)} de ${numero(vistos)} · ${numero(r.canceladas30d)} canceladas</small></div>
          </dl>
          <h3 class="modal__seccion">Equipo médico</h3>
          ${r.medicos.length ? `
          <div class="tabla-contenedor"><table class="tabla tabla--plana">
            <thead><tr><th>Médico</th><th>Especialidad</th><th class="num">Hoy</th><th class="num">Por atender</th></tr></thead>
            <tbody>${r.medicos.map(m => `
              <tr class="${m.activo ? '' : 'fila--inactiva'}">
                <td><b>${esc(m.nombre)}</b><small class="bloque tenue">${esc(m.cmp)}${m.activo ? '' : ' · Inactivo'}</small></td>
                <td>${esc(m.especialidad)}</td><td class="num">${numero(m.citasHoy)}</td><td class="num">${numero(m.porAtender)}</td>
              </tr>`).join('')}</tbody>
          </table></div>` : '<p class="tenue">Esta sede aún no tiene médicos asignados.</p>'}
          <h3 class="modal__seccion">Próximas citas</h3>
          ${r.proximas.length ? `<ul class="citas-afectadas">${r.proximas.map(c => `
            <li><span><b>${esc(etiquetaCita(c.fechaHora))}</b><small>${esc(c.paciente)} · ${esc(c.medico)} (${esc(c.especialidad)})</small></span>${badge(c.estado)}</li>`).join('')}</ul>`
            : '<p class="tenue">No hay citas por atender en esta sede.</p>'}
          <footer class="modal__pie">
            <button type="button" class="btn btn--suave" data-cerrar>Cerrar</button>
            <button type="button" class="btn btn--primario" data-ir>Ver todas las citas</button>
          </footer>
        </div>`;
      dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));
      dlg.querySelector('[data-ir]').addEventListener('click', () => { dlg.close(); location.hash = '#/citas'; });
    } catch (e) {
      dlg.innerHTML = `<div class="modal__cuerpo"><div class="alerta alerta--error">${esc(e.message)}</div><footer class="modal__pie"><button type="button" class="btn btn--suave" data-cerrar>Cerrar</button></footer></div>`;
      dlg.querySelector('[data-cerrar]').addEventListener('click', () => dlg.close());
    }
  }

  async function eliminar(sede, boton) {
    if (!await confirmar({ titulo: 'Eliminar sede', mensaje: `Se eliminará "${sede.nombre}". Esta acción no se puede deshacer.`, textoOk: 'Eliminar', peligro: true })) return;
    try {
      await conCarga(boton, () => api.eliminarSede(sede.id));
      olvidarMedicos();
      toast('Sede eliminada');
      cargar();
    } catch (e) { toast(e.message, 'error'); }
  }

  function formulario(sede) {
    const dlg = crearDialogo('modal');
    dlg.innerHTML = `
      <form class="modal__cuerpo" novalidate>
        <header class="modal__cabecera">
          <div><h2 class="modal__titulo">${sede ? 'Editar sede' : 'Nueva sede'}</h2>
            <p class="modal__sub">Los médicos se asignan a una sede al darlos de alta.</p></div>
          <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
        </header>
        <div class="grupo"><label class="campo__etiqueta" for="sd-nombre">Nombre</label>
          <input class="campo" id="sd-nombre" maxlength="100" required value="${esc(sede?.nombre || '')}" placeholder="Ej.: Sede Surco"></div>
        <div class="grupo"><label class="campo__etiqueta" for="sd-dir">Dirección</label>
          <input class="campo" id="sd-dir" maxlength="200" required value="${esc(sede?.direccion || '')}" placeholder="Ej.: Av. Primavera 1450"></div>
        <div class="grupo"><label class="campo__etiqueta" for="sd-dis">Distrito</label>
          <input class="campo" id="sd-dis" maxlength="80" required value="${esc(sede?.distrito || '')}" placeholder="Ej.: Santiago de Surco"></div>
        <p id="sd-error" class="alerta alerta--error" role="alert" hidden></p>
        <footer class="modal__pie">
          <button type="button" class="btn btn--suave" data-cerrar>Cancelar</button>
          <button type="submit" class="btn btn--primario" id="sd-guardar">${sede ? 'Guardar cambios' : 'Crear sede'}</button>
        </footer>
      </form>`;
    const error = dlg.querySelector('#sd-error');
    dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));
    dlg.querySelector('form').addEventListener('submit', async ev => {
      ev.preventDefault();
      error.hidden = true;
      const cuerpo = { nombre: dlg.querySelector('#sd-nombre').value.trim(), direccion: dlg.querySelector('#sd-dir').value.trim(), distrito: dlg.querySelector('#sd-dis').value.trim() };
      const faltante = !cuerpo.nombre ? ['Escribe el nombre de la sede.', '#sd-nombre'] : !cuerpo.direccion ? ['Escribe la dirección.', '#sd-dir'] : !cuerpo.distrito ? ['Escribe el distrito.', '#sd-dis'] : null;
      if (faltante) { error.textContent = faltante[0]; error.hidden = false; dlg.querySelector(faltante[1]).focus(); return; }
      try {
        await conCarga(dlg.querySelector('#sd-guardar'), () => sede ? api.actualizarSede(sede.id, cuerpo) : api.crearSede(cuerpo));
        olvidarMedicos();
        toast(sede ? 'Sede actualizada' : 'Sede creada');
        dlg.close();
        cargar();
      } catch (e) { error.textContent = e.message; error.hidden = false; }
    });
    dlg.showModal();
    dlg.querySelector('#sd-nombre').focus();
  }

  cont.querySelector('[data-nueva]').addEventListener('click', () => formulario(null));
  await cargar();
  return { recargar: cargar };
}
