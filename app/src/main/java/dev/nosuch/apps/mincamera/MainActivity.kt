package dev.nosuch.apps.mincamera

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.AmbientLifecycleOwner
import androidx.compose.ui.platform.AmbientView
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dev.nosuch.apps.mincamera.ui.theme.MinCameraTheme

class MainActivity : AppCompatActivity() {

    private val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.e("DEBUG", "${it.key} = ${it.value}")
                }
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMultiplePermissions.launch(
                arrayOf(
                        Manifest.permission.CAMERA
                )
        )

        setContent {
            val rearCamera = remember { mutableStateOf(true) }

            MinCameraTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                    .fillMaxSize()
                                    .padding(64.dp)) {

                        CameraPreviewer(rearCamera.value)

                        Switch(checked = rearCamera.value, onCheckedChange = {
                            rearCamera.value = it
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewer(useRearCamera: Boolean) {

    val context: Context = AmbientContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val lifecycleOwner = AmbientLifecycleOwner.current
    val viewGroup = AmbientView.current as ViewGroup

    Box(Modifier
            .preferredSize(100.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AndroidView(
                viewBlock = {
                    LayoutInflater.from(it).inflate(
                            R.layout.camera_host, viewGroup,
                            false
                    )
                },
                update = { view ->
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        bindCameraUseCasesToLifecycle(
                                lifecycleOwner,
                                view as PreviewView,
                                cameraProvider,
                                useRearCamera
                        )
                    }, ContextCompat.getMainExecutor(context))
                }, modifier = Modifier.fillMaxSize()
        )
    }

}

fun bindCameraUseCasesToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraProvider: ProcessCameraProvider,
        useRearCamera: Boolean
) {
    // Unbind if already bound, to avoid multiple bindings crash
    cameraProvider.unbindAll()

    // Select the front or back camera
    val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(if (useRearCamera) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT)
            .build()

    // Create the preview use case
    val preview: androidx.camera.core.Preview = androidx.camera.core.Preview.Builder().build()
    preview.setSurfaceProvider(previewView.surfaceProvider)

    // Bind use case to the lifecycle
    cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview
    )
}