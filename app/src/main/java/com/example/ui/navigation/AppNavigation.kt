package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.ScanSessionViewModel
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.CropEditScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.DocumentsListScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.MultiPageReviewScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: ScanSessionViewModel,
    modifier: Modifier = Modifier
) {
    val startDestination = if (viewModel.prefs.isOnboardingCompleted) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = viewModel,
                onFinishOnboarding = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCamera = { navController.navigate(Screen.CameraScan.route) },
                onNavigateToTools = { navController.navigate(Screen.PdfTools.route) },
                onNavigateToDocuments = { navController.navigate(Screen.DocumentsList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onOpenDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) },
                onOcrDocument = { id -> navController.navigate(Screen.Ocr.createRoute(id)) },
                onAiDocument = { id -> navController.navigate(Screen.AiAssistant.createRoute(id)) }
            )
        }

        composable(Screen.CameraScan.route) {
            CameraScanScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCrop = { pageIndex ->
                    navController.navigate(Screen.CropEdit.createRoute(pageIndex))
                },
                onNavigateToReview = {
                    navController.navigate(Screen.MultiPageReview.route)
                }
            )
        }

        composable(
            route = Screen.CropEdit.route,
            arguments = listOf(navArgument("pageIndex") { type = NavType.IntType })
        ) { backStackEntry ->
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
            CropEditScreen(
                pageIndex = pageIndex,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = {
                    navController.navigate(Screen.MultiPageReview.route) {
                        popUpTo(Screen.CameraScan.route)
                    }
                }
            )
        }

        composable(Screen.MultiPageReview.route) {
            MultiPageReviewScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddMorePages = { navController.navigate(Screen.CameraScan.route) },
                onEditPage = { pageIndex ->
                    navController.navigate(Screen.CropEdit.createRoute(pageIndex))
                },
                onDocumentSaved = { docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.DocumentDetail.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            DocumentDetailScreen(
                documentId = docId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOcr = { id -> navController.navigate(Screen.Ocr.createRoute(id)) },
                onNavigateToAi = { id -> navController.navigate(Screen.AiAssistant.createRoute(id)) }
            )
        }

        composable(Screen.DocumentsList.route) {
            DocumentsListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToTools = { navController.navigate(Screen.PdfTools.route) },
                onOpenDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) },
                onOcrDocument = { id -> navController.navigate(Screen.Ocr.createRoute(id)) },
                onAiDocument = { id -> navController.navigate(Screen.AiAssistant.createRoute(id)) }
            )
        }

        composable(
            route = Screen.Ocr.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            OcrScreen(
                documentId = docId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAi = { id -> navController.navigate(Screen.AiAssistant.createRoute(id)) }
            )
        }

        composable(
            route = Screen.AiAssistant.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            AiAssistantScreen(
                documentId = docId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfTools.route) {
            PdfToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToFiles = { navController.navigate(Screen.DocumentsList.route) },
                onAiDocument = { id -> navController.navigate(Screen.AiAssistant.createRoute(id)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLegal = { type -> navController.navigate(Screen.Legal.createRoute(type)) }
            )
        }

        composable(
            route = Screen.Legal.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "privacy"
            LegalScreen(
                type = type,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
