// Piezas comunes de los diálogos: creación, confirmación y botón con estado de carga.
import { esc } from './util.js';

export function crearDialogo(clase) {
  const dlg = document.createElement('dialog');
  dlg.className = clase;
  dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); });
  dlg.addEventListener('close', () => dlg.remove());
  document.body.appendChild(dlg);
  return dlg;
}

/** Pregunta de confirmación. Devuelve una promesa con true / false. */
export function confirmar({ titulo, mensaje, textoOk = 'Confirmar', peligro = false }) {
  return new Promise(resolve => {
    const dlg = crearDialogo('modal modal--chico');
    dlg.innerHTML = `
      <form method="dialog" class="modal__cuerpo">
        <h2 class="modal__titulo">${esc(titulo)}</h2>
        <p class="modal__texto">${esc(mensaje)}</p>
        <div class="modal__pie">
          <button class="btn btn--suave" value="no">Volver</button>
          <button class="btn ${peligro ? 'btn--peligro' : 'btn--primario'}" value="si" autofocus>${esc(textoOk)}</button>
        </div>
      </form>`;
    dlg.addEventListener('close', () => resolve(dlg.returnValue === 'si'));
    dlg.showModal();
  });
}

export async function conCarga(boton, tarea) {
  const original = boton.innerHTML;
  boton.disabled = true;
  boton.classList.add('cargando');
  try { return await tarea(); }
  finally { boton.disabled = false; boton.classList.remove('cargando'); boton.innerHTML = original; }
}
