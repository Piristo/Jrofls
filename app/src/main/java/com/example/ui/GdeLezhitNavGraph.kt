package com.example.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ItemRepository
import com.example.data.PhotoStorageManager
import com.example.ui.screens.AddEditItemScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.LocationDetailScreen
import com.example.ui.screens.QrScannerScreen

object AppRoutes {
  const val HOME = "home"
  const val ADD = "add?locationId={locationId}"
  const val EDIT = "edit/{itemId}"
  const val DETAIL = "detail/{itemId}"
  const val LOCATION_DETAIL = "location/{locationId}"
  const val SCANNER = "scanner"

  fun addRoute(locationId: Long? = null): String =
    if (locationId != null) "add?locationId=$locationId" else "add"

  fun editRoute(itemId: Long): String = "edit/$itemId"

  fun detailRoute(itemId: Long): String = "detail/$itemId"

  fun locationRoute(locationId: Long): String = "location/$locationId"
}

@Composable
fun GdeLezhitNavGraph(
  repository: ItemRepository,
  photoStorageManager: PhotoStorageManager,
) {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = AppRoutes.HOME) {
    composable(AppRoutes.HOME) {
      val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
      HomeScreen(
        viewModel = homeViewModel,
        onAddItemClick = { navController.navigate(AppRoutes.addRoute()) },
        onScanQrClick = { navController.navigate(AppRoutes.SCANNER) },
        onItemClick = { itemId -> navController.navigate(AppRoutes.detailRoute(itemId)) },
        onLocationClick = { locationId ->
          navController.navigate(AppRoutes.locationRoute(locationId))
        },
      )
    }

    composable(
      route = AppRoutes.ADD,
      arguments =
        listOf(
          navArgument("locationId") {
            type = NavType.LongType
            defaultValue = -1L
          }
        ),
    ) { backStackEntry ->
      val rawLocId = backStackEntry.arguments?.getLong("locationId") ?: -1L
      val prefilledLocationId = rawLocId.takeIf { it > 0L }
      val addViewModel: AddEditItemViewModel =
        viewModel(
          factory =
            AddEditItemViewModel.Factory(
              repository = repository,
              itemId = null,
              prefilledLocationId = prefilledLocationId,
            )
        )
      AddEditItemScreen(
        viewModel = addViewModel,
        photoStorageManager = photoStorageManager,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    composable(
      route = AppRoutes.EDIT,
      arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
    ) { backStackEntry ->
      val itemId = backStackEntry.arguments?.getLong("itemId")
      val editViewModel: AddEditItemViewModel =
        viewModel(factory = AddEditItemViewModel.Factory(repository, itemId = itemId))
      AddEditItemScreen(
        viewModel = editViewModel,
        photoStorageManager = photoStorageManager,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    composable(
      route = AppRoutes.DETAIL,
      arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
    ) { backStackEntry ->
      val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
      val detailViewModel: ItemDetailViewModel =
        viewModel(factory = ItemDetailViewModel.Factory(repository, itemId = itemId))
      ItemDetailScreen(
        viewModel = detailViewModel,
        onNavigateBack = { navController.popBackStack() },
        onEditClick = { id -> navController.navigate(AppRoutes.editRoute(id)) },
        onLocationClick = { locationId ->
          navController.navigate(AppRoutes.locationRoute(locationId))
        },
      )
    }

    composable(
      route = AppRoutes.LOCATION_DETAIL,
      arguments = listOf(navArgument("locationId") { type = NavType.LongType }),
    ) { backStackEntry ->
      val locationId = backStackEntry.arguments?.getLong("locationId") ?: return@composable
      val locationViewModel: LocationDetailViewModel =
        viewModel(factory = LocationDetailViewModel.Factory(repository, locationId = locationId))
      LocationDetailScreen(
        viewModel = locationViewModel,
        onNavigateBack = { navController.popBackStack() },
        onAddHereClick = { locId -> navController.navigate(AppRoutes.addRoute(locId)) },
        onItemClick = { itemId -> navController.navigate(AppRoutes.detailRoute(itemId)) },
      )
    }

    composable(AppRoutes.SCANNER) {
      val scannerViewModel: QrScannerViewModel =
        viewModel(factory = QrScannerViewModel.Factory(repository))
      QrScannerScreen(
        viewModel = scannerViewModel,
        onNavigateBack = { navController.popBackStack() },
        onLocationResolved = { locationId ->
          navController.navigate(AppRoutes.locationRoute(locationId)) {
            popUpTo(AppRoutes.SCANNER) { inclusive = true }
          }
        },
      )
    }
  }
}
