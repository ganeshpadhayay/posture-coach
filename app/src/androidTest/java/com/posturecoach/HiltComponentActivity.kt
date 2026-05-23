package com.posturecoach

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Empty Hilt-aware host activity for `createAndroidComposeRule<HiltComponentActivity>()`.
 * Compose UI tests set their own content via `composeRule.setContent { ... }`.
 */
@AndroidEntryPoint
class HiltComponentActivity : ComponentActivity()
