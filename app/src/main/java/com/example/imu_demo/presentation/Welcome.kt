import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.imu_demo.R
import com.example.imu_demo.navigations.BottomNavItem

@Composable
fun WelcomeScreen(navController: NavHostController) {
    // Coil 라이브러리를 사용하여 GIF 이미지 로드
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
    Column(){
        Image(
            painter = rememberAsyncImagePainter(R.drawable.welcome, imageLoader),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(BottomNavItem.Scan.screenRoute) // 다음 화면으로 이동
                },
            contentScale = ContentScale.Crop
        )
    }
}