// Reserva y reprogramación de citas: primero la especialidad, luego la fecha y por último el médico con sus horas libres.
// Si el médico elegido no tiene horas ese día, se ve cuándo vuelve a tener y qué otros médicos de la especialidad sí atienden.
import { api, medicosEnCache } from './api.js';
import { crearDialogo, conCarga } from './dialogo-base.js';
import { abrirNuevoPaciente } from './pacientes.js';
import { esc, toast, icono, puede, hoyLima, fechaCorta, DIAS_CORTOS, avatar } from './util.js';

const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];

function atencionDe(m) {
  if (!m.horarios.length) return 'Aún sin horarios de atención';
  const porDia = {};
  m.horarios.forEach(h => { (porDia[h.dia] ||= []).push(`${h.inicio}–${h.fin}`); });
  return DIAS.filter(d => porDia[d]).map(d => `${DIAS_CORTOS[DIAS.indexOf(d)]} ${porDia[d].join(', ')}`).join(' · ');
}

const tarjetaMedico = m => `
  <div class="tarjeta-medico" data-tarjeta="${m.id}" hidden>
    ${avatar(m.nombre, m.foto, 'avatar--l avatar--medico')}
    <div class="tarjeta-medico__texto">
      <h3>${esc(m.nombre)}</h3>
      <p class="tarjeta-medico__esp">${esc(m.especialidad)}</p>
      <dl class="tarjeta-medico__lista">
        <div><dt>Sede</dt><dd>${esc(m.sede)}</dd></div>
        <div><dt>CMP</dt><dd>${esc(m.cmp)}</dd></div>
        <div><dt>Atención</dt><dd>${esc(atencionDe(m))}</dd></div>
      </dl>
      <p class="tarjeta-medico__desc">${m.descripcion ? esc(m.descripcion) : '<span class="tenue">Este médico aún no ha agregado una presentación.</span>'}</p>
    </div>
  </div>`;

