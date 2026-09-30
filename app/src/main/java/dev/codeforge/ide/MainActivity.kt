package dev.codeforge.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.codeforge.ide.ui.IdeScreen
import dev.codeforge.ide.ui.IdeViewModel
import dev.codeforge.ide.workspace.Workspace
import java.io.File

class MainActivity : ComponentActivity() {

    private val workspace: Workspace by lazy {
        val dir = getExternalFilesDir(null) ?: filesDir
        Workspace(File(dir, "workspace"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val vm: IdeViewModel = viewModel(factory = IdeViewModel.factory(workspace))
                IdeScreen(vm)
            }
        }
    }
}
