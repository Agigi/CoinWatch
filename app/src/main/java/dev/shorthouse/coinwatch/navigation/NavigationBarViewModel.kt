package dev.shorthouse.coinwatch.navigation

import androidx.lifecycle.ViewModel

class NavigationBarViewModel : ViewModel() {
    suspend fun isReviewPromptEligible(): Boolean = false
    suspend fun recordReviewPromptAttempted() {}
}