export async function abrirReserva({ ctx, cita = null }) {
  const { rol } = ctx;
  let medicos, pacientes = [];
  try {
    medicos = await medicosEnCache();
    if (puede.elegirPaciente(rol) && !cita) pacientes = await api.pacientes();
  } catch (e) { toast(e.message, 'error'); return; }

  const reprogramando = !!cita;
  const medicoCita = cita ? medicos.find(m => m.id === cita.medicoId) : null;
  if (reprogramando && !medicoCita) { toast('El médico de esta cita ya no está disponible. Cancela la cita y reserva con otro médico.', 'error'); return; }

  const especialidades = [...new Map(medicos.map(m => [m.especialidadId, m.especialidad])).entries()]
    .map(([id, nombre]) => ({ id, nombre, total: medicos.filter(m => m.especialidadId === id).length }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
  const estado = {
    especialidadId: medicoCita ? medicoCita.especialidadId : (especialidades.length === 1 ? especialidades[0].id : null),
    fecha: hoyLima(), slot: null, disp: []
  };
  const nombreEsp = () => especialidades.find(e => e.id === estado.especialidadId)?.nombre ?? '';
  const dlg = crearDialogo('modal modal--ancho');

  dlg.innerHTML = `
    <form class="modal__cuerpo" novalidate>
      <header class="modal__cabecera">
        <div>
          <h2 class="modal__titulo">${reprogramando ? 'Reprogramar cita' : 'Nueva cita'}</h2>
          <p class="modal__sub">${reprogramando
            ? `${esc(cita.pacienteNombre)} · ${esc(cita.medicoNombre)}`
            : 'Elige la especialidad, el día y un horario libre.'}</p>
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

      ${reprogramando ? '' : `
      <fieldset class="espec" id="espec"><legend class="campo__etiqueta">Especialidad</legend>
        <div class="espec__lista">${especialidades.map(e => `
          <label class="espec__op"><input type="radio" name="espec" value="${e.id}" ${e.id === estado.especialidadId ? 'checked' : ''}>
            <span><b>${esc(e.nombre)}</b><small>${e.total} médico${e.total === 1 ? '' : 's'}</small></span></label>`).join('')}</div>
      </fieldset>`}

      <div class="grupo"><label class="campo__etiqueta" for="fecha">Fecha</label>
        <input id="fecha" class="campo" type="date" min="${hoyLima()}" value="${estado.fecha}" required></div>

      <div class="grupo"><span class="campo__etiqueta" id="medicos-etiqueta">${reprogramando ? 'Horario disponible' : 'Médicos y horarios disponibles'}</span>
        <div id="medicos" class="medicos-lista" aria-labelledby="medicos-etiqueta" aria-live="polite"></div></div>

      <div class="grupo"><label class="campo__etiqueta" for="motivo">Motivo de la consulta <span class="tenue">(opcional)</span></label>
        <textarea id="motivo" class="campo" rows="2" maxlength="255" placeholder="Ej.: control de presión arterial">${cita ? esc(cita.motivo || '') : ''}</textarea></div>

      <p class="reserva__resumen" id="resumen" aria-live="polite"></p>
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
    $('#enviar').disabled = !(okPaciente && estado.slot && estado.fecha);
    const m = estado.slot && medicos.find(x => x.id === estado.slot.medicoId);
    $('#resumen').textContent = m
      ? `${m.especialidad} · ${m.nombre} · ${fechaCorta(estado.fecha)}, ${estado.slot.hora} h`
      : (estado.especialidadId ? 'Elige un horario para continuar.' : '');
  };

  let pedido = 0;
  async function cargar() {
    estado.slot = null;
    validar();
    const lista = $('#medicos');
    if (!estado.especialidadId) { lista.innerHTML = '<p class="campo__ayuda">Elige una especialidad para ver sus médicos.</p>'; return; }
    if (!estado.fecha) { lista.innerHTML = '<p class="campo__ayuda">Elige una fecha.</p>'; return; }
    const mio = ++pedido;
    lista.innerHTML = '<div class="horas__cargando"><span></span><span></span><span></span><span></span></div>';
    try {
      let r = await api.disponibilidad(estado.especialidadId, estado.fecha);
      if (mio !== pedido) return;
      if (reprogramando) r = r.filter(d => d.medicoId === cita.medicoId);
      estado.disp = r;
      pintarMedicos();
    } catch (e) {
      if (mio === pedido) lista.innerHTML = `<p class="alerta alerta--error">${esc(e.message)}</p>`;
    }
  }

  function pintarMedicos() {
    const lista = $('#medicos');
    const items = estado.disp.map(d => ({ d, m: medicos.find(x => x.id === d.medicoId) })).filter(x => x.m)
      .sort((a, b) => (b.d.horas.length > 0) - (a.d.horas.length > 0) || a.m.nombre.localeCompare(b.m.nombre, 'es'));
    if (!items.length) { lista.innerHTML = '<p class="campo__ayuda">Esta especialidad aún no tiene médicos activos.</p>'; return; }

    const hayHoras = items.some(x => x.d.horas.length);
    const proximas = items.map(x => x.d.proximaFecha).filter(Boolean).sort();
    const aviso = hayHoras ? '' : `
      <div class="aviso-fecha" role="status">
        <p><b>${reprogramando ? 'El médico no tiene' : `Ningún médico de ${esc(nombreEsp())} tiene`} horas libres el ${esc(fechaCorta(estado.fecha))}.</b></p>
        ${proximas.length
          ? `<button type="button" class="btn btn--suave btn--chico" data-ir="${proximas[0]}">Ir al ${esc(fechaCorta(proximas[0]))}, la fecha libre más cercana</button>`
          : '<p class="tenue">Tampoco hay horas libres en los próximos 60 días.</p>'}
      </div>`;
    const conPerfil = rol === 'PACIENTE';
    lista.innerHTML = aviso + items.map(({ d, m }) => `
      <article class="medico-opcion ${d.horas.length ? '' : 'medico-opcion--sin'}" data-medico="${m.id}">
        <div class="medico-opcion__cab">
          ${avatar(m.nombre, m.foto, 'avatar--m avatar--medico')}
          <div class="medico-vista__datos"><b>${esc(m.nombre)}</b><small>${esc(m.sede)}</small></div>
          ${conPerfil ? `<button type="button" class="btn btn--texto btn--chico" data-perfil="${m.id}" aria-expanded="false">Ver perfil</button>` : ''}
        </div>
        ${conPerfil ? tarjetaMedico(m) : ''}
        ${d.horas.length
          ? `<div class="horas" role="radiogroup" aria-label="Horarios de ${esc(m.nombre)}">${d.horas.map(h => `
              <label class="hora"><input type="radio" name="slot" value="${m.id}|${h}"><span>${h}</span></label>`).join('')}</div>`
          : `<p class="medico-opcion__sin">Sin horas libres el ${esc(fechaCorta(estado.fecha))}.
              ${d.proximaFecha
                ? `<button type="button" class="btn btn--texto btn--chico" data-ir="${d.proximaFecha}">Próxima fecha libre: ${esc(fechaCorta(d.proximaFecha))}</button>`
                : 'No tiene horas libres en los próximos 60 días.'}</p>`}
      </article>`).join('');
  }

  $('#medicos').addEventListener('click', ev => {
    const ir = ev.target.closest('[data-ir]');
    if (ir) { estado.fecha = ir.dataset.ir; $('#fecha').value = estado.fecha; mostrarError(''); cargar(); return; }
    const perfil = ev.target.closest('[data-perfil]');
    if (perfil) {
      const tarjeta = $(`[data-tarjeta="${perfil.dataset.perfil}"]`);
      tarjeta.hidden = !tarjeta.hidden;
      perfil.setAttribute('aria-expanded', String(!tarjeta.hidden));
      perfil.textContent = tarjeta.hidden ? 'Ver perfil' : 'Ocultar perfil';
    }
  });
  $('#medicos').addEventListener('change', ev => {
    if (ev.target.name !== 'slot') return;
    const [medicoId, hora] = ev.target.value.split('|');
    estado.slot = { medicoId: Number(medicoId), hora };
    mostrarError('');
    dlg.querySelectorAll('.medico-opcion').forEach(c => c.classList.toggle('medico-opcion--elegido', c.dataset.medico === medicoId));
    validar();
  });
  dlg.querySelectorAll('input[name="espec"]').forEach(r => r.addEventListener('change', () => {
    estado.especialidadId = Number(r.value); mostrarError(''); cargar();
  }));
  $('#fecha').addEventListener('change', e => { estado.fecha = e.target.value; mostrarError(''); cargar(); });
  $('#paciente')?.addEventListener('change', validar);
  $('#nuevo-paciente')?.addEventListener('click', () => abrirNuevoPaciente({
    alCrear: p => {
      $('#paciente').add(new Option(`${p.nombre} · DNI ${p.dni}`, p.id, true, true));
      validar();
    }
  }));

  form.addEventListener('submit', async ev => {
    ev.preventDefault();
    mostrarError('');
    if (!estado.slot) return;
    const fechaHora = `${estado.fecha}T${estado.slot.hora}:00`;
    const motivo = $('#motivo').value.trim();
    try {
      await conCarga($('#enviar'), () => reprogramando
        ? api.actualizarCita(cita.id, { fechaHora, ...(motivo !== (cita.motivo || '') ? { motivo } : {}) })
        : api.crearCita({
            ...(puede.elegirPaciente(rol) ? { pacienteId: Number($('#paciente').value) } : {}),
            medicoId: estado.slot.medicoId, fechaHora, motivo: motivo || undefined
          }));
      toast(reprogramando ? 'Cita reprogramada' : 'Cita reservada con éxito');
      dlg.close();
      ctx.recargar();
    } catch (e) {
      mostrarError(e.message);
      if (e.status === 409) cargar(); // el horario pudo ocuparse mientras tanto
    }
  });

  dlg.showModal();
  validar();
  cargar();
}
