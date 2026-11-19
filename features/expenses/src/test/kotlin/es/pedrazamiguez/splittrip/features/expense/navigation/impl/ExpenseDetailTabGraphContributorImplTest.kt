package es.pedrazamiguez.splittrip.features.expense.navigation.impl

import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.features.expense.navigation.expenseDetailGraph
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ExpenseDetailTabGraphContributorImpl")
class ExpenseDetailTabGraphContributorImplTest {

    @Test
    fun `contributeGraph delegates to expenseDetailGraph`() {
        mockkStatic("es.pedrazamiguez.splittrip.features.expense.navigation.ExpensesNavigationKt")
        try {
            val builder = mockk<NavGraphBuilder>(relaxed = true)
            every { builder.expenseDetailGraph() } just Runs

            val contributor = ExpenseDetailTabGraphContributorImpl()
            contributor.contributeGraph(builder)

            verify(exactly = 1) { builder.expenseDetailGraph() }
        } finally {
            unmockkStatic("es.pedrazamiguez.splittrip.features.expense.navigation.ExpensesNavigationKt")
        }
    }
}
