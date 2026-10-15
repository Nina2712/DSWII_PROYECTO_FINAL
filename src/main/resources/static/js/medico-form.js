// Diálogo de alta y edición de médicos (ADMIN): datos, especialidad (con alta rápida), sede y horarios.
import { api, olvidarMedicos } from './api.js';
import { crearDialogo, conCarga } from './dialogo-base.js';
import { esc, icono, toast, badge, etiquetaCita } from './util.js';

const DIAS = [
  ['LUNES', 'Lunes'], ['MARTES', 'Martes'], ['MIERCOLES', 'Miércoles'], ['JUEVES', 'Jueves'],
  ['VIERNES', 'Viernes'], ['SABADO', 'Sábado'], ['DOMINGO', 'Domingo']
];
const RE_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_BLOQUE = 30;

const aMinutos = hhmm => { const [h, m] = hhmm.split(':').map(Number); return h * 60 + m; };

/** Primer problema de los horarios (misma regla que el servidor) o null si están bien. */
export function errorDeHorarios(horarios) {
  if (!horarios.length) return 'Agrega al menos un horario de atención.';
  for (const h of horarios) {
    const nombre = DIAS.find(d => d[0] === h.dia)[1].toLowerCase();
    if (!h.inicio || !h.fin) return `Completa la hora de inicio y de fin del ${nombre}.`;
    if (aMinutos(h.fin) <= aMinutos(h.inicio)) return `En el ${nombre}, la hora de fin debe ser posterior a la de inicio.`;
    if (aMinutos(h.fin) - aMinutos(h.inicio) < MIN_BLOQUE) return `El horario del ${nombre} debe durar al menos ${MIN_BLOQUE} minutos.`;
  }
  const orden = [...horarios].sort((a, b) => a.dia === b.dia ? aMinutos(a.inicio) - aMinutos(b.inicio) : DIAS.findIndex(d => d[0] === a.dia) - DIAS.findIndex(d => d[0] === b.dia));
  for (let i = 1; i < orden.length; i++) {
    if (orden[i].dia === orden[i - 1].dia && aMinutos(orden[i].inicio) < aMinutos(orden[i - 1].fin)) {
      return `Los horarios del ${DIAS.find(d => d[0] === orden[i].dia)[1].toLowerCase()} se solapan.`;
    }
  }
  return null;
}

/**
 * `medico` = null para un alta. `especialidades` y `sedes` son las listas de la API.
 * `alGuardar()` se llama después de guardar con éxito.
 */
