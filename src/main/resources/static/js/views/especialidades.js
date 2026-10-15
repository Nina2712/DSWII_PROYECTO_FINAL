// Vista "Especialidades" (ADMIN): alta, edición y baja de especialidades.
import { api, olvidarMedicos } from '../api.js';
import { esc, icono, toast, numero } from '../util.js';
import { crearDialogo, confirmar, conCarga } from '../dialogo-base.js';

export async function pintarEspecialidades(cont, ctx) {
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Especialidades</h1><p class="pagina__sub" id="conteo">Cargando…</p></div>
      <button class="btn btn--primario" data-nueva>${icono.mas}<span>Nueva especialidad</span></button>
    </header>
    <section class="tarjeta"><div id="tabla" class="tabla-contenedor"><div class="esqueleto-filas">${'<span></span>'.repeat(5)}</div></div></section>`;

  const tabla = cont.querySelector('#tabla');
  const conteo = cont.querySelector('#conteo');
  let lista = [];

  function dibujar() {
    conteo.textContent = `${numero(lista.length)} ${lista.length === 1 ? 'especialidad' : 'especialidades'}`;
    if (!lista.length) {
      tabla.innerHTML = `<div class="vacio">${icono.vacio}<p><b>Aún no hay especialidades</b></p><p class="tenue">Crea la primera para poder dar de alta médicos.</p></div>`;
      return;
    }
    tabla.innerHTML = `
      <table class="tabla tabla--plana">
        <thead><tr><th>Especialidad</th><th>Descripción</th><th>Médicos activos</th><th>Se atiende en</th><th><span class="sr-only">Acciones</span></th></tr></thead>
        <tbody>${lista.map(e => `
          <tr>
            <td><b>${esc(e.nombre)}</b></td>
            <td class="motivo">${e.descripcion ? esc(e.descripcion) : '<span class="tenue">—</span>'}</td>
            <td>${numero(e.medicos)}</td>
            <td>${e.sedes.length ? `<span class="etiquetas">${e.sedes.map(s => `<span class="etiqueta">${esc(s)}</span>`).join('')}</span>` : '<span class="tenue">Sin sedes</span>'}</td>
            <td class="acciones-fila">
              <button class="btn btn--suave btn--chico" data-editar="${e.id}">${icono.editar}<span>Editar</span></button>
              <button class="btn btn--peligro-suave btn--chico" data-eliminar="${e.id}" ${e.enUso ? 'disabled' : ''}
                title="${e.enUso ? 'Tiene médicos asignados (activos o no): reasígnalos para poder eliminarla' : 'Eliminar especialidad'}">${icono.basura}<span>Eliminar</span></button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    tabla.querySelectorAll('[data-editar]').forEach(b => b.addEventListener('click', () => formulario(lista.find(e => String(e.id) === b.dataset.editar))));
    tabla.querySelectorAll('[data-eliminar]').forEach(b => b.addEventListener('click', () => eliminar(lista.find(e => String(e.id) === b.dataset.eliminar), b)));
  }

  async function cargar() {
    try { lista = await api.especialidades(); dibujar(); }
    catch (e) {
      conteo.textContent = '';
      tabla.innerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      tabla.querySelector('[data-reintentar]').addEventListener('click', cargar);
    }
  }

  async function eliminar(esp, boton) {
    if (!await confirmar({ titulo: 'Eliminar especialidad', mensaje: `Se eliminará "${esp.nombre}". Esta acción no se puede deshacer.`, textoOk: 'Eliminar', peligro: true })) return;
    try {
      await conCarga(boton, () => api.eliminarEspecialidad(esp.id));
      olvidarMedicos();
      toast('Especialidad eliminada');
      cargar();
    } catch (e) { toast(e.message, 'error'); }
  }

  function formulario(esp) {
    const dlg = crearDialogo('modal');
    dlg.innerHTML = `
      <form class="modal__cuerpo" novalidate>
        <header class="modal__cabecera">
          <div><h2 class="modal__titulo">${esp ? 'Editar especialidad' : 'Nueva especialidad'}</h2>
            <p class="modal__sub">Aparece al dar de alta médicos y al reservar citas.</p></div>
          <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
        </header>
        <div class="grupo"><label class="campo__etiqueta" for="esp-nombre">Nombre</label>
          <input class="campo" id="esp-nombre" maxlength="80" required value="${esc(esp?.nombre || '')}" placeholder="Ej.: Neurología"></div>
        <div class="grupo"><label class="campo__etiqueta" for="esp-desc">Descripción <span class="tenue">(opcional)</span></label>
          <textarea class="campo" id="esp-desc" rows="3" maxlength="255" placeholder="Ej.: Sistema nervioso y cerebro">${esc(esp?.descripcion || '')}</textarea></div>
        <p id="error" class="alerta alerta--error" role="alert" hidden></p>
        <footer class="modal__pie">
          <button type="button" class="btn btn--suave" data-cerrar>Cancelar</button>
          <button type="submit" class="btn btn--primario" id="guardar">${esp ? 'Guardar cambios' : 'Crear especialidad'}</button>
        </footer>
      </form>`;
    const error = dlg.querySelector('#error');
    dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));
    dlg.querySelector('form').addEventListener('submit', async ev => {
      ev.preventDefault();
      error.hidden = true;
      const nombre = dlg.querySelector('#esp-nombre').value.trim();
      if (!nombre) { error.textContent = 'Escribe el nombre de la especialidad.'; error.hidden = false; dlg.querySelector('#esp-nombre').focus(); return; }
      const cuerpo = { nombre, descripcion: dlg.querySelector('#esp-desc').value.trim() || undefined };
      try {
        await conCarga(dlg.querySelector('#guardar'), () => esp ? api.actualizarEspecialidad(esp.id, cuerpo) : api.crearEspecialidad(cuerpo));
        olvidarMedicos();
        toast(esp ? 'Especialidad actualizada' : 'Especialidad creada');
        dlg.close();
        cargar();
      } catch (e) { error.textContent = e.message; error.hidden = false; }
    });
    dlg.showModal();
    dlg.querySelector('#esp-nombre').focus();
  }

  cont.querySelector('[data-nueva]').addEventListener('click', () => formulario(null));
  await cargar();
  return { recargar: cargar };
}
