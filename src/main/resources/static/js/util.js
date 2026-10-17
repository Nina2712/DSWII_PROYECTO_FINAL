// Utilidades compartidas: texto seguro, fechas (hora de Lima), etiquetas, avisos.
export const ZONA = 'America/Lima';

export function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export const ROLES = {
  ADMIN: 'Administrador', RECEPCIONISTA: 'Recepción', MEDICO: 'Médico', PACIENTE: 'Paciente'
};

export const TIPOS_SEGURO = { PARTICULAR: 'Particular', ESSALUD: 'EsSalud', EPS: 'EPS', SEGURO_PRIVADO: 'Seguro privado' };

export const ESTADOS = {
  PENDIENTE: { texto: 'Pendiente', clase: 'pendiente' },
  CONFIRMADA: { texto: 'Confirmada', clase: 'confirmada' },
  ATENDIDA: { texto: 'Atendida', clase: 'atendida' },
  CANCELADA: { texto: 'Cancelada', clase: 'cancelada' },
  NO_ASISTIO: { texto: 'No asistió', clase: 'noasistio' }
};
export const ESTADOS_FINALES = ['ATENDIDA', 'CANCELADA', 'NO_ASISTIO'];

export const puede = {
  reservar: rol => ['PACIENTE', 'RECEPCIONISTA', 'ADMIN'].includes(rol),
  elegirPaciente: rol => ['RECEPCIONISTA', 'ADMIN'].includes(rol),
  verTodo: rol => ['RECEPCIONISTA', 'ADMIN'].includes(rol),
  verPacientes: rol => ['RECEPCIONISTA', 'ADMIN'].includes(rol),
  administrar: rol => rol === 'ADMIN'
};

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];
export const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

/** "2026-10-14T09:30:00" -> { fecha: "2026-10-14", hora: "09:30", min: 570 } (sin conversión de zona) */
export function partes(iso) {
  const [fecha, resto = '00:00'] = String(iso).split('T');
  const hora = resto.slice(0, 5);
  const [h, m] = hora.split(':').map(Number);
  return { fecha, hora, min: h * 60 + m };
}

export function ymd(y, m, d) {
  return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}

export function sumarDias(fecha, n) {
  const [y, m, d] = fecha.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + n)).toISOString().slice(0, 10);
}

/** 0 = lunes … 6 = domingo */
export function diaSemana(fecha) {
  const [y, m, d] = fecha.split('-').map(Number);
  return (new Date(Date.UTC(y, m - 1, d)).getUTCDay() + 6) % 7;
}

export function inicioSemana(fecha) { return sumarDias(fecha, -diaSemana(fecha)); }

export function ahoraLima() {
  const p = Object.fromEntries(new Intl.DateTimeFormat('en-CA', {
    timeZone: ZONA, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }).formatToParts(new Date()).map(x => [x.type, x.value]));
  return { fecha: `${p.year}-${p.month}-${p.day}`, min: Number(p.hour) * 60 + Number(p.minute) };
}

export const hoyLima = () => ahoraLima().fecha;

export function fechaCorta(fecha) {
  const [y, m, d] = fecha.split('-').map(Number);
  return `${DIAS[diaSemana(fecha)].slice(0, 3)} ${d} ${MESES[m - 1].slice(0, 3)}`;
}

export function fechaLarga(fecha) {
  const [y, m, d] = fecha.split('-').map(Number);
  const dia = DIAS[diaSemana(fecha)];
  return `${dia.charAt(0).toUpperCase() + dia.slice(1)} ${d} de ${MESES[m - 1]} de ${y}`;
}

export function mesAnio(fecha) {
  const [y, m] = fecha.split('-').map(Number);
  return `${MESES[m - 1].charAt(0).toUpperCase() + MESES[m - 1].slice(1)} ${y}`;
}

export function etiquetaCita(iso) {
  const p = partes(iso);
  return `${fechaCorta(p.fecha)} · ${p.hora}`;
}

export function saludo() {
  const h = Math.floor(ahoraLima().min / 60);
  return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
}

export function iniciales(nombre) {
  return String(nombre || '?').split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
}

/** Avatar con la foto del usuario o, si no tiene, sus iniciales. `clase` admite avatar--m, avatar--l, avatar--medico. */
export function avatar(nombre, foto, clase = '') {
  const c = `avatar ${clase}`.trim();
  return foto
    ? `<span class="${c} avatar--foto"><img src="${esc(foto)}" alt=""></span>`
    : `<span class="${c}">${esc(iniciales(nombre))}</span>`;
}

const MAX_FOTO = 11500; // caracteres del data URL; el servidor acepta hasta 12000

