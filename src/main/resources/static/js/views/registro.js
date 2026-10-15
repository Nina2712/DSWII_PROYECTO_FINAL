// Pantalla "Crear cuenta": registro propio de pacientes.
import { api, sesion } from '../api.js';
import { icono, toast } from '../util.js';
import { camposPaciente, activarClave, leerPaciente, validarPaciente, mostrarErrores, limpiarErrores, erroresDelServidor } from '../formulario-paciente.js';

export function pintarRegistro(app, alEntrar, alternarTema, irALogin) {
  app.innerHTML = `
    <main class="login">
      <section class="login__marca" aria-hidden="true">
        <div class="login__marca-interior">
          <img src="/img/logo-blanco.svg" alt="" class="login__logo">
          <h1>Crea tu cuenta y reserva tu cita en minutos.</h1>
          <ul>
            <li><b>Elige médico y horario</b><span>Ves solo los bloques libres, sin llamadas ni esperas.</span></li>
            <li><b>Reprograma cuando lo necesites</b><span>Puedes cambiar o cancelar tu cita hasta 2 horas antes.</span></li>
            <li><b>Tu historial en un solo lugar</b><span>Consulta tus citas pasadas y las próximas.</span></li>
          </ul>
        </div>
      </section>
      <section class="login__panel">
        <button class="icono-btn login__tema" id="tema" aria-label="Cambiar entre modo claro y oscuro">${icono.luna}</button>
        <form class="login__form login__form--ancho" novalidate>
          <span class="login__logo--movil"><img src="/img/logo.svg" class="logo-claro" alt="MediCita"><img src="/img/logo-oscuro.svg" class="logo-oscuro" alt="MediCita"></span>
          <h2>Crea tu cuenta</h2>
          <p class="tenue">Para pacientes. Si eres médico o parte del personal, pide tu acceso a la administración.</p>
          ${camposPaciente('rg')}
          <p id="error" class="alerta alerta--error" role="alert" hidden></p>
          <p id="espera" class="alerta alerta--info" hidden>El servidor gratuito puede tardar hasta un minuto en despertar. Gracias por esperar…</p>
          <button class="btn btn--primario btn--ancho" id="crear" type="submit">Crear cuenta</button>
          <p class="login__alt">¿Ya tienes cuenta? <button type="button" class="btn btn--texto" id="volver">Inicia sesión</button></p>
        </form>
      </section>
    </main>`;

  const form = app.querySelector('form');
  const error = app.querySelector('#error');
  app.querySelector('#tema').addEventListener('click', alternarTema);
  app.querySelector('#volver').addEventListener('click', irALogin);
  activarClave(form);

  form.addEventListener('submit', async ev => {
    ev.preventDefault();
    error.hidden = true;
    const locales = validarPaciente(form);
    if (Object.keys(locales).length) { mostrarErrores(form, locales); return; }
    limpiarErrores(form);

    const boton = app.querySelector('#crear');
    boton.disabled = true; boton.classList.add('cargando'); boton.textContent = 'Creando tu cuenta…';
    const aviso = setTimeout(() => { app.querySelector('#espera').hidden = false; }, 4000);
    try {
      const r = await api.registrar(leerPaciente(form));
      sesion.set({
        token: r.token, email: r.email, nombres: r.nombres, apellidos: r.apellidos, rol: r.rol,
        expira: Date.now() + r.expiraEnMinutos * 60000
      });
      toast(`¡Bienvenido/a, ${r.nombres.split(' ')[0]}! Tu cuenta está lista.`);
      alEntrar();
    } catch (e) {
      const porCampo = erroresDelServidor(e);
      const sueltos = mostrarErrores(form, porCampo);
      const texto = porCampo ? sueltos.join('. ') : e.message;
      error.textContent = texto; error.hidden = !texto;
      boton.disabled = false; boton.classList.remove('cargando'); boton.textContent = 'Crear cuenta';
    } finally {
      clearTimeout(aviso);
      const espera = app.querySelector('#espera');
      if (espera) espera.hidden = true;
    }
  });
  form.querySelector('[name="nombres"]').focus();
}
