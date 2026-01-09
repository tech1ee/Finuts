# State Transition Animations in Jetpack Compose
**Research Date:** January 9, 2026
**Focus:** Finance App Patterns & Compose Multiplatform Compatibility (1.7.0+)

---

## 1. AnimatedContent vs Crossfade for Loading/Success/Error States

### Quick Comparison

| Feature | Crossfade | AnimatedContent |
|---------|-----------|-----------------|
| **Use Case** | Simple opacity transitions | Complex state changes with custom animations |
| **Customization** | Limited (fade only) | Full control: enter/exit/size transforms |
| **Animation Spec** | Default fade only | Custom `transitionSpec` |
| **Size Handling** | Doesn't animate size changes | `SizeTransform` for layout transitions |
| **Performance** | Lighter weight | Slightly heavier, still optimal |
| **Ideal For** | Loading → Content | Loading → Success → Error → Retry |

### Crossfade: Simple & Subtle

**When to use:** Loading state where you just need visual feedback that content is switching. Works well for simple, quick transitions.

```kotlin
// Simple loading state with Crossfade
Crossfade(
    targetState = uiState,
    label = "LoadingCrossfade"
) { state ->
    when (state) {
        UiState.Loading -> CircularProgressIndicator()
        UiState.Loaded -> ContentView()
        UiState.Error -> ErrorMessage()
    }
}
```

**Pros:**
- Minimal recomposition overhead
- Clean, straightforward API
- Perfect for quick transitions (< 300ms)

**Cons:**
- No size animation (abrupt layout shifts)
- Limited customization
- Not ideal for complex state machines

### AnimatedContent: Powerful & Flexible

**When to use:** Finance apps handling multiple states (loading, success, error, empty, retry) with different layouts and desired enter/exit animations.

```kotlin
// Finance app state transition with AnimatedContent
AnimatedContent(
    targetState = transactionState,
    transitionSpec = {
        when {
            // Loading → Success: slide up + fade
            initialState is TransactionState.Loading &&
            targetState is TransactionState.Success -> {
                slideInVertically { height -> height } + fadeIn() togetherWith
                slideOutVertically { height -> -height } + fadeOut()
            }
            // Success → Error: shake + fade
            initialState is TransactionState.Success &&
            targetState is TransactionState.Error -> {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                slideOutHorizontally { width -> width } + fadeOut()
            }
            // Default: cross fade
            else -> fadeIn() togetherWith fadeOut()
        }
    },
    modifier = Modifier.fillMaxWidth(),
    label = "TransactionStateAnimation"
) { state ->
    when (state) {
        is TransactionState.Loading -> LoadingState()
        is TransactionState.Success -> SuccessState(state.transaction)
        is TransactionState.Error -> ErrorState(state.message)
        is TransactionState.Empty -> EmptyState()
    }
}
```

**Pros:**
- Contextual animations (different transitions for different state changes)
- `SizeTransform` for smooth layout changes
- Better UX for complex workflows
- Proper composition lifecycle management

**Cons:**
- More boilerplate
- Heavier than Crossfade (but negligible in practice)

### Finance App Pattern: Smart State Transitions

```kotlin
sealed class FinanceScreenState {
    object Loading : FinanceScreenState()
    data class Success(val accounts: List<Account>) : FinanceScreenState()
    data class Error(val message: String, val retryable: Boolean) : FinanceScreenState()
    object Empty : FinanceScreenState()
}

@Composable
fun FinanceScreenContent(
    state: FinanceScreenState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            // Smart transitions based on state pairs
            when {
                initialState is FinanceScreenState.Loading -> {
                    fadeIn(animationSpec = tween(300)) +
                    slideInVertically(animationSpec = tween(300)) { 50 } togetherWith
                    fadeOut(animationSpec = tween(150)) +
                    slideOutVertically { -50 }
                }
                targetState is FinanceScreenState.Error -> {
                    slideInHorizontally(animationSpec = tween(400)) { -it } +
                    fadeIn(animationSpec = tween(400)) togetherWith
                    slideOutHorizontally { it } + fadeOut()
                }
                else -> {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                }
            }
        },
        modifier = modifier,
        label = "FinanceScreenState"
    ) { screenState ->
        when (screenState) {
            FinanceScreenState.Loading -> SkeletonLoadingScreen()
            is FinanceScreenState.Success -> AccountsList(screenState.accounts)
            is FinanceScreenState.Error -> ErrorScreen(
                message = screenState.message,
                retryable = screenState.retryable,
                onRetry = onRetry
            )
            FinanceScreenState.Empty -> EmptyStatePrompt()
        }
    }
}
```

