// Vista "Mi perfil" (todos los roles): datos propios, ficha del paciente y cambio de contraseña.
import { api, sesion } from '../api.js';
import { esc, toast, ROLES, TIPOS_SEGURO, badge, etiquetaCita } from '../util.js';
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
      <form class="tarjeta perfil__bloque" id="form-perfil" novalidate>
        <div class="tarjeta__cab"><h2>Mis datos</h2></div>
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
        <p class="alerta alerta--error" id="pf-error" role="alert" hidden></p>
        <div class="perfil__pie"><button class="btn btn--primario" id="pf-guardar" type="submit">Guardar cambios</button></div>
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

      ${esPaciente ? '<section class="tarjeta perfil__bloque perfil__ancho" id="historial"><div class="tarjeta__cab"><h2>Mis últimas atenciones</h2></div><div class="esqueleto-bloque"></div></section>' : ''}
    </div>`;

  const $ = s => contenido.querySelector(s);
  const mostrar = (sel, msg) => { const e = $(sel); e.textContent = msg; e.hidden = !msg; };

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
      const cambioNombre = s && (s.nombres !== r.nombres || s.apellidos !== r.apellidos);
      if (s) sesion.set({ ...s, nombres: r.nombres, apellidos: r.apellidos });
      toast('Perfil actualizado');
      if (cambioNombre) setTimeout(() => location.reload(), 700); // el menú lateral muestra el nombre
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
      const r = await api.citas({ estado: 'ATENDIDA', size: 5, orden: 'desc' });
      caja.innerHTML = `<div class="tarjeta__cab"><h2>Mis últimas atenciones</h2></div>` + (r.content.length
        ? `<ul class="citas-afectadas citas-afectadas--alta">${r.content.map(c => `
            <li><span><b>${esc(etiquetaCita(c.fechaHora))} · ${esc(c.medicoNombre)}</b><small>${esc(c.especialidad)} · ${esc(c.sede)}</small>
              <small class="nota-clinica">${c.observaciones ? esc(c.observaciones) : 'Sin observaciones del médico'}</small></span>${badge(c.estado)}</li>`).join('')}</ul>`
        : '<p class="tenue">Cuando te atiendan, aquí verás el resumen y las indicaciones de tu médico.</p>');
    } catch (e) { caja.innerHTML = `<div class="alerta alerta--error">${esc(e.message)}</div>`; }
  }
  return {};
}
