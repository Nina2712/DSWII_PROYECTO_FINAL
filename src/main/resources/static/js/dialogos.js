// Diálogos reutilizables: confirmación, ficha de la cita (panel lateral) y reserva / reprogramación.
import { api, medicosEnCache } from './api.js';
import {
  esc, badge, toast, icono, ROLES, ESTADOS_FINALES, puede, partes, fechaLarga, fechaCorta, hoyLima, DIAS_CORTOS,
  etiquetaCita, iniciales, diaSemana
} from './util.js';

function crearDialogo(clase) {
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

async function conCarga(boton, tarea) {
  const original = boton.innerHTML;
  boton.disabled = true;
  boton.classList.add('cargando');
  try { return await tarea(); }
  finally { boton.disabled = false; boton.classList.remove('cargando'); boton.innerHTML = original; }
}

// ------------------------------------------------------------------ ficha de la cita

export function abrirDetalle(cita, ctx) {
  const dlg = crearDialogo('panel');
  const { rol } = ctx;
  const final = ESTADOS_FINALES.includes(cita.estado);
  const p = partes(cita.fechaHora);

  const acciones = [];
  if (!final) {
    if (cita.estado === 'PENDIENTE' && puede.elegirPaciente(rol)) {
      acciones.push({ id: 'confirmar', texto: 'Confirmar cita', clase: 'btn--primario' });
    }
    if (cita.estado === 'CONFIRMADA' && (rol === 'MEDICO' || rol === 'ADMIN')) {
      acciones.push({ id: 'atendida', texto: 'Marcar como atendida', clase: 'btn--primario' });
      acciones.push({ id: 'noasistio', texto: 'No asistió', clase: 'btn--suave' });
    }
    if (puede.reservar(rol)) {
      acciones.push({ id: 'reprogramar', texto: 'Reprogramar', clase: 'btn--suave' });
      acciones.push({ id: 'cancelar', texto: 'Cancelar cita', clase: 'btn--peligro-suave' });
    }
  }
  const editaNotas = (rol === 'MEDICO' || rol === 'ADMIN') && (!final || cita.estado === 'ATENDIDA');

  dlg.innerHTML = `
    <article class="panel__contenido">
      <header class="panel__cabecera">
        <div>
          <p class="panel__sup">Cita #${cita.id}</p>
          <h2 class="panel__titulo">${esc(fechaLarga(p.fecha))}</h2>
          <p class="panel__hora">${esc(p.hora)} h · ${cita.duracionMin} min</p>
        </div>
        <button class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
      </header>
      <div class="panel__estado">${badge(cita.estado)}</div>

      <section class="ficha">
        <div class="persona"><span class="avatar avatar--m">${esc(iniciales(cita.pacienteNombre))}</span>
          <div><p class="persona__rol">Paciente</p><p class="persona__nombre">${esc(cita.pacienteNombre)}</p>
          <p class="persona__dato">DNI ${esc(cita.pacienteDni)}</p></div></div>
        <div class="persona"><span class="avatar avatar--m avatar--medico">${esc(iniciales(cita.medicoNombre))}</span>
          <div><p class="persona__rol">Médico</p><p class="persona__nombre">${esc(cita.medicoNombre)}</p>
          <p class="persona__dato">${esc(cita.especialidad)} · ${esc(cita.sede)}</p></div></div>
      </section>

      <section class="ficha ficha--lista">
        <div><dt>Motivo</dt><dd>${cita.motivo ? esc(cita.motivo) : '<span class="tenue">Sin motivo indicado</span>'}</dd></div>
        <div><dt>Registrada</dt><dd>${esc(etiquetaCita(cita.creadoEn))}</dd></div>
      </section>

      <section class="ficha">
        <label class="campo__etiqueta" for="notas">Observaciones clínicas</label>
        ${editaNotas
          ? `<textarea id="notas" class="campo" rows="4" maxlength="1000" placeholder="Anota indicaciones o diagnóstico…">${esc(cita.observaciones || '')}</textarea>
             <button class="btn btn--suave btn--chico" data-accion="notas">Guardar observaciones</button>`
          : `<p class="notas-texto">${cita.observaciones ? esc(cita.observaciones) : '<span class="tenue">Sin observaciones</span>'}</p>`}
      </section>

      <footer class="panel__pie">
        ${acciones.map(a => `<button class="btn ${a.clase}" data-accion="${a.id}">${esc(a.texto)}</button>`).join('')}
        ${rol === 'ADMIN' ? '<button class="btn btn--texto-peligro" data-accion="eliminar">Eliminar definitivamente</button>' : ''}
      </footer>
    </article>`;

  const cerrar = () => dlg.close();
  dlg.querySelector('[data-cerrar]').addEventListener('click', cerrar);

  const cambiar = async (boton, tarea, mensaje) => {
    try {
      await conCarga(boton, tarea);
      toast(mensaje);
      dlg.close();
      ctx.recargar();
    } catch (e) { toast(e.message, 'error'); }
  };

  dlg.addEventListener('click', async ev => {
    const boton = ev.target.closest('[data-accion]');
    if (!boton) return;
    switch (boton.dataset.accion) {
      case 'confirmar':
        return cambiar(boton, () => api.actualizarCita(cita.id, { estado: 'CONFIRMADA' }), 'Cita confirmada');
      case 'atendida':
        return cambiar(boton, () => api.actualizarCita(cita.id, { estado: 'ATENDIDA' }), 'Cita marcada como atendida');
      case 'noasistio':
        if (await confirmar({ titulo: 'Registrar inasistencia', mensaje: 'El paciente no asistió. El horario quedará libre.', textoOk: 'Registrar' })) {
          return cambiar(boton, () => api.actualizarCita(cita.id, { estado: 'NO_ASISTIO' }), 'Inasistencia registrada');
        }
        return;
      case 'notas':
        return cambiar(boton, () => api.actualizarCita(cita.id, { observaciones: dlg.querySelector('#notas').value }), 'Observaciones guardadas');
      case 'cancelar':
        if (await confirmar({ titulo: 'Cancelar cita', mensaje: 'La cita quedará cancelada y el horario volverá a estar libre. Se conserva en el historial.', textoOk: 'Sí, cancelar', peligro: true })) {
          return cambiar(boton, () => api.cancelarCita(cita.id), 'Cita cancelada');
        }
        return;
      case 'eliminar':
        if (await confirmar({ titulo: 'Eliminar definitivamente', mensaje: 'Se borrará de la base de datos y no se podrá recuperar.', textoOk: 'Eliminar', peligro: true })) {
          return cambiar(boton, () => api.eliminarCita(cita.id), 'Cita eliminada');
        }
        return;
      case 'reprogramar':
        dlg.close();
        return abrirReserva({ ctx, cita });
    }
  });
  dlg.showModal();
}

// ------------------------------------------------------------ reserva / reprogramación

export async function abrirReserva({ ctx, cita = null }) {
  const { rol } = ctx;
  let medicos, pacientes = [];
  try {
    medicos = await medicosEnCache();
    if (puede.elegirPaciente(rol) && !cita) pacientes = await api.pacientes();
  } catch (e) { toast(e.message, 'error'); return; }

  const reprogramando = !!cita;
  const estado = { medicoId: cita ? cita.medicoId : null, fecha: hoyLima(), hora: null };
  const dlg = crearDialogo('modal');

  dlg.innerHTML = `
    <form class="modal__cuerpo" novalidate>
      <header class="modal__cabecera">
        <div>
          <h2 class="modal__titulo">${reprogramando ? 'Reprogramar cita' : 'Nueva cita'}</h2>
          <p class="modal__sub">${reprogramando
            ? `${esc(cita.pacienteNombre)} · ${esc(cita.medicoNombre)}`
            : 'Elige médico, día y un horario libre.'}</p>
        </div>
        <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
      </header>

      ${puede.elegirPaciente(rol) && !reprogramando ? `
        <div class="grupo"><label class="campo__etiqueta" for="paciente">Paciente</label>
          <select id="paciente" class="campo" required>
            <option value="">Selecciona un paciente…</option>
            ${pacientes.map(p => `<option value="${p.id}">${esc(p.nombre)} · DNI ${esc(p.dni)}</option>`).join('')}
          </select></div>` : ''}

      <div class="grupo"><label class="campo__etiqueta" for="medico">Médico</label>
        <select id="medico" class="campo" required ${reprogramando ? 'disabled' : ''}>
          <option value="">Selecciona un médico…</option>
          ${medicos.map(m => `<option value="${m.id}" ${m.id === estado.medicoId ? 'selected' : ''}>${esc(m.nombre)} — ${esc(m.especialidad)} (${esc(m.sede)})</option>`).join('')}
        </select>
        <p id="atencion" class="campo__ayuda"></p></div>

      <div class="grupo"><label class="campo__etiqueta" for="fecha">Fecha</label>
        <input id="fecha" class="campo" type="date" min="${hoyLima()}" value="${estado.fecha}" required></div>

      <div class="grupo"><span class="campo__etiqueta" id="horas-etiqueta">Horario disponible</span>
        <div id="horas" class="horas" role="radiogroup" aria-labelledby="horas-etiqueta"><p class="campo__ayuda">Elige un médico para ver los horarios.</p></div></div>

      <div class="grupo"><label class="campo__etiqueta" for="motivo">Motivo de la consulta <span class="tenue">(opcional)</span></label>
        <textarea id="motivo" class="campo" rows="2" maxlength="255" placeholder="Ej.: control de presión arterial">${cita ? esc(cita.motivo || '') : ''}</textarea></div>

      <p id="error" class="alerta alerta--error" role="alert" hidden></p>

      <footer class="modal__pie">
        <button type="button" class="btn btn--suave" data-cerrar>Cancelar</button>
        <button type="submit" class="btn btn--primario" id="enviar" disabled>${reprogramando ? 'Guardar nuevo horario' : 'Reservar cita'}</button>
      </footer>
    </form>`;

  const $ = s => dlg.querySelector(s);
  const form = dlg.querySelector('form');
  const error = $('#error');
  dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));

  const mostrarError = texto => { error.textContent = texto; error.hidden = !texto; };
  const validar = () => {
    const okPaciente = !puede.elegirPaciente(rol) || reprogramando || $('#paciente').value;
    $('#enviar').disabled = !(okPaciente && estado.medicoId && estado.fecha && estado.hora);
  };

  function resumenAtencion() {
    const m = medicos.find(x => x.id === estado.medicoId);
    if (!m) return '';
    if (!m.horarios.length) return 'Este médico aún no tiene horarios de atención.';
    const porDia = {};
    m.horarios.forEach(h => { (porDia[h.dia] ||= []).push(`${h.inicio}–${h.fin}`); });
    const orden = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
    return 'Atiende: ' + orden.filter(d => porDia[d]).map(d => `${DIAS_CORTOS[orden.indexOf(d)]} ${porDia[d].join(', ')}`).join(' · ');
  }

  let pedido = 0;
  async function cargarHoras() {
    estado.hora = null;
    validar();
    const cont = $('#horas');
    $('#atencion').textContent = resumenAtencion();
    if (!estado.medicoId || !estado.fecha) { cont.innerHTML = '<p class="campo__ayuda">Elige un médico para ver los horarios.</p>'; return; }
    const miPedido = ++pedido;
    cont.innerHTML = '<div class="horas__cargando"><span></span><span></span><span></span><span></span></div>';
    try {
      const r = await api.horasLibres(estado.medicoId, estado.fecha);
      if (miPedido !== pedido) return;
      if (!r.horas.length) {
        cont.innerHTML = `<p class="campo__ayuda">No hay horarios libres el ${esc(fechaCorta(estado.fecha))}. Prueba con otro día de atención.</p>`;
        return;
      }
      cont.innerHTML = r.horas.map(h => `
        <label class="hora"><input type="radio" name="hora" value="${h}"><span>${h}</span></label>`).join('');
    } catch (e) {
      if (miPedido === pedido) cont.innerHTML = `<p class="alerta alerta--error">${esc(e.message)}</p>`;
    }
  }

  $('#medico').addEventListener('change', e => { estado.medicoId = e.target.value ? Number(e.target.value) : null; mostrarError(''); cargarHoras(); });
  $('#fecha').addEventListener('change', e => { estado.fecha = e.target.value; mostrarError(''); cargarHoras(); });
  $('#paciente')?.addEventListener('change', validar);
  $('#horas').addEventListener('change', e => { if (e.target.name === 'hora') { estado.hora = e.target.value; validar(); } });

  form.addEventListener('submit', async ev => {
    ev.preventDefault();
    mostrarError('');
    const fechaHora = `${estado.fecha}T${estado.hora}:00`;
    const motivo = $('#motivo').value.trim();
    try {
      await conCarga($('#enviar'), () => reprogramando
        ? api.actualizarCita(cita.id, { fechaHora, ...(motivo !== (cita.motivo || '') ? { motivo } : {}) })
        : api.crearCita({
            ...(puede.elegirPaciente(rol) ? { pacienteId: Number($('#paciente').value) } : {}),
            medicoId: estado.medicoId, fechaHora, motivo: motivo || undefined
          }));
      toast(reprogramando ? 'Cita reprogramada' : 'Cita reservada con éxito');
      dlg.close();
      ctx.recargar();
    } catch (e) {
      mostrarError(e.message);
      if (e.status === 409) cargarHoras(); // el horario pudo ocuparse mientras tanto
    }
  });

  dlg.showModal();
  if (estado.medicoId) cargarHoras(); else $('#atencion').textContent = '';
}