**Key Takeaways:**
- Use `AnimatedContent` for finance apps with multiple states
- Contextual animations improve UX (different animations for different transitions)
- Size transforms handle layout changes smoothly
- Keep total animation time 250-400ms for finance (feels responsive, not slow)

---

## 2. Skeleton Loading Screens: Best Practices

### Why Skeleton Screens for Finance Apps?

Finance apps benefit massively from skeleton screens because:
- **Perceived performance:** Users see structure immediately, even if data is loading
- **Trust:** Shows you're working on their data (better than blank screen)
- **Guidance:** Users know where to focus (header → balance → transactions)

### Core Implementation Pattern

```kotlin
// Reusable shimmer modifier
fun Modifier.shimmerBackground(
    shape: Shape = RectangleShape,
    shimmerColor: Color = Color(0xFFE0E0E0)
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val translateAnimation by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            shimmerColor,
            shimmerColor.copy(alpha = 0.5f),
            shimmerColor
        ),
        start = Offset(translateAnimation, 0f),
        end = Offset(translateAnimation + 400f, 0f)
    )

    this
        .clip(shape)
        .background(Color.LightGray.copy(alpha = 0.3f))
        .background(brush)
}

// Skeleton placeholder components
@Composable
fun SkeletonAccountCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Account name skeleton
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(16.dp)
                    .shimmerBackground(shape = RoundedCornerShape(8.dp))
            )

            // Balance skeleton
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp)
                    .shimmerBackground(shape = RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun SkeletonTransactionList() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(count = 5) {
            SkeletonTransactionItem()
        }
    }
}

@Composable
fun SkeletonTransactionItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon skeleton
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shimmerBackground(shape = CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Transaction title skeleton
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .shimmerBackground(shape = RoundedCornerShape(4.dp))
                )

                // Transaction category skeleton
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .shimmerBackground(shape = RoundedCornerShape(4.dp))
                )
            }
        }

        // Amount skeleton
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(16.dp)
                .shimmerBackground(shape = RoundedCornerShape(4.dp))
        )
    }
}

// Screen-level skeleton
@Composable
fun DashboardSkeleton() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SkeletonAccountCard()
            SkeletonAccountCard()
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SkeletonTransactionList()
        }
    }
}
```

### Timing Considerations (CRITICAL for Finance)

```kotlin
// Prevent flickering for fast-loading content
@Composable
fun SmartLoadingScreen(
    isLoading: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSkeleton by remember { mutableStateOf(true) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            // Don't show skeleton if content loads in < 150ms
            delay(150)
            showSkeleton = false
        } else {
            showSkeleton = true
        }
    }

    Crossfade(
        targetState = showSkeleton && isLoading,
        label = "LoadingState",
        modifier = modifier
    ) { loading ->
        if (loading) {
            DashboardSkeleton()
        } else {
            content()
        }
    }
}
```

**Best Practices:**
- Match skeleton layout exactly to loaded content (no layout shift)
- Use 1500-2000ms for shimmer animation (hypnotic, not distracting)
- Add minimum display time (150ms) to prevent flickering
- Stack skeletons for progressive loading (balance first, then transactions)

---

## 3. Stagger Animations for List Items

### The Psychology of Stagger

Staggered animations create a sense of progression and flow. Instead of all items appearing at once (robotic), they reveal one by one (natural, organic).

For finance apps: Users scan top→bottom for transactions. Stagger guides their eye naturally.

### Implementation: Stagger with Index Delay

```kotlin
@Composable
fun StaggeredTransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(
            items = transactions,
            key = { _, transaction -> transaction.id }
        ) { index, transaction ->
            val animationDelay = (index * 50).coerceAtMost(300)

            TransactionItemWithStagger(
                transaction = transaction,
                animationDelay = animationDelay
            )
        }
    }
}

@Composable
fun TransactionItemWithStagger(
    transaction: Transaction,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseOutCubic
            )
        ) { -it } + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier
    ) {
        TransactionCard(transaction)
    }
}
```

### Advanced: Stagger with updateTransition

Better for complex animations (scale + rotate + translate):

