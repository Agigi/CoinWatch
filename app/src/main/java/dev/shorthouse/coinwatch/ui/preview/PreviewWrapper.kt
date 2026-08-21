package dev.shorthouse.coinwatch.ui.preview

import androidx.compose.ui.tooling.preview.Preview
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Preview
annotation class PreviewWrapper(
    val wrapper: KClass<*> = AppPreviewWrapper::class
)
