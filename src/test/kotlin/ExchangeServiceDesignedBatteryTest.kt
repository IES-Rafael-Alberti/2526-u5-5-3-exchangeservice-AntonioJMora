package com.example.exchange

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import org.iesra.revilofe.ExchangeRateProvider
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.InMemoryExchangeRateProvider
import org.iesra.revilofe.Money

class ExchangeServiceDesignedBatteryTest : DescribeSpec({

    afterTest {
        clearAllMocks()
    }

    describe("battery designed from equivalence classes for ExchangeService") {

        describe("input validation") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            it("throws an exception when the amount is zero") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(0, "USD"), "EUR")
                }
            }

            it("throws an exception when the amount is negative") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(-100, "USD"), "EUR")
                }
            }

            it("throws an exception when the source currency code is invalid") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(100, "USD"), "EUR")
                }
            }

            it("throws an exception when the target currency code is invalid") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(100, "USD"), "EU")
            }
        }

        describe("misma moneda") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            it("devuelve la misma cantidad cuando el origen y el destino son iguales") {
                val result = service.exchange(Money(100, "USD"), "USD")
                result shouldBe 100
                verify(exactly = 0) { provider.rate(any()) }
            }
        }

        describe("tasa directa usando stub") {
            it("convierte correctamente usando una tasa directa") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate("USDEUR") } returns 0.90

                val service = ExchangeService(provider)
                val result = service.exchange(Money(100, "USD"), "EUR")

                result shouldBe 90
                }
        }

        describe("spy sobre InMemoryExchangeRateProvider") {
            it("usa un proveedor real y verifica la llamada exacta") {
                val realProvider = InMemoryExchangeRateProvider(mapOf("USDEUR" to 0.90))
                val spyProvider = spyk(realProvider)

                val service = ExchangeService(spyProvider)
                val result = service.exchange(Money(100, "USD"), "EUR")

                result shouldBe 90
                verify(exactly = 1) { spyProvider.rate("USDEUR") }
            }
        }

        describe("conversión cruzada usando mock") {
            it("resuelve una conversión cruzada cuando no existe la tasa directa") {
                val provider = mockk<ExchangeRateProvider>()
                // La ruta directa falla
                every { provider.rate("GBPUSD") } throws IllegalArgumentException("No hay tasa")
                // El servicio intentará rutas cruzadas. Asumiendo supported: USD, EUR, GBP, JPY
                // Intentará GBP -> EUR -> USD
                every { provider.rate("GBPEUR") } returns 1.15
                every { provider.rate("EURUSD") } returns 1.10
                // Para las demás que pueda probar, lanzamos excepción
                every { provider.rate("GBPJPY") } throws IllegalArgumentException()
                every { provider.rate("JPYUSD") } throws IllegalArgumentException()

                val service = ExchangeService(provider, setOf("EUR", "JPY"))

                val result = service.exchange(Money(100, "GBP"), "USD")

                result shouldBe (100 * 1.15 * 1.10).toLong()
            }

            it("intenta una segunda ruta intermedia si la primera falla") {
                val provider = mockk<ExchangeRateProvider>()
                // Ruta directa falla
                every { provider.rate("GBPUSD") } throws IllegalArgumentException()
                // Primera ruta cruzada: GBP -> EUR -> USD (falla la segunda parte)
                every { provider.rate("GBPEUR") } returns 1.15
                every { provider.rate("EURUSD") } throws IllegalArgumentException()
                // Segunda ruta cruzada: GBP -> JPY -> USD (funciona)
                every { provider.rate("GBPJPY") } returns 150.0
                every { provider.rate("JPYUSD") } returns 0.007

                // El orden en el set puede afectar, por lo que usamos una lista convertida a set que respete el orden
                val service = ExchangeService(provider, linkedSetOf("EUR", "JPY"))

                val result = service.exchange(Money(100, "GBP"), "USD")

                result shouldBe (100 * 150.0 * 0.007).toLong()
            }

            it("lanza una excepción si no existe ninguna ruta válida") {
                val provider = mockk<ExchangeRateProvider>()
                // Cualquier consulta falla
                every { provider.rate(any()) } throws IllegalArgumentException()

                val service = ExchangeService(provider, setOf("EUR", "JPY"))

                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(100, "GBP"), "USD")
                }
            }

            it("verifica el orden exacto de las llamadas al proveedor en una conversión cruzada") {
                val provider = mockk<ExchangeRateProvider>()
                // Ruta directa falla
                every { provider.rate("GBPUSD") } throws IllegalArgumentException()

                // EUR intermedia
                every { provider.rate("GBPEUR") } returns 1.15
                every { provider.rate("EURUSD") } throws IllegalArgumentException()

                // JPY intermedia
                every { provider.rate("GBPJPY") } returns 150.0
                every { provider.rate("JPYUSD") } returns 0.007

                val service = ExchangeService(provider, linkedSetOf("EUR", "JPY"))

                service.exchange(Money(100, "GBP"), "USD")

                verifySequence {
                    provider.rate("GBPUSD")
                    provider.rate("GBPEUR")
                    provider.rate("EURUSD")
                    provider.rate("GBPJPY")
                    provider.rate("JPYUSD")
                }
            }
        }
}}})
