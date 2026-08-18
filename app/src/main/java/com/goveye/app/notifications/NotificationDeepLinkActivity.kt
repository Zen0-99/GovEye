package com.goveye.app.notifications

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.navigation.DivisionDetailRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Transparent activity that receives notification taps and routes the user
 * to the division detail screen via the [DeepLinkNavigator].
 *
 * Nav3 doesn't support standard deep-link/PendingIntent integration directly,
 * so this activity bridges the gap: it reads the division ID + house from
 * intent extras, emits a [DivisionDetailRoute] to the DeepLinkNavigator
 * (which GovEyeApp observes), then finishes itself.
 */
@AndroidEntryPoint
class NotificationDeepLinkActivity : ComponentActivity() {

    @Inject lateinit var deepLinkNavigator: DeepLinkNavigator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val divisionId = intent.getIntExtra(NotificationHelper.EXTRA_DIVISION_ID, -1)
        val house = intent.getIntExtra(NotificationHelper.EXTRA_DIVISION_HOUSE, 1)

        if (divisionId > 0) {
            scope.launch {
                deepLinkNavigator.emit(DivisionDetailRoute(divisionId, house))
            }
        }

        // Finish immediately — the GovEyeApp will handle the navigation
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        fun createIntent(context: Context, divisionId: Int, house: Int) =
            android.content.Intent(context, NotificationDeepLinkActivity::class.java).apply {
                putExtra(NotificationHelper.EXTRA_DIVISION_ID, divisionId)
                putExtra(NotificationHelper.EXTRA_DIVISION_HOUSE, house)
            }
    }
}
