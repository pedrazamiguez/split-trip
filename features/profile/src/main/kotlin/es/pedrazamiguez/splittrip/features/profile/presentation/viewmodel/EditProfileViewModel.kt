package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.CropRect
import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.ProfileImageStorageService
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.EditProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.EditProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.EditProfileUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class EditProfileViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val userValidationService: UserValidationService,
    private val profileImageStorageService: ProfileImageStorageService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _actions = Channel<EditProfileUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    private var userId: String? = null

    init {
        loadProfile()
    }

    fun onEvent(event: EditProfileUiEvent) {
        when (event) {
            is EditProfileUiEvent.OnDisplayNameChanged -> {
                _uiState.update { it.copy(displayName = event.name, displayNameError = null) }
            }
            is EditProfileUiEvent.OnBioChanged -> {
                _uiState.update { it.copy(bio = event.bio, bioError = null) }
            }
            is EditProfileUiEvent.OnAvatarPicked -> {
                _uiState.update {
                    it.copy(
                        showCropOverlay = true,
                        cropSourceUri = event.uri,
                        localAvatarMimeType = event.mimeType
                    )
                }
            }
            is EditProfileUiEvent.OnCropConfirmed -> {
                performCropAndCompress(event.cropRect)
            }
            is EditProfileUiEvent.OnCropCancelled -> {
                _uiState.update { it.copy(showCropOverlay = false, cropSourceUri = null) }
            }
            is EditProfileUiEvent.OnAvatarRemoved -> {
                _uiState.update { it.copy(avatarUrl = null, localAvatarPath = null) }
            }
            is EditProfileUiEvent.OnSaveClicked -> {
                saveProfile()
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = getCurrentUserProfileUseCase()
                if (user != null) {
                    userId = user.userId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = user.displayName ?: "",
                            bio = user.bio ?: "",
                            avatarUrl = user.profileImagePath
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _actions.send(
                        EditProfileUiAction.ShowNotification(
                            UiText.StringResource(R.string.profile_error_loading)
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load profile for editing")
                _uiState.update { it.copy(isLoading = false) }
                _actions.send(
                    EditProfileUiAction.ShowNotification(
                        UiText.StringResource(R.string.profile_error_loading)
                    )
                )
            }
        }
    }

    private fun performCropAndCompress(cropRect: CropRect) {
        val currentUserId = userId ?: return
        val sourceUri = _uiState.value.cropSourceUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val compressedPath = profileImageStorageService.saveAndCompressAvatar(
                    userId = currentUserId,
                    sourceUri = sourceUri,
                    cropRect = cropRect
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showCropOverlay = false,
                        cropSourceUri = null,
                        localAvatarPath = compressedPath
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to crop and compress avatar image")
                _uiState.update { it.copy(isSaving = false, showCropOverlay = false, cropSourceUri = null) }
                _actions.send(
                    EditProfileUiAction.ShowNotification(
                        UiText.DynamicString("Failed to process image: ${e.localizedMessage}")
                    )
                )
            }
        }
    }

    private fun saveProfile() {
        val currentUserId = userId ?: return
        val displayName = _uiState.value.displayName
        val bio = _uiState.value.bio.takeIf { it.isNotBlank() }

        if (!validateInputs(displayName, bio)) return

        performSave(currentUserId, displayName, bio)
    }

    private fun validateInputs(displayName: String, bio: String?): Boolean {
        val nameValidation = userValidationService.validateDisplayName(displayName)
        val bioValidation = userValidationService.validateBio(bio)

        val isNameInvalid = nameValidation is ValidationResult.Invalid
        val isBioInvalid = bioValidation is ValidationResult.Invalid

        if (isNameInvalid || isBioInvalid) {
            val displayNameError = if (nameValidation is ValidationResult.Invalid) {
                if (nameValidation.message.contains("empty")) {
                    UiText.StringResource(R.string.edit_profile_error_name_empty)
                } else {
                    UiText.StringResource(R.string.edit_profile_error_name_length)
                }
            } else {
                null
            }

            val bioError = if (isBioInvalid) {
                UiText.StringResource(R.string.edit_profile_error_bio_length)
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    displayNameError = displayNameError,
                    bioError = bioError
                )
            }
            return false
        }
        return true
    }

    private fun performSave(currentUserId: String, displayName: String, bio: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val avatarPath = _uiState.value.localAvatarPath ?: _uiState.value.avatarUrl
                val result = updateUserProfileUseCase(
                    userId = currentUserId,
                    displayName = displayName,
                    bio = bio,
                    localAvatarUri = avatarPath
                )
                if (result.isSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    _actions.send(
                        EditProfileUiAction.ShowNotification(
                            UiText.StringResource(R.string.edit_profile_success_saved)
                        )
                    )
                    _actions.send(EditProfileUiAction.NavigateBack)
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "Failed to save profile changes")
                    _uiState.update { it.copy(isSaving = false) }
                    _actions.send(
                        EditProfileUiAction.ShowNotification(
                            UiText.DynamicString(error?.localizedMessage ?: "Failed to save profile changes")
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while saving profile changes")
                _uiState.update { it.copy(isSaving = false) }
                _actions.send(
                    EditProfileUiAction.ShowNotification(
                        UiText.DynamicString(e.localizedMessage ?: "Failed to save profile changes")
                    )
                )
            }
        }
    }
}
