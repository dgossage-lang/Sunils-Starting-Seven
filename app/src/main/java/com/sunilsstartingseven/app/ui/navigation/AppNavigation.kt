package com.sunilsstartingseven.app.ui.navigation

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sunilsstartingseven.app.R
import com.sunilsstartingseven.app.ui.screens.games.*
import com.sunilsstartingseven.app.ui.screens.fairness.FairnessScreen
import com.sunilsstartingseven.app.ui.screens.fairness.PlayerHistoryScreen
import com.sunilsstartingseven.app.ui.screens.home.HomeScreen
import com.sunilsstartingseven.app.ui.screens.settings.SettingsScreen
import com.sunilsstartingseven.app.ui.screens.team.*
import com.sunilsstartingseven.app.ui.theme.Space
import com.sunilsstartingseven.app.viewmodel.TeamViewModel
import java.io.File

private sealed class PhotoTarget {
    data class Player(val id: Long) : PhotoTarget()
    data class Game(val id: Long) : PhotoTarget()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: TeamViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val context = LocalContext.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val topLevel = route in listOf("home", "team", "games", "fairness")
    val snackbars = remember { SnackbarHostState() }
    var photoTarget by remember { mutableStateOf<PhotoTarget?>(null) }
    var cameraTarget by remember { mutableStateOf<PhotoTarget?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun applyPhoto(target: PhotoTarget, uri: Uri) {
        when (target) {
            is PhotoTarget.Player -> viewModel.setPlayerPhoto(target.id, context.contentResolver, uri)
            is PhotoTarget.Game -> viewModel.setGamePhoto(target.id, context.contentResolver, uri)
        }
    }
    fun removePhoto(target: PhotoTarget) {
        when (target) {
            is PhotoTarget.Player -> viewModel.removePlayerPhoto(target.id)
            is PhotoTarget.Game -> viewModel.removeGamePhoto(target.id)
        }
    }
    fun viewPhoto(target: PhotoTarget) {
        when (target) {
            is PhotoTarget.Player -> nav.navigate("photo/player/${target.id}")
            is PhotoTarget.Game -> nav.navigate("photo/game/${target.id}")
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportBackup(context.contentResolver, uri)
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readRestorePreview(context.contentResolver, uri)
    }
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = photoTarget
        if (uri != null && target != null) applyPhoto(target, uri)
        photoTarget = null
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val target = cameraTarget
        val uri = cameraUri
        if (success && target != null && uri != null) applyPhoto(target, uri)
        cameraTarget = null
        cameraUri = null
    }

    fun launchCamera(target: PhotoTarget) {
        val ownerDir = if (target is PhotoTarget.Player) "players" else "games"
        val file = File(context.cacheDir, "media-temp/$ownerDir/capture_${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraTarget = target
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun navigateTop(destination: String) {
        nav.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(state.error, state.backupMessage) {
        if (!state.loadFailed) state.error?.let { snackbars.showSnackbar(it); viewModel.clearError() }
        state.backupMessage?.let { snackbars.showSnackbar(it); viewModel.clearBackupMessage() }
    }
    BackHandler(state.saving) { }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(title = { Text(screenTitle(route, topLevel), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (topLevel) Image(painterResource(R.drawable.logo), null, Modifier.padding(start = Space.Normal, end = Space.Small).size(36.dp))
                    else IconButton({ if (!state.saving) { viewModel.clearError(); nav.popBackStack() } }, enabled = !state.saving) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
                actions = { if (route == "home") IconButton({ nav.navigate("settings") }, enabled = !state.saving) { Icon(Icons.Outlined.Settings, "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        bottomBar = {
            if (topLevel) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("team", "Team", Icons.Outlined.People), Triple("games", "Games", Icons.Outlined.DateRange), Triple("fairness", "Fairness", Icons.Outlined.Star)).forEach { (destination, label, icon) ->
                    NavigationBarItem(selected = route == destination, onClick = { navigateTop(destination) }, icon = { Icon(icon, null) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.loadFailed -> Column(Modifier.align(Alignment.Center).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) { Text(state.error ?: "Couldn't load your team."); Button(viewModel::retryLoad) { Text("Retry") } }
                else -> NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(state, { navigateTop("team") }, { nav.navigate("game/new") }, { nav.navigate("game/${it.id}") }) }
                    composable("team") { TeamScreen(state.players, state.saving, { nav.navigate("player/new") }, { nav.navigate("player/${it.id}/history") }, { nav.navigate("player/${it.id}/edit") }, viewModel::setActive) { photoTarget = PhotoTarget.Player(it.id) } }
                    composable("games") { GamesScreen(state.games, state.games.associate { it.id to state.attendanceSummary(it.id) }, { nav.navigate("game/new") }) { nav.navigate("game/${it.id}") } }
                    composable("fairness") { FairnessScreen(state.fairness) { nav.navigate("player/$it/history") } }
                    composable("settings") { SettingsScreen(state, { createBackupLauncher.launch(viewModel.backupFileName()) }, { restoreBackupLauncher.launch(arrayOf("application/octet-stream", "application/zip", "application/json", "text/*")) }, { viewModel.restoreBackup { nav.popBackStack("home", inclusive = false) } }, viewModel::clearRestorePreview) }
                    composable("player/new") { PlayerForm(false, "", "", state.saving, state.error, { first, last -> viewModel.savePlayer(null, first, last) { nav.popBackStack() } }, { viewModel.clearError(); nav.popBackStack() }) }
                    composable("player/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val player = state.players.find { it.id == id }
                        PlayerForm(true, player?.firstName.orEmpty(), player?.lastInitial.orEmpty(), state.saving, state.error, { first, last -> viewModel.savePlayer(id, first, last) { nav.popBackStack() } }, { viewModel.clearError(); nav.popBackStack() }, player) { photoTarget = PhotoTarget.Player(id) }
                    }
                    composable("player/{id}/history", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        PlayerHistoryScreen(state.fairness.player(id)) { gameId -> nav.navigate("game/$gameId/lineup") }
                    }
                    composable("game/new") { GameForm(state.saving, state.error, onSave = { round, date, notes -> viewModel.createGame(round, date, notes) { nav.popBackStack(); navigateTop("games") } }, onCancel = { viewModel.clearError(); nav.popBackStack() }) }
                    composable("game/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val game = state.game(id)
                        if (game == null) MissingGame() else GameDetailScreen(game, state.attendanceFor(id), state.missingActivePlayersFor(id), state.saving, { viewModel.initializeAttendance(id) }, { playerId, status -> viewModel.setAttendance(id, playerId, status) }, { viewModel.addMissingActivePlayers(id) }, { nav.navigate("game/$id/edit") }, { nav.navigate("game/$id/lineup") }, { photoTarget = PhotoTarget.Game(id) }, { nav.navigate("photo/game/$id") })
                    }
                    composable("game/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val game = state.game(id)
                        if (game == null) MissingGame() else GameForm(state.saving, state.error, game.roundNumber, game.gameDate, game.notes, "Edit game", { round, date, notes -> viewModel.updateGame(id, round, date, notes) { nav.popBackStack() } }, { viewModel.clearError(); nav.popBackStack() })
                    }
                    composable("game/{id}/lineup", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val game = state.game(id)
                        if (game == null) MissingGame() else LineupScreen(game, state.attendanceFor(id), state.assignmentsFor(id), state.saving, { viewModel.generateLineup(id) }, { viewModel.markPlayed(id) }, { pending, onSuccess -> viewModel.saveManualLineup(id, pending, onSuccess) })
                    }
                    composable("photo/player/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val player = state.players.find { it.id == backStack.arguments?.getLong("id") }
                        PhotoViewer(player?.photoPath, player?.displayName ?: "Player photo")
                    }
                    composable("photo/game/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val game = state.game(backStack.arguments?.getLong("id") ?: 0L)
                        PhotoViewer(game?.photoPath, game?.let { "Round ${it.roundNumber} photo" } ?: "Game photo")
                    }
                }
            }
        }
    }
    photoTarget?.let { target ->
        val hasPhoto = when (target) { is PhotoTarget.Player -> state.players.find { it.id == target.id }?.photoPath != null; is PhotoTarget.Game -> state.game(target.id)?.photoPath != null }
        PhotoOptionsDialog(
            hasPhoto = hasPhoto,
            onDismiss = { photoTarget = null },
            onView = { viewPhoto(target); photoTarget = null },
            onTake = { launchCamera(target); photoTarget = null },
            onChoose = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemove = { removePhoto(target); photoTarget = null }
        )
    }
}

@Composable
private fun PhotoOptionsDialog(hasPhoto: Boolean, onDismiss: () -> Unit, onView: () -> Unit, onTake: () -> Unit, onChoose: () -> Unit, onRemove: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPhoto) "Photo options" else "Add photo") },
        text = { Column { if (hasPhoto) TextButton(onView) { Text("View Photo") }; TextButton(onTake) { Text(if (hasPhoto) "Take New Photo" else "Take Photo") }; TextButton(onChoose) { Text(if (hasPhoto) "Choose New Photo" else "Choose Photo") }; if (hasPhoto) TextButton(onRemove) { Text("Remove Photo") } } },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PhotoViewer(photoPath: String?, description: String) {
    val context = LocalContext.current
    val bitmap = remember(photoPath) { photoPath?.let { runCatching { BitmapFactory.decodeFile(File(context.filesDir, it).absolutePath)?.asImageBitmap() }.getOrNull() } }
    Box(Modifier.fillMaxSize().padding(Space.Large), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, description, Modifier.fillMaxWidth().fillMaxHeight(), contentScale = ContentScale.Fit) else Text("Photo not available")
    }
}

@Composable
private fun MissingGame() { Box(Modifier.fillMaxSize().padding(Space.Large), contentAlignment = Alignment.Center) { Text("This game is no longer available.", style = MaterialTheme.typography.bodyLarge) } }

private fun screenTitle(route: String, topLevel: Boolean): String = when {
    topLevel -> "Sunil’s Starting Seven"
    route == "game/new" -> "New game"
    route == "player/new" -> "New player"
    route == "settings" -> "Settings"
    route.startsWith("photo/") -> "Photo"
    route.contains("/history") -> "Player history"
    route.contains("/edit") && route.startsWith("player/") -> "Edit player"
    route.contains("/lineup") -> "Lineup"
    route.contains("/edit") -> "Edit game"
    route.startsWith("game/") -> "Game setup"
    else -> "Player"
}
