package com.sunilsstartingseven.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sunilsstartingseven.app.R
import com.sunilsstartingseven.app.ui.screens.games.*
import com.sunilsstartingseven.app.ui.screens.home.HomeScreen
import com.sunilsstartingseven.app.ui.screens.team.*
import com.sunilsstartingseven.app.ui.theme.Space
import com.sunilsstartingseven.app.viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: TeamViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val topLevel = route in listOf("home", "team", "games")
    val snackbars = remember { SnackbarHostState() }
    fun navigateTop(destination: String) {
        nav.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(state.error, topLevel) {
        if (topLevel && !state.loadFailed) state.error?.let { snackbars.showSnackbar(it); viewModel.clearError() }
    }
    BackHandler(state.saving) { /* Keep the save destination alive until persistence completes. */ }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(title = { Text(screenTitle(route, topLevel), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (topLevel) Image(painterResource(R.drawable.logo), null, Modifier.padding(start = Space.Normal, end = Space.Small).size(36.dp))
                    else IconButton({ if (!state.saving) { viewModel.clearError(); nav.popBackStack() } }, enabled = !state.saving) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        bottomBar = {
            if (topLevel) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("team", "Team", Icons.Outlined.People), Triple("games", "Games", Icons.Outlined.DateRange)).forEach { (destination, label, icon) ->
                    NavigationBarItem(
                        selected = route == destination,
                        onClick = { navigateTop(destination) },
                        icon = { Icon(icon, null) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.loadFailed -> Column(Modifier.align(Alignment.Center).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
                    Text(state.error ?: "Couldn't load your team.")
                    Button(viewModel::retryLoad) { Text("Retry") }
                }
                else -> NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(state, { navigateTop("team") }, { nav.navigate("game/new") }, { nav.navigate("game/${it.id}") }) }
                    composable("team") { TeamScreen(state.players, state.saving, { nav.navigate("player/0") }, { nav.navigate("player/${it.id}") }, viewModel::setActive) }
                    composable("games") { GamesScreen(state.games, state.games.associate { it.id to state.attendanceSummary(it.id) }, { nav.navigate("game/new") }) { nav.navigate("game/${it.id}") } }
                    composable("player/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val player = state.players.find { it.id == id }
                        PlayerForm(id != 0L, player?.firstName.orEmpty(), player?.lastInitial.orEmpty(), state.saving, state.error,
                            { first, last -> viewModel.savePlayer(id.takeIf { it != 0L }, first, last) { nav.popBackStack() } },
                            { viewModel.clearError(); nav.popBackStack() })
                    }
                    composable("game/new") {
                        GameForm(
                            busy = state.saving,
                            error = state.error,
                            onSave = { round, date, notes -> viewModel.createGame(round, date, notes) { nav.popBackStack(); navigateTop("games") } },
                            onCancel = { viewModel.clearError(); nav.popBackStack() }
                        )
                    }
                    composable("game/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val game = state.game(id)
                        if (game == null) MissingGame()
                        else GameDetailScreen(
                            game = game,
                            attendance = state.attendanceFor(id),
                            missingPlayers = state.missingActivePlayersFor(id),
                            busy = state.saving,
                            onInitialize = { viewModel.initializeAttendance(id) },
                            onAttendance = { playerId, status -> viewModel.setAttendance(id, playerId, status) },
                            onAddMissing = { viewModel.addMissingActivePlayers(id) },
                            onEdit = { nav.navigate("game/$id/edit") },
                            onLineup = { nav.navigate("game/$id/lineup") }
                        )
                    }
                    composable("game/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val game = state.game(id)
                        if (game == null) MissingGame()
                        else GameForm(
                            busy = state.saving,
                            error = state.error,
                            initialRound = game.roundNumber,
                            initialDate = game.gameDate,
                            initialNotes = game.notes,
                            title = "Edit game",
                            onSave = { round, date, notes -> viewModel.updateGame(id, round, date, notes) { nav.popBackStack() } },
                            onCancel = { viewModel.clearError(); nav.popBackStack() }
                        )
                    }
                    composable("game/{id}/lineup", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                        val id = backStack.arguments?.getLong("id") ?: 0L
                        val game = state.game(id)
                        if (game == null) MissingGame()
                        else LineupScreen(
                            game = game,
                            attendance = state.attendanceFor(id),
                            assignments = state.assignmentsFor(id),
                            busy = state.saving,
                            onGenerate = { viewModel.generateLineup(id) },
                            onMarkPlayed = { viewModel.markPlayed(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingGame() {
    Box(Modifier.fillMaxSize().padding(Space.Large), contentAlignment = Alignment.Center) {
        Text("This game is no longer available.", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun screenTitle(route: String, topLevel: Boolean): String = when {
    topLevel -> "Sunil’s Starting Seven"
    route == "game/new" -> "New game"
    route.contains("/lineup") -> "Lineup"
    route.contains("/edit") -> "Edit game"
    route.startsWith("game/") -> "Game setup"
    else -> "Player"
}
