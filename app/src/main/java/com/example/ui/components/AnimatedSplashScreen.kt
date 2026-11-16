package com.example.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.DzCrimsonRed
import com.example.ui.theme.DzNearBlack
import com.example.ui.theme.DzRoyalBlue
import com.example.ui.theme.DzWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    // Check 6-byte GIF header cheaply before calling ImageDecoder so static PNGs are decoded only once
    val animatedDrawable: Drawable? = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val isGif = context.resources.openRawResource(R.drawable.dz_studio_logo).use { stream ->
                    val header = ByteArray(6)
                    val read = stream.read(header)
                    read == 6 && (
                        String(header, Charsets.US_ASCII) == "GIF87a" ||
                            String(header, Charsets.US_ASCII) == "GIF89a"
                        )
                }
                if (!isGif) {
                    null
                } else {
                    val source = ImageDecoder.createSource(context.resources, R.drawable.dz_studio_logo)
                    val decoded = ImageDecoder.decodeDrawable(source)
                    if (decoded is AnimatedImageDrawable) {
                        decoded.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        decoded.start()
                        decoded
                    } else {
                        null
                    }
                }
            }.getOrNull()
        } else {
            null
        }
    }

    val logoAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.90f) }
    val progressFraction = remember { Animatable(if (reducedMotion) 1f else 0f) }
    var showSubtitle by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(1100L)
            onFinished()
        } else {
            val animJob1 = launch {
                logoAlpha.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
            }
            val animJob2 = launch {
                logoScale.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
            }
            delay(250L)
            showSubtitle = true
            val animJob3 = launch {
                progressFraction.animateTo(1f, tween(durationMillis = 1900, easing = FastOutSlowInEasing))
            }
            animJob1.join()
            animJob2.join()
            animJob3.join()
            delay(220L)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DzWhite)
            .clickable { onFinished() }
            .testTag("animated_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
                contentAlignment = Alignment.Center
            ) {
                if (animatedDrawable != null) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setImageDrawable(animatedDrawable)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.dz_studio_logo),
                        contentDescription = stringResource(id = R.string.splash_logo_desc),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(420)),
                exit = fadeOut(tween(200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = DzNearBlack
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(DzRoyalBlue.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.value)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(DzRoyalBlue, DzCrimsonRed)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
