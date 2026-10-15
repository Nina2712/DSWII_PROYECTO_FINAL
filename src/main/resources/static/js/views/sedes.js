// Vista "Sedes" (ADMIN): sedes con los médicos y especialidades de cada una, y su administración.
import { api, olvidarMedicos } from '../api.js';
import { esc, icono, toast, numero } from '../util.js';
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
            <button class="btn btn--suave btn--chico" data-editar="${s.id}">${icono.editar}<span>Editar</span></button>
            <button class="btn btn--peligro-suave btn--chico" data-eliminar="${s.id}" ${s.enUso ? 'disabled' : ''}
              title="${s.enUso ? 'Tiene médicos asignados (activos o no): cámbialos de sede para poder eliminarla' : 'Eliminar sede'}">${icono.basura}<span>Eliminar</span></button>
          </footer>
        </article>`).join('')}</div>
      ${matriz()}`;
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