```kotlin
@Composable
fun StaggeredCategorySpendingList(
    categories: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(
            items = categories,
            key = { _, category -> category.id }
        ) { index, category ->
            val staggerDelay = index * 75 // 75ms between items

            CategorySpendingItemStaggered(
                category = category,
                staggerDelay = staggerDelay
            )
        }
    }
}

@Composable
fun CategorySpendingItemStaggered(
    category: CategorySpending,
    staggerDelay: Int,
    modifier: Modifier = Modifier
) {
    var showItem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(staggerDelay.toLong())
        showItem = true
    }

    val transition = updateTransition(
        targetState = showItem,
        label = "CategoryItemStagger"
    )

    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = { tween(durationMillis = 400) }
    ) { if (it) 1f else 0f }

    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = { tween(durationMillis = 400, easing = EaseOutBack) }
    ) { if (it) 1f else 0.8f }

    val offsetX by transition.animateDp(
        label = "offsetX",
        transitionSpec = { tween(durationMillis = 400, easing = EaseOutCubic) }
    ) { if (it) 0.dp else (-30).dp }

    CategorySpendingCard(
        category = category,
        modifier = modifier
            .offset(x = offsetX)
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
    )
}
```

### Third-Party: AnimatedSequence Library

For production apps, consider the AnimatedSequence library (KMP-compatible):

```kotlin
// From: github.com/pauloaapereira/AnimatedSequence

LazyAnimationHost(
    targetState = isVisible,
    staggerDelayMillis = 100, // 100ms between items
    modifier = Modifier.fillMaxSize()
) {
    LazyColumn {
        items(transactions.size) { index ->
            TransactionItem(transactions[index])
        }
    }
}
```

