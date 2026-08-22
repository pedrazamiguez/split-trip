package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseSearchServiceImplTest {

    private lateinit var service: ExpenseSearchService

    @BeforeEach
    fun setUp() {
        service = ExpenseSearchServiceImpl()
    }

    @Nested
    @DisplayName("Blank or Empty Query")
    inner class BlankQuery {

        @Test
        fun `empty query returns original list unmodified`() {
            val expenses = listOf(
                Expense(id = "1", title = "Dinner"),
                Expense(id = "2", title = "Taxi")
            )

            val result = service.search(expenses, "")

            assertEquals(expenses, result)
        }

        @Test
        fun `whitespace only query returns original list unmodified`() {
            val expenses = listOf(
                Expense(id = "1", title = "Dinner"),
                Expense(id = "2", title = "Taxi")
            )

            val result = service.search(expenses, "   ")

            assertEquals(expenses, result)
        }

        @Test
        fun `empty expenses list returns empty list`() {
            val result = service.search(emptyList(), "test")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Matching by Title")
    inner class MatchTitle {

        @Test
        fun `matches exact title case-insensitively`() {
            val dinner = Expense(id = "1", title = "Paella Dinner")
            val taxi = Expense(id = "2", title = "Airport Taxi")
            val expenses = listOf(dinner, taxi)

            val result = service.search(expenses, "paella")

            assertEquals(listOf(dinner), result)
        }

        @Test
        fun `matches partial title case-insensitively with mixed case`() {
            val dinner = Expense(id = "1", title = "Sushi Dinner at Tokyo")
            val lunch = Expense(id = "2", title = "Quick lunch")
            val museum = Expense(id = "3", title = "Museum ticket")
            val expenses = listOf(dinner, lunch, museum)

            val result = service.search(expenses, "DINNER")

            assertEquals(listOf(dinner), result)
        }

        @Test
        fun `trims query whitespace before searching title`() {
            val dinner = Expense(id = "1", title = "Hotel Stay")
            val taxi = Expense(id = "2", title = "Taxi Ride")
            val expenses = listOf(dinner, taxi)

            val result = service.search(expenses, "  Hotel  ")

            assertEquals(listOf(dinner), result)
        }
    }

    @Nested
    @DisplayName("Matching by Notes")
    inner class MatchNotes {

        @Test
        fun `matches notes case-insensitively`() {
            val dinner = Expense(id = "1", title = "Food", notes = "Seafood paella in Valencia")
            val coffee = Expense(id = "2", title = "Coffee", notes = "Espresso bar")
            val expenses = listOf(dinner, coffee)

            val result = service.search(expenses, "valencia")

            assertEquals(listOf(dinner), result)
        }

        @Test
        fun `matches either title or notes across multiple expenses`() {
            val e1 = Expense(id = "1", title = "Valencia Train", notes = "Renfe booking")
            val e2 = Expense(id = "2", title = "Dinner", notes = "Paella in Valencia")
            val e3 = Expense(id = "3", title = "Museum", notes = "Art gallery")
            val expenses = listOf(e1, e2, e3)

            val result = service.search(expenses, "valencia")

            assertEquals(listOf(e1, e2), result)
        }

        @Test
        fun `handles null notes safely without throwing exception`() {
            val e1 = Expense(id = "1", title = "Bus", notes = null)
            val e2 = Expense(id = "2", title = "Metro", notes = "Subway ticket")
            val expenses = listOf(e1, e2)

            val result = service.search(expenses, "subway")

            assertEquals(listOf(e2), result)
        }

        @Test
        fun `null notes expense still matches when title matches query`() {
            val e1 = Expense(id = "1", title = "Ferry Ride", notes = null)
            val e2 = Expense(id = "2", title = "Bus Ride", notes = null)
            val expenses = listOf(e1, e2)

            val result = service.search(expenses, "ferry")

            assertEquals(listOf(e1), result)
        }
    }

    @Nested
    @DisplayName("Matching by Vendor")
    inner class MatchVendor {

        @Test
        fun `matches vendor case-insensitively`() {
            val dinner = Expense(id = "1", title = "Dinner", vendor = "Mercadona")
            val coffee = Expense(id = "2", title = "Coffee", vendor = "Starbucks")
            val expenses = listOf(dinner, coffee)

            val result = service.search(expenses, "mercadona")

            assertEquals(listOf(dinner), result)
        }

        @Test
        fun `matches partial vendor name with mixed case`() {
            val hotel = Expense(id = "1", title = "Accommodation", vendor = "Chengdu Jinjiang Hotel")
            val flight = Expense(id = "2", title = "Flight", vendor = "Air China")
            val expenses = listOf(hotel, flight)

            val result = service.search(expenses, "CHENGDU")

            assertEquals(listOf(hotel), result)
        }

        @Test
        fun `handles null vendor safely without throwing exception`() {
            val e1 = Expense(id = "1", title = "Bus", vendor = null)
            val e2 = Expense(id = "2", title = "Groceries", vendor = "Carrefour")
            val expenses = listOf(e1, e2)

            val result = service.search(expenses, "carrefour")

            assertEquals(listOf(e2), result)
        }

        @Test
        fun `matches across title, vendor, and notes in different expenses`() {
            val e1 = Expense(id = "1", title = "Chengdu Flight", vendor = "Iberia", notes = "Direct flight")
            val e2 = Expense(id = "2", title = "Hotel Stay", vendor = "Chengdu Inn", notes = "Near downtown")
            val e3 = Expense(id = "3", title = "Dinner", vendor = "Local Restaurant", notes = "Chengdu hotpot")
            val e4 = Expense(id = "4", title = "Taxi", vendor = "Didi", notes = "Airport transfer")
            val expenses = listOf(e1, e2, e3, e4)

            val result = service.search(expenses, "chengdu")

            assertEquals(listOf(e1, e2, e3), result)
        }
    }

    @Nested
    @DisplayName("Diacritics and Accents")
    inner class DiacriticsAndAccents {

        @Test
        fun `matches unaccented query against accented title`() {
            val jungle = Expense(id = "1", title = "Expedición a la selva")
            val beach = Expense(id = "2", title = "Día en la playa")
            val expenses = listOf(jungle, beach)

            val result = service.search(expenses, "expedicion")

            assertEquals(listOf(jungle), result)
        }

        @Test
        fun `matches accented query against unaccented title`() {
            val jungle = Expense(id = "1", title = "Expedicion a la selva")
            val beach = Expense(id = "2", title = "Dia en la playa")
            val expenses = listOf(jungle, beach)

            val result = service.search(expenses, "expedición")

            assertEquals(listOf(jungle), result)
        }

        @Test
        fun `matches German umlauts and diaeresis`() {
            val train = Expense(id = "1", title = "München Hbf Ticket")
            val bus = Expense(id = "2", title = "Berlin Bus")
            val expenses = listOf(train, bus)

            val result = service.search(expenses, "munchen")

            assertEquals(listOf(train), result)
        }

        @Test
        fun `matches tildes and cedillas in Portuguese and Spanish`() {
            val cafe = Expense(id = "1", title = "Açaí Bowl", vendor = "São Paulo Bar")
            val restaurant = Expense(id = "2", title = "Restaurante El Niño")
            val expenses = listOf(cafe, restaurant)

            val resultSao = service.search(expenses, "sao paulo")
            val resultAcai = service.search(expenses, "acai")
            val resultNino = service.search(expenses, "nino")

            assertEquals(listOf(cafe), resultSao)
            assertEquals(listOf(cafe), resultAcai)
            assertEquals(listOf(restaurant), resultNino)
        }

        @Test
        fun `matches diacritics in notes and vendor`() {
            val e1 = Expense(id = "1", title = "Dinner", vendor = "Café Central", notes = "Tapas con jalapeño")
            val e2 = Expense(id = "2", title = "Lunch", vendor = "Burger Joint", notes = "Simple meal")
            val expenses = listOf(e1, e2)

            val resultVendor = service.search(expenses, "cafe")
            val resultNotes = service.search(expenses, "jalapeno")

            assertEquals(listOf(e1), resultVendor)
            assertEquals(listOf(e1), resultNotes)
        }
    }

    @Nested
    @DisplayName("Punctuation and Symbols")
    inner class PunctuationAndSymbols {

        @Test
        fun `matches dot-separated query against space-separated title`() {
            val jungle = Expense(id = "1", title = "Expedición a la selva")
            val expenses = listOf(jungle)

            val result = service.search(expenses, "a.la.selva")

            assertEquals(listOf(jungle), result)
        }

        @Test
        fun `matches query when title contains exclamation marks or question marks`() {
            val party = Expense(id = "1", title = "¡Fiesta de Bienvenida!")
            val expenses = listOf(party)

            val result = service.search(expenses, "fiesta de bienvenida")

            assertEquals(listOf(party), result)
        }

        @Test
        fun `matches query with mixed punctuation and symbols against title`() {
            val flight = Expense(id = "1", title = "Flight: MAD -> BCN (Terminal 4)")
            val expenses = listOf(flight)

            val result = service.search(expenses, "flight mad bcn terminal 4")

            assertEquals(listOf(flight), result)
        }

        @Test
        fun `punctuation only query returns original list unmodified`() {
            val e1 = Expense(id = "1", title = "Hotel")
            val e2 = Expense(id = "2", title = "Dinner")
            val expenses = listOf(e1, e2)

            val resultDots = service.search(expenses, "...")
            val resultMixed = service.search(expenses, "!?!? -- ,,")

            assertEquals(expenses, resultDots)
            assertEquals(expenses, resultMixed)
        }
    }

    @Nested
    @DisplayName("Whitespace Normalization")
    inner class WhitespaceNormalization {

        @Test
        fun `matches query with multiple consecutive spaces and punctuation`() {
            val jungle = Expense(id = "1", title = "Expedición a la selva")
            val expenses = listOf(jungle)

            val result = service.search(expenses, "expedición    a la. selva")

            assertEquals(listOf(jungle), result)
        }

        @Test
        fun `matches query when target title contains multiple spaces`() {
            val museum = Expense(id = "1", title = "Prado   Museum    Tour")
            val expenses = listOf(museum)

            val result = service.search(expenses, "prado museum tour")

            assertEquals(listOf(museum), result)
        }
    }

    @Nested
    @DisplayName("No Matches and Ordering")
    inner class NoMatchesAndOrdering {

        @Test
        fun `returns empty list when query does not match any title, vendor, or notes`() {
            val e1 = Expense(id = "1", title = "Dinner", vendor = "Pizzeria", notes = "Pizza")
            val e2 = Expense(id = "2", title = "Taxi", vendor = "Uber", notes = "Airport")
            val expenses = listOf(e1, e2)

            val result = service.search(expenses, "NonExistentKeyword")

            assertTrue(result.isEmpty())
        }

        @Test
        fun `preserves original relative order of matching expenses`() {
            val e1 = Expense(id = "1", title = "Breakfast", notes = "Coffee and croissant")
            val e2 = Expense(id = "2", title = "Lunch", vendor = "Cafe", notes = "Sandwich")
            val e3 = Expense(id = "3", title = "Dinner", notes = "Steak with coffee afterwards")
            val expenses = listOf(e1, e2, e3)

            val result = service.search(expenses, "coffee")

            assertEquals(listOf(e1, e3), result)
        }
    }

    @Nested
    @DisplayName("Matching by Sub-Expenses")
    inner class MatchSubExpenses {

        @Test
        fun `matches query against sub-expense title`() {
            val boatExpense = Expense(
                id = "1",
                title = "Bacalar Activity",
                subExpenses = listOf(
                    SubExpense(id = "sub-1", title = "Deposit Reservation"),
                    SubExpense(id = "sub-2", title = "Final Cash Payment")
                )
            )
            val taxi = Expense(id = "2", title = "Taxi")
            val expenses = listOf(boatExpense, taxi)

            val result = service.search(expenses, "deposit")

            assertEquals(listOf(boatExpense), result)
        }

        @Test
        fun `matches query against sub-expense notes`() {
            val boatExpense = Expense(
                id = "1",
                title = "Bacalar Activity",
                subExpenses = listOf(
                    SubExpense(id = "sub-1", title = "Tranche 1", notes = "Wire transfer fee"),
                    SubExpense(id = "sub-2", title = "Tranche 2")
                )
            )
            val taxi = Expense(id = "2", title = "Taxi")
            val expenses = listOf(boatExpense, taxi)

            val result = service.search(expenses, "wire transfer")

            assertEquals(listOf(boatExpense), result)
        }
    }
}
