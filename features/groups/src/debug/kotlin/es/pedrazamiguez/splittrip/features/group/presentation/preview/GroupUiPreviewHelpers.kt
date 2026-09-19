package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.impl.GroupUiMapperImpl
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun GroupUiPreviewHelper(
    domainGroup: Group = GROUP_DOMAIN_1,
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (GroupUiModel) -> Unit
) {
    MappedPreview(
        domain = domainGroup,
        mapper = { localeProvider, resourceProvider ->
            GroupUiMapperImpl(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.toGroupUiModel(domain, memberProfiles)
        },
        content = content
    )
}

@Composable
fun GroupsScreenPreviewHelper(
    domainGroups: List<Group> = PREVIEW_GROUPS,
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (ImmutableList<GroupUiModel>) -> Unit
) {
    MappedPreview(
        domain = domainGroups,
        mapper = { localeProvider, resourceProvider ->
            GroupUiMapperImpl(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.toGroupUiModelList(domain, memberProfiles)
        },
        content = content
    )
}