export function abrirFormularioMedico({ medico = null, especialidades, sedes, alGuardar }) {
  const edita = !!medico;
  const lista = [...especialidades];
  let horarios = edita && medico.horarios.length
    ? medico.horarios.map(h => ({ dia: h.dia, inicio: h.inicio, fin: h.fin }))
    : [{ dia: 'LUNES', inicio: '08:00', fin: '13:00' }];

  const dlg = crearDialogo('modal modal--ancho');
  dlg.innerHTML = `
    <form class="modal__cuerpo" novalidate>
      <header class="modal__cabecera">
        <div><h2 class="modal__titulo">${edita ? 'Editar médico' : 'Nuevo médico'}</h2>
          <p class="modal__sub">${edita ? esc(`${medico.nombres} ${medico.apellidos} · ${medico.cmp}`) : 'Crea su cuenta de acceso, su especialidad y sus horarios de atención.'}</p></div>
        <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
      </header>

      <div class="fila-campos">
        <div class="grupo"><label class="campo__etiqueta" for="md-nombres">Nombres</label>
          <input class="campo" id="md-nombres" maxlength="80" required value="${esc(medico?.nombres || '')}"></div>
        <div class="grupo"><label class="campo__etiqueta" for="md-apellidos">Apellidos</label>
          <input class="campo" id="md-apellidos" maxlength="80" required value="${esc(medico?.apellidos || '')}"></div>
      </div>
      <div class="fila-campos">
        <div class="grupo"><label class="campo__etiqueta" for="md-email">Correo (es su usuario de acceso)</label>
          <input class="campo" id="md-email" type="email" maxlength="120" required autocomplete="off" value="${esc(medico?.email || '')}"></div>
        <div class="grupo"><label class="campo__etiqueta" for="md-password">${edita ? 'Nueva contraseña' : 'Contraseña inicial'}${edita ? ' <span class="tenue">(opcional)</span>' : ''}</label>
          <div class="campo__clave"><input class="campo" id="md-password" type="password" maxlength="72" autocomplete="new-password" ${edita ? 'placeholder="Déjala vacía para no cambiarla"' : 'required'}>
            <button type="button" class="btn btn--texto" id="md-ver" aria-pressed="false">Mostrar</button></div>
          <p class="campo__ayuda">Mínimo 8 caracteres, con una letra y un número.</p></div>
      </div>
      <div class="fila-campos">
        <div class="grupo"><label class="campo__etiqueta" for="md-cmp">CMP</label>
          <input class="campo" id="md-cmp" maxlength="10" required autocomplete="off" placeholder="CMP-12345" value="${esc(medico?.cmp || '')}">
          <p class="campo__ayuda">Colegio Médico del Perú. De 3 a 10 caracteres.</p></div>
        <div class="grupo"><label class="campo__etiqueta" for="md-sede">Sede</label>
          <select class="campo" id="md-sede" required><option value="">Selecciona una sede…</option>
            ${sedes.map(s => `<option value="${s.id}" ${medico?.sedeId === s.id ? 'selected' : ''}>${esc(s.nombre)}</option>`).join('')}</select></div>
      </div>

      <div class="grupo"><label class="campo__etiqueta" for="md-esp">Especialidad</label>
        <div class="campo__clave"><select class="campo" id="md-esp" required></select>
          <button type="button" class="btn btn--suave btn--chico nowrap" id="md-nueva-esp">${icono.mas}<span>Nueva</span></button></div>
        <div class="inline-nueva" id="md-bloque-esp" hidden>
          <input class="campo" id="md-esp-nombre" maxlength="80" placeholder="Nombre de la nueva especialidad" aria-label="Nombre de la nueva especialidad">
          <button type="button" class="btn btn--primario btn--chico nowrap" id="md-crear-esp">Crear y usar</button>
        </div></div>

      <fieldset class="horarios-grupo">
        <legend class="campo__etiqueta">Horarios de atención</legend>
        <p class="campo__ayuda">Las citas se reservan en bloques de ${MIN_BLOQUE} minutos dentro de estos tramos.</p>
        <div id="md-horarios" class="horarios"></div>
        <div class="horarios__acciones">
          <button type="button" class="btn btn--suave btn--chico" id="md-agregar">${icono.mas}<span>Agregar horario</span></button>
          <button type="button" class="btn btn--texto" id="md-lv">Repetir el primero de lunes a viernes</button>
        </div>
      </fieldset>

      ${edita ? `
      <label class="interruptor"><input type="checkbox" id="md-activo" ${medico.activo ? 'checked' : ''}>
        <span><b>Médico activo</b><small>Si lo desactivas, no aparece al reservar y no podrá iniciar sesión. Solo se puede desactivar sin citas por atender.</small></span></label>` : ''}

      <p id="error" class="alerta alerta--error" role="alert" hidden></p>
      <footer class="modal__pie">
        <button type="button" class="btn btn--suave" data-cerrar>Cancelar</button>
        <button type="submit" class="btn btn--primario" id="md-guardar">${edita ? 'Guardar cambios' : 'Dar de alta'}</button>
      </footer>
    </form>`;

  const $ = s => dlg.querySelector(s);
  const error = $('#error');
  const mostrarError = t => { error.textContent = t; error.hidden = !t; if (t) error.scrollIntoView({ block: 'nearest' }); };
  dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));

  // ----- contraseña
  $('#md-ver').addEventListener('click', e => {
    const campo = $('#md-password'); const visible = campo.type === 'text';
    campo.type = visible ? 'password' : 'text';
    e.currentTarget.textContent = visible ? 'Mostrar' : 'Ocultar';
    e.currentTarget.setAttribute('aria-pressed', String(!visible));
  });

  // ----- especialidades
  function pintarEspecialidades(seleccionada) {
    const actual = seleccionada ?? $('#md-esp').value ?? medico?.especialidadId;
    $('#md-esp').innerHTML = '<option value="">Selecciona una especialidad…</option>' +
      lista.map(e => `<option value="${e.id}" ${String(e.id) === String(actual) ? 'selected' : ''}>${esc(e.nombre)}</option>`).join('');
  }
  pintarEspecialidades(medico?.especialidadId);
  $('#md-nueva-esp').addEventListener('click', () => {
    $('#md-bloque-esp').hidden = !$('#md-bloque-esp').hidden;
    if (!$('#md-bloque-esp').hidden) $('#md-esp-nombre').focus();
  });
  async function crearEspecialidad() {
    const nombre = $('#md-esp-nombre').value.trim();
    if (!nombre) { mostrarError('Escribe el nombre de la nueva especialidad.'); $('#md-esp-nombre').focus(); return; }
    mostrarError('');
    try {
      const nueva = await conCarga($('#md-crear-esp'), () => api.crearEspecialidad({ nombre }));
      lista.push(nueva);
      lista.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
      pintarEspecialidades(nueva.id);
      $('#md-esp-nombre').value = '';
      $('#md-bloque-esp').hidden = true;
      olvidarMedicos();
      toast(`Especialidad creada: ${nueva.nombre}`);
    } catch (e) { mostrarError(e.message); }
  }
  $('#md-crear-esp').addEventListener('click', crearEspecialidad);
  $('#md-esp-nombre').addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); crearEspecialidad(); } });

  // ----- horarios
  function pintarHorarios() {
    $('#md-horarios').innerHTML = horarios.map((h, i) => `
      <div class="horario-fila" data-i="${i}">
        <select class="campo" data-campo="dia" aria-label="Día del horario ${i + 1}">
          ${DIAS.map(([v, t]) => `<option value="${v}" ${h.dia === v ? 'selected' : ''}>${t}</option>`).join('')}</select>
        <input class="campo" type="time" step="900" data-campo="inicio" value="${esc(h.inicio)}" aria-label="Hora de inicio ${i + 1}">
        <span class="tenue" aria-hidden="true">a</span>
        <input class="campo" type="time" step="900" data-campo="fin" value="${esc(h.fin)}" aria-label="Hora de fin ${i + 1}">
        <button type="button" class="icono-btn" data-quitar aria-label="Quitar horario ${i + 1}" ${horarios.length === 1 ? 'disabled' : ''}>${icono.cerrar}</button>
      </div>`).join('');
  }
  pintarHorarios();
  $('#md-horarios').addEventListener('input', e => {
    const fila = e.target.closest('.horario-fila');
    if (fila && e.target.dataset.campo) horarios[Number(fila.dataset.i)][e.target.dataset.campo] = e.target.value;
  });
  $('#md-horarios').addEventListener('click', e => {
    const quitar = e.target.closest('[data-quitar]');
    if (!quitar || horarios.length === 1) return;
    horarios.splice(Number(quitar.closest('.horario-fila').dataset.i), 1);
    pintarHorarios();
  });
  $('#md-agregar').addEventListener('click', () => {
    const ultimo = horarios[horarios.length - 1];
    const sig = Math.min(DIAS.findIndex(d => d[0] === ultimo.dia) + 1, DIAS.length - 1);
    horarios.push({ dia: DIAS[sig][0], inicio: ultimo.inicio || '08:00', fin: ultimo.fin || '13:00' });
    pintarHorarios();
    $('#md-horarios').lastElementChild.querySelector('select').focus();
  });
  $('#md-lv').addEventListener('click', () => {
    const base = horarios[0];
    horarios = DIAS.slice(0, 5).map(([dia]) => ({ dia, inicio: base.inicio, fin: base.fin }));
    pintarHorarios();
  });

  // ----- guardar
  $('form').addEventListener('submit', async ev => {
    ev.preventDefault();
    mostrarError('');
    const nombres = $('#md-nombres').value.trim(), apellidos = $('#md-apellidos').value.trim();
    const email = $('#md-email').value.trim(), password = $('#md-password').value;
    const cmp = $('#md-cmp').value.trim(), sedeId = $('#md-sede').value, especialidadId = $('#md-esp').value;

    const problema =
      !nombres ? ['Escribe los nombres del médico.', '#md-nombres'] :
      !apellidos ? ['Escribe los apellidos del médico.', '#md-apellidos'] :
      !RE_EMAIL.test(email) ? ['Escribe un correo válido.', '#md-email'] :
      (!edita || password) && (password.length < 8 || !/\p{L}/u.test(password) || !/\d/.test(password))
        ? ['La contraseña necesita al menos 8 caracteres, con una letra y un número.', '#md-password'] :
      !/^[A-Za-z0-9-]{3,10}$/.test(cmp) ? ['El CMP debe tener de 3 a 10 caracteres (letras, números o guion).', '#md-cmp'] :
      !especialidadId ? ['Selecciona una especialidad o crea una nueva.', '#md-esp'] :
      !sedeId ? ['Selecciona la sede donde atiende.', '#md-sede'] :
      errorDeHorarios(horarios) ? [errorDeHorarios(horarios), '#md-agregar'] : null;
    if (problema) { mostrarError(problema[0]); $(problema[1]).focus(); return; }

    const cuerpo = {
      nombres, apellidos, email, cmp,
      especialidadId: Number(especialidadId), sedeId: Number(sedeId),
      horarios: horarios.map(h => ({ dia: h.dia, inicio: h.inicio, fin: h.fin })),
      ...(password ? { password } : {}),
      ...(edita ? { activo: $('#md-activo').checked } : {})
    };
    try {
      const guardado = await conCarga($('#md-guardar'), () => edita ? api.actualizarMedico(medico.id, cuerpo) : api.crearMedico(cuerpo));
      olvidarMedicos();
      toast(edita ? 'Médico actualizado' : `Médico dado de alta: ${nombres} ${apellidos}`);
      dlg.close();
      alGuardar?.();
      if (guardado?.citasFueraDeHorario?.length) avisarCitasFueraDeHorario(guardado);
    } catch (e) { mostrarError(e.message); }
  });

  dlg.showModal();
  $('#md-nombres').focus();
}

