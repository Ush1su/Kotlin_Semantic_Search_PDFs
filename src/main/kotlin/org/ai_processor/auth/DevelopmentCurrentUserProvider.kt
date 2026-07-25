package org.ai_processor.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DevelopmentCurrentUserProvider(
    @Value("\${app.auth.dev-user-id}")
    private val devUserId: UUID
) : CurrentUserProvider {

    override fun currentUserId(): UUID = devUserId
}