package com.tianqi.camera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tianqi.camera.model.CollageEditState
import com.tianqi.camera.model.SlotEditState
import com.tianqi.camera.model.TemplateRepository
import com.tianqi.camera.service.DraftStore
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.ui.pages.camera.CameraPage
import com.tianqi.camera.ui.pages.collage.CollageEditorPage
import com.tianqi.camera.ui.pages.export.ExportPage
import com.tianqi.camera.ui.pages.home.HomePage
import com.tianqi.camera.ui.pages.photo.PhotoEditorPage
import com.tianqi.camera.ui.pages.settings.SettingsPage
import com.tianqi.camera.ui.pages.template.TemplatePickerPage

/** 页面路由，对应 PRD 第 4 节页面结构 */
object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val TEMPLATE_PICKER = "template_picker"
    const val COLLAGE_EDITOR = "collage_editor"
    const val PHOTO_EDITOR = "photo_editor"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
}

@Composable
fun TianqiNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    fun goExport() {
        EditSession.viewingWorkPath = null
        navController.navigate(Routes.EXPORT)
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomePage(
                onCameraClick = { navController.navigate(Routes.CAMERA) },
                onPhotosPicked = { uris ->
                    EditSession.pickedPhotos = uris
                    navController.navigate(Routes.TEMPLATE_PICKER)
                },
                onWorkClick = { file ->
                    EditSession.viewingWorkPath = file.absolutePath
                    navController.navigate(Routes.EXPORT)
                },
                onRestoreDraft = {
                    val draft = DraftStore.loadCollage(context)
                    val template = draft?.let { TemplateRepository.byId(context, it.templateId) }
                    if (draft != null && template != null) {
                        EditSession.collageTemplate = template
                        EditSession.collageState = draft.state
                        EditSession.collageLayers = draft.layers
                        navController.navigate(Routes.COLLAGE_EDITOR)
                    } else {
                        DraftStore.clear(context)
                    }
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CAMERA) {
            CameraPage(
                onBack = { navController.popBackStack() },
                onCaptured = { uri ->
                    EditSession.capturedPhoto = uri
                    EditSession.beautyState = com.tianqi.camera.model.BeautyState()
                    EditSession.beautyFaces = null
                    EditSession.photoLayers = emptyList()
                    navController.navigate(Routes.PHOTO_EDITOR)
                }
            )
        }
        composable(Routes.TEMPLATE_PICKER) {
            TemplatePickerPage(
                onBack = { navController.popBackStack() },
                onTemplateSelected = { template ->
                    EditSession.collageTemplate = template
                    EditSession.collageLayers = emptyList()
                    EditSession.collageState = CollageEditState(
                        templateId = template.id,
                        canvasRatio = template.defaultRatio,
                        slots = template.slots.mapIndexed { index, _ ->
                            SlotEditState(uri = EditSession.pickedPhotos.getOrNull(index))
                        }
                    )
                    navController.navigate(Routes.COLLAGE_EDITOR)
                }
            )
        }
        composable(Routes.COLLAGE_EDITOR) {
            CollageEditorPage(
                onBack = { navController.popBackStack() },
                onExport = { goExport() }
            )
        }
        composable(Routes.PHOTO_EDITOR) {
            PhotoEditorPage(
                onBack = { navController.popBackStack() },
                onExport = { goExport() }
            )
        }
        composable(Routes.EXPORT) { ExportPage(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsPage(onBack = { navController.popBackStack() }) }
    }
}
