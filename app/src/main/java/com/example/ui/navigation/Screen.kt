package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CameraScan : Screen("camera_scan")
    object CropEdit : Screen("crop_edit/{pageIndex}") {
        fun createRoute(pageIndex: Int) = "crop_edit/$pageIndex"
    }
    object MultiPageReview : Screen("multi_page_review")
    object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: Long) = "document_detail/$documentId"
    }
    object DocumentsList : Screen("documents_list")
    object Ocr : Screen("ocr/{documentId}") {
        fun createRoute(documentId: Long) = "ocr/$documentId"
    }
    object AiAssistant : Screen("ai_assistant/{documentId}") {
        fun createRoute(documentId: Long) = "ai_assistant/$documentId"
    }
    object PdfTools : Screen("pdf_tools")
    object Settings : Screen("settings")
    object Legal : Screen("legal/{type}") {
        fun createRoute(type: String) = "legal/$type"
    }
    object Onboarding : Screen("onboarding")
}
