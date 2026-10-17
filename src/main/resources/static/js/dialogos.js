// Diálogos de la cita: ficha (panel lateral) y reserva / reprogramación.
// Los diálogos genéricos (crear, confirmar, botón con carga) están en dialogo-base.js.
import { api, medicosEnCache } from './api.js';
import { crearDialogo, confirmar, conCarga } from './dialogo-base.js';
import { abrirNuevoPaciente } from './pacientes.js';
import {
  esc, badge, toast, icono, ROLES, ESTADOS_FINALES, puede, partes, fechaLarga, fechaCorta, hoyLima, DIAS_CORTOS,
  etiquetaCita, iniciales, avatar, diaSemana, TIPOS_SEGURO
} from './util.js';

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
  // Solo el médico de esta cita escribe las observaciones; el administrador y recepción las leen.
  const editaNotas = rol === 'MEDICO' && (!final || cita.estado === 'ATENDIDA');

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
        <div class="persona"><span id="avatar-paciente">${avatar(cita.pacienteNombre, null, 'avatar--m')}</span>
          <div><p class="persona__rol">Paciente</p><p class="persona__nombre">${esc(cita.pacienteNombre)}</p>
          <p class="persona__dato">DNI ${esc(cita.pacienteDni)}</p></div></div>
        <div class="persona"><span id="avatar-medico">${avatar(cita.medicoNombre, null, 'avatar--m avatar--medico')}</span>
          <div><p class="persona__rol">Médico</p><p class="persona__nombre">${esc(cita.medicoNombre)}</p>
          <p class="persona__dato">${esc(cita.especialidad)} · ${esc(cita.sede)}</p></div></div>
      </section>

      ${rol !== 'PACIENTE' ? '<section class="ficha ficha--clinica" id="ficha-paciente" aria-live="polite"><p class="tenue">Cargando ficha del paciente…</p></section>' : ''}

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

      ${rol === 'MEDICO' || rol === 'ADMIN' ? '<section class="ficha historial" id="historial-clinico" aria-live="polite"><p class="tenue">Cargando historial clínico…</p></section>' : ''}

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
        {
          // si el médico ya escribió observaciones, se guardan junto con la atención
          const notas = dlg.querySelector('#notas')?.value.trim();
          return cambiar(boton, () => api.actualizarCita(cita.id, notas ? { estado: 'ATENDIDA', observaciones: notas } : { estado: 'ATENDIDA' }), 'Cita marcada como atendida');
        }
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
  if (rol !== 'MEDICO') {
    medicosEnCache().then(lista => {
      const foto = lista.find(m => m.id === cita.medicoId)?.foto;
      if (foto) dlg.querySelector('#avatar-medico').innerHTML = avatar(cita.medicoNombre, foto, 'avatar--m avatar--medico');
    }).catch(() => {});
  }
  if (rol !== 'PACIENTE') cargarFicha(dlg, cita);
  if (rol === 'MEDICO' || rol === 'ADMIN') cargarHistorial(dlg, cita);
}

/** Alergias, seguro y contacto de emergencia: lo que el personal necesita saber al atender. */
async function cargarFicha(dlg, cita) {
  const caja = dlg.querySelector('#ficha-paciente');
  try {
    const f = await api.fichaPaciente(cita.pacienteId);
    if (f.foto) dlg.querySelector('#avatar-paciente').innerHTML = avatar(cita.pacienteNombre, f.foto, 'avatar--m');
    const dato = (t, v) => `<div><dt>${t}</dt><dd>${v ? esc(v) : '<span class="tenue">—</span>'}</dd></div>`;
    caja.innerHTML = `
      <p class="alergia ${f.alergias ? 'alergia--si' : ''}"><b>Alergias</b>${f.alergias ? esc(f.alergias) : 'Sin alergias registradas'}</p>
      <dl class="ficha--lista ficha--dos">
        ${dato('Edad', f.edad != null ? `${f.edad} años` : '')}
        ${dato('Seguro', f.tipoSeguro ? TIPOS_SEGURO[f.tipoSeguro] : '')}
        ${dato('Teléfono', f.telefono)}
        ${dato('Contacto de emergencia', [f.contactoEmergenciaNombre, f.contactoEmergenciaTelefono].filter(Boolean).join(' · '))}
      </dl>`;
  } catch { caja.remove(); }
}

