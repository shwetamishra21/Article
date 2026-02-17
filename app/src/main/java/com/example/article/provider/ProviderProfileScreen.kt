package com.example.article.provider

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.article.Repository.ProfileViewModel
import com.example.article.Repository.ProfileUiState
import com.example.article.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// Trade-specific skills registry
// Each service type maps to its own relevant skill specializations
// ─────────────────────────────────────────────────────────────────────────────
private val SKILLS_BY_SERVICE_TYPE: Map<String, List<String>> = mapOf(
    "Plumber" to listOf(
        "Pipe Fitting & Installation",
        "Leak Detection & Repair",
        "Drain Cleaning & Unblocking",
        "Water Heater Installation",
        "Bathroom Fitting & Fixtures",
        "Kitchen Plumbing",
        "Sewer Line Repair",
        "Water Tank Installation",
        "Gas Line Work",
        "Emergency Plumbing"
    ),
    "Electrician" to listOf(
        "Wiring & Rewiring",
        "Panel / DB Installation & Repair",
        "Switchboard & Socket Repair",
        "Ceiling Fan Installation",
        "AC Electrical Wiring",
        "CCTV & Security Wiring",
        "Inverter & UPS Setup",
        "LED Fixture Installation",
        "Earth Leakage Detection",
        "Industrial Wiring"
    ),
    "Carpenter" to listOf(
        "Custom Furniture Making",
        "Door & Window Fitting",
        "Wardrobe Installation",
        "Modular Kitchen Work",
        "Wood Polishing & Finishing",
        "False Ceiling (Wood)",
        "Cabinet Repair",
        "Flooring Installation",
        "Partition Walls",
        "Staircase Fabrication"
    ),
    "Cleaner" to listOf(
        "Deep Home Cleaning",
        "Kitchen & Bathroom Cleaning",
        "Carpet & Sofa Cleaning",
        "Post-Construction Cleanup",
        "Water Tank Cleaning",
        "Window & Glass Cleaning",
        "Move-In / Move-Out Cleaning",
        "Sewage & Drain Cleaning",
        "Office Cleaning",
        "Industrial Cleaning"
    ),
    "Painter" to listOf(
        "Interior Wall Painting",
        "Exterior Painting",
        "Texture & Design Painting",
        "Wood Staining & Varnishing",
        "Waterproofing Coating",
        "Metal Surface Painting",
        "Wallpaper Installation",
        "Epoxy Floor Coating",
        "Anti-Fungal Treatment",
        "Commercial Painting"
    ),
    "Gardener" to listOf(
        "Lawn Mowing & Edging",
        "Plant Pruning & Trimming",
        "Irrigation System Setup",
        "Landscape Design",
        "Tree Planting & Care",
        "Pest & Disease Control",
        "Soil Preparation & Fertilization",
        "Garden Cleanup",
        "Rooftop / Terrace Garden",
        "Seasonal Planting"
    ),
    "AC Repair" to listOf(
        "AC Installation",
        "Gas / Refrigerant Refilling",
        "Deep Cleaning Service",
        "Compressor Repair",
        "PCB & Circuit Board Repair",
        "Inverter AC Servicing",
        "Duct Cleaning",
        "Central AC Maintenance",
        "Split AC Repair",
        "Window AC Service"
    ),
    "Appliance Repair" to listOf(
        "Refrigerator Repair",
        "Washing Machine Repair",
        "Microwave / OTG Repair",
        "TV & Display Repair",
        "Dishwasher Repair",
        "Water Purifier Service",
        "Geyser / Water Heater Repair",
        "Mixer Grinder Repair",
        "Induction Cooktop Repair",
        "Chimney Cleaning & Repair"
    ),
    "Pest Control" to listOf(
        "Cockroach Treatment",
        "Termite Control",
        "Bed Bug Treatment",
        "Mosquito Control",
        "Rat & Rodent Control",
        "Ant Treatment",
        "Lizard Control",
        "Spider Treatment",
        "Pre-Construction Anti-Termite",
        "Annual Maintenance Contract"
    ),
    "Locksmith" to listOf(
        "Lock Installation & Repair",
        "Key Duplication",
        "Car Key Programming",
        "Safe Opening & Repair",
        "Door Lock Replacement",
        "Digital / Smart Lock Setup",
        "Deadbolt Installation",
        "Padlock Repair",
        "Emergency Lockout Service",
        "Master Key Systems"
    ),
    "Handyman" to listOf(
        "General Home Repairs",
        "Furniture Assembly",
        "TV Wall Mounting",
        "Curtain & Blind Fitting",
        "Door & Window Repair",
        "Tile & Grout Repair",
        "Caulking & Sealing",
        "Minor Painting Touch-ups",
        "Shelf & Rack Installation",
        "Light Fixture Replacement"
    ),
    "Mason" to listOf(
        "Brick & Block Laying",
        "Plastering & Rendering",
        "Tile Fixing",
        "Stone & Paving Work",
        "Concrete Work",
        "Waterproofing",
        "Terrace & Roof Repair",
        "Crack Filling & Repair",
        "Wall Construction",
        "Demolition Work"
    ),
    "Welder" to listOf(
        "MIG Welding",
        "TIG Welding",
        "Arc Welding",
        "Gate & Grill Fabrication",
        "Stainless Steel Work",
        "Aluminum Fabrication",
        "Structural Steel Work",
        "Pipe Welding",
        "On-Site Welding Repairs",
        "Custom Metal Fabrication"
    ),
    "Tailor" to listOf(
        "Dress & Suit Stitching",
        "Alteration & Repairs",
        "Blouse & Kurti Stitching",
        "Men's Shirts & Pants",
        "Curtain Stitching",
        "Embroidery Work",
        "Bridal Wear",
        "Kids Clothing",
        "Uniform Stitching",
        "Leather & Bag Repair"
    ),
    "Beautician" to listOf(
        "Haircut & Styling",
        "Facial & Skin Care",
        "Waxing & Threading",
        "Bridal Makeup",
        "Nail Art & Extensions",
        "Hair Coloring & Highlights",
        "Keratin & Smoothening",
        "Pedicure & Manicure",
        "Mehendi / Henna",
        "Eyebrow Shaping & Tinting"
    ),
    "Tutor" to listOf(
        "Mathematics",
        "Science (Physics / Chemistry / Biology)",
        "English Language & Literature",
        "Competitive Exam Preparation",
        "Programming & Coding",
        "Music Lessons",
        "Art & Drawing",
        "Foreign Languages",
        "Accounts & Commerce",
        "Primary School Support"
    ),
    "Chef/Cook" to listOf(
        "North Indian Cuisine",
        "South Indian Cuisine",
        "Continental Cuisine",
        "Chinese Cuisine",
        "Baking & Desserts",
        "Party & Event Catering",
        "Diet / Healthy Meal Prep",
        "Tiffin / Dabba Service",
        "Cake Decoration",
        "Jain / Vegan Cooking"
    ),
    "Driver" to listOf(
        "Daily Office Commute",
        "Airport Transfer",
        "Outstation / Long Trips",
        "School Pick-up & Drop",
        "Night Duty Available",
        "Heavy Vehicle Driving",
        "Luxury Car Handling",
        "City Tour & Sightseeing",
        "Patient / Medical Transport",
        "24/7 On-Call Availability"
    ),
    "Security Guard" to listOf(
        "Residential Security",
        "Commercial / Office Security",
        "Event Security",
        "Night Patrol",
        "CCTV Monitoring",
        "Access Control Management",
        "Armed Security",
        "Unarmed Security",
        "Building & Gate Security",
        "Gated Community Security"
    ),
    "Moving & Packing" to listOf(
        "Home Shifting",
        "Office Relocation",
        "Furniture Disassembly & Moving",
        "Safe Packing & Unpacking",
        "Vehicle / Bike Transport",
        "Piano & Heavy Item Moving",
        "Same-Day Moving",
        "Long Distance Shifting",
        "Storage Solutions",
        "Fragile Items Handling"
    ),
    "Interior Designer" to listOf(
        "Home Interior Design",
        "Office & Commercial Interior",
        "3D Visualization & Renders",
        "Modular Kitchen Design",
        "False Ceiling & Lighting Design",
        "Furniture Selection & Sourcing",
        "Color & Material Consultation",
        "Space Planning",
        "Renovation Supervision",
        "Smart Home Integration"
    ),
    "Solar Panel Installer" to listOf(
        "Rooftop Solar Installation",
        "Off-Grid Solar Systems",
        "On-Grid / Grid-Tied Systems",
        "Solar Water Heater Setup",
        "Battery Storage Setup",
        "System Maintenance & Repair",
        "Energy Audit",
        "Government Subsidy Assistance",
        "Commercial Solar Projects",
        "EV Charging Point Installation"
    ),
    "Water Tank Cleaner" to listOf(
        "Underground Sump Cleaning",
        "Overhead Tank Cleaning",
        "Anti-Bacterial Treatment",
        "Bio-Enzyme Treatment",
        "Pipe Flushing",
        "Silt & Sediment Removal",
        "Tank Inspection & Report",
        "Water Quality Testing",
        "Moss & Algae Removal",
        "Annual Maintenance Contract"
    ),
    "Car Wash" to listOf(
        "Exterior Foam Wash",
        "Full Interior Detailing",
        "Engine Bay Cleaning",
        "Ceramic Coating",
        "Paint Protection Film",
        "Seat Shampooing",
        "Dashboard & Console Polish",
        "Headlight Restoration",
        "Doorstep Service",
        "Bike Wash & Detailing"
    ),
    "Other" to listOf(
        "General Home Services",
        "Specialized Skill (On Request)",
        "Residential Work",
        "Commercial Work",
        "Emergency Services",
        "Installation",
        "Repair & Maintenance",
        "24/7 Available",
        "5+ Years Experience",
        "Background Checked"
    )
)

