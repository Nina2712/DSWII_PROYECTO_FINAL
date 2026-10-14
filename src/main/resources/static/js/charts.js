// Gráficos sin librerías externas: SVG/HTML que usan las variables de color del tema.
import { esc, numero, DIAS_CORTOS, diaSemana, ESTADOS } from './util.js';

/** Anillo con el total al centro. datos: [{etiqueta, total, clase}] */
export function dona(datos, titulo) {
  const total = datos.reduce((s, d) => s + d.total, 0);
  const R = 52, C = 2 * Math.PI * R;
  let acumulado = 0;
  const arcos = total === 0 ? '' : datos.filter(d => d.total > 0).map(d => {
    const largo = (d.total / total) * C;
    const arco = `<circle class="dona__arco dona__arco--${d.clase}" cx="70" cy="70" r="${R}" fill="none" stroke-width="18"
      stroke-dasharray="${Math.max(largo - 2, 0.5)} ${C}" stroke-dashoffset="${-acumulado}" transform="rotate(-90 70 70)">
      <title>${esc(d.etiqueta)}: ${d.total}</title></circle>`;
    acumulado += largo;
    return arco;
  }).join('');
  const leyenda = datos.map(d => `
    <li><span class="punto punto--${d.clase}"></span><span class="leyenda__texto">${esc(d.etiqueta)}</span>
    <b>${numero(d.total)}</b></li>`).join('');
  return `
    <div class="dona" role="img" aria-label="${esc(titulo)}: ${total} citas">
      <svg viewBox="0 0 140 140" class="dona__svg">
        <circle cx="70" cy="70" r="${R}" fill="none" stroke-width="18" class="dona__fondo"/>
        ${arcos}
        <text x="70" y="68" text-anchor="middle" class="dona__total">${numero(total)}</text>
        <text x="70" y="86" text-anchor="middle" class="dona__sub">citas</text>
      </svg>
      <ul class="leyenda">${leyenda}</ul>
    </div>`;
}

/** Barras verticales por día. puntos: [{fecha, total, canceladas}] */
export function barrasPorDia(puntos, hoy) {
  const max = Math.max(1, ...puntos.map(p => p.total));
  const barras = puntos.map(p => {
    const alto = (p.total / max) * 100;
    const esHoy = p.fecha === hoy;
    const futuro = p.fecha > hoy;
    const d = Number(p.fecha.slice(8));
    return `
      <div class="barra ${esHoy ? 'barra--hoy' : ''} ${futuro ? 'barra--futuro' : ''}" title="${esc(p.fecha)}: ${p.total} citas">
        <span class="barra__valor">${p.total || ''}</span>
        <div class="barra__pista"><div class="barra__relleno" style="--alto:${alto}%"></div></div>
        <span class="barra__dia">${DIAS_CORTOS[diaSemana(p.fecha)].slice(0, 2)}</span>
        <span class="barra__num">${d}</span>
      </div>`;
  }).join('');
  return `<div class="barras" role="img" aria-label="Citas por día">${barras}</div>`;
}

/** Barras horizontales con etiqueta y valor. datos: [{etiqueta, total}] */
export function barrasHorizontales(datos, max = 6) {
  if (!datos.length) return '<p class="vacio-texto">Sin datos en este período.</p>';
  const lista = datos.slice(0, max);
  const tope = Math.max(1, ...lista.map(d => d.total));
  return `<div class="hbarras">${lista.map(d => `
    <div class="hbarra">
      <span class="hbarra__texto" title="${esc(d.etiqueta)}">${esc(d.etiqueta)}</span>
      <div class="hbarra__pista"><div class="hbarra__relleno" style="--ancho:${(d.total / tope) * 100}%"></div></div>
      <b>${numero(d.total)}</b>
    </div>`).join('')}</div>`;
}

export const datosEstado = porEstado => porEstado.map(e => ({
  etiqueta: (ESTADOS[e.etiqueta] || { texto: e.etiqueta }).texto,
  total: e.total,
  clase: (ESTADOS[e.etiqueta] || { clase: 'pendiente' }).clase
}));
