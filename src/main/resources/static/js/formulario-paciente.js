// Campos, validación y errores del formulario de paciente. Lo usan la pantalla "Crear cuenta"
// (registro propio) y el diálogo con el que recepción registra a un paciente.
import { hoyLima, sumarDias } from './util.js';

const RE_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** HTML de los campos. `p` es un prefijo para que los id no se repitan si hay dos formularios. */
export function camposPaciente(p) {
  const ayer = sumarDias(hoyLima(), -1);
  const campo = (nombre, etiqueta, atributos, extra = '') => `
    <div class="grupo" data-campo="${nombre}">
      <label class="campo__etiqueta" for="${p}-${nombre}">${etiqueta}</label>
      <input class="campo" id="${p}-${nombre}" name="${nombre}" ${atributos}>
      ${extra}
      <p class="campo__error" role="alert" hidden></p>
    </div>`;
  return `
    <div class="fila-campos">
      ${campo('nombres', 'Nombres', 'type="text" autocomplete="given-name" maxlength="80" required')}
      ${campo('apellidos', 'Apellidos', 'type="text" autocomplete="family-name" maxlength="80" required')}
    </div>
    <div class="fila-campos">
      ${campo('dni', 'DNI', 'type="text" inputmode="numeric" maxlength="8" autocomplete="off" required placeholder="8 dígitos"')}
      ${campo('telefono', 'Teléfono <span class="tenue">(opcional)</span>', 'type="tel" inputmode="tel" maxlength="15" autocomplete="tel" placeholder="987654321"')}
    </div>
    ${campo('fechaNacimiento', 'Fecha de nacimiento <span class="tenue">(opcional)</span>', `type="date" max="${ayer}" autocomplete="bday"`)}
    ${campo('email', 'Correo electrónico', 'type="email" maxlength="120" autocomplete="email" required placeholder="nombre@correo.com"')}
    <div class="fila-campos">
      <div class="grupo" data-campo="password">
        <label class="campo__etiqueta" for="${p}-password">Contraseña</label>
        <div class="campo__clave"><input class="campo" id="${p}-password" name="password" type="password" maxlength="72" autocomplete="new-password" required>
          <button type="button" class="btn btn--texto" data-ver-clave aria-pressed="false">Mostrar</button></div>
        <ul class="clave-reglas" aria-label="Requisitos de la contraseña">
          <li data-regla="largo">8 o más caracteres</li><li data-regla="letra">Una letra</li><li data-regla="numero">Un número</li>
        </ul>
        <p class="campo__error" role="alert" hidden></p>
      </div>
      ${campo('confirmacion', 'Repite la contraseña', 'type="password" maxlength="72" autocomplete="new-password" required')}
    </div>`;
}

/** Conecta el botón Mostrar/Ocultar y la lista de requisitos de la contraseña. */
export function activarClave(raiz) {
  const clave = raiz.querySelector('[name="password"]');
  const boton = raiz.querySelector('[data-ver-clave]');
  const reglas = {
    largo: v => v.length >= 8,
    letra: v => /\p{L}/u.test(v),
    numero: v => /\d/.test(v)
  };
  const pintar = () => raiz.querySelectorAll('[data-regla]').forEach(li => {
    li.classList.toggle('cumple', reglas[li.dataset.regla](clave.value));
  });
  clave.addEventListener('input', pintar);
  pintar();
  boton.addEventListener('click', () => {
    const visible = clave.type === 'text';
    for (const nombre of ['password', 'confirmacion']) raiz.querySelector(`[name="${nombre}"]`).type = visible ? 'password' : 'text';
    boton.textContent = visible ? 'Mostrar' : 'Ocultar';
    boton.setAttribute('aria-pressed', String(!visible));
  });
}

/** Lee los valores listos para enviar a la API (sin la confirmación). */
export function leerPaciente(raiz) {
  const v = n => raiz.querySelector(`[name="${n}"]`).value;
  return {
    nombres: v('nombres').trim(),
    apellidos: v('apellidos').trim(),
    dni: v('dni').trim(),
    email: v('email').trim(),
    password: v('password'),
    telefono: v('telefono').trim() || undefined,
    fechaNacimiento: v('fechaNacimiento') || undefined
  };
}

/** Devuelve los errores por campo; un objeto vacío significa que todo está bien. */
export function validarPaciente(raiz) {
  const d = leerPaciente(raiz);
  const e = {};
  if (!d.nombres) e.nombres = 'Escribe tus nombres.';
  if (!d.apellidos) e.apellidos = 'Escribe tus apellidos.';
  if (!/^\d{8}$/.test(d.dni)) e.dni = 'El DNI debe tener exactamente 8 dígitos.';
  if (d.telefono && !/^[0-9+ ]{7,15}$/.test(d.telefono)) e.telefono = 'Usa solo números (de 7 a 15 dígitos).';
  if (d.fechaNacimiento && d.fechaNacimiento >= hoyLima()) e.fechaNacimiento = 'La fecha debe ser anterior a hoy.';
  if (!RE_EMAIL.test(d.email)) e.email = 'Escribe un correo válido, por ejemplo nombre@correo.com.';
  if (d.password.length < 8) e.password = 'La contraseña necesita al menos 8 caracteres.';
  else if (!/\p{L}/u.test(d.password) || !/\d/.test(d.password)) e.password = 'Incluye al menos una letra y un número.';
  if (raiz.querySelector('[name="confirmacion"]').value !== d.password) e.confirmacion = 'Las contraseñas no coinciden.';
  return e;
}

export function limpiarErrores(raiz) {
  raiz.querySelectorAll('[data-campo]').forEach(g => {
    g.querySelector('.campo__error').hidden = true;
    g.querySelectorAll('.campo').forEach(c => c.removeAttribute('aria-invalid'));
  });
}

/**
 * Muestra cada error junto a su campo y enfoca el primero.
 * Devuelve los mensajes que no corresponden a ningún campo para mostrarlos en un aviso general.
 */
export function mostrarErrores(raiz, errores) {
  limpiarErrores(raiz);
  const sueltos = [];
  let primero = null;
  for (const [nombre, mensaje] of Object.entries(errores || {})) {
    const grupo = raiz.querySelector(`[data-campo="${nombre}"]`);
    if (!grupo) { sueltos.push(mensaje); continue; }
    const aviso = grupo.querySelector('.campo__error');
    aviso.textContent = mensaje;
    aviso.hidden = false;
    const entrada = grupo.querySelector('.campo');
    entrada.setAttribute('aria-invalid', 'true');
    primero ||= entrada;
  }
  primero?.focus();
  return sueltos;
}

/** Convierte un error de la API en errores por campo (los duplicados de correo o DNI van junto a su campo). */
export function erroresDelServidor(e) {
  if (e.errores) return e.errores;
  if (e.status === 409 && /correo/i.test(e.message)) return { email: e.message };
  if (e.status === 409 && /DNI/.test(e.message)) return { dni: e.message };
  return null;
}

