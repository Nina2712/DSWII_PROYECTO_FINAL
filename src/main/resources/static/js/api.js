// Cliente de la API REST de MediCita (mismo origen que el dashboard).
const CLAVE = 'medicita.sesion';

export const sesion = {
  get() { try { return JSON.parse(sessionStorage.getItem(CLAVE)); } catch { return null; } },
  set(valor) { sessionStorage.setItem(CLAVE, JSON.stringify(valor)); },
  clear() { sessionStorage.removeItem(CLAVE); }
};

export class ApiError extends Error {
  constructor(status, mensaje, errores) { super(mensaje); this.status = status; this.errores = errores; }
}

let alExpirar = () => {};
export function alExpirarSesion(fn) { alExpirar = fn; }

async function pedir(metodo, ruta, { params, body, auth = true } = {}) {
  const url = new URL(ruta, location.origin);
  for (const [k, v] of Object.entries(params || {})) {
    if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
  }
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const s = sesion.get();
  if (auth && s) headers.Authorization = `Bearer ${s.token}`;

  let res;
  try {
    res = await fetch(url, { method: metodo, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.');
  }
  if (res.status === 204) return null;
  let datos = null;
  try { datos = await res.json(); } catch { /* sin cuerpo JSON */ }
  if (!res.ok) {
    if (res.status === 401 && auth) alExpirar();
    let mensaje = datos?.mensaje || `Error inesperado (${res.status})`;
    if (datos?.errores) mensaje = Object.values(datos.errores).join('. ');
    throw new ApiError(res.status, mensaje, datos?.errores);
  }
  return datos;
}

export const api = {
  login: (email, password) => pedir('POST', '/api/auth/login', { body: { email, password }, auth: false }),
  registrar: cuerpo => pedir('POST', '/api/auth/registro', { body: cuerpo, auth: false }),
  resumen: () => pedir('GET', '/api/dashboard/resumen'),
  citas: params => pedir('GET', '/api/citas', { params }),
  cita: id => pedir('GET', `/api/citas/${id}`),
  crearCita: cuerpo => pedir('POST', '/api/citas', { body: cuerpo }),
  actualizarCita: (id, cuerpo) => pedir('PUT', `/api/citas/${id}`, { body: cuerpo }),
  cancelarCita: id => pedir('DELETE', `/api/citas/${id}`),
  eliminarCita: id => pedir('DELETE', `/api/citas/${id}`, { params: { definitivo: true } }),
  medicos: () => pedir('GET', '/api/catalogo/medicos'),
  pacientes: () => pedir('GET', '/api/catalogo/pacientes'),
  sedes: () => pedir('GET', '/api/catalogo/sedes'),
  sedesDetalle: () => pedir('GET', '/api/sedes'),
  crearSede: cuerpo => pedir('POST', '/api/sedes', { body: cuerpo }),
  actualizarSede: (id, cuerpo) => pedir('PUT', `/api/sedes/${id}`, { body: cuerpo }),
  eliminarSede: id => pedir('DELETE', `/api/sedes/${id}`),
  crearPaciente: cuerpo => pedir('POST', '/api/pacientes', { body: cuerpo }),
  especialidades: () => pedir('GET', '/api/especialidades'),
  crearEspecialidad: cuerpo => pedir('POST', '/api/especialidades', { body: cuerpo }),
  actualizarEspecialidad: (id, cuerpo) => pedir('PUT', `/api/especialidades/${id}`, { body: cuerpo }),
  eliminarEspecialidad: id => pedir('DELETE', `/api/especialidades/${id}`),
  medicosAdmin: () => pedir('GET', '/api/medicos'),
  crearMedico: cuerpo => pedir('POST', '/api/medicos', { body: cuerpo }),
  actualizarMedico: (id, cuerpo) => pedir('PUT', `/api/medicos/${id}`, { body: cuerpo }),
  horasLibres: (medicoId, fecha) => pedir('GET', `/api/catalogo/medicos/${medicoId}/horas-libres`, { params: { fecha } })
};

/** Trae todas las páginas de citas de un rango (para la agenda). */
export async function todasLasCitas(filtros) {
  const todas = [];
  for (let page = 0; page < 20; page++) {
    const r = await api.citas({ ...filtros, page, size: 50, orden: 'asc' });
    todas.push(...r.content);
    if (page + 1 >= r.totalPages) break;
  }
  return todas;
}

let catalogoMedicos = null;
export async function medicosEnCache() {
  if (!catalogoMedicos) catalogoMedicos = await api.medicos();
  return catalogoMedicos;
}

/** Descarta el catálogo guardado (tras crear o editar médicos y especialidades). */
export function olvidarMedicos() { catalogoMedicos = null; }