/** Lista las citas por atender que ya no caben en el horario nuevo (no se cancelaron ni se movieron). */
function avisarCitasFueraDeHorario(medico) {
  const citas = medico.citasFueraDeHorario;
  const dlg = crearDialogo('modal');
  dlg.innerHTML = `
    <div class="modal__cuerpo">
      <header class="modal__cabecera">
        <div><h2 class="modal__titulo">${citas.length} ${citas.length === 1 ? 'cita quedó' : 'citas quedaron'} fuera del nuevo horario</h2>
          <p class="modal__sub">${esc(medico.nombres)} ${esc(medico.apellidos)}</p></div>
        <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
      </header>
      <p class="modal__texto">El horario se guardó, pero estas citas por atender no se cancelaron ni se movieron.
        Recepción debe reprogramarlas o cancelarlas desde <b>Citas</b>.</p>
      <ul class="citas-afectadas">${citas.map(c => `
        <li><span><b>${esc(etiquetaCita(c.fechaHora))}</b><small>${esc(c.pacienteNombre)}</small></span>${badge(c.estado)}</li>`).join('')}</ul>
      <footer class="modal__pie">
        <button type="button" class="btn btn--suave" data-cerrar>Entendido</button>
        <button type="button" class="btn btn--primario" data-ir>Ir a Citas</button>
      </footer>
    </div>`;
  dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));
  dlg.querySelector('[data-ir]').addEventListener('click', () => { dlg.close(); location.hash = '#/citas'; });
  dlg.showModal();
}
