package org.ai_processor.auth

import java.util.UUID

interface CurrentUserProvider {
    fun currentUserId(): UUID
}