/** Returns the skill list for the given service type, with a sensible fallback. */
fun getSkillsForServiceType(serviceType: String): List<String> =
    SKILLS_BY_SERVICE_TYPE[serviceType]
        ?: listOf(
            "Residential Work", "Commercial Work", "Emergency Services",
            "Installation", "Repair & Maintenance", "24/7 Available",
            "5+ Years Experience", "Background Checked", "Licensed & Insured"
        )

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editServiceType by remember { mutableStateOf("Plumber") }
    var editSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAvailable by remember { mutableStateOf(true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var expandedServiceType by remember { mutableStateOf(false) }
    var showSkillsDialog by remember { mutableStateOf(false) }

    // Provider stats
    var completedJobs by remember { mutableStateOf(0) }
    var activeRequests by remember { mutableStateOf(0) }
    var avgRating by remember { mutableStateOf(0.0f) }
    var ratingCount by remember { mutableStateOf(0) }

    val serviceTypes = SKILLS_BY_SERVICE_TYPE.keys.toList()

    // Dynamically computed available skills based on the currently selected service type
    val availableSkills by remember(editServiceType) {
        derivedStateOf { getSkillsForServiceType(editServiceType) }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadProfileImage(it, context) {
                selectedImageUri = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // Load provider stats & Firestore extras whenever profile loads successfully
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            scope.launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                    // Load service type, availability, and rating from user document
                    val userDoc = firestore.collection("users")
                        .document(userId)
                        .get()
                        .await()

                    editServiceType = userDoc.getString("serviceType") ?: "Plumber"
                    isAvailable = userDoc.getBoolean("isAvailable") ?: true
                    avgRating = (userDoc.getDouble("averageRating") ?: 0.0).toFloat()
                    ratingCount = (userDoc.getLong("ratingCount") ?: 0L).toInt()

                    // Completed jobs count
                    val completedSnapshot = firestore.collection("service_requests")
                        .whereEqualTo("providerId", userId)
                        .whereEqualTo("status", "completed")
                        .get()
                        .await()
                    completedJobs = completedSnapshot.size()

                    // Active requests count (accepted + in_progress)
                    val activeSnapshot = firestore.collection("service_requests")
                        .whereEqualTo("providerId", userId)
                        .whereIn("status", listOf("accepted", "in_progress"))
                        .get()
                        .await()
                    activeRequests = activeSnapshot.size()

                } catch (e: Exception) {
                    // Silently fail — stats are non-critical
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = BlueOnPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BluePrimary, BlueSecondary)
                        )
                    )
                    .shadow(
                        elevation = 6.dp,
                        spotColor = BluePrimary.copy(alpha = 0.4f)
                    )
            )
        }
    ) { padding ->
        when (val state = uiState) {

            ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(BackgroundLight),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceLight,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFFD32F2F)
                            )
                            Text(
                                "Error loading profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                state.message,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadProfile() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BluePrimary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            is ProfileUiState.Success -> {
                val profile = state.profile

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(BackgroundLight)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp)
                ) {

                    // ── Profile Header Card ─────────────────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceLight,
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {

                            // ── Profile Image ───────────────────────────────
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(
                                        width = 4.dp,
                                        brush = Brush.linearGradient(
                                            listOf(BluePrimary, BlueSecondary)
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePicker.launch("image/*") }
                                    .background(Color(0xFFF0F0F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile.photoUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(profile.photoUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(52.dp),
                                        tint = Color(0xFF999999)
                                    )
                                }

                                // Upload progress overlay
                                if (isUpdating) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Camera badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp),
                                    shape = CircleShape,
                                    color = BluePrimary,
                                    shadowElevation = 4.dp
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Change photo",
                                        modifier = Modifier.padding(6.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            // ── Edit Mode Fields ────────────────────────────
                            if (isEditing) {

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Name", fontSize = 14.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )

                                // Service type dropdown
                                ExposedDropdownMenuBox(
                                    expanded = expandedServiceType,
                                    onExpandedChange = { expandedServiceType = it }
                                ) {
                                    OutlinedTextField(
                                        value = editServiceType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Service Type", fontSize = 14.sp) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expandedServiceType
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BluePrimary,
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedServiceType,
                                        onDismissRequest = { expandedServiceType = false },
                                        modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        serviceTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type, fontSize = 13.sp) },
                                                onClick = {
                                                    // Clear skills when trade changes — old ones are irrelevant
                                                    if (type != editServiceType) {
                                                        editSkills = emptyList()
                                                    }
                                                    editServiceType = type
                                                    expandedServiceType = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // ── Skills Selector (contextual to trade) ───
                                OutlinedCard(
                                    onClick = { showSkillsDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFE0E0E0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Skills for $editServiceType",
                                                fontSize = 12.sp,
                                                color = Color(0xFF666666)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = when {
                                                    editSkills.isEmpty() ->
                                                        "Tap to select your specializations..."
                                                    editSkills.size <= 2 ->
                                                        editSkills.joinToString(", ")
                                                    else ->
                                                        "${editSkills.take(2).joinToString(", ")} +${editSkills.size - 2} more"
                                                },
                                                fontSize = 14.sp,
                                                color = if (editSkills.isEmpty()) Color(0xFF999999)
                                                else Color(0xFF1a1a1a)
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF666666)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = editBio,
                                    onValueChange = { editBio = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Bio", fontSize = 14.sp) },
                                    placeholder = {
                                        Text("Tell clients about your expertise and experience...")
                                    },
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )

                            } else {
                                // ── View Mode ───────────────────────────────

                                Text(
                                    text = profile.name.ifEmpty { "Set your name" },
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceLight
                                )

                                // Service type badge
                                Surface(
                                    color = BluePrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 18.dp,
                                            vertical = 10.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = BluePrimary
                                        )
                                        Text(
                                            text = editServiceType,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                }

                                // Star rating display (shown when provider has been rated)
                                if (avgRating > 0f) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(5) { i ->
                                            Icon(
                                                imageVector = if (i < avgRating.toInt())
                                                    Icons.Default.Star
                                                else
                                                    Icons.Default.StarBorder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (i < avgRating.toInt())
                                                    Color(0xFFFFC107)
                                                else
                                                    Color(0xFFCCCCCC)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${"%.1f".format(avgRating)} ($ratingCount reviews)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                }

                                // Skills chips (displayed in rows of 2, matching original layout)
                                if (profile.skills.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        profile.skills.chunked(2).forEach { rowSkills ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(
                                                    8.dp,
                                                    Alignment.CenterHorizontally
                                                )
                                            ) {
                                                rowSkills.forEach { skill ->
                                                    Surface(
                                                        color = BlueSecondary.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 6.dp
                                                            ),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = BlueSecondary
                                                            )
                                                            Text(
                                                                text = skill,
                                                                fontSize = 12.sp,
                                                                color = BlueSecondary,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = profile.bio.ifEmpty {
                                        "Add a bio to tell clients about your expertise"
                                    },
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }

                            // ── Edit / Save Buttons ─────────────────────────
                            if (isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditing = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isUpdating
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateProviderProfile(
                                                editName,
                                                editBio,
                                                editServiceType,
                                                editSkills,
                                                isAvailable
                                            ) {
                                                isEditing = false
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isUpdating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BluePrimary
                                        )
                                    ) {
                                        if (isUpdating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.5.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Save", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        editName = profile.name
                                        editBio = profile.bio
                                        editSkills = profile.skills
                                        isEditing = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BluePrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Edit Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Availability Toggle ─────────────────────────────────
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = BluePrimary.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceLight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAvailable)
                                        Icons.Default.CheckCircle
                                    else
                                        Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Availability Status",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceLight
                                    )
                                    Text(
                                        text = if (isAvailable)
                                            "Available for requests"
                                        else
                                            "Not accepting requests",
                                        fontSize = 13.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }

                            Switch(
                                checked = isAvailable,
                                onCheckedChange = { newValue ->
                                    isAvailable = newValue
                                    // Persist immediately without requiring a full profile save
                                    scope.launch {
                                        try {
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                                                ?: return@launch
                                            FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(uid)
                                                .update("isAvailable", newValue)
                                                .await()
                                        } catch (_: Exception) {}
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = BluePrimary,
                                    checkedThumbColor = BlueOnPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Stats Cards ─────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Completed",
                            value = completedJobs.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Active",
                            value = activeRequests.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Rating",
                            value = if (avgRating > 0f) "%.1f".format(avgRating) else "—",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ── Skills Selection Dialog ─────────────────────────────────────────────
    // Skills list is always contextual to the currently selected service type
    if (showSkillsDialog) {
        AlertDialog(
            onDismissRequest = { showSkillsDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Select Skills", fontWeight = FontWeight.Bold)
                    Text(
                        "Specializations for $editServiceType",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSkills.forEach { skill ->
                        val isSelected = skill in editSkills
                        Surface(
                            onClick = {
                                editSkills = if (isSelected) {
                                    editSkills - skill
                                } else {
                                    editSkills + skill
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BluePrimary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BluePrimary else Color(0xFFE0E0E0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = skill,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold
                                    else FontWeight.Normal,
                                    color = if (isSelected) BluePrimary else Color(0xFF1a1a1a),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSkillsDialog = false }) {
                    Text(
                        text = if (editSkills.isEmpty()) "Done"
                        else "Done  (${editSkills.size} selected)",
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Logout Confirmation Dialog ──────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(
                        "Logout",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatCard — reusable stat display component (kept at module level for reuse)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            spotColor = BluePrimary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BluePrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
        }
    }
}