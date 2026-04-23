[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/Og7iRJ-r)
# Mock, Stub o Spy en Kotlin

Antes de empezar, recuerda que el objetivo es realizar la práctica aqui enlazada:
- [pŕactica](#ejercicio-propuesto)
- [preguntas](#preguntas)

Este proyecto usa `Kotest` y `MockK` para probar un servicio de cambio de divisas. Antes de escribir pruebas, conviene tener clara la diferencia entre tres dobles de prueba muy habituales: `stub`, `spy` y `mock`.

## ¿Mock, Stub o Spy? ¿Cuál es la diferencia?

Los tres son dobles de prueba, es decir, objetos que sustituyen a dependencias reales durante un test. La diferencia principal está en el nivel de control que necesitamos y en lo que queremos comprobar.

### Stub

Un `stub` es el doble más simple. Devuelve respuestas fijas y predecibles, normalmente sin importar demasiado cómo se le llame.

Úsalo cuando:

- solo necesitas que una dependencia responda algo concreto
- no te interesa verificar interacciones
- quieres que la prueba sea muy sencilla y estable

Idea clave:

- el `stub` responde
- el test se centra en el resultado final

Ejemplo mental:

- "Si me piden la tasa `USDEUR`, devuelvo `0.92` y ya está"

### Spy

Un `spy` envuelve un objeto real o una implementación real y deja que su comportamiento siga funcionando, pero además permite observar cómo se ha usado.

Úsalo cuando:

- quieres conservar el comportamiento real
- necesitas verificar llamadas, parámetros o número de invocaciones
- quieres sustituir solo una parte concreta y dejar el resto intacto

Idea clave:

- el `spy` ejecuta comportamiento real
- el test además inspecciona cómo se interactuó con él

Ejemplo mental:

- "Quiero usar un proveedor real en memoria, pero comprobar si se llamó con el par correcto"

### Mock

Un `mock` es el doble más configurable y más poderoso. Permite definir de antemano qué debe pasar cuando se invoquen ciertos métodos, y además permite verificar interacciones.

Úsalo cuando:

- necesitas controlar totalmente una dependencia
- quieres simular distintos comportamientos según parámetros o número de llamada
- quieres provocar errores, respuestas distintas o flujos concretos
- la interacción con la dependencia forma parte importante de lo que se está probando

Idea clave:

- el `mock` responde como tú configuras
- el test valida tanto resultado como interacción

Ejemplo mental:

- "La primera llamada falla, la segunda devuelve una tasa, y además verifico el orden de las llamadas"

## Resumen rápido

| Doble  | Qué hace                                         | Cuándo usarlo                                                 |
|--------|--------------------------------------------------|---------------------------------------------------------------|
| `Stub` | Devuelve datos fijos                             | Cuando solo necesitas una respuesta simple                    |
| `Spy`  | Usa comportamiento real y permite observar       | Cuando quieres comprobar interacciones sin perder lógica real |
| `Mock` | Simula comportamiento configurable y verificable | Cuando necesitas control total sobre la dependencia           |

## Regla práctica

Una forma sencilla de decidirlo es esta:

- si solo necesitas una respuesta fija, usa `stub`
- si quieres observar un objeto real, usa `spy`
- si necesitas controlar y verificar todo, usa `mock`

En general, conviene empezar por la opción más simple. Si un `stub` basta, no hace falta subir a `spy` o `mock`.

## Cómo hacerlo con Kotest y MockK

`Kotest` aporta la estructura y las aserciones del test. `MockK` aporta los dobles de prueba.

En este proyecto, la dependencia que solemos doblar es `ExchangeRateProvider`, que usa `ExchangeService`.

## 1. Stub con MockK

Aquí usamos `MockK` para crear un doble que devuelve siempre un valor esperado. No nos interesa demasiado cuántas veces se llama, sino que permita probar el cálculo.

```kotlin
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.iesra.revilofe.ExchangeRateProvider
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.Money

class ExchangeServiceStubTest : DescribeSpec({

    describe("con un stub") {
        it("devuelve una conversión usando una respuesta fija") {
            val provider = mockk<ExchangeRateProvider>()
            every { provider.rate("USDEUR") } returns 0.92

            val service = ExchangeService(provider)

            service.exchange(Money(1000, "USD"), "EUR") shouldBe 920
        }
    }
})
```

Qué está pasando:

- `provider` actúa como `stub`
- la llamada `rate("USDEUR")` devuelve siempre `0.92`
- no estamos usando el doble para inspeccionar comportamiento, solo para dar una respuesta controlada

## 2. Spy con MockK

Un `spy` es especialmente útil cuando ya tienes una implementación real y quieres mantenerla. En este proyecto encaja bien con `InMemoryExchangeRateProvider`.

```kotlin
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.InMemoryExchangeRateProvider
import org.iesra.revilofe.Money

class ExchangeServiceSpyTest : DescribeSpec({

    describe("con un spy") {
        it("usa el comportamiento real y permite verificar la interacción") {
            val realProvider = InMemoryExchangeRateProvider(
                mapOf("USDEUR" to 0.92)
            )
            val providerSpy = spyk(realProvider)

            val service = ExchangeService(providerSpy)

            service.exchange(Money(1000, "USD"), "EUR") shouldBe 920

            verify(exactly = 1) { providerSpy.rate("USDEUR") }
        }
    }
})
```

Qué está pasando:

- `spyk(realProvider)` crea un `spy` sobre un objeto real
- la lógica de `InMemoryExchangeRateProvider` sigue funcionando
- además podemos verificar la llamada realizada

También podrías usar un `spy` para sustituir solo una parte del comportamiento real.

## 3. Mock con MockK

El `mock` es útil cuando quieres modelar escenarios concretos y además verificar interacciones complejas, por ejemplo una conversión cruzada.

```kotlin
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.iesra.revilofe.ExchangeRateProvider
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.Money

class ExchangeServiceMockTest : DescribeSpec({

    describe("con un mock") {
        it("permite controlar un flujo de conversión cruzada") {
            val provider = mockk<ExchangeRateProvider>()

            every { provider.rate("GBPJPY") } throws IllegalArgumentException()
            every { provider.rate("GBPUSD") } returns 1.27
            every { provider.rate("USDJPY") } returns 150.5

            val service = ExchangeService(
                provider,
                supportedCurrencies = setOf("USD", "EUR", "GBP", "JPY")
            )

            service.exchange(Money(2, "GBP"), "JPY") shouldBe (2 * 1.27 * 150.5).toLong()

            verifySequence {
                provider.rate("GBPJPY")
                provider.rate("GBPUSD")
                provider.rate("USDJPY")
            }
        }
    }
})
```

Qué está pasando:

- el `mock` no ejecuta una implementación real
- definimos exactamente qué debe ocurrir en cada llamada
- verificamos incluso el orden de las invocaciones

Esto es lo más útil cuando el comportamiento de la dependencia cambia según el caso.

## Qué aporta Kotest aquí

`Kotest` no crea mocks por sí mismo en este ejemplo. Su papel es:

- organizar los tests con `DescribeSpec`
- hacer las aserciones con `shouldBe`
- mejorar la legibilidad del test

`MockK` se encarga de:

- `mockk()` para crear mocks y stubs
- `spyk()` para crear spies
- `every { ... } returns ...` para definir comportamiento
- `verify` y `verifySequence` para comprobar interacciones

## Entonces, con MockK, ¿cómo distingo stub de mock?

En `MockK`, muchas veces el mismo objeto se crea con `mockk()`. La diferencia no está tanto en la función que usas para crearlo, sino en el uso que le das dentro del test.

Por ejemplo:

- si usas `mockk()` solo para devolver datos fijos, lo estás usando como `stub`
- si usas `mockk()` para configurar distintos comportamientos y verificar llamadas, lo estás usando como `mock`
- si usas `spyk()`, lo estás usando como `spy`

Es decir, en la práctica con `MockK`:

- `mockk()` puede comportarse como `stub` o como `mock`
- `spyk()` se usa para `spy`

## Cuándo usar cada uno en este proyecto

Para `ExchangeService`, una guía razonable sería:

- usa `stub` si solo quieres probar una conversión directa simple
- usa `spy` si quieres aprovechar `InMemoryExchangeRateProvider` y además comprobar llamadas
- usa `mock` si quieres forzar rutas cruzadas, errores, excepciones o secuencias de llamadas

## Recomendación final

Empieza siempre por el doble más simple que permita expresar bien la prueba:

1. `stub` si solo necesitas una respuesta fija
2. `spy` si te interesa conservar comportamiento real
3. `mock` si necesitas control total y verificación detallada

Eso suele producir tests más claros, más mantenibles y menos frágiles.

## Ejercicio Propuesto

El ejercicio consiste en diseñar una batería de pruebas para `ExchangeService` desde cero, a partir de la especificación del servicio y aplicando clases de equivalencia y selección consciente de dobles de prueba.

siguiendo el estilo de `DescribeSpec` de `Kotest` y usando `MockK` para los dobles.

`Describe` y `it` deben describir claramente el caso de prueba, la clase de equivalencia. Basate en los apunte spara definir las clases de equivalencia y justificar el uso de `stub`, `spy` o `mock` en cada caso.

### Base de código a probar

El servicio bajo prueba es `ExchangeService`.

Su responsabilidad es:

- validar la entrada
- devolver la misma cantidad si la moneda origen y destino coinciden
- usar una tasa directa cuando existe
- intentar una conversión cruzada con una sola moneda intermedia cuando no exista la tasa directa
- lanzar una excepción si no existe ninguna ruta válida

La dependencia del servicio es `ExchangeRateProvider`, y existe una implementación concreta llamada `InMemoryExchangeRateProvider`.

La idea del ejercicio es aislar la lógica de `ExchangeService` para probarla sin depender de una fuente real de tasas, es decir, sin depender de `InMemoryExchangeRateProvider` a menos que sea para un caso concreto de `spy`.

### Diseño de la batería de pruebas

#### 1. Clases de equivalencia

A continuación se proponen clases de equivalencia para cada aspecto relevante del servicio, aunque tu puedes definir otras o subdividirlas según consideres.

##### A. Validación de entrada

Clases válidas:

- cantidad positiva
- moneda origen con 3 letras
- moneda destino con 3 letras

Clases inválidas:

- cantidad igual a cero
- cantidad negativa
- moneda origen con longitud distinta de 3
- moneda destino con longitud distinta de 3

##### B. Relación entre moneda origen y destino

Clases relevantes:

- origen y destino iguales
- origen y destino distintos con tasa directa
- origen y destino distintos sin tasa directa pero con ruta cruzada válida
- origen y destino distintos sin ninguna ruta posible

##### C. Estrategia de búsqueda de tasas

Clases relevantes:

- éxito en consulta directa
- fallo en consulta directa y éxito en primer cruce válido
- fallo en primer cruce y éxito en un cruce alternativo posterior
- fallo en todas las consultas

#### 2. Elección del doble de prueba

##### Casos para `stub`

Usa `stub` cuando solo quieras fijar una tasa y centrarte en el resultado del cálculo.

Casos mínimos:

- conversión directa simple `USD -> EUR`
- comprobación de que una conversión directa devuelve la cantidad esperada

##### Casos para `spy`

Usa `spy` cuando quieras conservar el comportamiento real de `InMemoryExchangeRateProvider` y verificar interacción.

Casos mínimos:

- misma moneda: comprobar que no se consulta el proveedor
- ruta directa real en memoria: comprobar que se consulta exactamente el par correcto

##### Casos para `mock`

Usa `mock` cuando quieras controlar por completo la interacción con `ExchangeRateProvider`.

Casos mínimos:

- la ruta directa falla pero existe una ruta intermedia válida
- la primera ruta intermedia falla y la segunda funciona
- no existe ninguna ruta posible
- verificación del orden de llamadas

#### 3. Batería mínima exigida

Implementa una batería con al menos los siguientes tests:

1. Debe lanzar excepción si la cantidad es `0`.
2. Debe lanzar excepción si la cantidad es negativa.
3. Debe lanzar excepción si la moneda origen no tiene 3 letras.
4. Debe lanzar excepción si la moneda destino no tiene 3 letras.
5. Debe devolver la misma cantidad si origen y destino son iguales.
6. Debe convertir correctamente usando una tasa directa con `stub`.
7. Debe usar `spy` sobre `InMemoryExchangeRateProvider` para verificar una llamada real correcta.
8. Debe resolver una conversión cruzada cuando la tasa directa no exista usando `mock`.
9. Debe intentar una segunda ruta intermedia si la primera falla usando `mock`.
10. Debe lanzar excepción si no existe ninguna ruta válida.
11. Debe verificar el orden exacto de las llamadas al proveedor en una conversión cruzada.

### Qué debe entregar el alumnado

El alumnado debe implementar una batería de pruebas propia para `ExchangeService`. Puedes usar las clases de equivalencia y casos propuestos como guía, o puedes diseñar tus propias clases de equivalencia y casos partiendo de los que ya hay. Lo importante es que la batería cubra aspectos relevantes (Clses de equivalencias) del servicio y que el uso de `stub`, `spy` y `mock` esté justificado por el caso concreto.

El alumnado debe responder a las preguntas de mas abajo-.

La solución debe:

- justificar implícitamente el uso de `stub`, `spy` y `mock`
- cubrir las clases de equivalencia anterioresv o tuyas
- verificar tanto resultados como interacciones cuando corresponda

## Preguntas

Te dejo una batería de **preguntas de reflexión/evaluación** directamente alineadas con el ejercicio.

Las preguntas están formuladas para que **mires tu propio código**, justifiques decisiones y, muy importante, **dejes enlaces permanentes (permalinks) al repositorio** como evidencia evaluable.


### Preguntas de evaluación sobre la batería de pruebas

> 📌 **Instrucción común para todas las preguntas:**
> En cada respuesta debes incluir **enlaces permanentes (permalinks) al código** donde se evidencie lo que explicas (tests concretos, configuraciones, uso de mocks, etc.).


#### 🔹 1) CE b) Se han definido casos de prueba

**Pregunta:**

Identifica **al menos 3 casos de prueba de tu batería** y explica:

* Qué **clase de equivalencia** cubre cada uno (válida o inválida).
* Qué **condición concreta del servicio** estás validando (validación, tasa directa, conversión cruzada, etc.).
* Por qué ese caso es representativo dentro del conjunto de pruebas.

Incluye enlaces a los tests correspondientes.


#### 🔹 2) CE f) Se han efectuado pruebas unitarias de clases y funciones

**Pregunta:**

Selecciona uno de tus tests y explica cómo se trata de una **prueba unitaria real sobre `ExchangeService`**:

* Qué método estás probando exactamente.
* Cómo has aislado la lógica de la clase respecto a sus dependencias.
* Qué entrada proporcionas y qué salida verificas.

Justifica por qué este test cumple con el concepto de prueba unitaria según el módulo 

Incluye enlace al test.


#### 🔹 3) CE g) Se han implementado pruebas automáticas

**Pregunta:**

Explica cómo se ejecuta tu batería de pruebas de forma automática:

* Qué herramienta utilizas (Kotest, Gradle, etc.).
* Cómo se lanzan todas las pruebas sin intervención manual.
* Qué evidencia tienes de que los tests verifican automáticamente el comportamiento del sistema (por ejemplo: assertions, fallos, etc.).

Incluye enlace a:

* configuración (build.gradle.kts o similar)
* ejecución de tests


#### 🔹 4) CE h) Se han documentado las incidencias detectadas

**Pregunta:**

Durante el desarrollo de la batería de pruebas, identifica **al menos una incidencia o comportamiento inesperado** que hayas detectado:

* Qué test la detectó.
* Qué comportamiento incorrecto observaste.
* Cómo lo solucionaste (o cómo debería solucionarse).

Relaciona esto con la importancia de documentar incidencias en el proceso de pruebas 

Incluye enlace al test implicado.


#### 🔹 5) CE i) Se han utilizado dobles de prueba para aislar los componentes durante las pruebas

**Pregunta:**

Analiza el uso de dobles de prueba en tu batería y explica:

* Un caso donde hayas usado **stub**, otro con **mock** y otro con **spy**.
* Qué objetivo tiene cada uno en ese test concreto.
* Qué problema tendrías si usaras directamente `InMemoryExchangeRateProvider` en todos los casos.

Relaciona tu explicación con la necesidad de reducir el acoplamiento en pruebas unitarias 

Incluye enlaces a los tests donde se utilicen.


## Fuente conceptual

La explicación conceptual de este documento se apoya en el artículo:

- Rob Bell, "Mock, Stub, or Spy? What's the Difference, and When Should I Use Each?", Atomic Object
