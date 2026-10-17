// Vista "Mi perfil" (todos los roles): datos propios, ficha del paciente y cambio de contraseña.
import { api, sesion, medicosEnCache } from '../api.js';
import { esc, toast, ROLES, TIPOS_SEGURO, badge, etiquetaCita, avatar, procesarFoto } from '../util.js';
import { conCarga } from '../dialogo-base.js';

export async function pintarPerfil(cont, ctx) {
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Mi perfil</h1><p class="pagina__sub">${esc(ROLES[ctx.rol] || ctx.rol)} · ${esc(ctx.usuario.email)}</p></div>
    </header>
    <div id="contenido"><div class="esqueleto-bloque"></div></div>`;
  const contenido = cont.querySelector('#contenido');

  let p;
  try { p = await api.perfil(); }
  catch (e) {
    contenido.innerHTML = `<div class="alerta alerta--error">${esc(e.message)}</div>`;
    return {};
  }
  const esPaciente = p.rol === 'PACIENTE', esMedico = p.rol === 'MEDICO';

  const campo = (id, etiqueta, valor, extra = '') =>
    `<div class="grupo"><label class="campo__etiqueta" for="${id}">${etiqueta}</label><input class="campo" id="${id}" value="${esc(valor ?? '')}" ${extra}></div>`;
  const fijo = (etiqueta, valor) =>
    `<div class="grupo"><span class="campo__etiqueta">${etiqueta}</span><p class="campo-fijo">${esc(valor || '—')}</p></div>`;

  contenido.innerHTML = `
    <div class="perfil">
      <section class="tarjeta perfil__bloque perfil__ancho perfil__foto" aria-label="Foto de perfil">
        <span id="pf-avatar">${avatar(`${p.nombres} ${p.apellidos}`, p.foto, 'avatar--l')}</span>
        <div class="perfil__foto-texto">
          <b>${esc(p.nombres)} ${esc(p.apellidos)}</b>
          <small>${esc(ROLES[p.rol] || p.rol)}</small>
          <div class="perfil__foto-acciones">
            <button class="btn btn--suave btn--chico" type="button" id="pf-cambiar-foto">${p.foto ? 'Cambiar foto' : 'Subir foto'}</button>
            <button class="btn btn--texto-peligro btn--chico" type="button" id="pf-quitar-foto" ${p.foto ? '' : 'hidden'}>Quitar foto</button>
            <input type="file" id="pf-archivo" accept="image/jpeg,image/png,image/webp" hidden>
          </div>
          <p class="alerta alerta--error" id="pf-foto-error" role="alert" hidden></p>
        </div>
      </section>
      <form class="tarjeta perfil__bloque" id="form-perfil" novalidate>
        <div class="tarjeta__cab"><h2>Mis datos</h2></div>
        <fieldset class="perfil__campos" id="pf-campos" disabled>
        <div class="fila-campos">
          ${campo('pf-nombres', 'Nombres', p.nombres, 'maxlength="80" required autocomplete="given-name"')}
          ${campo('pf-apellidos', 'Apellidos', p.apellidos, 'maxlength="80" required autocomplete="family-name"')}
        </div>
        ${esPaciente ? `
          <div class="fila-campos">${fijo('DNI', p.dni)}
            <div class="grupo"><label class="campo__etiqueta" for="pf-nac">Fecha de nacimiento</label>
              <input class="campo" id="pf-nac" type="date" value="${esc(p.fechaNacimiento || '')}"></div></div>` : ''}
        ${esMedico ? `<div class="fila-campos">${fijo('CMP', p.cmp)}${fijo('Especialidad y sede', `${p.especialidad} · ${p.sede}`)}</div>` : ''}
        ${esPaciente || esMedico ? campo('pf-tel', 'Teléfono', p.telefono, 'inputmode="tel" maxlength="15" placeholder="987654321" autocomplete="tel"') : ''}
        ${esPaciente ? `
          ${campo('pf-dir', 'Dirección', p.direccion, 'maxlength="200" autocomplete="street-address"')}
          <div class="grupo"><label class="campo__etiqueta" for="pf-seg">Seguro</label>
            <select class="campo" id="pf-seg"><option value="">Sin indicar</option>
              ${Object.entries(TIPOS_SEGURO).map(([k, t]) => `<option value="${k}" ${p.tipoSeguro === k ? 'selected' : ''}>${esc(t)}</option>`).join('')}</select></div>
          <div class="grupo"><label class="campo__etiqueta" for="pf-ale">Alergias y observaciones</label>
            <textarea class="campo" id="pf-ale" rows="3" maxlength="500" placeholder="Ej.: alérgico a la penicilina. Déjalo vacío si no tienes.">${esc(p.alergias || '')}</textarea>
            <p class="campo__ayuda">Tu médico lo verá al atenderte.</p></div>
          <div class="fila-campos">
            ${campo('pf-cen', 'Contacto de emergencia', p.contactoEmergenciaNombre, 'maxlength="100" placeholder="Nombre"')}
            ${campo('pf-cet', 'Teléfono del contacto', p.contactoEmergenciaTelefono, 'inputmode="tel" maxlength="15"')}
          </div>` : ''}
        ${esMedico ? `
          <div class="grupo"><label class="campo__etiqueta" for="pf-desc">Presentación</label>
            <textarea class="campo" id="pf-desc" rows="3" maxlength="500" placeholder="Experiencia, enfoque de atención…">${esc(p.descripcion || '')}</textarea></div>` : ''}
        </fieldset>
        <p class="alerta alerta--error" id="pf-error" role="alert" hidden></p>
        <div class="perfil__pie">
          <button class="btn btn--suave" id="pf-editar" type="button">Editar perfil</button>
          <button class="btn btn--texto" id="pf-cancelar" type="button" hidden>Cancelar</button>
          <button class="btn btn--primario" id="pf-guardar" type="submit" hidden>Guardar cambios</button>
        </div>
      </form>

      <form class="tarjeta perfil__bloque" id="form-clave" novalidate>
        <div class="tarjeta__cab"><h2>Cambiar contraseña</h2></div>
        <div class="grupo"><label class="campo__etiqueta" for="pw-actual">Contraseña actual</label>
          <input class="campo" id="pw-actual" type="password" autocomplete="current-password"></div>
        <div class="grupo"><label class="campo__etiqueta" for="pw-nueva">Contraseña nueva</label>
          <input class="campo" id="pw-nueva" type="password" maxlength="72" autocomplete="new-password">
          <p class="campo__ayuda">Mínimo 8 caracteres, con una letra y un número.</p></div>
        <div class="grupo"><label class="campo__etiqueta" for="pw-rep">Repite la contraseña nueva</label>
          <input class="campo" id="pw-rep" type="password" maxlength="72" autocomplete="new-password"></div>
        <p class="alerta alerta--error" id="pw-error" role="alert" hidden></p>
        <div class="perfil__pie"><button class="btn btn--suave" id="pw-guardar" type="submit">Cambiar contraseña</button></div>
      </form>

      ${esPaciente ? '<section class="tarjeta perfil__bloque perfil__ancho" id="historial"><div class="tarjeta__cab"><h2>Mi historial clínico</h2></div><div class="esqueleto-bloque"></div></section>' : ''}
    </div>`;

  const $ = s => contenido.querySelector(s);
  const mostrar = (sel, msg) => { const e = $(sel); e.textContent = msg; e.hidden = !msg; };

  const avisarSesion = (nombre, foto) => window.dispatchEvent(new CustomEvent('perfil-actualizado', { detail: { nombre, foto } }));

  // Los datos se ven en solo lectura; "Editar perfil" los habilita y, al guardar, vuelven a bloquearse.
  const modoEdicion = on => {
    $('#pf-campos').disabled = !on;
    $('#pf-editar').hidden = on;
    $('#pf-guardar').hidden = !on;
    $('#pf-cancelar').hidden = !on;
    mostrar('#pf-error', '');
    if (on) $('#pf-campos').querySelector('input:not([disabled]), textarea, select')?.focus();
  };
  $('#pf-editar').addEventListener('click', () => modoEdicion(true));
  $('#pf-cancelar').addEventListener('click', () => { $('#form-perfil').reset(); modoEdicion(false); });

  const guardarFoto = async valor => {
    mostrar('#pf-foto-error', '');
    try {
      const r = await api.actualizarFoto(valor);
      p.foto = r.foto || null;
      $('#pf-avatar').innerHTML = avatar(`${r.nombres} ${r.apellidos}`, p.foto, 'avatar--l');
      $('#pf-cambiar-foto').textContent = p.foto ? 'Cambiar foto' : 'Subir foto';
      $('#pf-quitar-foto').hidden = !p.foto;
      avisarSesion(`${r.nombres} ${r.apellidos}`, p.foto);
      toast(p.foto ? 'Foto actualizada' : 'Foto eliminada');
    } catch (e) { mostrar('#pf-foto-error', e.message); }
  };
  $('#pf-cambiar-foto').addEventListener('click', () => $('#pf-archivo').click());
  $('#pf-quitar-foto').addEventListener('click', () => guardarFoto(''));
  $('#pf-archivo').addEventListener('change', async ev => {
    const archivo = ev.target.files[0];
    ev.target.value = '';
    if (!archivo) return;
    try { await guardarFoto(await procesarFoto(archivo)); }
    catch (e) { mostrar('#pf-foto-error', e.message); }
  });

  $('#form-perfil').addEventListener('submit', async ev => {
    ev.preventDefault();
    mostrar('#pf-error', '');
    const v = id => $(id)?.value.trim() ?? '';
    const cuerpo = {
      nombres: v('#pf-nombres'), apellidos: v('#pf-apellidos'),
      telefono: v('#pf-tel'), fechaNacimiento: v('#pf-nac') || null, direccion: v('#pf-dir'),
      tipoSeguro: v('#pf-seg') || null, alergias: v('#pf-ale'),
      contactoEmergenciaNombre: v('#pf-cen'), contactoEmergenciaTelefono: v('#pf-cet'), descripcion: v('#pf-desc')
    };
    if (!cuerpo.nombres) return mostrar('#pf-error', 'Escribe tus nombres.');
    if (!cuerpo.apellidos) return mostrar('#pf-error', 'Escribe tus apellidos.');
    const tel = /^$|^[0-9+ ]{7,15}$/;
    if (!tel.test(cuerpo.telefono)) return mostrar('#pf-error', 'El teléfono solo puede tener números (7 a 15 dígitos).');
    if (!tel.test(cuerpo.contactoEmergenciaTelefono)) return mostrar('#pf-error', 'El teléfono del contacto solo puede tener números (7 a 15 dígitos).');
    try {
      const r = await conCarga($('#pf-guardar'), () => api.actualizarPerfil(cuerpo));
      const s = sesion.get();
      if (s) sesion.set({ ...s, nombres: r.nombres, apellidos: r.apellidos });
      avisarSesion(`${r.nombres} ${r.apellidos}`, r.foto);
      toast('Perfil actualizado');
      return pintarPerfil(cont, ctx); // vuelve a la vista de solo lectura con los datos guardados
    } catch (e) { mostrar('#pf-error', e.message); }
  });

  $('#form-clave').addEventListener('submit', async ev => {
    ev.preventDefault();
    mostrar('#pw-error', '');
    const actual = $('#pw-actual').value, nueva = $('#pw-nueva').value, rep = $('#pw-rep').value;
    if (!actual) return mostrar('#pw-error', 'Escribe tu contraseña actual.');
    if (nueva.length < 8 || !/\p{L}/u.test(nueva) || !/\d/.test(nueva)) return mostrar('#pw-error', 'La contraseña nueva necesita al menos 8 caracteres, con una letra y un número.');
    if (nueva !== rep) return mostrar('#pw-error', 'Las contraseñas nuevas no coinciden.');
    try {
      await conCarga($('#pw-guardar'), () => api.cambiarPassword({ actual, nueva }));
      ev.target.reset();
      toast('Contraseña cambiada');
    } catch (e) { mostrar('#pw-error', e.message); }
  });

  if (esPaciente) {
    const caja = $('#historial');
    try {
      const [r, medicos] = await Promise.all([
        api.citas({ estado: 'ATENDIDA', size: 5, orden: 'desc' }),
        medicosEnCache().catch(() => [])
      ]);
      caja.innerHTML = `<div class="tarjeta__cab"><h2>Mi historial clínico</h2></div>` + (r.content.length
        ? `<ul class="citas-afectadas citas-afectadas--alta">${r.content.map(c => `
            <li>${avatar(c.medicoNombre, medicos.find(m => m.id === c.medicoId)?.foto, 'avatar--medico')}<span><b>${esc(etiquetaCita(c.fechaHora))} · ${esc(c.medicoNombre)}</b><small>${esc(c.especialidad)} · ${esc(c.sede)}</small>
              <small class="nota-clinica">${c.observaciones ? esc(c.observaciones) : 'Sin observaciones del médico'}</small></span>${badge(c.estado)}</li>`).join('')}</ul>`
        : '<p class="tenue">Cuando te atiendan, aquí verás el resumen y las indicaciones de tu médico.</p>');
    } catch (e) { caja.innerHTML = `<div class="alerta alerta--error">${esc(e.message)}</div>`; }
  }
  return {};
}
