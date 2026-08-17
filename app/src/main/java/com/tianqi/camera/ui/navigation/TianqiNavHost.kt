package com.tianqi.camera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tianqi.camera.ui.pages.camera.CameraPage
import com.tianqi.camera.ui.pages.collage.CollageEditorPage
import com.tianqi.camera.ui.pages.export.ExportPage
import com.tianqi.camera.ui.pages.home.HomePage
import com.tianqi.camera.ui.pages.photo.PhotoEditorPage
import com.tianqi.camera.ui.pages.template.TemplatePickerPage

/** 页面路由，对应 PRD 第 4 节页面结构 */
object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val TEMPLATE_PICKER = "template_picker"
    const val COLLAGE_EDITOR = "collage_editor"
    const val PHOTO_EDITOR = "photo_editor"
    const val EXPORT = "export"
}

@Composable
fun TianqiNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomePage(
                onCameraClick = { navController.navigate(Routes.CAMERA) },
                onCollageClick = { navController.navigate(Routes.TEMPLATE_PICKER) }
            )
        }
        composable(Routes.CAMERA) { CameraPage(onBack = { navController.popBackStack() }) }
        composable(Routes.TEMPLATE_PICKER) { TemplatePickerPage(onBack = { navController.popBackStack() }) }
        composable(Routes.COLLAGE_EDITOR) { CollageEditorPage(onBack = { navController.popBackStack() }) }
        composable(Routes.PHOTO_EDITOR) { PhotoEditorPage(onBack = { navController.popBackStack() }) }
        composable(Routes.EXPORT) { ExportPage(onBack = { navController.popBackStack() }) }
    }
}
