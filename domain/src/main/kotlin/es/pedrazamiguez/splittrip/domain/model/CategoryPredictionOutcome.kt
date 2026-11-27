package es.pedrazamiguez.splittrip.domain.model

sealed interface CategoryPredictionOutcome {
    data class Matched(val suggestion: CategorySuggestion) : CategoryPredictionOutcome
    data object NoMatch : CategoryPredictionOutcome
    data object AiUnavailable : CategoryPredictionOutcome
    data class AiTimeout(val timeoutMs: Long) : CategoryPredictionOutcome
    data class AiFailure(val message: String?) : CategoryPredictionOutcome
}
