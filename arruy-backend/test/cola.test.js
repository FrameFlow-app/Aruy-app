const test = require('node:test');
const assert = require('node:assert/strict');
const { crearLimitador, ErrorImagen } = require('../imagen');

const dormir = (ms) => new Promise((r) => setTimeout(r, ms));

test('la fila de espera: nunca mas de 2 a la vez, 3 esperan, el resto recibe 503', async () => {
  const conCupo = crearLimitador(2, 3);
  let simultaneas = 0;
  let maximo = 0;
  const tarea = async () => {
    simultaneas += 1;
    maximo = Math.max(maximo, simultaneas);
    await dormir(40);
    simultaneas -= 1;
    return 'ok';
  };
  const resultados = await Promise.allSettled(Array.from({ length: 8 }, () => conCupo(tarea)));
  const hechas = resultados.filter((r) => r.status === 'fulfilled');
  const rechazadas = resultados.filter((r) => r.status === 'rejected');

  assert.equal(maximo, 2, 'nunca mas de 2 al mismo tiempo');
  assert.equal(hechas.length, 5, '2 en proceso + 3 en espera');
  assert.equal(rechazadas.length, 3);
  for (const r of rechazadas) {
    assert.ok(r.reason instanceof ErrorImagen);
    assert.equal(r.reason.status, 503);
  }
});

test('despues de terminar, los cupos quedan libres (no se pierde ninguno)', async () => {
  const conCupo = crearLimitador(2, 3);
  await Promise.all(Array.from({ length: 5 }, () => conCupo(() => dormir(10))));
  let simultaneas = 0;
  let maximo = 0;
  await Promise.all(
    Array.from({ length: 2 }, () =>
      conCupo(async () => {
        simultaneas += 1;
        maximo = Math.max(maximo, simultaneas);
        await dormir(20);
        simultaneas -= 1;
      })
    )
  );
  assert.equal(maximo, 2, 'los 2 cupos siguen disponibles');
});

test('si la tarea falla, el cupo se libera igual', async () => {
  const conCupo = crearLimitador(1, 1);
  await assert.rejects(conCupo(async () => { throw new Error('fallo'); }), /fallo/);
  assert.equal(await conCupo(async () => 'sigue funcionando'), 'sigue funcionando');
});
