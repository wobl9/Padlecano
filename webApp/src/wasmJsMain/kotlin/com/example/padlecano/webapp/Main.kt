import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.padlecano.composeapp.PadlecanoAppPlaceholder

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        PadlecanoAppPlaceholder()
    }
}