/** Recorta al centro, reduce a 128x128 y comprime a JPEG hasta que quepa. Devuelve un data URL. */
export async function procesarFoto(archivo) {
  if (!archivo || !/^image\/(jpeg|png|webp|gif)$/.test(archivo.type)) throw new Error('Elige una imagen JPG, PNG o WebP.');
  if (archivo.size > 10 * 1024 * 1024) throw new Error('La imagen pesa más de 10 MB. Elige una más pequeña.');
  const url = URL.createObjectURL(archivo);
  try {
    const img = await new Promise((ok, mal) => {
      const i = new Image(); i.onload = () => ok(i); i.onerror = () => mal(new Error('No se pudo leer la imagen.')); i.src = url;
    });
    const lado = Math.min(img.naturalWidth, img.naturalHeight);
    const sx = (img.naturalWidth - lado) / 2, sy = (img.naturalHeight - lado) / 2;
    for (const medida of [128, 96, 64]) {
      const lienzo = document.createElement('canvas'); lienzo.width = lienzo.height = medida;
      const g = lienzo.getContext('2d');
      g.fillStyle = '#fff'; g.fillRect(0, 0, medida, medida);
      g.drawImage(img, sx, sy, lado, lado, 0, 0, medida, medida);
      for (let q = 0.85; q >= 0.35; q -= 0.1) {
        const datos = lienzo.toDataURL('image/jpeg', q);
        if (datos.length <= MAX_FOTO) return datos;
      }
    }
    throw new Error('No se pudo reducir la imagen. Prueba con otra.');
  } finally { URL.revokeObjectURL(url); }
}

export function badge(estado) {
  const e = ESTADOS[estado] || { texto: estado, clase: 'pendiente' };
  return `<span class="badge badge--${e.clase}">${esc(e.texto)}</span>`;
}

export function numero(n) { return new Intl.NumberFormat('es-PE').format(n ?? 0); }

export function toast(mensaje, tipo = 'ok') {
  const cont = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = `toast toast--${tipo}`;
  el.textContent = mensaje;
  cont.appendChild(el);
  setTimeout(() => { el.classList.add('toast--sale'); setTimeout(() => el.remove(), 250); }, tipo === 'error' ? 6000 : 3500);
}

export const icono = {
  resumen: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 13h6V4H4zm0 7h6v-5H4zm10 0h6V11h-6zm0-16v5h6V4z"/></svg>',
  agenda: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 2v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2zm12 8v10H5V10z"/></svg>',
  citas: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h10v2H4z"/></svg>',
  pacientes: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16 11a3 3 0 1 0-3-3 3 3 0 0 0 3 3zM8 11a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm0 2c-2.3 0-7 1.2-7 3.5V19h14v-2.5C15 14.2 10.3 13 8 13zm8 0c-.3 0-.6 0-1 .1 1.2.8 2 1.9 2 3.4V19h6v-2.5c0-2.3-4.7-3.5-7-3.5z"/></svg>',
  medico: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-2.7 0-8 1.3-8 4v2h16v-2c0-2.7-5.3-4-8-4zM6 10V7H4v3H1v2h3v3h2v-3h3v-2z"/></svg>',
  especialidad: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M17 3H7a2 2 0 0 0-2 2v16l7-3 7 3V5a2 2 0 0 0-2-2z"/></svg>',
  sede: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2 3 7v2h18V7zM5 11v8H3v2h18v-2h-2v-8h-2v8h-3v-8h-2v8h-2v-8H9v8H7v-8z"/></svg>',
  perfil: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.3 0-8 1.7-8 5v1h16v-1c0-3.3-4.7-5-8-5z"/></svg>',
  editar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 17.3V21h3.7L17.8 9.9l-3.7-3.7zM20.7 7a1 1 0 0 0 0-1.4l-2.3-2.3a1 1 0 0 0-1.4 0l-1.8 1.8 3.7 3.7z"/></svg>',
  buscar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15.5 14h-.8l-.3-.3A6.5 6.5 0 1 0 14 15.5l.3.3v.8l5 5 1.5-1.5zm-6 0a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9z"/></svg>',
  basura: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6zM19 4h-3.5l-1-1h-5l-1 1H5v2h14z"/></svg>',
  mas: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6z"/></svg>',
  sol: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM11 1h2v3h-2zm0 19h2v3h-2zM1 11h3v2H1zm19 0h3v2h-3zM4.2 5.6l1.4-1.4 2.1 2.1-1.4 1.4zm12.1 12.1 1.4-1.4 2.1 2.1-1.4 1.4zM4.2 18.4l2.1-2.1 1.4 1.4-2.1 2.1zM16.3 6.3l2.1-2.1 1.4 1.4-2.1 2.1z"/></svg>',
  luna: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>',
  salir: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h5v-2H5V5h5zm6 4-1.4 1.4 2.6 2.6H9v2h8.2l-2.6 2.6L16 17l5-5z"/></svg>',
  menu: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 6h18v2H3zm0 5h18v2H3zm0 5h18v2H3z"/></svg>',
  cerrar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6.4 5 5.6 5.6L17.6 5 19 6.4 13.4 12l5.6 5.6-1.4 1.4-5.6-5.6L6.4 19 5 17.6l5.6-5.6L5 6.4z"/></svg>',
  izq: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14 6-6 6 6 6 1.4-1.4L10.8 12l4.6-4.6z"/></svg>',
  der: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m10 6-1.4 1.4 4.6 4.6-4.6 4.6L10 18l6-6z"/></svg>',
  vacio: '<svg viewBox="0 0 64 64" aria-hidden="true"><rect x="10" y="12" width="44" height="42" rx="8" fill="none" stroke="currentColor" stroke-width="3"/><path d="M10 26h44M22 8v8M42 8v8" stroke="currentColor" stroke-width="3" stroke-linecap="round"/><path d="M24 40h16" stroke="currentColor" stroke-width="3" stroke-linecap="round"/></svg>'
};