**Timing Guidelines:**
- **50-100ms between items:** Feels natural, guides eye
- **150ms+ between items:** Feels sluggish
- **Max total stagger:** 300-400ms (don't make users wait)

---

## 4. Performance Considerations

### Key Metrics for Finance Apps

| Metric | Target | Why |
|--------|--------|-----|
| Frame rate | 60fps (Android), 120fps (iOS) | Users expect smoothness |
| Animation duration | 250-400ms | Feels responsive, not slow |
| Time to interactive | < 500ms | Users get to actions quickly |
| Recomposition | Minimal per frame | Battery & thermal issues |

### Performance Best Practices

#### 1. Use `graphicsLayer` for Animations (Not Layout Phase)

```kotlin
// ✅ GOOD: Uses graphicsLayer (draw phase)
val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)
Box(
    modifier = Modifier.graphicsLayer {
        this.alpha = alpha
    }
)

// ❌ BAD: Uses Modifier.alpha (triggers recomposition)
val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)
Box(modifier = Modifier.alpha(alpha))

// ✅ GOOD: Color animation with drawBehind
val animatedColor by animateColorAsState(targetColor)
Box(
    modifier = Modifier.drawBehind {
        drawRect(animatedColor)
    }
)

// ❌ BAD: Using Modifier.background (expensive)
val animatedColor by animateColorAsState(targetColor)
Box(modifier = Modifier.background(animatedColor))
```

#### 2. Avoid Animating Heavy Composables

```kotlin
// ❌ BAD: Animating entire screen composition
AnimatedContent(targetState = state) { currentState ->
    when (currentState) {
        Loading -> LoadingScreen()
        Success -> SuccessScreen()
    }
}

// ✅ GOOD: Animate container, not content
Box(modifier = Modifier.animateContentSize()) {
    when (state) {
        Loading -> LoadingScreen()
        Success -> SuccessScreen()
    }
}
```

#### 3. Use derivedStateOf for Scroll-Based Animations

```kotlin
// ✅ GOOD: Prevents recomposition on every scroll pixel
val listState = rememberLazyListState()
val isScrolling by remember {
    derivedStateOf { listState.isScrollInProgress }
}

// ❌ BAD: Recomposes on every scroll event
val isScrolling = listState.isScrollInProgress
```

#### 4. Hoist State Outside LazyLayouts

```kotlin
// ✅ GOOD: LaunchedEffect triggers once
var animationState by remember { mutableStateOf(false) }

LaunchedEffect(Unit) {
    animationState = true
}

LazyColumn {
    items(items.size) { index ->
        ItemWithAnimation(animationState, index)
    }
}

// ❌ BAD: LaunchedEffect retriggers on scroll
LazyColumn {
    items(items.size) { index ->
        var localState by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { // Retriggers when item scrolls on/off
            localState = true
        }
    }
}
```

#### 5. Profile on Real Devices

```kotlin
// Add timing instrumentation
@Composable
fun DashboardContent(state: DashboardState) {
    val startTime = remember { System.currentTimeMillis() }

    LaunchedEffect(state) {
        val duration = System.currentTimeMillis() - startTime
        Kermit.d(tag = "Performance") { "DashboardContent recomposed in ${duration}ms" }
    }

    // Content...
}
```

### iOS-Specific Performance (Compose Multiplatform 1.7.0+)

**Good news:** Compose Multiplatform 1.7.0 brought major improvements:
- **AnimatedVisibility:** 6% faster
- **LazyGrid scrolling:** 9% faster
- **Visual effects:** 3.6x faster
- **Frame stability:** < 8.33ms (perfect for 120Hz displays)

**Key for iOS:**
```kotlin
// Skia on iOS integrates with Metal (GPU acceleration)
// Use same patterns as Android - Compose handles platform differences

// NO need for platform-specific animation code:
// ❌ Don't do this:
// expect fun platformAnimation()
// actual fun platformAnimation() { /* iOS */ }
// actual fun platformAnimation() { /* Android */ }

// ✅ Just use standard Compose - it works on both:
AnimatedVisibility(visible) {
    TransactionCard()
}
```

---

## 5. Compose Multiplatform Compatibility

### Version Stability Status (Jan 2026)

| Platform | Status | Min Version | Notes |
|----------|--------|-------------|-------|
| Android | Stable | 1.5.0+ | Perfect compatibility |
| iOS | Stable (1.9.3+) | 1.9.3 | Full animation support |
| Desktop | Stable | 1.6.0+ | No performance gaps |
| Web | Alpha | 1.7.0+ | Not recommended for finance apps yet |

### Shared Animation Code: 100% Works

```kotlin
// This composable works identically on Android, iOS, and Desktop
@Composable
fun FinanceAppAnimation() {
    var state by remember { mutableStateOf(LoadingState) }

    // Exact same animation code runs on all platforms
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn() + slideInVertically() togetherWith
            fadeOut() + slideOutVertically()
        }
    ) { currentState ->
        when (currentState) {
            LoadingState -> SkeletonLoading()
            SuccessState -> Content()
            ErrorState -> ErrorMessage()
        }
    }
}
```

### Platform-Specific Handling (When Needed)

```kotlin
@Composable
fun AnimationDurationModifier(): Modifier =
    if (isIOSPlatform()) {
        // iOS can handle faster on high-refresh displays
        Modifier
    } else {
        // Android default
        Modifier
    }

// Usage
val transitionDuration = if (isIOSPlatform()) 250 else 300

AnimatedVisibility(
    visible = isVisible,
    enter = slideInVertically(tween(transitionDuration)),
    exit = slideOutVertically(tween(transitionDuration))
) {
    TransactionCard()
}
```

### Testing Animations on Both Platforms

```kotlin
// Run same test on both platforms
@OptIn(ExperimentalTestApi::class)
class AnimationTest {
    @Test
    fun transitionAnimationCompletes() = runTest {
        var state by mutableStateOf(LoadingState)

        composeRule.setContent {
            AnimatedContent(state) { current ->
                when (current) {
                    LoadingState -> Text("Loading")
                    SuccessState -> Text("Success")
                }
            }
        }

        advanceTimeBy(400) // Wait for animation
        state = SuccessState

        composeRule.waitUntil {
            composeRule.onNodeWithText("Success").exists()
        }
    }
}
```

### Migration Path (If Upgrading Compose Multiplatform)

```gradle
// Current (stable, tested)
kotlin {
    jvm()
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
}

dependencies {
    implementation("org.jetbrains.compose.material3:material3:1.9.3")
}

// To: Latest with all features
// No animation changes needed - they'll just run faster!
```

---

## 6. Complete Finance App Example

### Combining All Patterns

```kotlin
// ============ DOMAIN ============
sealed class TransactionListState {
    object Loading : TransactionListState()
    data class Success(val transactions: List<Transaction>) : TransactionListState()
    data class Error(val message: String) : TransactionListState()
    object Empty : TransactionListState()
}

data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val timestamp: Long
)

// ============ VIEWMODEL ============
class TransactionListViewModel : ViewModel() {
    private val _state = MutableStateFlow<TransactionListState>(TransactionListState.Loading)
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            try {
                delay(500) // Simulate network
                _state.value = TransactionListState.Success(
                    listOf(
                        Transaction("1", "Grocery", "Food", 52.50, System.currentTimeMillis()),
                        Transaction("2", "Gas", "Transport", 45.00, System.currentTimeMillis() - 3600000),
                        // ...
                    )
                )
            } catch (e: Exception) {
                _state.value = TransactionListState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun retry() {
        _state.value = TransactionListState.Loading
        loadTransactions()
    }
}

// ============ UI ============
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            when {
                initialState is TransactionListState.Loading -> {
                    fadeIn(tween(300)) + slideInVertically(tween(300)) togetherWith
                    fadeOut(tween(150)) + slideOutVertically()
                }
                targetState is TransactionListState.Error -> {
                    slideInHorizontally(tween(400)) { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
                }
                else -> {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "TransactionListState"
    ) { currentState ->
        when (currentState) {
            TransactionListState.Loading -> {
                SkeletonTransactionList()
            }
            is TransactionListState.Success -> {
                StaggeredTransactionList(
                    transactions = currentState.transactions
                )
            }
            is TransactionListState.Error -> {
                ErrorScreen(
                    message = currentState.message,
                    onRetry = { viewModel.retry() }
                )
            }
            TransactionListState.Empty -> {
                EmptyStatePrompt()
            }
        }
    }
}

@Composable
fun StaggeredTransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(
            items = transactions,
            key = { _, tx -> tx.id }
        ) { index, transaction ->
            TransactionItemWithStagger(
                transaction = transaction,
                animationDelay = (index * 50).coerceAtMost(300)
            )
        }
    }
}

@Composable
fun TransactionItemWithStagger(
    transaction: Transaction,
    animationDelay: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(tween(300)) { -it } + fadeIn(),
        exit = slideOutHorizontally() + fadeOut(),
        modifier = modifier
    ) {
        TransactionCard(transaction)
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.title, fontWeight = FontWeight.Bold)
                Text(text = transaction.category, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = "$${transaction.amount}",
                fontWeight = FontWeight.Bold,
                color = if (transaction.amount > 0) Color.Red else Color.Green
            )
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Red
        )
        Text(text = message, modifier = Modifier.padding(top = 16.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Retry")
        }
    }
}
```

---

## Summary & Recommendations for Finuts

### For Dashboard (Finuts)

1. **State transitions:** Use `AnimatedContent` with contextual animations
2. **Skeleton loading:** Implement for accounts and recent transactions
3. **List animations:** Stagger transaction items (50ms delay)
4. **Target duration:** 250-350ms for all animations
5. **Performance:** Profile on iPhone 14/15 and Samsung Galaxy S24

### Implementation Checklist

- [ ] Define `UiState` sealed class for each screen
- [ ] Create skeleton versions of all cards/lists
- [ ] Implement shimmer effect (reusable `shimmerBackground` modifier)
- [ ] Test `AnimatedContent` transitions (Loading → Success → Error)
- [ ] Stagger LazyColumn items (50-100ms between items)
- [ ] Profile animation performance (target < 60fps drops)
- [ ] Test on iOS with Compose Multiplatform 1.9.3+
- [ ] Verify no layout shifts during transitions

### Libraries to Add (If Not Already Present)

```gradle
dependencies {
    // Already in CLAUDE.md, verified for 2.3.0:
    implementation("androidx.compose.foundation:foundation:${ComposeVersion}")
    implementation("androidx.compose.animation:animation:${ComposeVersion}")

    // For advanced animations (optional, KMP-compatible):
    // implementation("com.pauloaapereira:animated-sequence:1.0.0")
}
```

---

## Sources

- [Compose Multiplatform 1.7.0 Released](https://blog.jetbrains.com/kotlin/2024/10/compose-multiplatform-1-7-0-released/)
- [Quick Guide to Animations in Jetpack Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide)
- [Loading Shimmer in Compose - Touchlab](https://touchlab.co/loading-shimmer-in-compose)
- [Banking App UI Best Practices - ProCreator](https://procreator.design/blog/banking-app-ui-top-best-practices/)
- [The Best UX Design Practices for Finance Apps - G & Co.](https://www.g-co.agency/insights/the-best-ux-design-practices-for-finance-apps)
- [Animating LazyList Items - Gergely Kőrössy](https://medium.com/@gregkorossy/animating-lazylist-items-in-jetpack-compose-6b40f94aaa1a)
- [Compose Multiplatform iOS Performance - KotlinConf 2024](https://kotlinconf.com/2024/talks/578918/)
- [Android Best Practices - Follow Best Practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
