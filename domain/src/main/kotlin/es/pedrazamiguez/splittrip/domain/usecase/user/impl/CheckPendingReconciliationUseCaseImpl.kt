package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.CheckPendingReconciliationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val DEFAULT_RECONCILIATION_TIMEOUT_MS = 5_000L

class CheckPendingReconciliationUseCaseImpl(
    private val userRepository: UserRepository,
    private val timeoutMs: Long = DEFAULT_RECONCILIATION_TIMEOUT_MS
) : CheckPendingReconciliationUseCase {
    @Suppress("TooGenericExceptionCaught")
    override suspend operator fun invoke(email: String): Result<Boolean> {
        val pendingUserId = User.generatePendingUserId(email)
        return try {
            val users = withTimeout(timeoutMs) {
                userRepository.getUsersByIds(listOf(pendingUserId))
            }
            Result.success(users.containsKey(pendingUserId))
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
