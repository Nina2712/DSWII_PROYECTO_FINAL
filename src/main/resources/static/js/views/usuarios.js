// Vista "Usuarios" (ADMIN): personal del sistema (administradores y recepcionistas).
import { api } from '../api.js';
import { esc, icono, toast, numero, ROLES, iniciales } from '../util.js';
import { crearDialogo, conCarga } from '../dialogo-base.js';

const ROLES_PERSONAL = ['ADMIN', 'RECEPCIONISTA'];

export async function pintarUsuarios(cont, ctx) {
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Usuarios</h1><p class="pagina__sub" id="conteo">Cargando…</p></div>
      <button class="btn btn--primario" data-nuevo>${icono.mas}<span>Nuevo usuario</span></button>
    </header>
    <p class="nota">Aquí se gestiona el personal administrativo. Los médicos se dan de alta en <b>Médicos</b> y los pacientes se registran solos o en recepción.</p>
    <section class="tarjeta"><div id="tabla" class="tabla-contenedor"><div class="esqueleto-filas">${'<span></span>'.repeat(4)}</div></div></section>`;

  const tabla = cont.querySelector('#tabla');
  const conteo = cont.querySelector('#conteo');
  const miEmail = (ctx.usuario?.email || '').toLowerCase();
  let lista = [];

  const esYo = u => u.email.toLowerCase() === miEmail;

  function dibujar() {
    const activos = lista.filter(u => u.activo).length;
    conteo.textContent = `${numero(lista.length)} ${lista.length === 1 ? 'usuario' : 'usuarios'} · ${numero(activos)} ${activos === 1 ? 'activo' : 'activos'}`;
    tabla.innerHTML = `
      <table class="tabla tabla--plana">
        <thead><tr><th>Usuario</th><th>Rol</th><th>Estado</th><th><span class="sr-only">Acciones</span></th></tr></thead>
        <tbody>${lista.map(u => `
          <tr>
            <td><div class="celda-persona"><span class="avatar">${esc(iniciales(u.nombres + ' ' + u.apellidos))}</span>
              <div><b>${esc(u.nombres)} ${esc(u.apellidos)}</b>${esYo(u) ? ' <span class="etiqueta">Tú</span>' : ''}<small class="bloque tenue">${esc(u.email)}</small></div></div></td>
            <td>${esc(ROLES[u.rol])}</td>
            <td><span class="badge ${u.activo ? 'badge--atendida' : 'badge--cancelada'}">${u.activo ? 'Activo' : 'Inactivo'}</span></td>
            <td class="acciones-fila"><button class="btn btn--suave btn--chico" data-editar="${u.id}">${icono.editar}<span>Editar</span></button></td>
          </tr>`).join('')}</tbody>
      </table>`;
    tabla.querySelectorAll('[data-editar]').forEach(b => b.addEventListener('click', () => formulario(lista.find(u => String(u.id) === b.dataset.editar))));
  }

  async function cargar() {
    try { lista = await api.usuarios(); dibujar(); }
    catch (e) {
      conteo.textContent = '';
      tabla.innerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      tabla.querySelector('[data-reintentar]').addEventListener('click', cargar);
    }
  }

  function formulario(usuario) {
    const edita = !!usuario;
    const yo = edita && esYo(usuario);
    const dlg = crearDialogo('modal');
    dlg.innerHTML = `
      <form class="modal__cuerpo" novalidate>
        <header class="modal__cabecera">
          <div><h2 class="modal__titulo">${edita ? 'Editar usuario' : 'Nuevo usuario'}</h2>
            <p class="modal__sub">${edita ? esc(usuario.email) : 'Recibirá acceso con el correo y la contraseña que definas.'}</p></div>
          <button type="button" class="icono-btn" data-cerrar aria-label="Cerrar">${icono.cerrar}</button>
        </header>
        <div class="fila-campos">
          <div class="grupo"><label class="campo__etiqueta" for="us-nombres">Nombres</label>
            <input class="campo" id="us-nombres" maxlength="80" required value="${esc(usuario?.nombres || '')}" autocomplete="off"></div>
          <div class="grupo"><label class="campo__etiqueta" for="us-apellidos">Apellidos</label>
            <input class="campo" id="us-apellidos" maxlength="80" required value="${esc(usuario?.apellidos || '')}" autocomplete="off"></div>
        </div>
        ${edita ? '' : `<div class="grupo"><label class="campo__etiqueta" for="us-email">Correo</label>
          <input class="campo" id="us-email" type="email" maxlength="120" required autocomplete="off" placeholder="nombre@medicita.pe"></div>`}
        <div class="grupo"><label class="campo__etiqueta" for="us-rol">Rol</label>
          <select class="campo" id="us-rol" ${yo ? 'disabled' : ''}>${ROLES_PERSONAL.map(r => `<option value="${r}" ${usuario?.rol === r ? 'selected' : ''}>${esc(ROLES[r])}</option>`).join('')}</select>
          <p class="campo__ayuda">El administrador gestiona todo; recepción registra pacientes y administra citas.</p></div>
        <div class="grupo"><label class="campo__etiqueta" for="us-password">${edita ? 'Nueva contraseña (opcional)' : 'Contraseña'}</label>
          <input class="campo" id="us-password" type="password" autocomplete="new-password" placeholder="${edita ? 'Déjala vacía para no cambiarla' : 'Mínimo 8 caracteres, con letra y número'}">
          ${edita ? '<p class="campo__ayuda">Úsala para restablecer el acceso de alguien que olvidó su clave.</p>' : ''}</div>
        ${edita ? `<label class="interruptor"><input type="checkbox" id="us-activo" ${usuario.activo ? 'checked' : ''} ${yo ? 'disabled' : ''}>
          <span><b>Cuenta activa</b><small>${yo ? 'No puedes desactivar tu propia cuenta.' : 'Si la desactivas, no podrá iniciar sesión. Siempre debe quedar un administrador activo.'}</small></span></label>` : ''}
        <p id="us-error" class="alerta alerta--error" role="alert" hidden></p>
        <footer class="modal__pie">
          <button type="button" class="btn btn--suave" data-cerrar>Cancelar</button>
          <button type="submit" class="btn btn--primario" id="us-guardar">${edita ? 'Guardar cambios' : 'Crear usuario'}</button>
        </footer>
      </form>`;
    const $ = s => dlg.querySelector(s);
    const error = $('#us-error');
    const falla = (msg, sel) => { error.textContent = msg; error.hidden = false; $(sel).focus(); };
    dlg.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => dlg.close()));
    $('form').addEventListener('submit', async ev => {
      ev.preventDefault();
      error.hidden = true;
      const nombres = $('#us-nombres').value.trim(), apellidos = $('#us-apellidos').value.trim();
      const email = edita ? '' : $('#us-email').value.trim();
      const password = $('#us-password').value;
      if (!nombres) return falla('Escribe los nombres.', '#us-nombres');
      if (!apellidos) return falla('Escribe los apellidos.', '#us-apellidos');
      if (!edita && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return falla('El correo no tiene un formato válido.', '#us-email');
      if ((!edita || password) && (password.length < 8 || !/\p{L}/u.test(password) || !/\d/.test(password)))
        return falla('La contraseña necesita al menos 8 caracteres, con una letra y un número.', '#us-password');
      const rol = $('#us-rol').value;
      try {
        await conCarga($('#us-guardar'), () => edita
          ? api.actualizarUsuario(usuario.id, { nombres, apellidos, rol: yo ? usuario.rol : rol, activo: yo ? true : $('#us-activo').checked, ...(password ? { password } : {}) })
          : api.crearUsuario({ nombres, apellidos, email, rol, password }));
        toast(edita ? 'Usuario actualizado' : 'Usuario creado');
        dlg.close();
        cargar();
      } catch (e) { error.textContent = e.message; error.hidden = false; }
    });
    dlg.showModal();
    $('#us-nombres').focus();
  }

  cont.querySelector('[data-nuevo]').addEventListener('click', () => formulario(null));
  await cargar();
  return { recargar: cargar };
}
