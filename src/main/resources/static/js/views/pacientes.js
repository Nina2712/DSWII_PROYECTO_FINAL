// Vista "Pacientes" (recepción y administración): listado con búsqueda y alta de pacientes.
import { api } from '../api.js';
import { esc, iniciales, icono, numero } from '../util.js';
import { abrirNuevoPaciente } from '../pacientes.js';

const sinTildes = t => String(t).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

export async function pintarPacientes(cont, ctx) {
  cont.innerHTML = `
    <header class="pagina__cab">
      <div><h1 class="pagina__titulo">Pacientes</h1><p class="pagina__sub" id="conteo">Cargando…</p></div>
      <button class="btn btn--primario" data-nuevo>${icono.mas}<span>Registrar paciente</span></button>
    </header>
    <section class="tarjeta">
      <form class="filtros" onsubmit="return false">
        <label class="filtro filtro--ancho"><span>Buscar</span>
          <input class="campo" type="search" id="buscar" placeholder="Nombre o DNI" autocomplete="off"></label>
      </form>
      <div id="tabla" class="tabla-contenedor"><div class="esqueleto-filas">${'<span></span>'.repeat(5)}</div></div>
    </section>`;

  let pacientes = [];
  const tabla = cont.querySelector('#tabla');
  const conteo = cont.querySelector('#conteo');
  const buscar = cont.querySelector('#buscar');

  function dibujar() {
    const q = sinTildes(buscar.value.trim());
    const lista = q ? pacientes.filter(p => sinTildes(p.nombre).includes(q) || p.dni.includes(q)) : pacientes;
    conteo.textContent = q
      ? `${numero(lista.length)} de ${numero(pacientes.length)} pacientes`
      : `${numero(pacientes.length)} ${pacientes.length === 1 ? 'paciente registrado' : 'pacientes registrados'}`;
    if (!lista.length) {
      tabla.innerHTML = `<div class="vacio">${icono.vacio}<p><b>${pacientes.length ? 'Ningún paciente coincide' : 'Aún no hay pacientes'}</b></p>
        <p class="tenue">${pacientes.length ? 'Prueba con otro nombre o DNI.' : 'Registra al primero con el botón de arriba.'}</p></div>`;
      return;
    }
    tabla.innerHTML = `
      <table class="tabla tabla--plana">
        <thead><tr><th>Paciente</th><th>DNI</th></tr></thead>
        <tbody>${lista.map(p => `
          <tr><td><span class="celda-persona"><span class="avatar">${esc(iniciales(p.nombre))}</span><b>${esc(p.nombre)}</b></span></td>
          <td class="nowrap">${esc(p.dni)}</td></tr>`).join('')}</tbody>
      </table>`;
  }

  async function cargar() {
    try {
      pacientes = await api.pacientes();
      dibujar();
    } catch (e) {
      conteo.textContent = '';
      tabla.innerHTML = `<div class="alerta alerta--error">${esc(e.message)} <button class="btn btn--texto" data-reintentar>Reintentar</button></div>`;
      tabla.querySelector('[data-reintentar]').addEventListener('click', cargar);
    }
  }

  buscar.addEventListener('input', dibujar);
  cont.querySelector('[data-nuevo]').addEventListener('click', () => abrirNuevoPaciente({ alCrear: cargar }));
  await cargar();
  return { recargar: cargar };
}
