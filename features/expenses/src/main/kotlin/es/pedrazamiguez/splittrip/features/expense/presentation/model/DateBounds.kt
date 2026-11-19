package es.pedrazamiguez.splittrip.features.expense.presentation.model

import java.time.LocalDate

data class DateBounds(
    val startDate: LocalDate?,
    val endDate: LocalDate?
)
