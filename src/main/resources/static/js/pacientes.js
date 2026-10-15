// Diálogo con el que recepción o administración registra a un paciente que llega a la clínica.
import { api } from './api.js';
import { crearDialogo, conCarga } from './dialogo-base.js';
import { icono, toast } from './util.js';
import { camposPaciente, activarClave, leerPaciente, validarPaciente, mostrarErrores, limpiarErrores, erroresDelServidor } from './formulario-paciente.js';

/** `alCrear(paciente)` recibe {id, nombre, dni} del paciente recién registrado. */
export function abrirNuevoPaciente({ alCrear }) {
  const dlg = crearDialogo('modal');
  dlg.innerHTML = `
    <form class="modal__cuerpo" novalidate>
      <header class="modal__cabecera">
        <div><h2 class="modal__titulo">Registrar paciente</h2>
          <p class="modal__sub">Se crea su cuenta. Entrégale la contraseña inicial para que pueda entrar.</p></div>
        <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
      </header>
      ${camposPaciente('np')}
      <p id="np-error" class="alerta alerta--error" role="alert" hidden></p>
      <footer class="modal__pie">
        <button type="button" class="btn btn--suave" data-cerrar>Cancelar</button>
        <button type="submit" class="btn btn--primario" id="np-enviar">Registrar paciente</button>
      </footer>
    </form>`;
  const form = dlg.querySelector('form');
  const error = dlg.querySelector('#np-error');
  dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));
  activarClave(form);

  form.addEventListener('submit', async ev => {
    ev.preventDefault();
    error.hidden = true;
    const locales = validarPaciente(form);
    if (Object.keys(locales).length) { mostrarErrores(form, locales); return; }
    limpiarErrores(form);
    try {
      const paciente = await conCarga(dlg.querySelector('#np-enviar'), () => api.crearPaciente(leerPaciente(form)));
      toast(`Paciente registrado: ${paciente.nombre}`);
      dlg.close();
      alCrear?.(paciente);
    } catch (e) {
      const porCampo = erroresDelServidor(e);
      const sueltos = mostrarErrores(form, porCampo);
      const texto = porCampo ? sueltos.join('. ') : e.message;
      error.textContent = texto; error.hidden = !texto;
    }
  });
  dlg.showModal();
  form.querySelector('[name="nombres"]').focus();
}