/** Historial clínico: atenciones anteriores del paciente con cualquier médico, solo lectura. */
async function cargarHistorial(dlg, cita) {
  const caja = dlg.querySelector('#historial-clinico');
  try {
    const todas = await api.historialPaciente(cita.pacienteId);
    const previas = todas.filter(a => a.citaId !== cita.id);
    caja.innerHTML = `<h3 class="historial__titulo">Historial clínico</h3>` + (previas.length
      ? `<ol class="historial__lista">${previas.map(a => `
          <li class="historial__item ${a.propia ? 'historial__item--propia' : ''}">
            <p class="historial__cab"><b>${esc(etiquetaCita(a.fechaHora))}</b>${a.propia ? '<span class="etiqueta-propia">Tú</span>' : ''}</p>
            <p class="historial__medico">${esc(a.medicoNombre)} · ${esc(a.especialidad)}<small>${esc(a.sede)}</small></p>
            ${a.motivo ? `<p class="historial__motivo">Motivo: ${esc(a.motivo)}</p>` : ''}
            <p class="notas-texto">${a.observaciones ? esc(a.observaciones) : '<span class="tenue">Sin observaciones</span>'}</p>
          </li>`).join('')}</ol>
        <p class="campo__ayuda">Solo lectura. Cada médico edita únicamente las observaciones de sus propias citas.</p>`
      : '<p class="tenue">Es la primera atención registrada de este paciente.</p>');
  } catch { caja.remove(); }
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
          </select>
          <button type="button" class="btn btn--texto" id="nuevo-paciente">${icono.mas}<span>Registrar paciente nuevo</span></button></div>` : ''}

      <div class="grupo"><label class="campo__etiqueta" for="medico">Médico</label>
        <select id="medico" class="campo" required ${reprogramando ? 'disabled' : ''}>
          <option value="">Selecciona un médico…</option>
          ${medicos.map(m => `<option value="${m.id}" ${m.id === estado.medicoId ? 'selected' : ''}>${esc(m.nombre)} — ${esc(m.especialidad)} (${esc(m.sede)})</option>`).join('')}
        </select>
        <div id="medico-vista" class="medico-vista" aria-live="polite"></div>
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

  // Foto y datos del médico elegido. Solo el paciente ve el enlace a la mini tarjeta con su perfil.
  function pintarMedico() {
    const caja = $('#medico-vista');
    const m = medicos.find(x => x.id === estado.medicoId);
    if (!m) { caja.innerHTML = ''; return; }
    const conTarjeta = rol === 'PACIENTE';
    caja.innerHTML = `
      <div class="medico-vista__fila">
        ${avatar(m.nombre, m.foto, 'avatar--m avatar--medico')}
        <div class="medico-vista__datos"><b>${esc(m.nombre)}</b><small>${esc(m.especialidad)} · ${esc(m.sede)}</small></div>
        ${conTarjeta ? '<button type="button" class="btn btn--texto btn--chico" id="ver-medico" aria-expanded="false" aria-controls="medico-tarjeta">Ver perfil del médico</button>' : ''}
      </div>
      ${conTarjeta ? `
        <div class="tarjeta-medico" id="medico-tarjeta" hidden>
          ${avatar(m.nombre, m.foto, 'avatar--l avatar--medico')}
          <div class="tarjeta-medico__texto">
            <h3>${esc(m.nombre)}</h3>
            <p class="tarjeta-medico__esp">${esc(m.especialidad)}</p>
            <dl class="tarjeta-medico__lista">
              <div><dt>Sede</dt><dd>${esc(m.sede)}</dd></div>
              <div><dt>CMP</dt><dd>${esc(m.cmp)}</dd></div>
              <div><dt>Atención</dt><dd>${esc(resumenAtencion().replace(/^Atiende: /, ''))}</dd></div>
            </dl>
            <p class="tarjeta-medico__desc">${m.descripcion ? esc(m.descripcion) : '<span class="tenue">Este médico aún no ha agregado una presentación.</span>'}</p>
          </div>
        </div>` : ''}`;
    const boton = caja.querySelector('#ver-medico');
    boton?.addEventListener('click', () => {
      const tarjeta = caja.querySelector('#medico-tarjeta');
      tarjeta.hidden = !tarjeta.hidden;
      boton.setAttribute('aria-expanded', String(!tarjeta.hidden));
      boton.textContent = tarjeta.hidden ? 'Ver perfil del médico' : 'Ocultar perfil';
    });
  }

  let pedido = 0;
  async function cargarHoras() {
    estado.hora = null;
    validar();
    pintarMedico();
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
  $('#nuevo-paciente')?.addEventListener('click', () => abrirNuevoPaciente({
    alCrear: p => {
      const opcion = new Option(`${p.nombre} · DNI ${p.dni}`, p.id, true, true);
      $('#paciente').add(opcion);
      validar();
    }
  }));
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
