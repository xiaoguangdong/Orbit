package com.xiaoguangdong.orbit

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.xiaoguangdong.orbit.ui.OrbitApp
import com.xiaoguangdong.orbit.ui.OrbitViewModel
import com.xiaoguangdong.orbit.ui.OrbitViewModelFactory
import com.xiaoguangdong.orbit.ui.theme.OrbitTheme

class MainActivity : ComponentActivity() {
    private val viewModel: OrbitViewModel by viewModels {
        OrbitViewModelFactory((application as OrbitApplication).appContainer)
    }

    private val notificationsPermissionRequester = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationsIfNeeded()
        setContent {
            OrbitTheme {
                OrbitApp(
                    viewModel = viewModel,
                    initialHabitId = intent?.getLongExtra(EXTRA_HABIT_ID, -1L)?.takeIf { it > 0L },
                )
            }
        }
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habitId"
    }
}
